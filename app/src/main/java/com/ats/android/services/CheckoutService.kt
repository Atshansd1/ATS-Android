package com.ats.android.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.ats.android.MainActivity
import com.ats.android.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Background service to perform automatic checkout when user taps "No, check me out" 
 * from the checkout reminder notification.
 */
class CheckoutService : Service() {
    
    companion object {
        private const val TAG = "CheckoutService"
        private const val CHANNEL_ID = "checkout_service_channel"
        private const val NOTIFICATION_ID = 3001
        private const val RESULT_NOTIFICATION_ID = 3002
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firestoreService by lazy { FirestoreService.getInstance() }
    private val authService by lazy { AuthService.getInstance() }
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val geocodingService by lazy { GeocodingService(this) }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🚀 CheckoutService started")
        
        // Start as foreground service with progress notification
        startForeground(NOTIFICATION_ID, createProgressNotification())
        
        // Perform checkout in background
        serviceScope.launch {
            performCheckout()
            stopSelf()
        }
        
        return START_NOT_STICKY
    }
    
    private suspend fun performCheckout() {
        try {
            val employee = authService.currentUser
            if (employee == null) {
                Log.e(TAG, "❌ No current employee found")
                showResultNotification(false, getString(R.string.checkout_failed))
                return
            }
            
            Log.d(TAG, "📍 Getting current location...")
            val location = getCurrentLocation()
            
            if (location == null) {
                Log.e(TAG, "❌ Could not get location")
                showResultNotification(false, getString(R.string.location_unavailable))
                return
            }
            
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            
            // Get place name for checkout location
            val placeName = try {
                geocodingService.getPlaceName(location.latitude, location.longitude)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to geocode: ${e.message}")
                null
            }
            
            Log.d(TAG, "🔄 Performing checkout for ${employee.displayName}")
            
            // Get active attendance record
            val activeRecord = firestoreService.getActiveCheckIn(employee.employeeId)
            
            if (activeRecord == null) {
                Log.e(TAG, "❌ No active check-in found")
                showResultNotification(false, getString(R.string.no_active_checkin))
                return
            }
            
            // Perform checkout
            val result = firestoreService.checkOut(
                employeeId = employee.employeeId,
                location = geoPoint,
                placeName = placeName
            )
            
            if (result.isSuccess) {
                Log.d(TAG, "✅ Checkout successful!")
                
                // Cancel checkout reminder
                LocalNotificationManager.getInstance(this@CheckoutService).cancelCheckOutReminder()
                
                // Stop location tracking service
                val stopTrackingIntent = Intent(this@CheckoutService, LocationTrackingService::class.java)
                stopService(stopTrackingIntent)
                
                showResultNotification(true, getString(R.string.checkout_success))
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "❌ Checkout failed: $error")
                showResultNotification(false, error)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Checkout error: ${e.message}", e)
            showResultNotification(false, e.message ?: "Checkout failed")
        }
    }
    
    private suspend fun getCurrentLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted")
            return null
        }
        
        return try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location: ${e.message}")
            null
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Checkout Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background checkout processing"
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createProgressNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.checking_out))
            .setContentText(getString(R.string.please_wait))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private fun showResultNotification(success: Boolean, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val title = if (success) getString(R.string.checkout_complete) else getString(R.string.checkout_failed)
        
        val notification = NotificationCompat.Builder(this, "hodoor_reminders")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(RESULT_NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "🛑 CheckoutService destroyed")
    }
}
