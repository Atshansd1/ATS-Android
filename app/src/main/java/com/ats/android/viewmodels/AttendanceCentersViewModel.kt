package com.ats.android.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ats.android.models.AttendanceCenter
import com.ats.android.models.Employee
import com.ats.android.services.FirestoreService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.GeoPoint

class AttendanceCentersViewModel : ViewModel() {
    private val firestoreService = FirestoreService.getInstance()
    
    private val _centers = MutableStateFlow<List<AttendanceCenter>>(emptyList())
    val centers: StateFlow<List<AttendanceCenter>> = _centers.asStateFlow()

    private val _selectedCenter = MutableStateFlow<AttendanceCenter?>(null)
    val selectedCenter: StateFlow<AttendanceCenter?> = _selectedCenter.asStateFlow()
    
    // Using a list of all employees for selection
    private val _allEmployees = MutableStateFlow<List<Employee>>(emptyList())
    val allEmployees: StateFlow<List<Employee>> = _allEmployees.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadData()
    }
    
    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Load Centers
                val centerList = firestoreService.getAllAttendanceCenters()
                _centers.value = centerList
                Log.d(TAG, "Loaded ${centerList.size} centers")
                
                // Load Employees (active only)
                val employeeList = firestoreService.getAllEmployees().filter { it.isActive }
                _allEmployees.value = employeeList
                Log.d(TAG, "Loaded ${employeeList.size} active employees")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading data", e)
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCenter(centerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = firestoreService.getAttendanceCenter(centerId)
                result.onSuccess { center ->
                    _selectedCenter.value = center
                }.onFailure { e ->
                    _errorMessage.value = e.message
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createCenter(center: AttendanceCenter) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            try {
                val result = firestoreService.createAttendanceCenter(center)
                result.onSuccess {
                    _successMessage.value = "Attendance Center created successfully"
                    loadData() // Refresh list
                }.onFailure { e ->
                    _errorMessage.value = e.message
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateCenter(center: AttendanceCenter) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            try {
                val result = firestoreService.updateAttendanceCenter(center)
                result.onSuccess {
                    _successMessage.value = "Attendance Center updated successfully"
                    loadData() // Refresh list
                }.onFailure { e ->
                    _errorMessage.value = e.message
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteCenter(centerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = firestoreService.deleteAttendanceCenter(centerId)
                result.onSuccess {
                    _successMessage.value = "Attendance Center deleted"
                    // Update local state immediately for better UX
                    _centers.value = _centers.value.filter { it.id != centerId }
                }.onFailure { e ->
                    _errorMessage.value = e.message
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    companion object {
        private const val TAG = "AttendanceCentersVM"
    }
}
