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
    
    private val cache = mutableMapOf<String, Pair<String, Long>>()
    
    companion object {
        private const val TAG = "GeocodingService"
        private const val CACHE_DURATION = 3600000L // 1 hour
    }
    
    // Get geocoder with current locale
    private fun getGeocoder(): Geocoder {
        return Geocoder(context, Locale.getDefault())
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
                
                Log.d(TAG, "🔍 Geocoding: $latitude, $longitude (Locale: ${Locale.getDefault().language})")
                
                val geocoder = getGeocoder() // Get geocoder with current locale
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
        
        // Priority 1: Premises (specific building/place name)
        address.premises?.let { premises ->
            if (premises.isNotBlank() && !premises.matches(Regex("^\\d+$"))) {
                parts.add(premises)
                Log.d(TAG, "✅ Premises: $premises")
            }
        }
        
        // Priority 2: Feature name (Civil Defense Center, hospital, mall, etc.)
        address.featureName?.let { featureName ->
            // Only add if it's not a numeric value (street number) and not already added
            if (!featureName.matches(Regex("^\\d+$")) && !parts.contains(featureName)) {
                parts.add(featureName)
                Log.d(TAG, "✅ Feature: $featureName")
            }
        }
        
        // Priority 3: Sub-thoroughfare (building number with street name if feature/premises not found)
        if (parts.isEmpty()) {
            val street = buildString {
                address.subThoroughfare?.let { append("$it ") } // Street number
                address.thoroughfare?.let { append(it) } // Street name
            }.trim()
            if (street.isNotEmpty()) {
                parts.add(street)
                Log.d(TAG, "✅ Street: $street")
            }
        }
        
        // Priority 4: Just thoroughfare (street name) if we still don't have anything
        if (parts.isEmpty()) {
            address.thoroughfare?.let { 
                parts.add(it)
                Log.d(TAG, "✅ Thoroughfare: $it")
            }
        }
        
        // Priority 5: Sublocality or neighborhood (only if we have a place name already)
        if (parts.isNotEmpty()) {
            address.subLocality?.let { 
                if (!parts.contains(it)) {
                    parts.add(it)
                    Log.d(TAG, "✅ SubLocality: $it")
                }
            }
        }
        
        // Priority 6: City/Locality (always add for context)
        address.locality?.let { 
            if (!parts.contains(it)) {
                parts.add(it)
                Log.d(TAG, "✅ Locality: $it")
            }
        } ?: address.subAdminArea?.let { 
            if (!parts.contains(it)) {
                parts.add(it)
                Log.d(TAG, "✅ SubAdminArea: $it")
            }
        }
        
        // Priority 7: Admin area (state/province) - only if we don't have enough detail
        if (parts.size < 2) {
            address.adminArea?.let { 
                if (!parts.contains(it)) {
                    parts.add(it)
                    Log.d(TAG, "✅ AdminArea: $it")
                }
            }
        }
        
        val result = if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            "Unknown Location"
        }
        
        Log.d(TAG, "📍 Final formatted address: $result")
        return result
    }
}
