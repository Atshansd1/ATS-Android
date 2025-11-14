package com.ats.android.models

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

data class EmployeeActivity(
    val id: String = UUID.randomUUID().toString(),
    val employeeId: String = "",
    val employeeName: String = "",
    val action: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val type: ActivityType = ActivityType.CHECK_IN
) {
    val icon: String
        get() = when (type) {
            ActivityType.CHECK_IN -> "arrow_downward"
            ActivityType.CHECK_OUT -> "arrow_upward"
            ActivityType.STATUS_CHANGE -> "edit"
        }
    
    val timeAgo: String
        get() {
            val now = Date()
            val activityDate = timestamp.toDate()
            val diffInMillis = now.time - activityDate.time
            val diffInMinutes = diffInMillis / (60 * 1000)
            val diffInHours = diffInMillis / (60 * 60 * 1000)
            val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)
            
            return when {
                diffInMinutes < 1 -> "Just now"
                diffInMinutes < 60 -> "${diffInMinutes}m ago"
                diffInHours < 24 -> "${diffInHours}h ago"
                diffInDays < 7 -> String.format(Locale.US, "%dd ago", diffInDays)
                else -> {
                    val formatter = SimpleDateFormat("MMM d", Locale.US)
                    formatter.format(activityDate)
                }
            }
        }
}

enum class ActivityType {
    CHECK_IN,
    CHECK_OUT,
    STATUS_CHANGE
}
