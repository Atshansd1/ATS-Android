package com.ats.android.viewmodels

import android.app.Application
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ats.android.models.AttendanceRecord
import com.ats.android.models.Employee
import com.ats.android.services.FirestoreService
import com.ats.android.services.GeocodingService
import com.ats.android.services.LocationService
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CheckInViewModel(application: Application) : AndroidViewModel(application) {
    
    private val firestoreService = FirestoreService.getInstance()
    private val locationService = LocationService(application.applicationContext)
    private val geocodingService = GeocodingService(application.applicationContext)
    
    private val _uiState = MutableStateFlow<CheckInUiState>(CheckInUiState.Loading)
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()
    
    private val _isCheckedIn = MutableStateFlow(false)
    val isCheckedIn: StateFlow<Boolean> = _isCheckedIn.asStateFlow()
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    
    private val _placeName = MutableStateFlow<String?>(null)
    val placeName: StateFlow<String?> = _placeName.asStateFlow()
    
    private val _activeRecord = MutableStateFlow<AttendanceRecord?>(null)
    val activeRecord: StateFlow<AttendanceRecord?> = _activeRecord.asStateFlow()
    
    fun initialize(employee: Employee?) {
        if (employee == null) {
            _uiState.value = CheckInUiState.Error("Employee not found")
            return
        }
        
        viewModelScope.launch {
            try {
                // Set to Ready immediately so UI can render
                _uiState.value = CheckInUiState.Ready
                
                // Check if already checked in
                val activeRecord = firestoreService.getActiveCheckIn(employee.employeeId)
                _activeRecord.value = activeRecord
                _isCheckedIn.value = activeRecord != null
                
                Log.d(TAG, "✅ Check-in initialized: isCheckedIn=${_isCheckedIn.value}")
                
                // If already checked in, start location tracking service
                if (_isCheckedIn.value) {
                    com.ats.android.services.LocationTrackingService.startTracking(
                        getApplication(),
                        employee.employeeId,
                        employee.displayName
                    )
                    Log.d(TAG, "📍 Resumed location tracking service")
                }
                
                // Get current location asynchronously (doesn't block UI)
                getCurrentLocation()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error initializing: ${e.message}", e)
                _uiState.value = CheckInUiState.Error(e.message ?: "Initialization failed")
            }
        }
    }
    
    /**
     * Clear error state after showing to user
     */
    fun clearError() {
        _uiState.value = CheckInUiState.Ready
    }
    
    private suspend fun getCurrentLocation() {
        try {
            // Set to loading state
            _placeName.value = "Locating..."
            
            // Check permissions first
            if (!locationService.hasLocationPermission()) {
                _placeName.value = "Location permission required"
                Log.e(TAG, "❌ No location permission")
                return
            }
            
            // Check if location is enabled
            if (!locationService.isLocationEnabled()) {
                _placeName.value = "Please enable location in settings"
                Log.e(TAG, "❌ Location disabled")
                return
            }
            
            // Check Google Play Services
            if (!locationService.isGooglePlayServicesAvailable()) {
                _placeName.value = "Google Play Services required"
                Log.e(TAG, "❌ Google Play Services not available")
                return
            }
            
            // Get location with extended timeout for real devices (10 seconds)
            Log.d(TAG, "📍 Getting location...")
            val location = kotlinx.coroutines.withTimeoutOrNull(10000L) {
                locationService.getCurrentLocation()
            }
            
            _currentLocation.value = location
            
            if (location != null) {
                // Show coordinates immediately
                val coords = "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                _placeName.value = coords
                Log.d(TAG, "✅ Location: $coords, accuracy: ${location.accuracy}m")
                
                // Try to get place name in background (don't block UI)
                viewModelScope.launch {
                    try {
                        val place = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                            geocodingService.getPlaceName(location.latitude, location.longitude)
                        }
                        if (place != null) {
                            _placeName.value = place
                            Log.d(TAG, "📍 Place name: $place")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to get place name: ${e.message}")
                        // Keep showing coordinates
                    }
                }
            } else {
                _placeName.value = "Unable to get location. Please try again."
                Log.e(TAG, "❌ Location is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Location error: ${e.message}", e)
            _placeName.value = "Location error. Please try again."
        }
    }
    
    // Data class to hold bilingual center validation result
    private data class CenterValidationResult(
        val isValid: Boolean,
        val nameEn: String? = null,
        val nameAr: String? = null
    ) {
        val localizedName: String?
            get() {
                val isArabic = java.util.Locale.getDefault().language == "ar"
                return if (isArabic) nameAr ?: nameEn else nameEn ?: nameAr
            }
    }
    
    fun checkIn(employee: Employee) {
        viewModelScope.launch {
            try {
                _uiState.value = CheckInUiState.Processing
                Log.d(TAG, "⏱️ Checking in: ${employee.displayName}")
                
                // Check permissions
                if (!locationService.hasLocationPermission()) {
                    _uiState.value = CheckInUiState.Error("Location permission is required. Please grant permission in app settings.")
                    return@launch
                }
                
                // Check if location is enabled
                if (!locationService.isLocationEnabled()) {
                    _uiState.value = CheckInUiState.Error("Please enable location services in device settings.")
                    return@launch
                }
                
                // Get location (with extended timeout for check-in)
                var location = _currentLocation.value
                if (location == null || !isLocationRecent(location)) {
                    Log.d(TAG, "Getting fresh location for check-in...")
                    _placeName.value = "Getting location..."
                    
                    location = kotlinx.coroutines.withTimeoutOrNull(10000L) {
                        locationService.getCurrentLocation()
                    }
                    _currentLocation.value = location
                }
                
                if (location == null) {
                    _uiState.value = CheckInUiState.Error("Unable to get your location. Please ensure GPS is enabled and try again.")
                    return@launch
                }
                
                // Security Check: Mock Location
                if (locationService.isMockLocation(location)) {
                     _uiState.value = CheckInUiState.Error("Mock location detected. Please disable any 'Fake GPS' apps and try again.")
                     return@launch
                }
                
                // Security Check: Attendance Centers (Geofencing)
                val validationResult = validateLocationWithAttendanceCenters(employee.employeeId, location)
                if (!validationResult.isValid) {
                     _uiState.value = CheckInUiState.Error("You are not within an authorized attendance center.")
                     return@launch
                }
                
                // Use localized place name based on device language
                var placeName = validationResult.localizedName ?: _placeName.value
                if (placeName == null || 
                    placeName.contains("permission", ignoreCase = true) ||
                    placeName.contains("unable", ignoreCase = true) ||
                    placeName.contains("error", ignoreCase = true) ||
                    placeName.contains("Locating", ignoreCase = true)) {
                    placeName = "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                }
                
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                val result = firestoreService.checkIn(
                    employeeId = employee.employeeId,
                    employeeName = employee.displayName,
                    location = geoPoint,
                    placeName = placeName
                )
                
                if (result.isSuccess) {
                    _isCheckedIn.value = true
                    _uiState.value = CheckInUiState.Success("Checked in successfully")
                    Log.d(TAG, "✅ Check-in successful")
                    
                    // Start location tracking service
                    com.ats.android.services.LocationTrackingService.startTracking(
                        getApplication(),
                        employee.employeeId,
                        employee.displayName
                    )
                    Log.d(TAG, "📍 Started location tracking service")
                    
                    // Reload active record in background
                    launch {
                        val activeRecord = firestoreService.getActiveCheckIn(employee.employeeId)
                        _activeRecord.value = activeRecord
                    }
                } else {
                    _uiState.value = CheckInUiState.Error(result.exceptionOrNull()?.message ?: "Check-in failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Check-in error: ${e.message}", e)
                _uiState.value = CheckInUiState.Error(e.message ?: "Check-in failed")
            }
        }
    }
    
    // MARK: - Attendance Center Validation
    private suspend fun validateLocationWithAttendanceCenters(employeeId: String, location: Location): CenterValidationResult {
        try {
            // Fetch active attendance centers
            val centers = firestoreService.getAllAttendanceCenters()
            val activeCenters = centers.filter { it.isActive }
            
            if (activeCenters.isEmpty()) {
                // No centers configured = allow check-in from anywhere
                Log.d(TAG, "No attendance centers configured, allowing check-in")
                return CenterValidationResult(isValid = true)
            }
            
            Log.d(TAG, "🔍 Validating employee '$employeeId' against ${activeCenters.size} active centers")
            
            // Filter centers for this employee
            val employeeCenters = activeCenters.filter { center ->
                val isAssigned = center.assignedEmployeeIds.isEmpty() || center.assignedEmployeeIds.contains(employeeId)
                if (!isAssigned) {
                     Log.d(TAG, "   - Center '${center.name}': ID '$employeeId' NOT found in ${center.assignedEmployeeIds}")
                } else {
                     Log.d(TAG, "   - Center '${center.name}': ID '$employeeId' FOUND (or list empty)")
                }
                isAssigned
            }
            
            if (employeeCenters.isEmpty()) {
                Log.d(TAG, "❌ Employee not assigned to any attendance center")
                return CenterValidationResult(isValid = false)
            }
            
            // Check if location is within any assigned center's radius
            for (center in employeeCenters) {
               val centerLocation = Location("center").apply {
                   latitude = center.coordinate.latitude
                   longitude = center.coordinate.longitude
               }
               val distance = location.distanceTo(centerLocation)
               
               Log.d(TAG, "🔍 Validation Check: Center='${center.name}' (${center.coordinate.latitude}, ${center.coordinate.longitude}), Radius=${center.radiusMeters}m")
               Log.d(TAG, "📍 User Location: (${location.latitude}, ${location.longitude})")
               Log.d(TAG, "📏 Distance: ${distance}m (Allowed: ${center.radiusMeters}m)")
               
               if (center.coordinate.latitude == 0.0 && center.coordinate.longitude == 0.0) {
                   Log.e(TAG, "⚠️ Center '${center.name}' has (0,0) coordinates! This might be a data migration issue.")
               }

                if (distance <= center.radiusMeters) {
                    // Return both English and Arabic names for bilingual support
                    val nameEn = if (!center.nameEn.isNullOrEmpty()) center.nameEn else center.name
                    val nameAr = if (!center.nameAr.isNullOrEmpty()) center.nameAr else center.name
                    Log.d(TAG, "✅ Location validated: within ${center.name} (En: $nameEn, Ar: $nameAr)")
                    return CenterValidationResult(isValid = true, nameEn = nameEn, nameAr = nameAr)
                } else {
                    Log.w(TAG, "❌ Too far from center '${center.name}'. Distance: ${distance}m > Radius: ${center.radiusMeters}m")
                }
            }
            
            Log.d(TAG, "❌ Not within any assigned attendance center radius")
            return CenterValidationResult(isValid = false)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating attendance centers: ${e.message}")
            // On error, allow check-in to avoid blocking employees
            return CenterValidationResult(isValid = true)
        }
    }
    
    fun checkOut(employee: Employee) {
        viewModelScope.launch {
            try {
                _uiState.value = CheckInUiState.Processing
                Log.d(TAG, "⏱️ Checking out: ${employee.displayName}")
                
                // Try current location first
                var location = _currentLocation.value
                var placeName: String? = null
                
                // If no current location, try quick fetch
                if (location == null) {
                    Log.d(TAG, "Getting location for check-out...")
                    location = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                        locationService.getCurrentLocation()
                    }
                    _currentLocation.value = location
                }
                
                // If still no location, use check-in location
                if (location == null && _activeRecord.value?.checkInLocation != null) {
                    val checkInLoc = _activeRecord.value!!.checkInLocation!!
                    location = Location("fallback").apply {
                        latitude = checkInLoc.latitude
                        longitude = checkInLoc.longitude
                    }
                    placeName = _activeRecord.value?.checkInPlaceName
                    Log.d(TAG, "⚠️ Using check-in location as fallback")
                }
                
                if (location == null) {
                    _uiState.value = CheckInUiState.Error("Location not available. Please try again.")
                    return@launch
                }
                
                // If we got a fresh location, try to get the place name
                if (placeName == null) {
                    Log.d(TAG, "🔍 Fetching place name for checkout location...")
                    placeName = kotlinx.coroutines.withTimeoutOrNull(3000L) {
                        geocodingService.getPlaceName(location.latitude, location.longitude)
                    }
                    
                    if (placeName != null) {
                        Log.d(TAG, "✅ Checkout place name: $placeName")
                    } else {
                        Log.w(TAG, "⚠️ Could not get place name, using coordinates")
                    }
                }
                
                // Use place name if available, otherwise use coordinates
                if (placeName == null) {
                    placeName = "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                }
                
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                val result = firestoreService.checkOut(
                    employeeId = employee.employeeId,
                    location = geoPoint,
                    placeName = placeName
                )
                
                if (result.isSuccess) {
                    _isCheckedIn.value = false
                    _activeRecord.value = null
                    _uiState.value = CheckInUiState.Success("Checked out successfully")
                    Log.d(TAG, "✅ Check-out successful")
                    
                    // Stop location tracking service
                    com.ats.android.services.LocationTrackingService.stopTracking(getApplication())
                    Log.d(TAG, "🛑 Stopped location tracking service")
                } else {
                    _uiState.value = CheckInUiState.Error(result.exceptionOrNull()?.message ?: "Check-out failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Check-out error: ${e.message}", e)
                _uiState.value = CheckInUiState.Error(e.message ?: "Check-out failed")
            }
        }
    }
    
    fun refreshLocation() {
        viewModelScope.launch {
            getCurrentLocation()
        }
    }
    
    private fun isLocationRecent(location: Location): Boolean {
        val ageMillis = System.currentTimeMillis() - location.time
        return ageMillis < 60000 // Less than 1 minute old
    }
    
    companion object {
        private const val TAG = "CheckInViewModel"
    }
}

sealed class CheckInUiState {
    object Loading : CheckInUiState()
    object Ready : CheckInUiState()
    object Processing : CheckInUiState()
    data class Success(val message: String) : CheckInUiState()
    data class Error(val message: String) : CheckInUiState()
}
