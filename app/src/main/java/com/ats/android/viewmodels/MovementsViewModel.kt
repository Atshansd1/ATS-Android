package com.ats.android.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ats.android.models.LocationMovement
import com.ats.android.models.MovementType
import com.ats.android.services.FirestoreService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovementsViewModel : ViewModel() {
    
    private val firestoreService = FirestoreService.getInstance()
    
    private val _uiState = MutableStateFlow<MovementsUiState>(MovementsUiState.Loading)
    val uiState: StateFlow<MovementsUiState> = _uiState.asStateFlow()
    
    private val _movements = MutableStateFlow<List<LocationMovement>>(emptyList())
    val movements: StateFlow<List<LocationMovement>> = _movements.asStateFlow()
    
    private val _selectedEmployeeId = MutableStateFlow<String?>(null)
    val selectedEmployeeId: StateFlow<String?> = _selectedEmployeeId.asStateFlow()
    
    init {
        Log.d(TAG, "🚀 MovementsViewModel initialized")
        startListening(null)
    }
    
    fun startListening(employeeId: String?) {
        _selectedEmployeeId.value = employeeId
        Log.d(TAG, "📍 Starting real-time movements listener for: ${employeeId ?: "all employees"}")
        
        viewModelScope.launch {
            try {
                _uiState.value = MovementsUiState.Loading
                
                // Listen to today's movements with real-time updates
                firestoreService.observeTodayMovements(employeeId) { movementsList ->
                    viewModelScope.launch {
                        Log.d(TAG, "🔔 Received ${movementsList.size} movements")
                        _movements.value = movementsList
                        _uiState.value = if (movementsList.isEmpty()) {
                            MovementsUiState.Empty
                        } else {
                            MovementsUiState.Success
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error starting movements listener: ${e.message}", e)
                _uiState.value = MovementsUiState.Error(e.message ?: "Failed to load movements")
            }
        }
    }
    
    fun filterByEmployee(employeeId: String?) {
        startListening(employeeId)
    }
    
    companion object {
        private const val TAG = "MovementsViewModel"
    }
}

sealed class MovementsUiState {
    object Loading : MovementsUiState()
    object Success : MovementsUiState()
    object Empty : MovementsUiState()
    data class Error(val message: String) : MovementsUiState()
}
