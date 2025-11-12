package com.ats.android.services

import android.content.Context
import android.util.Log
import com.ats.android.models.GooglePlaceDetails
import com.ats.android.models.GooglePlacePrediction
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service for Google Places API integration
 */
class GooglePlacesService(private val context: Context) {
    private val placesClient: PlacesClient by lazy {
        if (!Places.isInitialized()) {
            Places.initialize(context, getApiKey())
        }
        Places.createClient(context)
    }
    
    private val sessionToken = AutocompleteSessionToken.newInstance()
    
    private fun getApiKey(): String {
        // Get API key from BuildConfig or resources
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            appInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get API key", e)
            ""
        }
    }
    
    /**
     * Search for places using autocomplete
     */
    suspend fun searchPlaces(
        query: String,
        languageCode: String = "en"
    ): Result<List<GooglePlacePrediction>> = suspendCancellableCoroutine { continuation ->
        if (query.isBlank()) {
            continuation.resume(Result.success(emptyList()))
            return@suspendCancellableCoroutine
        }
        
        Log.d(TAG, "[GooglePlaces] Searching for: $query")
        
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setQuery(query)
            .build()
        
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val predictions = response.autocompletePredictions.map { prediction ->
                    GooglePlacePrediction(
                        placeId = prediction.placeId,
                        description = prediction.getFullText(null).toString(),
                        mainText = prediction.getPrimaryText(null).toString(),
                        secondaryText = prediction.getSecondaryText(null)?.toString() ?: ""
                    )
                }
                Log.d(TAG, "[GooglePlaces] Found ${predictions.size} results")
                continuation.resume(Result.success(predictions))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "[GooglePlaces] Search failed", exception)
                continuation.resume(Result.failure(exception))
            }
        
        continuation.invokeOnCancellation {
            Log.d(TAG, "[GooglePlaces] Search cancelled")
        }
    }
    
    /**
     * Fetch place details by place ID
     */
    suspend fun fetchPlaceDetails(placeId: String): Result<GooglePlaceDetails> = 
        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "[GooglePlaces] Fetching details for: $placeId")
            
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )
            
            val request = FetchPlaceRequest.builder(placeId, placeFields)
                .setSessionToken(sessionToken)
                .build()
            
            placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    val place = response.place
                    val latLng = place.latLng
                    
                    if (latLng != null) {
                        val details = GooglePlaceDetails(
                            placeId = place.id ?: placeId,
                            name = place.name ?: "",
                            formattedAddress = place.address ?: "",
                            latitude = latLng.latitude,
                            longitude = latLng.longitude
                        )
                        Log.d(TAG, "[GooglePlaces] Got details: ${details.name}")
                        continuation.resume(Result.success(details))
                    } else {
                        val error = Exception("Place has no location")
                        Log.e(TAG, "[GooglePlaces] Error: Place has no location")
                        continuation.resume(Result.failure(error))
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "[GooglePlaces] Fetch failed", exception)
                    continuation.resume(Result.failure(exception))
                }
            
            continuation.invokeOnCancellation {
                Log.d(TAG, "[GooglePlaces] Fetch cancelled")
            }
        }
    
    /**
     * Search for places (unwrapped version for easier use)
     */
    suspend fun searchPlaces(query: String): List<PlacePrediction> {
        return searchPlaces(query, "en").getOrElse { emptyList() }
            .map { prediction ->
                PlacePrediction(
                    placeId = prediction.placeId,
                    description = prediction.description,
                    primaryText = prediction.mainText,
                    secondaryText = prediction.secondaryText
                )
            }
    }
    
    /**
     * Get place details (unwrapped version)
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetails {
        val result = fetchPlaceDetails(placeId).getOrThrow()
        return PlaceDetails(
            placeId = result.placeId,
            name = result.name,
            address = result.formattedAddress,
            latitude = result.latitude,
            longitude = result.longitude
        )
    }
    
    /**
     * Data classes for simplified API
     */
    data class PlacePrediction(
        val placeId: String,
        val description: String,
        val primaryText: String,
        val secondaryText: String
    )
    
    data class PlaceDetails(
        val placeId: String,
        val name: String,
        val address: String,
        val latitude: Double,
        val longitude: Double
    )
    
    companion object {
        private const val TAG = "GooglePlacesService"
        
        @Volatile
        private var INSTANCE: GooglePlacesService? = null
        
        fun getInstance(context: Context): GooglePlacesService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GooglePlacesService(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
        
        fun getInstance(): GooglePlacesService {
            return INSTANCE ?: throw IllegalStateException("GooglePlacesService not initialized")
        }
    }
}
