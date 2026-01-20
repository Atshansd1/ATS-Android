package com.ats.android.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class AttendanceCenter(
    @DocumentId val id: String? = null,
    val name: String = "",
    val address: String = "",
    val coordinate: GeoPointData = GeoPointData(),
    val radiusMeters: Double = 200.0,
    val assignedEmployeeIds: List<String> = emptyList(),
    val isActive: Boolean = true,
    
    // Check-out Restrictions
    val allowRemoteCheckout: Boolean = true, // If true, all assigned employees can check out from anywhere
    val remoteCheckoutEmployeeIds: List<String> = emptyList(), // Specific employees who can check out from anywhere
    
    // Bilingual support
    val nameEn: String? = null,
    val nameAr: String? = null,
    val addressEn: String? = null,
    val addressAr: String? = null,
    
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    /**
     * Returns the localized name based on device locale.
     * Falls back to base name if localized version is not available.
     */
    val localizedName: String
        get() {
            val isArabic = java.util.Locale.getDefault().language == "ar"
            return if (isArabic) {
                nameAr?.takeIf { it.isNotEmpty() } ?: name
            } else {
                nameEn?.takeIf { it.isNotEmpty() } ?: name
            }
        }
    
    /**
     * Returns the localized address based on device locale.
     */
    val localizedAddress: String
        get() {
            val isArabic = java.util.Locale.getDefault().language == "ar"
            return if (isArabic) {
                addressAr?.takeIf { it.isNotEmpty() } ?: address
            } else {
                addressEn?.takeIf { it.isNotEmpty() } ?: address
            }
        }
}

