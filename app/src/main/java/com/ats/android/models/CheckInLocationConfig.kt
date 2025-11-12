package com.ats.android.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

/**
 * Configuration for location-based check-in restrictions
 */
data class CheckInLocationConfig(
    val id: String? = null,
    val name: String = "Check-In Policy",
    val type: LocationRestrictionType = LocationRestrictionType.ANYWHERE,
    val allowedLocations: List<AllowedLocation> = emptyList(),
    val applicableEmployeeIds: List<String> = emptyList(), // Empty means applies to all
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun appliesToAllEmployees(): Boolean = applicableEmployeeIds.isEmpty()
}

/**
 * Type of location restriction policy
 */
enum class LocationRestrictionType(val displayName: String, val arabicName: String) {
    ANYWHERE("Anywhere", "في أي مكان"),
    SPECIFIC("Specific Location", "موقع محدد"),
    MULTIPLE("Multiple Locations", "مواقع متعددة");
    
    companion object {
        fun fromString(value: String): LocationRestrictionType {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: ANYWHERE
        }
    }
}

/**
 * An allowed location for check-in
 */
data class AllowedLocation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double = 100.0, // meters
    val placeId: String? = null
) {
    fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
    
    fun isWithinRadius(userLat: Double, userLng: Double): Boolean {
        val distance = calculateDistance(latitude, longitude, userLat, userLng)
        return distance <= radius
    }
    
    companion object {
        /**
         * Calculate distance between two coordinates using Haversine formula
         * Returns distance in meters
         */
        fun calculateDistance(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val earthRadius = 6371000.0 // meters
            
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            
            return earthRadius * c
        }
    }
}

/**
 * Google Places Prediction (for search results)
 */
data class GooglePlacePrediction(
    val placeId: String,
    val description: String,
    val mainText: String,
    val secondaryText: String
)

/**
 * Google Place Details
 */
data class GooglePlaceDetails(
    val placeId: String,
    val name: String,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double
)
