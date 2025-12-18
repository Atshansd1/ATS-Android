package com.ats.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ats.android.models.*
import com.ats.android.services.FirestoreService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class LeaveViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreService = FirestoreService.getInstance()
    
    private val _myRequests = MutableStateFlow<List<LeaveRequest>>(emptyList())
    val myRequests: StateFlow<List<LeaveRequest>> = _myRequests.asStateFlow()
    
    private val _pendingRequests = MutableStateFlow<List<LeaveRequest>>(emptyList())
    val pendingRequests: StateFlow<List<LeaveRequest>> = _pendingRequests.asStateFlow()
    
    private val _leaveBalance = MutableStateFlow<LeaveBalance?>(null)
    val leaveBalance: StateFlow<LeaveBalance?> = _leaveBalance.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun loadMyRequests(employeeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _myRequests.value = firestoreService.getMyLeaveRequests(employeeId)
            _isLoading.value = false
        }
    }
    
    fun loadPendingRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            _pendingRequests.value = firestoreService.getPendingRequests()
            _isLoading.value = false
        }
    }
    
    fun loadBalance(employeeId: String) {
        viewModelScope.launch {
            _leaveBalance.value = firestoreService.getLeaveBalance(employeeId)
        }
    }
    
    fun submitRequest(
        employeeId: String,
        employeeName: String,
        leaveType: LeaveType,
        startDate: Date,
        endDate: Date,
        reason: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = firestoreService.submitLeaveRequest(
                employeeId, employeeName, leaveType, startDate, endDate, reason
            )
            
            if (result.isSuccess) {
                _successMessage.value = "Leave request submitted successfully"
                loadMyRequests(employeeId)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to submit request"
            }
            _isLoading.value = false
        }
    }
    
    fun approveRequest(request: LeaveRequest, reviewerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = firestoreService.approveLeaveRequest(request.id!!, reviewerId, null)
            
            if (result.isSuccess) {
                // Update balance
                val days = request.getNumberOfDays()
                firestoreService.updateLeaveBalance(
                    request.employeeId,
                    java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                    request.getLeaveType(),
                    days
                )
                
                _successMessage.value = "Request approved"
                loadPendingRequests()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to approve request"
            }
            _isLoading.value = false
        }
    }
    
    fun rejectRequest(request: LeaveRequest, reviewerId: String, notes: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = firestoreService.rejectLeaveRequest(request.id!!, reviewerId, notes)
            
            if (result.isSuccess) {
                _successMessage.value = "Request rejected"
                loadPendingRequests()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to reject request"
            }
            _isLoading.value = false
        }
    }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun getLeaveSummaries(): List<LeaveSummary> {
        val balance = _leaveBalance.value ?: return emptyList()
        return listOf(
            LeaveSummary(LeaveType.VACATION, balance.vacationTotal, balance.vacationUsed, balance.vacationRemaining()),
            LeaveSummary(LeaveType.SICK, balance.sickTotal, balance.sickUsed, balance.sickRemaining()),
            LeaveSummary(LeaveType.PERSONAL, balance.personalTotal, balance.personalUsed, balance.personalRemaining())
        )
    }
}
