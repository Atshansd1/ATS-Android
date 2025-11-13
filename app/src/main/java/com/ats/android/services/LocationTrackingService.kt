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
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

/**
 * Foreground service that tracks employee location while checked in
 * Updates Firestore activeLocations collection every 2 minutes
 */
class LocationTrackingService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var locationService: LocationService
    private lateinit var geocodingService: GeocodingService
    private lateinit var firestoreService: FirestoreService
    
    private var employeeId: String? = null
    private var employeeName: String? = null
    private var locationUpdateJob: Job? = null
    
    companion object {
        private const val TAG = "LocationTrackingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking"
        private const val UPDATE_INTERVAL_MS = 120000L // 2 minutes
        
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
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startLocationTracking() {
        locationUpdateJob?.cancel()
        
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
    
    private suspend fun updateLocation() {
        try {
            val empId = employeeId ?: return
            
            Log.d(TAG, "📍 Getting location for update...")
            
            // Get current location
            val location = locationService.getCurrentLocation()
            if (location == null) {
                Log.w(TAG, "⚠️ Could not get location for update")
                return
            }
            
            Log.d(TAG, "📍 Got location: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")
            
            // Get place name (with timeout)
            var placeName: String? = null
            try {
                placeName = withTimeoutOrNull(5000L) {
                    geocodingService.getPlaceName(location.latitude, location.longitude)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get place name: ${e.message}")
            }
            
            if (placeName.isNullOrBlank()) {
                placeName = "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
            }
            
            // Update Firestore
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            firestoreService.updateEmployeeLocation(empId, geoPoint, placeName)
            
            Log.d(TAG, "✅ Location updated for $empId at $placeName")
            
            // Update notification with latest location
            val notification = createNotification(employeeName!!, placeName)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update location error: ${e.message}", e)
        }
    }
    
    private fun stopLocationTracking() {
        locationUpdateJob?.cancel()
        locationUpdateJob = null
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
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
        serviceScope.cancel()
        Log.d(TAG, "📍 Location tracking service destroyed")
    }
}
