package com.ats.android.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ats.android.models.*
import com.ats.android.services.FirestoreService
import com.ats.android.services.GooglePlacesService
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(context: Context? = null) : ViewModel() {
    
    private val firestoreService = FirestoreService.getInstance()
    private val placesService = context?.let { GooglePlacesService(it) }
    
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    
    private val _employeeLocations = MutableStateFlow<List<EmployeeLocation>>(emptyList())
    val employeeLocations: StateFlow<List<EmployeeLocation>> = _employeeLocations.asStateFlow()
    
    private val _filteredEmployeeLocations = MutableStateFlow<List<EmployeeLocation>>(emptyList())
    val filteredEmployeeLocations: StateFlow<List<EmployeeLocation>> = _filteredEmployeeLocations.asStateFlow()
    
    private val _mapCenter = MutableStateFlow<LatLng?>(null)
    val mapCenter: StateFlow<LatLng?> = _mapCenter.asStateFlow()
    
    // Filters
    private val _selectedTeamFilter = MutableStateFlow<String?>(null)
    val selectedTeamFilter: StateFlow<String?> = _selectedTeamFilter.asStateFlow()
    
    private val _selectedRoleFilter = MutableStateFlow<EmployeeRole?>(null)
    val selectedRoleFilter: StateFlow<EmployeeRole?> = _selectedRoleFilter.asStateFlow()
    
    // Search
    private val _searchLocation = MutableStateFlow<LatLng?>(null)
    val searchLocation: StateFlow<LatLng?> = _searchLocation.asStateFlow()
    
    private val _nearestEmployee = MutableStateFlow<EmployeeLocation?>(null)
    val nearestEmployee: StateFlow<EmployeeLocation?> = _nearestEmployee.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<GooglePlacePrediction>>(emptyList())
    val searchResults: StateFlow<List<GooglePlacePrediction>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()
    
    private val _selectedPlaceDetails = MutableStateFlow<GooglePlaceDetails?>(null)
    val selectedPlaceDetails: StateFlow<GooglePlaceDetails?> = _selectedPlaceDetails.asStateFlow()
    
    // Store employee data for avatar display
    private val employeeDataMap = mutableMapOf<String, Employee>()
    
    init {
        startLocationTracking()
    }
    
    private fun startLocationTracking() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📍 Starting real-time location tracking...")
                _uiState.value = MapUiState.Loading
                
                // Get real-time updates from Firestore
                firestoreService.observeActiveLocations { locations ->
                    viewModelScope.launch {
                        Log.d(TAG, "📍 Received ${locations.size} active locations from Firestore")
                        
                        // 1. Fetch verified check-in times
                        val employeeIds = locations.map { it.first.employeeId }
                        val verifiedTimes = firestoreService.getLatestCheckInTimes(employeeIds)

                        // 2. Map to EmployeeLocation with original positions
                        val initialList = locations.map { (employee, activeLocation) ->
                             Log.d(TAG, "📍 Mapping location for: ${employee.displayName}")
                             employeeDataMap[employee.employeeId] = employee
                             
                             // Use verified time if available
                             val realCheckInTime = verifiedTimes[employee.employeeId] ?: activeLocation.checkInTime.toDate()
                             
                             EmployeeLocation(
                                employeeId = employee.employeeId,
                                employeeName = employee.displayName,
                                position = LatLng(
                                    activeLocation.location.latitude,
                                    activeLocation.location.longitude
                                ),
                                placeName = activeLocation.placeName,
                                timestamp = activeLocation.lastUpdated.toDate(),
                                role = employee.role,
                                avatarUrl = employee.avatarURL,
                                checkInTime = realCheckInTime
                            )
                        }
                        
                        // 2. Group by rounded position to find overlaps (approx 30m resolution)
                        // Using smaller multiplier (3000) makes the grid cells larger (~35m) to catch nearby markers
                        val groupedByPosition = initialList.groupBy { 
                            val roundedLat = (it.position.latitude * 3000).toInt()
                            val roundedLng = (it.position.longitude * 3000).toInt()
                            roundedLat to roundedLng
                        }
                        
                        // 3. Re-map with offsets if needed
                        val finalEmployeeLocations = initialList.map { empLoc ->
                            Log.d(TAG, "👤 Avatar URL for ${empLoc.employeeName}: ${empLoc.avatarUrl}")
                            
                            val roundedLat = (empLoc.position.latitude * 3000).toInt()
                            val roundedLng = (empLoc.position.longitude * 3000).toInt()
                            val key = roundedLat to roundedLng
                            
                            val group = groupedByPosition[key] ?: emptyList()
                            if (group.size > 1) {
                                val index = group.indexOfFirst { it.employeeId == empLoc.employeeId }
                                if (index > 0) {
                                    // Apply jitter - spread out more for visibility (approx 45m radius)
                                    val angle = (2 * Math.PI / group.size) * index
                                    val radius = 0.0004
                                    val newLat = empLoc.position.latitude + (radius * Math.cos(angle))
                                    val newLng = empLoc.position.longitude + (radius * Math.sin(angle))
                                    
                                    Log.d(TAG, "🕸️ Spiderfied ${empLoc.employeeName} from ${empLoc.position} to ($newLat, $newLng)")
                                    empLoc.copy(position = LatLng(newLat, newLng))
                                } else {
                                    empLoc
                                }
                            } else {
                                empLoc
                            }
                        }
                        
                        _employeeLocations.value = finalEmployeeLocations
                        
                        // Apply filters
                        applyFilters()
                        
                        // Set map center to first employee location if available
                        if (finalEmployeeLocations.isNotEmpty()) {
                             if (_mapCenter.value == null) {
                                 _mapCenter.value = finalEmployeeLocations.first().position
                             }
                        } else {
                             Log.w(TAG, "⚠️ No employee locations available")
                        }
                        
                        _uiState.value = MapUiState.Success
                        Log.d(TAG, "✅ Map state set to Success with ${finalEmployeeLocations.size} locations")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error starting location tracking: ${e.message}", e)
                _uiState.value = MapUiState.Error(e.message ?: "Failed to load locations")
            }
        }
    }
    
    fun refresh() {
        startLocationTracking()
    }
    
    // Filters matching iOS AdminMapViewModel
    fun setTeamFilter(team: String?) {
        _selectedTeamFilter.value = team
        applyFilters()
    }
    
    fun setRoleFilter(role: EmployeeRole?) {
        _selectedRoleFilter.value = role
        applyFilters()
    }
    
    fun clearFilters() {
        _selectedTeamFilter.value = null
        _selectedRoleFilter.value = null
        applyFilters()
    }
    
    private fun applyFilters() {
        var filtered = _employeeLocations.value
        
        // Filter by team
        _selectedTeamFilter.value?.let { team ->
            filtered = filtered.filter { it.employeeName.contains(team, ignoreCase = true) }
        }
        
        // Filter by role
        _selectedRoleFilter.value?.let { role ->
            filtered = filtered.filter { it.role == role }
        }
        
        _filteredEmployeeLocations.value = filtered
        Log.d(TAG, "🔍 Filtered to ${filtered.size} employees")
    }
    
    // Find nearest employee to a location (matching iOS)
    fun findNearestEmployee(location: LatLng) {
        _searchLocation.value = location
        
        val employees = _filteredEmployeeLocations.value.ifEmpty { _employeeLocations.value }
        if (employees.isEmpty()) {
            _nearestEmployee.value = null
            return
        }
        
        val nearest = employees.minByOrNull { employee ->
            calculateDistance(location, employee.position)
        }
        
        _nearestEmployee.value = nearest
        
        nearest?.let {
            val distance = calculateDistance(location, it.position)
            Log.d(TAG, "🎯 Nearest employee: ${it.employeeName} at ${String.format("%.2f", distance / 1000)}km")
        }
    }
    
    // Search for places (matching iOS)
    fun searchPlaces(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }
            
            _isSearching.value = true
            _searchError.value = null
            _searchQuery.value = query
            
            placesService?.searchPlaces(query, "en")?.fold(
                onSuccess = { predictions ->
                    _searchResults.value = predictions
                    Log.d(TAG, "🔍 Found ${predictions.size} search results")
                },
                onFailure = { error ->
                    _searchError.value = error.message
                    Log.e(TAG, "❌ Search error: ${error.message}")
                }
            )
            
            _isSearching.value = false
        }
    }
    
    // Select a place from search results (matching iOS)
    fun selectPlace(placeId: String) {
        viewModelScope.launch {
            _searchError.value = null
            
            placesService?.fetchPlaceDetails(placeId)?.fold(
                onSuccess = { details ->
                    val location = LatLng(details.latitude, details.longitude)
                    _searchLocation.value = location
                    _mapCenter.value = location
                    _selectedPlaceDetails.value = details
                    findNearestEmployee(location)
                    Log.d(TAG, "📍 Selected place: ${details.name}")
                },
                onFailure = { error ->
                    _searchError.value = error.message
                    Log.e(TAG, "❌ Place details error: ${error.message}")
                }
            )
        }
    }
    
    fun clearSearch() {
        _searchLocation.value = null
        _nearestEmployee.value = null
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _searchError.value = null
        _selectedPlaceDetails.value = null
    }
    
    // Get employee data for a given ID
    fun getEmployee(employeeId: String): Employee? {
        return employeeDataMap[employeeId]
    }
    
    // Admin: Force Checkout
    fun forceCheckout(employeeId: String) {
        viewModelScope.launch {
            try {
                firestoreService.forceCheckout(employeeId).onSuccess {
                    Log.d(TAG, "✅ Force checkout successful for $employeeId")
                    refresh() // Refresh map data
                }.onFailure { e ->
                    Log.e(TAG, "❌ Force checkout failed: ${e.message}")
                    _uiState.value = MapUiState.Error("Force checkout failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Force checkout exception: ${e.message}")
            }
        }
    }
    
    // Get sorted employees by distance from search location (matching iOS)
    fun getSortedEmployeesByDistance(): List<EmployeeLocation> {
        val searchLoc = _searchLocation.value ?: return _employeeLocations.value
        
        return _employeeLocations.value.sortedBy { employee ->
            calculateDistance(searchLoc, employee.position)
        }
    }
    
    // Calculate distance between two points (Haversine formula)
    private fun calculateDistance(from: LatLng, to: LatLng): Double {
        val earthRadius = 6371000.0 // meters
        
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(from.latitude)) * Math.cos(Math.toRadians(to.latitude)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c // distance in meters
    }
    
    companion object {
        private const val TAG = "MapViewModel"
    }
}

data class EmployeeLocation(
    val employeeId: String,
    val employeeName: String,
    val position: LatLng,
    val placeName: String?,
    val timestamp: java.util.Date,
    val role: EmployeeRole,
    val avatarUrl: String? = null,
    val checkInTime: java.util.Date? = null
)

sealed class MapUiState {
    object Loading : MapUiState()
    object Success : MapUiState()
    data class Error(val message: String) : MapUiState()
}

/**
 * Factory for creating MapViewModel with Context
 */
class MapViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
