package com.ats.android.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

/**
 * Custom GeoPoint wrapper that matches iOS format
 * iOS stores location as: {latitude, longitude, accuracy, timestamp}
 * This allows Android to deserialize the same format
 */
data class GeoPointData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Double? = null,
    val timestamp: Timestamp? = null
) {
    /**
     * Convert to Firebase GeoPoint for compatibility
     */
    fun toGeoPoint(): GeoPoint {
        return GeoPoint(latitude, longitude)
    }
    
    companion object {
        /**
         * Create from Firebase GeoPoint
         */
        fun fromGeoPoint(geoPoint: GeoPoint): GeoPointData {
            return GeoPointData(
                latitude = geoPoint.latitude,
                longitude = geoPoint.longitude,
                accuracy = null,
                timestamp = Timestamp.now()
            )
        }
    }
}
