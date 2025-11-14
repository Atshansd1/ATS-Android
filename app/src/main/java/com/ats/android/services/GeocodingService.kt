package com.ats.android.services

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GeocodingService(private val context: Context) {
    
    private val geocoder = Geocoder(context, Locale.getDefault())
    private val cache = mutableMapOf<String, Pair<String, Long>>()
    
    companion object {
        private const val TAG = "GeocodingService"
        private const val CACHE_DURATION = 3600000L // 1 hour
    }
    
    suspend fun getPlaceName(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = "${String.format("%.3f", latitude)},${String.format("%.3f", longitude)}"
                
                // Check cache
                cache[cacheKey]?.let { (placeName, timestamp) ->
                    if (System.currentTimeMillis() - timestamp < CACHE_DURATION) {
                        Log.d(TAG, "✅ Cache hit for $cacheKey")
                        return@withContext placeName
                    }
                }
                
                Log.d(TAG, "🔍 Geocoding: $latitude, $longitude")
                
                val placeName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // New API for Android 13+
                    suspendCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            val name = addresses.firstOrNull()?.let { formatAddress(it) }
                            continuation.resume(name)
                        }
                    }
                } else {
                    // Legacy API
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    addresses?.firstOrNull()?.let { formatAddress(it) }
                }
                
                // Cache result
                placeName?.let {
                    cache[cacheKey] = Pair(it, System.currentTimeMillis())
                    Log.d(TAG, "✅ Geocoded: $it")
                }
                
                placeName
            } catch (e: Exception) {
                Log.e(TAG, "❌ Geocoding error: ${e.message}", e)
                null
            }
        }
    }
    
    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()
        
        // Priority 1: Feature name (building, landmark, business)
        address.featureName?.let { featureName ->
            // Only add if it's not a numeric value (street number)
            if (!featureName.matches(Regex("^\\d+$"))) {
                parts.add(featureName)
            }
        }
        
        // Priority 2: Street with number
        val street = buildString {
            address.subThoroughfare?.let { append("$it ") } // Street number
            address.thoroughfare?.let { append(it) } // Street name
        }.trim()
        if (street.isNotEmpty() && !parts.contains(street)) {
            parts.add(street)
        }
        
        // Priority 3: Sublocality or neighborhood
        address.subLocality?.let { 
            if (!parts.contains(it)) parts.add(it) 
        }
        
        // Priority 4: City/Locality
        address.locality?.let { 
            if (!parts.contains(it)) parts.add(it)
        } ?: address.subAdminArea?.let { 
            if (!parts.contains(it)) parts.add(it)
        }
        
        // Priority 5: Admin area (state/province) - only if we don't have enough detail
        if (parts.size <= 2) {
            address.adminArea?.let { 
                if (!parts.contains(it)) parts.add(it)
            }
        }
        
        // Fallback: Use coordinates if no address parts found
        return parts.joinToString(", ").ifEmpty {
            "${String.format("%.4f", address.latitude)}, ${String.format("%.4f", address.longitude)}"
        }
    }
}
