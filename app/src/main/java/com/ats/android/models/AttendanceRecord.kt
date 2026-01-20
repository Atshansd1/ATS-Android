package com.ats.android.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Attendance record model fully compatible with iOS format
 * Matches iOS AttendanceRecord structure exactly for cross-platform compatibility
 */
data class AttendanceRecord(
    @DocumentId
    val id: String? = null,
    val employeeId: String = "",
    val checkInTime: Timestamp = Timestamp.now(),
    val checkOutTime: Timestamp? = null,
    val checkInLocation: GeoPointData? = null, // iOS-compatible format
    val checkOutLocation: GeoPointData? = null, // iOS-compatible format
    val checkInPlaceName: String? = null,
    val checkOutPlaceName: String? = null,
    val checkInPlaceNameEn: String? = null,
    val checkInPlaceNameAr: String? = null,
    val checkOutPlaceNameEn: String? = null,
    val checkOutPlaceNameAr: String? = null,
    val totalDuration: Double? = null, // iOS uses TimeInterval (Double), not Long
    @get:com.google.firebase.firestore.PropertyName("status")
    @set:com.google.firebase.firestore.PropertyName("status")
    var statusString: String = "checked_in",
    val syncStatus: String? = "synced", // iOS field
    val idempotencyKey: String? = null, // iOS field
    val metadata: AttendanceMetadata? = null, // iOS field
    // Legacy Android fields (optional for backward compatibility)
    val employeeName: String? = null,
    val date: Timestamp? = null,
    val duration: Long? = null
) {
    @get:com.google.firebase.firestore.Exclude
    val status: AttendanceStatus
        get() = AttendanceStatus.fromString(statusString)
    
    /**
     * Get duration in hours for display
     */
    @get:com.google.firebase.firestore.Exclude
    val durationHours: Double
        get() = (totalDuration ?: duration?.toDouble() ?: 0.0) / 3600.0
    
    /**
     * Get localized check-in place name based on language preference
     */
    @com.google.firebase.firestore.Exclude
    fun getLocalizedCheckInPlaceName(isArabic: Boolean): String {
        return if (isArabic) {
            checkInPlaceNameAr ?: checkInPlaceName ?: ""
        } else {
            checkInPlaceNameEn ?: checkInPlaceName ?: ""
        }
    }
    
    /**
     * Get localized check-out place name based on language preference
     */
    @com.google.firebase.firestore.Exclude
    fun getLocalizedCheckOutPlaceName(isArabic: Boolean): String {
        return if (isArabic) {
            checkOutPlaceNameAr ?: checkOutPlaceName ?: ""
        } else {
            checkOutPlaceNameEn ?: checkOutPlaceName ?: ""
        }
    }
}

/**
 * Attendance metadata matching iOS format
 */
data class AttendanceMetadata(
    val deviceInfo: String? = null,
    val appVersion: String? = null,
    val batteryLevel: Double? = null,
    val notes: String? = null
)

enum class AttendanceStatus(val value: String) {
    CHECKED_IN("checked_in"),
    CHECKED_OUT("checked_out"),
    ON_LEAVE("on_leave"),
    ABSENT("absent");
    
    companion object {
        fun fromString(value: String?): AttendanceStatus {
            if (value == null) return CHECKED_IN
            return when (value.lowercase().replace(" ", "_")) {
                "checked_in", "checked in" -> CHECKED_IN
                "checked_out", "checked out" -> CHECKED_OUT
                "on_leave", "on leave" -> ON_LEAVE
                "absent" -> ABSENT
                else -> CHECKED_IN
            }
        }
    }
    
    override fun toString(): String = value
}

data class ActiveLocation(
    val employeeId: String = "",
    val location: GeoPointData = GeoPointData(),
    val timestamp: Timestamp = Timestamp.now(),
    val checkInTime: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val placeName: String? = null,
    val placeNameEn: String? = null,
    val placeNameAr: String? = null,
    val previousPlaceName: String? = null,
    val batteryLevel: Double? = null,
    val speed: Double? = null,
    val accuracy: Double? = null,
    // Use lastUpdated instead of timestamp for sorting to match iOS
    val lastUpdated: Timestamp = Timestamp.now()
) {
    fun getLocalizedPlaceName(): String? {
        val isArabic = java.util.Locale.getDefault().language == "ar"
        return if (isArabic) {
            placeNameAr ?: placeName
        } else {
            placeNameEn ?: placeName
        }
    }
}
