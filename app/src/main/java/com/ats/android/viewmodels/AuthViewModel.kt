package com.ats.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ats.android.models.Employee
import com.ats.android.services.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    
    private val authService = AuthService.getInstance()
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _currentEmployee = MutableStateFlow<Employee?>(null)
    val currentEmployee: StateFlow<Employee?> = _currentEmployee.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    private fun checkAuthState() {
        viewModelScope.launch {
            if (authService.isAuthenticated) {
                val employee = authService.loadCurrentEmployee()
                if (employee != null) {
                    _currentEmployee.value = employee
                    _uiState.value = AuthUiState.Authenticated(employee)
                } else {
                    _uiState.value = AuthUiState.Unauthenticated
                }
            } else {
                _uiState.value = AuthUiState.Unauthenticated
            }
        }
    }
    
    fun signIn(emailOrEmployeeId: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = authService.signIn(emailOrEmployeeId, password)
            
            _uiState.value = if (result.isSuccess) {
                val employee = result.getOrNull()!!
                _currentEmployee.value = employee
                AuthUiState.Authenticated(employee)
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Sign in failed")
            }
        }
    }
    
    fun signOut() {
        authService.signOut()
        _currentEmployee.value = null
        _uiState.value = AuthUiState.Unauthenticated
    }
}

sealed class AuthUiState {
    object Loading : AuthUiState()
    object Unauthenticated : AuthUiState()
    data class Authenticated(val employee: Employee) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
