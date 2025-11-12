package com.ats.android.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.text.SimpleDateFormat
import java.util.*

data class LocationMovement(
    @DocumentId
    val id: String? = null,
    val employeeId: String = "",
    val employeeName: String = "",
    val movementType: MovementType = MovementType.SIGNIFICANT_MOVE,
    
    // From location
    val fromLatitude: Double = 0.0,
    val fromLongitude: Double = 0.0,
    val fromAddress: String? = null,
    
    // To location
    val toLatitude: Double = 0.0,
    val toLongitude: Double = 0.0,
    val toAddress: String? = null,
    
    // Movement details
    val distance: Double = 0.0, // in kilometers
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp? = null,
    val duration: Long? = null, // in seconds
    
    // Check-in reference
    val checkInId: String = "",
    val checkInLatitude: Double = 0.0,
    val checkInLongitude: Double = 0.0,
    
    val createdAt: Timestamp = Timestamp.now()
) {
    fun formattedDistance(): String {
        return if (distance < 1.0) {
            String.format("%.0f m", distance * 1000)
        } else {
            String.format("%.2f km", distance)
        }
    }
    
    fun formattedDuration(): String? {
        duration?.let {
            val minutes = (it / 60).toInt()
            val hours = minutes / 60
            val mins = minutes % 60
            
            return if (hours > 0) {
                "${hours}h ${mins}m"
            } else {
                "${mins}m"
            }
        }
        return null
    }
    
    fun timeAgo(): String {
        val now = Date()
        val then = startTime.toDate()
        val seconds = (now.time - then.time) / 1000
        val minutes = (seconds / 60).toInt()
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }
    
    fun distanceFromCheckIn(): Double {
        val checkInLat = checkInLatitude
        val checkInLng = checkInLongitude
        val currentLat = toLatitude
        val currentLng = toLongitude
        
        // Haversine formula
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(currentLat - checkInLat)
        val dLng = Math.toRadians(currentLng - checkInLng)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(checkInLat)) * Math.cos(Math.toRadians(currentLat)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}

enum class MovementType(val value: String) {
    SIGNIFICANT_MOVE("SIGNIFICANT_MOVE"),      // Moved 1km+ from last location
    STATIONARY_STAY("STATIONARY_STAY"),        // Stayed 15min+ at location
    RETURNED_TO_CHECKIN("RETURNED_TO_CHECKIN"), // Returned to check-in location
    LEFT_CHECKIN_AREA("LEFT_CHECKIN_AREA");     // Left check-in area (1km+)
    
    val displayName: String
        get() = when (this) {
            SIGNIFICANT_MOVE -> "Moved"
            STATIONARY_STAY -> "Stayed"
            RETURNED_TO_CHECKIN -> "Returned"
            LEFT_CHECKIN_AREA -> "Left Area"
        }
    
    val iconName: String
        get() = when (this) {
            SIGNIFICANT_MOVE -> "arrow_forward"
            STATIONARY_STAY -> "place"
            RETURNED_TO_CHECKIN -> "u_turn_left"
            LEFT_CHECKIN_AREA -> "warning"
        }
    
    companion object {
        fun fromString(value: String): MovementType {
            return values().find { it.value == value } ?: SIGNIFICANT_MOVE
        }
    }
}
