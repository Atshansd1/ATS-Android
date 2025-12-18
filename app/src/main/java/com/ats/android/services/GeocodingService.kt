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
    
    // Get geocoder with specific locale
    private fun getGeocoder(locale: Locale): Geocoder {
        return Geocoder(context, locale)
    }
    
    /**
     * Get the app's selected locale (not device locale)
     * This ensures geocoding returns results in the app's language
     */
    private fun getAppLocale(): Locale {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("app_language", null)
        val effectiveLanguage = languageCode ?: Locale.getDefault().language
        Log.d(TAG, "🌍 App language: saved='$languageCode', effective='$effectiveLanguage', device='${Locale.getDefault().language}'")
        return Locale(effectiveLanguage)
    }
    
    suspend fun getPlaceName(latitude: Double, longitude: Double, locale: Locale? = null): String? {
        val effectiveLocale = locale ?: getAppLocale() // Use app's locale if not specified
        return withContext(Dispatchers.IO) {
            try {
                // Add locale to cache key to distinguish between languages
                val cacheKey = "${String.format(Locale.US, "%.3f", latitude)},${String.format(Locale.US, "%.3f", longitude)}_${effectiveLocale.language}"
                
                // Check cache
                cache[cacheKey]?.let { (placeName, timestamp) ->
                    if (System.currentTimeMillis() - timestamp < CACHE_DURATION) {
                        Log.d(TAG, "✅ Cache hit for $cacheKey")
                        return@withContext placeName
                    }
                }
                
                Log.d(TAG, "🔍 Geocoding: $latitude, $longitude (Locale: ${effectiveLocale.language})")
                
                val geocoder = getGeocoder(effectiveLocale) // Get geocoder with app's locale
                val placeName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // New API for Android 13+
                    suspendCoroutine { continuation ->
                        try {
                            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                                val name = addresses.firstOrNull()?.let { formatAddress(it) }
                                continuation.resume(name)
                            }
                        } catch (e: Exception) {
                             Log.e(TAG, "❌ Geocoder Tiramisu error: ${e.message}")
                             continuation.resume(null)
                        }
                    }
                } else {
                    // Legacy API
                    @Suppress("DEPRECATION")
                    try {
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        addresses?.firstOrNull()?.let { formatAddress(it) }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Geocoder Legacy error: ${e.message}")
                        null
                    }
                }
                
                // Cache result
                placeName?.let {
                    cache[cacheKey] = Pair(it, System.currentTimeMillis())
                    Log.d(TAG, "✅ Geocoded [${effectiveLocale.language}]: $it")
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
