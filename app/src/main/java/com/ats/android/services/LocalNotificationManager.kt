package com.ats.android.services

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ats.android.MainActivity
import com.ats.android.R
import java.util.Calendar

/**
 * LocalNotificationManager - Matches iOS NotificationService functionality
 * 
 * Features:
 * - Daily check-in reminders (weekdays at configurable time)
 * - Check-out reminders (after X hours of being checked in)
 * - Geofence arrival notifications
 * - Shift start/end reminders
 */
class LocalNotificationManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "LocalNotificationManager"
        
        // Notification IDs
        private const val DAILY_REMINDER_BASE_ID = 1000
        private const val CHECKOUT_REMINDER_ID = 2000
        private const val GEOFENCE_REMINDER_ID = 3000
        private const val SHIFT_START_ID = 4000
        private const val SHIFT_END_ID = 5000
        
        // Channel IDs
        private const val CHANNEL_REMINDERS = "hodoor_reminders"
        private const val CHANNEL_GEOFENCE = "hodoor_geofence"
        
        // Request codes
        private const val REQUEST_DAILY_REMINDER = 100
        private const val REQUEST_CHECKOUT_REMINDER = 200
        
        // Preferences
        private const val PREFS_NAME = "notification_prefs"
        private const val PREF_DAILY_REMINDER_ENABLED = "daily_reminder_enabled"
        private const val PREF_DAILY_REMINDER_TIME = "daily_reminder_time"
        private const val PREF_CHECKOUT_REMINDER_ENABLED = "checkout_reminder_enabled"
        private const val PREF_CHECKOUT_HOURS = "checkout_hours"
        private const val PREF_GEOFENCE_ENABLED = "geofence_enabled"
        private const val PREF_LEAVE_REQUEST_NOTIFICATIONS = "leave_request_notifications"
        private const val PREF_LATE_CHECKIN_NOTIFICATIONS = "late_checkin_notifications"
        private const val PREF_MISSED_CHECKIN_NOTIFICATIONS = "missed_checkin_notifications"
        
        // Action constants for interactive checkout notification
        const val ACTION_STILL_WORKING = "com.ats.android.ACTION_STILL_WORKING"
        const val ACTION_CHECKOUT_NOW = "com.ats.android.ACTION_CHECKOUT_NOW"
        
        @Volatile
        private var instance: LocalNotificationManager? = null
        
        fun getInstance(context: Context): LocalNotificationManager {
            return instance ?: synchronized(this) {
                instance ?: LocalNotificationManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Reminders channel
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.check_in_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily check-in and check-out reminders"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(remindersChannel)
            
            // Geofence channel
            val geofenceChannel = NotificationChannel(
                CHANNEL_GEOFENCE,
                "Location Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Arrival and departure notifications"
            }
            notificationManager.createNotificationChannel(geofenceChannel)
        }
    }
    
    // MARK: - Daily Check-In Reminder
    
    /**
     * Schedule daily check-in reminder for weekdays
     * @param hour Hour (0-23)
     * @param minute Minute (0-59)
     */
    fun scheduleDailyCheckInReminder(hour: Int = 9, minute: Int = 0) {
        // Save time preference only (enabled is set by setDailyReminderEnabled)
        prefs.edit()
            .putString(PREF_DAILY_REMINDER_TIME, "$hour:$minute")
            .commit()
        
        // Cancel existing alarms before scheduling new ones
        cancelDailyAlarms()
        
        // Schedule for each weekday (Monday = 2, Friday = 6)
        for (weekday in Calendar.MONDAY..Calendar.FRIDAY) {
            scheduleWeekdayReminder(weekday, hour, minute)
        }
        
        Log.d(TAG, "✅ Daily reminders scheduled for $hour:$minute (Mon-Fri)")
    }
    
    private fun scheduleWeekdayReminder(weekday: Int, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, weekday)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // If time already passed this week, schedule for next week
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }
        
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "DAILY_CHECK_IN_REMINDER"
            putExtra("notification_id", DAILY_REMINDER_BASE_ID + weekday)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_DAILY_REMINDER + weekday,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Use setRepeating for weekly repeat
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY * 7, // Weekly
            pendingIntent
        )
    }
    
    fun cancelDailyCheckInReminder() {
        cancelDailyAlarms()
        Log.d(TAG, "✅ Daily reminders cancelled")
    }
    
    private fun cancelDailyAlarms() {
        for (weekday in Calendar.MONDAY..Calendar.FRIDAY) {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_DAILY_REMINDER + weekday,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }
    
    fun isDailyReminderEnabled(): Boolean {
        val enabled = prefs.getBoolean(PREF_DAILY_REMINDER_ENABLED, false)
        Log.d(TAG, "📖 Reading daily reminder enabled = $enabled")
        return enabled
    }
    
    fun setDailyReminderEnabled(enabled: Boolean) {
        val success = prefs.edit().putBoolean(PREF_DAILY_REMINDER_ENABLED, enabled).commit()
        Log.d(TAG, "📝 Saved daily reminder enabled = $enabled, success = $success")
        // Verify save
        val readBack = prefs.getBoolean(PREF_DAILY_REMINDER_ENABLED, !enabled)
        Log.d(TAG, "📖 Verified read-back = $readBack")
    }
    
    fun getDailyReminderTime(): Pair<Int, Int> {
        val time = prefs.getString(PREF_DAILY_REMINDER_TIME, "9:0") ?: "9:0"
        val parts = time.split(":")
        return Pair(parts[0].toIntOrNull() ?: 9, parts[1].toIntOrNull() ?: 0)
    }
    
    // MARK: - Check-Out Reminder
    
    /**
     * Schedule check-out reminder after X hours of being checked in
     * @param hours Hours after check-in to remind
     */
    fun scheduleCheckOutReminder(hours: Double = 8.0) {
        prefs.edit()
            .putBoolean(PREF_CHECKOUT_REMINDER_ENABLED, true)
            .putFloat(PREF_CHECKOUT_HOURS, hours.toFloat())
            .apply()
        
        val triggerTime = System.currentTimeMillis() + (hours * 60 * 60 * 1000).toLong()
        
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "CHECK_OUT_REMINDER"
            putExtra("notification_id", CHECKOUT_REMINDER_ID)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CHECKOUT_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
        
        Log.d(TAG, "✅ Check-out reminder scheduled for $hours hours from now")
    }
    
    fun cancelCheckOutReminder() {
        prefs.edit().putBoolean(PREF_CHECKOUT_REMINDER_ENABLED, false).apply()
        
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CHECKOUT_REMINDER,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
        Log.d(TAG, "✅ Check-out reminder cancelled")
    }
    
    fun isCheckoutReminderEnabled(): Boolean = prefs.getBoolean(PREF_CHECKOUT_REMINDER_ENABLED, true)
    fun getCheckoutHours(): Float = prefs.getFloat(PREF_CHECKOUT_HOURS, 8f)
    
    fun setCheckoutReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_CHECKOUT_REMINDER_ENABLED, enabled).apply()
    }
    
    fun setCheckoutHours(hours: Int) {
        prefs.edit().putFloat(PREF_CHECKOUT_HOURS, hours.toFloat()).apply()
    }
    
    // MARK: - Geofence Reminder
    
    /**
     * Send immediate notification when arriving at work location
     */
    fun sendGeofenceCheckInReminder(locationName: String) {
        if (!isGeofenceEnabled()) return
        
        showNotification(
            id = GEOFENCE_REMINDER_ID,
            title = context.getString(R.string.check_in_reminder_title),
            body = context.getString(R.string.check_in_reminder_body),
            channelId = CHANNEL_GEOFENCE
        )
        Log.d(TAG, "✅ Geofence reminder sent for: $locationName")
    }
    
    fun setGeofenceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_GEOFENCE_ENABLED, enabled).apply()
    }
    
    fun isGeofenceEnabled(): Boolean = prefs.getBoolean(PREF_GEOFENCE_ENABLED, true)
    
    // MARK: - Admin Notifications
    
    fun isLeaveRequestNotificationsEnabled(): Boolean = 
        prefs.getBoolean(PREF_LEAVE_REQUEST_NOTIFICATIONS, true)
    
    fun setLeaveRequestNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_LEAVE_REQUEST_NOTIFICATIONS, enabled).apply()
    }
    
    fun isLateCheckInNotificationsEnabled(): Boolean = 
        prefs.getBoolean(PREF_LATE_CHECKIN_NOTIFICATIONS, true)
    
    fun setLateCheckInNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_LATE_CHECKIN_NOTIFICATIONS, enabled).apply()
    }
    
    fun isMissedCheckInNotificationsEnabled(): Boolean = 
        prefs.getBoolean(PREF_MISSED_CHECKIN_NOTIFICATIONS, true)
    
    fun setMissedCheckInNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_MISSED_CHECKIN_NOTIFICATIONS, enabled).apply()
    }
    
    // MARK: - Shift Reminders
    
    /**
     * Schedule shift start reminder
     * @param minutesBefore Minutes before shift to remind
     */
    fun scheduleShiftStartReminder(shiftStartTime: Long, minutesBefore: Int = 15) {
        val triggerTime = shiftStartTime - (minutesBefore * 60 * 1000)
        
        if (triggerTime <= System.currentTimeMillis()) return
        
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "SHIFT_START_REMINDER"
            putExtra("notification_id", SHIFT_START_ID)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SHIFT_START_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
        
        Log.d(TAG, "✅ Shift start reminder scheduled")
    }
    
    fun scheduleShiftEndReminder(shiftEndTime: Long, minutesBefore: Int = 15) {
        val triggerTime = shiftEndTime - (minutesBefore * 60 * 1000)
        
        if (triggerTime <= System.currentTimeMillis()) return
        
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "SHIFT_END_REMINDER"
            putExtra("notification_id", SHIFT_END_ID)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SHIFT_END_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
        
        Log.d(TAG, "✅ Shift end reminder scheduled")
    }
    
    // MARK: - Utility
    
    private fun showNotification(id: Int, title: String, body: String, channelId: String = CHANNEL_REMINDERS) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(id, notification)
    }
    
    fun cancelAllNotifications() {
        cancelDailyCheckInReminder()
        cancelCheckOutReminder()
        notificationManager.cancelAll()
        Log.d(TAG, "✅ All notifications cancelled")
    }
    
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    // MARK: - Smart Notification Logic
    
    fun setUserRole(role: String) {
        prefs.edit().putString("user_role", role).apply()
        Log.d(TAG, "👤 User role saved: $role")
        
        // If admin, ensure reminders are cancelled
        if (role.equals("admin", ignoreCase = true) || role.equals("super_admin", ignoreCase = true)) {
            cancelDailyCheckInReminder()
            Log.d(TAG, "🚫 Admin role detected - cancelled all check-in reminders")
        }
    }
    
    fun setLastCheckInDate(timestamp: Long) {
        prefs.edit().putLong("last_check_in_date", timestamp).apply()
        Log.d(TAG, "✅ Last check-in date updated: $timestamp")
    }
    
    fun shouldShowDailyReminder(): Boolean {
        // 1. Check if Admin
        val role = prefs.getString("user_role", "employee")
        if (role.equals("admin", ignoreCase = true) || role.equals("super_admin", ignoreCase = true)) {
            Log.d(TAG, "🔕 Skipping reminder: User is Admin")
            return false
        }
        
        // 2. Check if already checked in TODAY
        val lastCheckIn = prefs.getLong("last_check_in_date", 0)
        if (lastCheckIn > 0) {
            val calendar = Calendar.getInstance()
            val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
            val todayYear = calendar.get(Calendar.YEAR)
            
            calendar.timeInMillis = lastCheckIn
            val checkInDay = calendar.get(Calendar.DAY_OF_YEAR)
            val checkInYear = calendar.get(Calendar.YEAR)
            
            if (todayDay == checkInDay && todayYear == checkInYear) {
                Log.d(TAG, "🔕 Skipping reminder: Already checked in today")
                return false
            }
        }
        
        return true
    }
}

