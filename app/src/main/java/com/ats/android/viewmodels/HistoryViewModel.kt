package com.ats.android.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ats.android.models.AttendanceRecord
import com.ats.android.models.Employee
import com.ats.android.services.FirestoreService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class HistoryViewModel : ViewModel() {
    
    private val firestoreService = FirestoreService.getInstance()
    
    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()
    
    private val _startDate = MutableStateFlow(getStartOfMonth())
    val startDate: StateFlow<Date> = _startDate.asStateFlow()
    
    private val _endDate = MutableStateFlow(Date())
    val endDate: StateFlow<Date> = _endDate.asStateFlow()
    
    fun loadHistory(employee: Employee?) {
        if (employee == null) {
            _uiState.value = HistoryUiState.Error("Employee not found")
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.value = HistoryUiState.Loading
                Log.d(TAG, "📋 Loading attendance history for: ${employee.displayName}")
                
                val records = firestoreService.getAttendanceHistory(
                    employeeId = employee.employeeId,
                    startDate = _startDate.value,
                    endDate = _endDate.value
                )
                
                _attendanceRecords.value = records
                _uiState.value = HistoryUiState.Success
                
                Log.d(TAG, "✅ Loaded ${records.size} attendance records")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading history: ${e.message}", e)
                _uiState.value = HistoryUiState.Error(e.message ?: "Failed to load history")
            }
        }
    }
    
    fun setDateRange(start: Date, end: Date, employee: Employee?) {
        _startDate.value = start
        _endDate.value = end
        loadHistory(employee)
    }
    
    fun refresh(employee: Employee?) {
        loadHistory(employee)
    }
    
    private fun getStartOfMonth(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    companion object {
        private const val TAG = "HistoryViewModel"
    }
}

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    object Success : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}
