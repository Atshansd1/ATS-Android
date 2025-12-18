package com.ats.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ats.android.R
import com.ats.android.models.AttendanceCenter
import com.ats.android.models.MovementType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.*
import java.util.Date
import kotlin.math.*

/**
 * Foreground service that tracks employee location while checked in.
 * 
 * Features matching iOS parity:
 * - Updates every 2 minutes
 * - Matches location to Attendance Centers
 * - Detects Movements (Significant, Stationary, Left Area)
 * - Updates Firestore ActiveLocation
 */
class LocationTrackingService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var locationService: LocationService
    private lateinit var geocodingService: GeocodingService
    private lateinit var firestoreService: FirestoreService
    
    private var employeeId: String? = null
    private var employeeName: String? = null
    private var locationUpdateJob: Job? = null
    
    // Tracking State
    private var attendanceCenters: List<AttendanceCenter> = emptyList()
    private var checkInLocation: Location? = null // To track distance from check-in
    private var lastRecordedLocation: Location? = null // To track significant moves
    private var stationaryStartLocation: Location? = null // To track stationary stays
    private var stationaryStartTime: Long = 0L
    private var hasRecordedStationary: Boolean = false
    private var hasLeftCheckInArea: Boolean = false
    
    companion object {
        private const val TAG = "LocationTrackingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking"
        private const val UPDATE_INTERVAL_MS = 120000L // 2 minutes
        
        // Thresholds matching iOS
        private const val SIGNIFICANT_MOVE_THRESHOLD_M = 1000f // 1km
        private const val CHECKIN_AREA_THRESHOLD_M = 1000f // 1km
        private const val STATIONARY_RADIUS_M = 50f // 50 meters
        private const val STATIONARY_TIME_MS = 15 * 60 * 1000L // 15 minutes
        
        const val ACTION_START_TRACKING = "START_TRACKING"
        const val ACTION_STOP_TRACKING = "STOP_TRACKING"
        const val EXTRA_EMPLOYEE_ID = "EMPLOYEE_ID"
        const val EXTRA_EMPLOYEE_NAME = "EMPLOYEE_NAME"
        
        fun startTracking(context: Context, employeeId: String, employeeName: String) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_EMPLOYEE_ID, employeeId)
                putExtra(EXTRA_EMPLOYEE_NAME, employeeName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        locationService = LocationService(applicationContext)
        geocodingService = GeocodingService(applicationContext)
        firestoreService = FirestoreService.getInstance()
        Log.d(TAG, "📍 Location tracking service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                employeeId = intent.getStringExtra(EXTRA_EMPLOYEE_ID)
                employeeName = intent.getStringExtra(EXTRA_EMPLOYEE_NAME)
                
                if (employeeId != null && employeeName != null) {
                    startForeground(NOTIFICATION_ID, createNotification(employeeName!!))
                    initializeTrackingState()
                    startLocationTracking()
                    Log.d(TAG, "✅ Started tracking for: $employeeName ($employeeId)")
                } else {
                    Log.e(TAG, "❌ Missing employee info, cannot start tracking")
                    stopSelf()
                }
            }
            ACTION_STOP_TRACKING -> {
                Log.d(TAG, "🛑 Stopping location tracking")
                stopLocationTracking()
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    private fun initializeTrackingState() {
        serviceScope.launch {
            try {
                // 1. Load Attendance Centers
                attendanceCenters = firestoreService.getAllAttendanceCenters()
                Log.d(TAG, "📥 Loaded ${attendanceCenters.size} attendance centers for matching")
                
                // 2. Load Active Check-In info
                employeeId?.let { uid ->
                    val checkInRecord = firestoreService.getActiveCheckIn(uid)
                    checkInRecord?.let { record ->
                        checkInLocation = Location("").apply {
                            latitude = record.checkInLocation?.latitude ?: 0.0
                            longitude = record.checkInLocation?.longitude ?: 0.0
                        }
                        Log.d(TAG, "📍 Initialized check-in location: ${checkInLocation?.latitude}, ${checkInLocation?.longitude}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize tracking state: ${e.message}")
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private var checkInListener: com.google.firebase.firestore.ListenerRegistration? = null
    
    private fun startLocationTracking() {
        locationUpdateJob?.cancel()
        
        // Listen for remote checkouts (Force Checkout)
        checkInListener?.remove()
        employeeId?.let { id ->
            checkInListener = firestoreService.listenToActiveCheckIn(id) { record ->
                if (record == null) {
                    Log.w(TAG, "⚠️ Remote checkout detected! Stopping service.")
                    showForceCheckoutNotification()
                    stopSelf()
                }
            }
        }
        
        locationUpdateJob = serviceScope.launch {
            while (isActive) {
                try {
                    updateLocation()
                    delay(UPDATE_INTERVAL_MS) // Wait 2 minutes before next update
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Location update error: ${e.message}", e)
                    delay(30000) // Wait 30 seconds on error
                }
            }
        }
    }
    
    private data class PlaceNames(val display: String, val nameEn: String?, val nameAr: String?)
    
    private suspend fun updateLocation() {
        try {
            val empId = employeeId ?: return
            
            Log.d(TAG, "📍 Updating location...")
            
            // 1. Get current location
            val location = locationService.getCurrentLocation()
            if (location == null) {
                Log.w(TAG, "⚠️ Could not get valid location")
                return
            }
            
            // 2. Determine Place Name (Centers OR Geocoding)
            val placeNames = resolvePlaceName(location)
            
            // 3. Update Firestore Active Location
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            firestoreService.updateEmployeeLocation(
                employeeId = empId, 
                location = geoPoint, 
                placeName = placeNames.display,
                placeNameEn = placeNames.nameEn,
                placeNameAr = placeNames.nameAr
            )
            
            // 4. Process Movement Logic
            processMovements(location, placeNames.display)
            
            // 5. Update Notification
            updateNotification(placeNames.display)
            
            // Update tracking references
            stationaryStartLocation = stationaryStartLocation ?: location
            if (stationaryStartTime == 0L) stationaryStartTime = System.currentTimeMillis()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update location error: ${e.message}", e)
        }
    }
    
    private suspend fun resolvePlaceName(location: Location): PlaceNames {
        // A. Check Attendance Centers
        val matchedCenter = attendanceCenters.find { center ->
            val centerLoc = Location("").apply {
                latitude = center.coordinate.latitude
                longitude = center.coordinate.longitude
            }
            location.distanceTo(centerLoc) <= center.radiusMeters
        }
        
        if (matchedCenter != null) {
            Log.d(TAG, "✅ Matched attendance center: ${matchedCenter.name}")
            // Prefer English name if available, or just name
            val nameEn = if (!matchedCenter.nameEn.isNullOrEmpty()) matchedCenter.nameEn else matchedCenter.name
            val nameAr = if (!matchedCenter.nameAr.isNullOrEmpty()) matchedCenter.nameAr else matchedCenter.name
            
            // Determine display name based on current device locale
            val isArabic = java.util.Locale.getDefault().language == "ar"
            val displayName = if (isArabic) nameAr else nameEn
            
            return PlaceNames(displayName, nameEn, nameAr)
        }
        
        // B. Fallback to Geocoding
        return try {
            val nameEn = withTimeoutOrNull(5000L) {
                geocodingService.getPlaceName(location.latitude, location.longitude, java.util.Locale.US)
            }
            
            val nameAr = withTimeoutOrNull(5000L) {
                geocodingService.getPlaceName(location.latitude, location.longitude, java.util.Locale("ar"))
            }
            
            val coordinates = "${String.format(java.util.Locale.US, "%.4f", location.latitude)}, ${String.format(java.util.Locale.US, "%.4f", location.longitude)}"
            val defaultNameEn = nameEn ?: coordinates
            val defaultNameAr = nameAr ?: coordinates
            
            val isArabic = java.util.Locale.getDefault().language == "ar"
            val displayName = if (isArabic) defaultNameAr else defaultNameEn
            
            PlaceNames(displayName, defaultNameEn, defaultNameAr)
        } catch (e: Exception) {
            val coordinates = "${String.format(java.util.Locale.US, "%.4f", location.latitude)}, ${String.format(java.util.Locale.US, "%.4f", location.longitude)}"
            PlaceNames(coordinates, coordinates, coordinates)
        }
    }
    
    private suspend fun processMovements(currentLocation: Location, currentPlace: String) {
        val empId = employeeId ?: return
        val now = Timestamp.now()
        
        // --- Logic 1: Stationary Detection ---
        val distFromStationaryStart = stationaryStartLocation?.distanceTo(currentLocation) ?: 0f
        
        if (distFromStationaryStart < STATIONARY_RADIUS_M) {
            // User is stationary
            val timeStationary = System.currentTimeMillis() - stationaryStartTime
            
            if (timeStationary >= STATIONARY_TIME_MS && !hasRecordedStationary) {
                // Record Stationary Event
                Log.d(TAG, "🛑 User stationary for > 15 mins. Recording event.")
                firestoreService.recordMovement(
                    employeeId = empId,
                    fromLat = stationaryStartLocation!!.latitude,
                    fromLng = stationaryStartLocation!!.longitude,
                    fromPlace = currentPlace, // Approximate
                    toLat = currentLocation.latitude,
                    toLng = currentLocation.longitude,
                    toPlace = currentPlace,
                    distance = (distFromStationaryStart / 1000).toDouble(),
                    startTime = Timestamp(Date(stationaryStartTime)),
                    endTime = now,
                    movementType = MovementType.STATIONARY_STAY
                )
                hasRecordedStationary = true
            }
        } else {
            // User moved beyond radius -> Reset stationary tracker
            stationaryStartLocation = currentLocation
            stationaryStartTime = System.currentTimeMillis()
            hasRecordedStationary = false
        }
        
        // --- Logic 2: Significant Movement ---
        if (lastRecordedLocation != null) {
            val distFromLast = lastRecordedLocation!!.distanceTo(currentLocation)
            if (distFromLast >= SIGNIFICANT_MOVE_THRESHOLD_M) {
                Log.d(TAG, "➡️ Significant movement detected: ${distFromLast}m")
                
                // Fetch previous place name if possible (omitted for simplicity, used "Unknown")
                // In a perfect world we'd track previous place name too.
                
                firestoreService.recordMovement(
                    employeeId = empId,
                    fromLat = lastRecordedLocation!!.latitude,
                    fromLng = lastRecordedLocation!!.longitude,
                    fromPlace = null, // simplified
                    toLat = currentLocation.latitude,
                    toLng = currentLocation.longitude,
                    toPlace = currentPlace,
                    distance = (distFromLast / 1000).toDouble(),
                    startTime = Timestamp(Date(System.currentTimeMillis() - UPDATE_INTERVAL_MS)), // Approx
                    endTime = now,
                    movementType = MovementType.SIGNIFICANT_MOVE
                )
                lastRecordedLocation = currentLocation
            }
        } else {
            lastRecordedLocation = currentLocation
        }
        
        // --- Logic 3: Check-in Area Departure ---
        if (checkInLocation != null && !hasLeftCheckInArea) {
            val distFromCheckIn = checkInLocation!!.distanceTo(currentLocation)
            if (distFromCheckIn >= CHECKIN_AREA_THRESHOLD_M) {
                Log.d(TAG, "⚠️ User left check-in area: ${distFromCheckIn}m")
                
                firestoreService.recordMovement(
                    employeeId = empId,
                    fromLat = checkInLocation!!.latitude,
                    fromLng = checkInLocation!!.longitude,
                    fromPlace = "Check-in Point",
                    toLat = currentLocation.latitude,
                    toLng = currentLocation.longitude,
                    toPlace = currentPlace,
                    distance = (distFromCheckIn / 1000).toDouble(),
                    startTime = Timestamp(Date(System.currentTimeMillis())), 
                    endTime = now,
                    movementType = MovementType.LEFT_CHECKIN_AREA
                )
                hasLeftCheckInArea = true
            }
        }
    }
    
    private fun stopLocationTracking() {
        locationUpdateJob?.cancel()
        locationUpdateJob = null
        checkInListener?.remove()
        checkInListener = null
    }
    
    private fun updateNotification(placeName: String) {
        val notification = createNotification(employeeName!!, placeName)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotification(employeeName: String, location: String? = null): Notification {
        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your location while checked in"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create intent to open app when notification is tapped
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val contentText = if (location != null) {
            "Last updated: $location"
        } else {
            "Tracking your location..."
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$employeeName - Checked In")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun showForceCheckoutNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_force_checkout_title))
            .setContentText(getString(R.string.notification_force_checkout_body))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
        serviceScope.cancel()
        Log.d(TAG, "📍 Location tracking service destroyed")
    }
}