/**
 * BroadcastReceiver for handling scheduled notification alarms
 */
class NotificationReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "NotificationReceiver"
        private const val CHECKOUT_NOTIFICATION_ID = 2000
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📩 Received: ${intent.action}")
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent.getIntExtra("notification_id", 0)
        
        when (intent.action) {
            "DAILY_CHECK_IN_REMINDER" -> {
                // Feature: Check if we should actually show this reminder
                if (!LocalNotificationManager.getInstance(context).shouldShowDailyReminder()) {
                    Log.d(TAG, "🔕 Daily reminder suppressed based on smart logic")
                    return
                }
                showSimpleNotification(
                    context,
                    notificationManager,
                    notificationId,
                    context.getString(R.string.check_in_reminder_title),
                    context.getString(R.string.check_in_reminder_body)
                )
            }
            "CHECK_OUT_REMINDER" -> {
                // Show interactive notification with action buttons
                showCheckoutReminderWithActions(context, notificationManager)
            }
            LocalNotificationManager.ACTION_STILL_WORKING -> {
                // User clicked "Yes, still working" - dismiss and reschedule for 1 hour later
                Log.d(TAG, "👍 User is still working - rescheduling reminder")
                notificationManager.cancel(CHECKOUT_NOTIFICATION_ID)
                LocalNotificationManager.getInstance(context).scheduleCheckOutReminder(1.0)
            }
            LocalNotificationManager.ACTION_CHECKOUT_NOW -> {
                // User clicked "No, check me out" - perform automatic checkout
                Log.d(TAG, "👋 User wants to checkout - starting CheckoutService")
                notificationManager.cancel(CHECKOUT_NOTIFICATION_ID)
                
                // Start background checkout service
                val checkoutIntent = Intent(context, CheckoutService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(checkoutIntent)
                } else {
                    context.startService(checkoutIntent)
                }
            }
            "SHIFT_START_REMINDER" -> {
                showSimpleNotification(
                    context,
                    notificationManager,
                    notificationId,
                    context.getString(R.string.shift_start_reminder_title),
                    context.getString(R.string.shift_start_reminder_body)
                )
            }
            "SHIFT_END_REMINDER" -> {
                showSimpleNotification(
                    context,
                    notificationManager,
                    notificationId,
                    context.getString(R.string.shift_end_reminder_title),
                    context.getString(R.string.shift_end_reminder_body)
                )
            }
        }
    }
    
    private fun showSimpleNotification(
        context: Context,
        notificationManager: NotificationManager,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, "hodoor_reminders")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "✅ Notification shown: $title")
    }
    
    private fun showCheckoutReminderWithActions(context: Context, notificationManager: NotificationManager) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // "Yes, still working" action
        val stillWorkingIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = LocalNotificationManager.ACTION_STILL_WORKING
        }
        val stillWorkingPendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            stillWorkingIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // "No, check me out" action
        val checkoutIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = LocalNotificationManager.ACTION_CHECKOUT_NOW
        }
        val checkoutPendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            checkoutIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Get localized strings
        val title = context.getString(R.string.still_working_title)
        val body = context.getString(R.string.still_working_body)
        val yesButton = context.getString(R.string.yes_still_working)
        val noButton = context.getString(R.string.no_check_me_out)
        
        val notification = NotificationCompat.Builder(context, "hodoor_reminders")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true) // Make it persistent until user responds
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_notification, yesButton, stillWorkingPendingIntent)
            .addAction(R.drawable.ic_notification, noButton, checkoutPendingIntent)
            .build()
        
        notificationManager.notify(CHECKOUT_NOTIFICATION_ID, notification)
        Log.d(TAG, "✅ Interactive checkout reminder shown with action buttons")
    }
}
