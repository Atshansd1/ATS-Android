package com.ats.android.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ats.android.models.Employee
import com.ats.android.models.EmployeeRole
import com.ats.android.services.FirestoreService
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EmployeeManagementViewModel : ViewModel() {
    
    private val firestoreService = FirestoreService.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val _uiState = MutableStateFlow<EmployeeManagementUiState>(EmployeeManagementUiState.Loading)
    val uiState: StateFlow<EmployeeManagementUiState> = _uiState.asStateFlow()
    
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _filteredEmployees = MutableStateFlow<List<Employee>>(emptyList())
    val filteredEmployees: StateFlow<List<Employee>> = _filteredEmployees.asStateFlow()
    
    init {
        loadEmployees()
    }
    
    fun loadEmployees() {
        viewModelScope.launch {
            try {
                _uiState.value = EmployeeManagementUiState.Loading
                Log.d(TAG, "Loading employees...")
                
                val employees = firestoreService.getAllEmployees()
                _employees.value = employees
                _filteredEmployees.value = employees
                _uiState.value = EmployeeManagementUiState.Success
                
                Log.d(TAG, "Loaded ${employees.size} employees")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading employees: ${e.message}", e)
                _uiState.value = EmployeeManagementUiState.Error(e.message ?: "Failed to load employees")
            }
        }
    }
    
    fun searchEmployees(query: String) {
        _searchQuery.value = query
        _filteredEmployees.value = if (query.isBlank()) {
            _employees.value
        } else {
            _employees.value.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                (it.email?.contains(query, ignoreCase = true) == true) ||
                it.employeeId.contains(query, ignoreCase = true)
            }
        }
    }
    
    suspend fun addEmployee(
        nameEn: String,
        nameAr: String,
        email: String,
        phoneNumber: String,
        role: EmployeeRole,
        avatarUri: Uri?
    ): Result<String> {
        return try {
            Log.d(TAG, "Adding new employee: $nameEn")
            
            // Generate employee ID
            val employeeId = "EMP${System.currentTimeMillis()}"
            
            // Upload avatar if provided
            val avatarUrl = avatarUri?.let { uploadAvatar(employeeId, it) }
            
            // Create employee
            val employee = Employee(
                uid = "",
                employeeId = employeeId,
                englishName = nameEn,
                arabicName = nameAr,
                email = email,
                phoneNumber = phoneNumber,
                roleString = role.value,
                departmentEn = "General",
                departmentAr = "عام",
                isActive = true,
                avatarURL = avatarUrl,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            firestoreService.createEmployee(employee)
            loadEmployees()
            
            Log.d(TAG, "Employee added successfully: $employeeId")
            Result.success(employeeId)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding employee: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateEmployee(
        employeeId: String,
        nameEn: String,
        nameAr: String,
        email: String,
        phoneNumber: String,
        role: EmployeeRole,
        avatarUri: Uri?,
        currentAvatarUrl: String?
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Updating employee: $employeeId")
            
            // Upload new avatar if provided
            val avatarUrl = if (avatarUri != null) {
                uploadAvatar(employeeId, avatarUri)
            } else {
                currentAvatarUrl
            }
            
            // Update employee
            val updates = mapOf(
                "nameEn" to nameEn,
                "nameAr" to nameAr,
                "email" to email,
                "phoneNumber" to phoneNumber,
                "role" to role.value,
                "avatarURL" to avatarUrl,
                "updatedAt" to Timestamp.now()
            )
            
            firestoreService.updateEmployeeFields(employeeId, updates)
            loadEmployees()
            
            Log.d(TAG, "Employee updated successfully: $employeeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating employee: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteEmployee(employeeId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting employee: $employeeId")
            
            // Delete avatar from storage if exists
            val employee = _employees.value.find { it.employeeId == employeeId }
            employee?.avatarURL?.let { url ->
                try {
                    storage.getReferenceFromUrl(url).delete().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete avatar: ${e.message}")
                }
            }
            
            // Delete employee
            firestoreService.deleteEmployee(employeeId)
            loadEmployees()
            
            Log.d(TAG, "Employee deleted successfully: $employeeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting employee: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun toggleEmployeeStatus(employeeId: String, isActive: Boolean): Result<Unit> {
        return try {
            Log.d(TAG, "Toggling employee status: $employeeId -> $isActive")
            
            firestoreService.updateEmployeeFields(
                employeeId,
                mapOf(
                    "isActive" to isActive,
                    "updatedAt" to Timestamp.now()
                )
            )
            loadEmployees()
            
            Log.d(TAG, "Employee status updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating employee status: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun uploadAvatar(employeeId: String, uri: Uri): String {
        return try {
            val ref = storage.reference.child("avatars/$employeeId.jpg")
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            Log.d(TAG, "Avatar uploaded successfully: $url")
            url
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading avatar: ${e.message}", e)
            throw e
        }
    }
    
    companion object {
        private const val TAG = "EmployeeManagementVM"
    }
}

sealed class EmployeeManagementUiState {
    object Loading : EmployeeManagementUiState()
    object Success : EmployeeManagementUiState()
    data class Error(val message: String) : EmployeeManagementUiState()
}
