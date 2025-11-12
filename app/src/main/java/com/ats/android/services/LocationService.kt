package com.ats.android.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
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
    }
    
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.e(TAG, "❌ No location permission")
            return null
        }
        
        return try {
            Log.d(TAG, "📍 Getting current location...")
            
            // Try to get last known location first (faster)
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                Log.d(TAG, "✅ Got last location: ${lastLocation.latitude}, ${lastLocation.longitude}")
                return lastLocation
            }
            
            // If no last location, try a quick fresh request
            Log.d(TAG, "📍 Requesting fresh location with 2s timeout...")
            val request = com.google.android.gms.location.CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setDurationMillis(2000) // 2 second timeout
                .setMaxUpdateAgeMillis(60000) // Accept location up to 1 minute old
                .build()
            
            val location = kotlinx.coroutines.withTimeoutOrNull(2500L) {
                fusedLocationClient.getCurrentLocation(request, null).await()
            }
            
            if (location != null) {
                Log.d(TAG, "✅ Fresh location: ${location.latitude}, ${location.longitude}")
                return location
            }
            
            // If still no location, return a default (for emulator/testing)
            Log.w(TAG, "⚠️ No location available, using default location")
            return Location("default").apply {
                latitude = 24.7136 // Riyadh
                longitude = 46.6753
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Location error: ${e.message}, using default location", e)
            // Return default location on error (for emulator)
            return Location("default").apply {
                latitude = 24.7136
                longitude = 46.6753
            }
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
