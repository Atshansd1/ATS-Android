package com.ats.android.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ats.android.models.*
import com.ats.android.services.FirestoreService
import com.ats.android.services.ImagePreloader
import com.ats.android.ui.theme.ATSColors
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.ats.android.R

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val firestoreService = FirestoreService.getInstance()
    private val context = application.applicationContext
    
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()
    
    private val _activeEmployees = MutableStateFlow<List<ActiveEmployeeInfo>>(emptyList())
    val activeEmployees: StateFlow<List<ActiveEmployeeInfo>> = _activeEmployees.asStateFlow()
    
    private val _recentActivity = MutableStateFlow<List<EmployeeActivity>>(emptyList())
    val recentActivity: StateFlow<List<EmployeeActivity>> = _recentActivity.asStateFlow()
    
    private val _activityItems = MutableStateFlow<List<com.ats.android.ui.screens.ActivityItem>>(emptyList())
    val activityItems: StateFlow<List<com.ats.android.ui.screens.ActivityItem>> = _activityItems.asStateFlow()
    
    private val _activeEmployeeItems = MutableStateFlow<List<com.ats.android.ui.screens.ActiveEmployeeItem>>(emptyList())
    val activeEmployeeItems: StateFlow<List<com.ats.android.ui.screens.ActiveEmployeeItem>> = _activeEmployeeItems.asStateFlow()
    
    init {
        Log.d(TAG, "🚀 DashboardViewModel initialized")
        loadDashboardData()
        // Set up real-time listeners
        Log.d(TAG, "🎧 Setting up real-time listeners...")
        startRealTimeListeners()
    }
    
    private fun startRealTimeListeners() {
        Log.d(TAG, "🎧 startRealTimeListeners() called")
        viewModelScope.launch {
            try {
                Log.d(TAG, "📡 Calling firestoreService.observeActiveLocations...")
                // Listen to active locations for real-time updates
                firestoreService.observeActiveLocations { locations ->
                    Log.d(TAG, "🔔 Real-time callback triggered with ${locations.size} locations")
                    viewModelScope.launch {
                        val activeEmployeesList = locations.map { pair ->
                            val employee = pair.first
                            val location = pair.second
                            val displayPlaceName = location.getLocalizedPlaceName() ?: location.placeName
                            Log.d(TAG, "   Processing: ${employee.displayName} at $displayPlaceName")
                            ActiveEmployeeInfo(
                                id = employee.employeeId,
                                name = employee.displayName,
                                department = employee.team,
                                checkInTime = formatTime(location.checkInTime.toDate()),
                                duration = formatDuration(location.checkInTime.toDate()),
                                placeName = displayPlaceName
                            )
                        }
                        _activeEmployees.value = activeEmployeesList
                        _activeEmployeeItems.value = locations.map { pair ->
                            val employee = pair.first
                            val location = pair.second
                            val displayPlaceName = location.getLocalizedPlaceName() ?: location.placeName
                            com.ats.android.ui.screens.ActiveEmployeeItem(
                                id = employee.employeeId,
                                name = employee.displayName,
                                department = employee.team,
                                checkInTime = formatTime(location.checkInTime.toDate()),
                                duration = formatDuration(location.checkInTime.toDate()),
                                placeName = displayPlaceName,
                                avatarUrl = employee.avatarURL,
                                role = employee.role
                            )
                        }
                        
                        // Update stats
                        val currentStats = _stats.value
                        _stats.value = currentStats.copy(
                            activeNow = activeEmployeesList.size
                        )
                        
                        Log.d(TAG, "🔄 Real-time update complete: ${activeEmployeesList.size} active employees, stats updated")
                    }
                }
                Log.d(TAG, "✅ Real-time listener registered successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error setting up real-time listeners: ${e.message}", e)
            }
        }
    }
    
    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.value = DashboardUiState.Loading
                
                Log.d(TAG, "📊 Loading dashboard data...")
                
                // Load all employees
                val allEmployees = firestoreService.getAllEmployees()
                Log.d(TAG, "✅ Loaded ${allEmployees.size} employees")
                
                // Preload employee avatars in background for faster display
                ImagePreloader.preloadEmployeeAvatars(context, allEmployees)
                
                // DON'T load active locations here - real-time listener handles it
                // Loading here was overwriting real-time data with stale data
                Log.d(TAG, "⏭️ Skipping active locations load - real-time listener active")
                
                // Load recent activity (last 10 check-ins/check-outs)
                loadRecentActivity()
                
                // Calculate stats (use real-time listener data for active count)
                val activeCount = _activeEmployees.value.size
                val totalCount = allEmployees.size
                val onLeaveCount = 0 // TODO: Implement leave tracking
                val checkedInToday = activeCount // For now, same as active
                
                _stats.value = DashboardStats(
                    activeNow = activeCount,
                    totalEmployees = totalCount,
                    onLeave = onLeaveCount,
                    checkedInToday = checkedInToday
                )
                
                _uiState.value = DashboardUiState.Success
                Log.d(TAG, "✅ Dashboard data loaded successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading dashboard data: ${e.message}", e)
                _uiState.value = DashboardUiState.Error(e.message ?: "Failed to load data")
            }
        }
    }

    private suspend fun loadRecentActivity() {
        try {
            // Get recent attendance records to build activity feed
            val recentRecords = firestoreService.getRecentAttendance(limit = 10)
            val allEmployees = firestoreService.getAllEmployees()
            
            val activities = mutableListOf<EmployeeActivity>()
            
            recentRecords.forEach { record ->
                val employee = allEmployees.find { it.employeeId == record.employeeId }
                
                // Add check-in activity
                if (record.checkInTime != null) {
                    activities.add(
                        EmployeeActivity(
                            employeeId = record.employeeId,
                            employeeName = employee?.displayName ?: record.employeeId,
                            action = context.getString(R.string.action_checked_in),
                            timestamp = record.checkInTime,
                            type = ActivityType.CHECK_IN
                        )
                    )
                }
                
                // Add check-out activity if exists
                if (record.checkOutTime != null) {
                    activities.add(
                        EmployeeActivity(
                            employeeId = record.employeeId,
                            employeeName = employee?.displayName ?: record.employeeId,
                            action = context.getString(R.string.action_checked_out),
                            timestamp = record.checkOutTime,
                            type = ActivityType.CHECK_OUT
                        )
                    )
                }
            }
            
            // Sort by timestamp descending
            _recentActivity.value = activities.sortedByDescending { it.timestamp.toDate() }.take(10)
            
            // Convert to iOS-style activity items
            _activityItems.value = activities.sortedByDescending { it.timestamp.toDate() }.take(10).map { activity ->
                com.ats.android.ui.screens.ActivityItem(
                    employeeName = activity.employeeName,
                    action = activity.action,
                    timeAgo = getRelativeTimeString(activity.timestamp.toDate()),
                    icon = when (activity.type) {
                        ActivityType.CHECK_IN -> Icons.Default.ArrowCircleDown
                        ActivityType.CHECK_OUT -> Icons.Default.ArrowCircleUp
                        ActivityType.STATUS_CHANGE -> Icons.Default.Edit
                    },
                    iconColor = when (activity.type) {
                        ActivityType.CHECK_IN -> ATSColors.CheckInGreen
                        ActivityType.CHECK_OUT -> ATSColors.CheckOutBlue
                        ActivityType.STATUS_CHANGE -> ATSColors.StatusChangeOrange
                    }
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading recent activity: ${e.message}", e)
        }
    }
    
    private fun getRelativeTimeString(date: Date): String {
        val now = Date()
        val diff = now.time - date.time
        
        return when {
            diff < 60_000 -> context.getString(R.string.time_just_now)
            diff < 3600_000 -> context.getString(R.string.time_ago_m, diff / 60_000)
            diff < 86400_000 -> context.getString(R.string.time_ago_h, diff / 3600_000)
            else -> context.getString(R.string.time_ago_d, diff / 86400_000)
        }
    }
    
    fun refresh() {
        loadDashboardData()
    }
    
    companion object {
        private const val TAG = "DashboardViewModel"
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    object Success : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
