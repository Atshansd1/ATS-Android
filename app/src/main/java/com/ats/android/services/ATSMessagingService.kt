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
 * Supports both employee and admin notification channels
 */
class ATSMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "ATSMessagingService"
        
        // Employee notification channel
        private const val CHANNEL_ID_EMPLOYEE = "hodoor_employee_notifications"
        private const val CHANNEL_NAME_EMPLOYEE = "Hodoor+ Notifications"
        
        // Admin notification channel
        private const val CHANNEL_ID_ADMIN = "hodoor_admin_notifications"
        private const val CHANNEL_NAME_ADMIN = "Admin Notifications"
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
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
                body = notification.body ?: "",
                isAdmin = false
            )
        }
    }
    
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: return
        val employeeName = data["employeeName"] ?: ""
        
        when (type) {
            // Employee notifications
            "force_checkout" -> {
                Log.d(TAG, "⚠️ Force checkout notification")
                showNotification(
                    title = getString(R.string.admin_force_checkout_title),
                    body = getString(R.string.admin_force_checkout_body),
                    isAdmin = false
                )
            }
            "check_in_reminder" -> {
                showNotification(
                    title = getString(R.string.check_in_reminder_title),
                    body = getString(R.string.check_in_reminder_body),
                    isAdmin = false
                )
            }
            "check_out_reminder" -> {
                showNotification(
                    title = getString(R.string.check_out_reminder_title),
                    body = getString(R.string.check_out_reminder_body),
                    isAdmin = false
                )
            }
            "shift_start_reminder" -> {
                showNotification(
                    title = getString(R.string.shift_start_reminder_title),
                    body = getString(R.string.shift_start_reminder_body),
                    isAdmin = false
                )
            }
            "shift_end_reminder" -> {
                showNotification(
                    title = getString(R.string.shift_end_reminder_title),
                    body = getString(R.string.shift_end_reminder_body),
                    isAdmin = false
                )
            }
            
            // Admin notifications
            "admin_new_checkin" -> {
                showNotification(
                    title = getString(R.string.admin_new_checkin_title),
                    body = getString(R.string.admin_new_checkin_body, employeeName),
                    isAdmin = true
                )
            }
            "admin_new_checkout" -> {
                showNotification(
                    title = getString(R.string.admin_new_checkout_title),
                    body = getString(R.string.admin_new_checkout_body, employeeName),
                    isAdmin = true
                )
            }
            "admin_leave_request" -> {
                showNotification(
                    title = getString(R.string.admin_leave_request_title),
                    body = getString(R.string.admin_leave_request_body, employeeName),
                    isAdmin = true
                )
            }
            "admin_late_checkin" -> {
                showNotification(
                    title = getString(R.string.admin_late_checkin_title),
                    body = getString(R.string.admin_late_checkin_body, employeeName),
                    isAdmin = true
                )
            }
            "admin_absent_alert" -> {
                showNotification(
                    title = getString(R.string.admin_absent_alert_title),
                    body = getString(R.string.admin_absent_alert_body, employeeName),
                    isAdmin = true
                )
            }
            
            else -> {
                Log.d(TAG, "Unknown message type: $type")
                // Show generic notification for unknown types
                showNotification(
                    title = getString(R.string.general_notification_title),
                    body = getString(R.string.general_notification_body),
                    isAdmin = false
                )
            }
        }
    }
    
    private fun showNotification(title: String, body: String, isAdmin: Boolean) {
        val channelId = if (isAdmin) CHANNEL_ID_ADMIN else CHANNEL_ID_EMPLOYEE
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, channelId)
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
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Employee channel
            val employeeChannel = NotificationChannel(
                CHANNEL_ID_EMPLOYEE,
                CHANNEL_NAME_EMPLOYEE,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Hodoor+ employee notifications - reminders, check-in/out alerts"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(employeeChannel)
            
            // Admin channel
            val adminChannel = NotificationChannel(
                CHANNEL_ID_ADMIN,
                CHANNEL_NAME_ADMIN,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Admin notifications - employee check-ins, leave requests, alerts"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(adminChannel)
        }
    }
}
