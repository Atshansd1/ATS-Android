package com.ats.android.models

import java.text.SimpleDateFormat
import java.util.*

data class ActiveEmployeeInfo(
    val id: String,
    val name: String,
    val department: String,
    val checkInTime: String,
    val duration: String,
    val placeName: String? = null
)

data class DashboardStats(
    val activeNow: Int = 0,
    val totalEmployees: Int = 0,
    val onLeave: Int = 0,
    val checkedInToday: Int = 0
)

fun formatDuration(startTime: Date, endTime: Date = Date()): String {
    val diffInMillis = endTime.time - startTime.time
    val hours = (diffInMillis / (1000 * 60 * 60)).toInt()
    val minutes = ((diffInMillis / (1000 * 60)) % 60).toInt()
    
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

fun formatTime(date: Date): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(date)
}
