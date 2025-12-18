package com.ats.android.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LocationService(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    companion object {
        private const val TAG = "LocationService"
        private const val UPDATE_INTERVAL = 30000L // 30 seconds
        private const val FASTEST_INTERVAL = 10000L // 10 seconds
        private const val LOCATION_TIMEOUT = 8000L // 8 seconds timeout for real devices
    }
    
    fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return hasFine || hasCoarse
    }
    
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }
    
    fun isGooglePlayServicesAvailable(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }
    
    suspend fun getCurrentLocation(): Location? {
        try {
            // Check permissions
            if (!hasLocationPermission()) {
                Log.e(TAG, "❌ No location permission")
                return null
            }
            
            // Check if location is enabled
            if (!isLocationEnabled()) {
                Log.e(TAG, "❌ Location is disabled in device settings")
                return null
            }
            
            // Check Google Play Services
            if (!isGooglePlayServicesAvailable()) {
                Log.e(TAG, "❌ Google Play Services not available")
                return null
            }
            
            Log.d(TAG, "📍 Getting current location...")
            
            // Strategy 1: Try last known location first (instant)
            try {
                val lastLocation = fusedLocationClient.lastLocation.await()
                if (lastLocation != null && isLocationRecent(lastLocation)) {
                    if (isMockLocation(lastLocation)) {
                        Log.e(TAG, "❌ Rejected recent mock location")
                    } else {
                        Log.d(TAG, "✅ Got recent last location: ${lastLocation.latitude}, ${lastLocation.longitude}, age: ${(System.currentTimeMillis() - lastLocation.time) / 1000}s")
                        return lastLocation
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get last location: ${e.message}")
            }
            
            // Strategy 2: Request fresh location with HIGH accuracy
            Log.d(TAG, "📍 Requesting fresh location with ${LOCATION_TIMEOUT / 1000}s timeout...")
            
            try {
                val request = com.google.android.gms.location.CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY) // Use HIGH accuracy for real devices
                    .setDurationMillis(LOCATION_TIMEOUT) // 8 second timeout
                    .setMaxUpdateAgeMillis(30000) // Accept location up to 30 seconds old
                    .build()
                
                val location = kotlinx.coroutines.withTimeoutOrNull(LOCATION_TIMEOUT + 1000) {
                    fusedLocationClient.getCurrentLocation(request, null).await()
                }
                
                if (location != null) {
                    if (isMockLocation(location)) {
                        Log.e(TAG, "❌ Rejected mock location (Fake GPS detected)")
                        return null
                    }
                    Log.d(TAG, "✅ Fresh location: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")
                    return location
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fresh location request failed: ${e.message}", e)
            }
            
            // Strategy 3: Try any last location as fallback
            try {
                val fallbackLocation = fusedLocationClient.lastLocation.await()
                if (fallbackLocation != null) {
                    if (isMockLocation(fallbackLocation)) {
                        Log.e(TAG, "❌ Rejected fallback mock location")
                    } else {
                        val ageMinutes = (System.currentTimeMillis() - fallbackLocation.time) / 60000
                        Log.w(TAG, "⚠️ Using old last location (${ageMinutes}min old): ${fallbackLocation.latitude}, ${fallbackLocation.longitude}")
                        return fallbackLocation
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get fallback location: ${e.message}")
            }
            
            Log.e(TAG, "❌ Could not get location from any source")
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Location error: ${e.message}", e)
            return null
        }
    }
    
    private fun isLocationRecent(location: Location): Boolean {
        val ageMillis = System.currentTimeMillis() - location.time
        return ageMillis < 60000 // Less than 1 minute old
    }

    fun isMockLocation(location: Location): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }
    
    fun getLocationUpdates(): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            Log.e(TAG, "❌ No location permission for updates")
            close()
            return@callbackFlow
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            setWaitForAccurateLocation(false)
        }.build()
        
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    Log.d(TAG, "📍 Location update: ${location.latitude}, ${location.longitude}")
                    trySend(location)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            ).await()
            
            Log.d(TAG, "✅ Location updates started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Location updates error: ${e.message}", e)
            close(e)
        }
        
        awaitClose {
            Log.d(TAG, "🛑 Stopping location updates")
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}
