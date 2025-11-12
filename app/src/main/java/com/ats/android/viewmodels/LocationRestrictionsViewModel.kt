package com.ats.android.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ats.android.models.*
import com.ats.android.services.FirestoreService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationRestrictionsViewModel : ViewModel() {
    private val firestoreService = FirestoreService.getInstance()
    
    private val _restrictionType = MutableStateFlow(LocationRestrictionType.ANYWHERE)
    val restrictionType: StateFlow<LocationRestrictionType> = _restrictionType.asStateFlow()
    
    private val _allowedLocations = MutableStateFlow<List<AllowedLocation>>(emptyList())
    val allowedLocations: StateFlow<List<AllowedLocation>> = _allowedLocations.asStateFlow()
    
    private val _appliesToAllEmployees = MutableStateFlow(true)
    val appliesToAllEmployees: StateFlow<Boolean> = _appliesToAllEmployees.asStateFlow()
    
    private val _selectedEmployeeIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedEmployeeIds: StateFlow<Set<String>> = _selectedEmployeeIds.asStateFlow()
    
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    private var currentConfig: CheckInLocationConfig? = null
    
    init {
        Log.d(TAG, "LocationRestrictionsViewModel initialized")
    }
    
    fun loadConfiguration() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📥 Loading configuration...")
                _isLoading.value = true
                _errorMessage.value = null
                
                // Load employees
                val employeesList = firestoreService.getAllEmployees()
                    .filter { it.isActive }
                Log.d(TAG, "Loaded ${employeesList.size} active employees")
                _employees.value = employeesList
                
                // Load configuration
                val result = firestoreService.getCheckInLocationConfig()
                result.onSuccess { config ->
                    if (config != null) {
                        currentConfig = config
                        _restrictionType.value = config.type
                        _allowedLocations.value = config.allowedLocations
                        _appliesToAllEmployees.value = config.appliesToAllEmployees()
                        _selectedEmployeeIds.value = config.applicableEmployeeIds.toSet()
                        
                        Log.d(TAG, "✅ Loaded config: ${config.type}, applies to all employees: ${config.appliesToAllEmployees()}")
                    } else {
                        // Create default configuration
                        Log.d(TAG, "📝 Creating default configuration")
                        _restrictionType.value = LocationRestrictionType.ANYWHERE
                        _allowedLocations.value = emptyList()
                        _appliesToAllEmployees.value = true
                        _selectedEmployeeIds.value = emptySet()
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ Error loading configuration", error)
                    _errorMessage.value = error.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading configuration", e)
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun saveConfiguration() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Saving configuration...")
                Log.d(TAG, "  - Type: ${_restrictionType.value}")
                Log.d(TAG, "  - Locations: ${_allowedLocations.value.size}")
                Log.d(TAG, "  - Applies to all: ${_appliesToAllEmployees.value}")
                Log.d(TAG, "  - Selected employees: ${_selectedEmployeeIds.value.size}")
                
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null
                
                val config = CheckInLocationConfig(
                    id = currentConfig?.id,
                    name = "Check-In Policy",
                    type = _restrictionType.value,
                    allowedLocations = _allowedLocations.value,
                    applicableEmployeeIds = if (_appliesToAllEmployees.value) emptyList() else _selectedEmployeeIds.value.toList(),
                    isActive = true,
                    createdAt = currentConfig?.createdAt,
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                
                Log.d(TAG, "📝 Saving to Firestore...")
                val result = firestoreService.saveCheckInLocationConfig(config)
                
                result.onSuccess {
                    currentConfig = config
                    Log.d(TAG, "✅ Configuration saved successfully!")
                    Log.d(TAG, "  - Saved type: ${config.type}")
                    Log.d(TAG, "  - Saved locations: ${config.allowedLocations.size}")
                    Log.d(TAG, "  - Saved employee IDs: ${config.applicableEmployeeIds.size}")
                    _successMessage.value = "Configuration saved successfully"
                }.onFailure { error ->
                    Log.e(TAG, "❌ Error saving configuration", error)
                    _errorMessage.value = error.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving configuration", e)
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateRestrictionType(type: LocationRestrictionType) {
        Log.d(TAG, "📝 Updating restriction type: $type")
        _restrictionType.value = type
        
        // Clear locations if switching to ANYWHERE
        if (type == LocationRestrictionType.ANYWHERE) {
            _allowedLocations.value = emptyList()
        }
        // Keep only first location if switching to SPECIFIC
        else if (type == LocationRestrictionType.SPECIFIC && _allowedLocations.value.size > 1) {
            _allowedLocations.value = listOf(_allowedLocations.value.first())
        }
    }
    
    fun addLocation(location: AllowedLocation) {
        Log.d(TAG, "➕ Adding location: ${location.name}")
        when (_restrictionType.value) {
            LocationRestrictionType.SPECIFIC -> {
                // Replace existing location for specific type
                _allowedLocations.value = listOf(location)
                Log.d(TAG, "  Replaced (specific location policy)")
            }
            LocationRestrictionType.MULTIPLE -> {
                // Add to list for multiple type
                _allowedLocations.value = _allowedLocations.value + location
                Log.d(TAG, "  Added to list (total: ${_allowedLocations.value.size})")
            }
            LocationRestrictionType.ANYWHERE -> {
                Log.d(TAG, "  Ignored (anywhere policy)")
            }
        }
    }
    
    fun removeLocation(locationId: String) {
        Log.d(TAG, "➖ Removing location: $locationId")
        _allowedLocations.value = _allowedLocations.value.filter { it.id != locationId }
    }
    
    fun updateLocationRadius(locationId: String, newRadius: Double) {
        Log.d(TAG, "📏 Updating location radius: $locationId -> $newRadius meters")
        _allowedLocations.value = _allowedLocations.value.map { location ->
            if (location.id == locationId) {
                location.copy(radius = newRadius)
            } else {
                location
            }
        }
    }
    
    fun toggleAppliesToAllEmployees() {
        _appliesToAllEmployees.value = !_appliesToAllEmployees.value
        Log.d(TAG, "👥 Applies to all employees: ${_appliesToAllEmployees.value}")
        
        // Clear selected employees if switching to all
        if (_appliesToAllEmployees.value) {
            _selectedEmployeeIds.value = emptySet()
        }
    }
    
    fun toggleEmployeeSelection(employeeId: String) {
        val currentSelection = _selectedEmployeeIds.value.toMutableSet()
        if (currentSelection.contains(employeeId)) {
            currentSelection.remove(employeeId)
            Log.d(TAG, "❌ Removed employee: $employeeId")
        } else {
            currentSelection.add(employeeId)
            Log.d(TAG, "✅ Added employee: $employeeId")
        }
        _selectedEmployeeIds.value = currentSelection
        Log.d(TAG, "👥 Total selected: ${currentSelection.size}")
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearSuccess() {
        _successMessage.value = null
    }
    
    companion object {
        private const val TAG = "LocationRestrictionsVM"
    }
}
