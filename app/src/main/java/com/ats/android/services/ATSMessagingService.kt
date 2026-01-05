package com.ats.android.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ats.android.MainActivity
import com.ats.android.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging Service
 * Handles FCM token refresh and incoming push notifications
 */
class ATSMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "ATSMessagingService"
        private const val CHANNEL_ID = "ats_notifications"
        private const val CHANNEL_NAME = "Hodoor+ Notifications"
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    /**
     * Called when FCM token is refreshed
     * Store token in Firestore for server-side push notifications
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 New FCM Token: $token")
        
        // Store token in Firestore
        serviceScope.launch {
            try {
                val authService = AuthService.getInstance()
                val currentUser = authService.currentUser
                
                if (currentUser != null) {
                    val firestoreService = FirestoreService.getInstance()
                    firestoreService.updateFCMToken(currentUser.employeeId, token)
                    Log.d(TAG, "✅ FCM token stored in Firestore")
                } else {
                    // Store locally, will be synced on login
                    getSharedPreferences("ats_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("pending_fcm_token", token)
                        .apply()
                    Log.d(TAG, "📦 FCM token stored locally (user not logged in)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to store FCM token: ${e.message}", e)
            }
        }
    }
    
    /**
     * Called when a message is received
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "📩 Message received from: ${remoteMessage.from}")
        
        // Check for data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "📦 Data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }
        
        // Check for notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "🔔 Notification: ${notification.title} - ${notification.body}")
            showNotification(
                title = notification.title ?: getString(R.string.app_name),
                body = notification.body ?: ""
            )
        }
    }
    
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: return
        
        when (type) {
            "force_checkout" -> {
                val employeeId = data["employeeId"]
                Log.d(TAG, "⚠️ Force checkout notification for: $employeeId")
                showNotification(
                    title = getString(R.string.force_checkout_title),
                    body = getString(R.string.force_checkout_message)
                )
            }
            "check_in_reminder" -> {
                showNotification(
                    title = getString(R.string.check_in_reminder_title),
                    body = getString(R.string.check_in_reminder_message)
                )
            }
            "check_out_reminder" -> {
                showNotification(
                    title = getString(R.string.check_out_reminder_title),
                    body = getString(R.string.check_out_reminder_message)
                )
            }
            else -> {
                Log.d(TAG, "Unknown message type: $type")
            }
        }
    }
    
    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Hodoor+ push notifications"
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
