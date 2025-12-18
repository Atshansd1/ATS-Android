package com.ats.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ats.android.models.*
import com.ats.android.services.FirestoreService
import com.ats.android.utils.DebugLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class AnalyticsViewModel : ViewModel() {
    private val firestoreService = FirestoreService.getInstance()
    private val TAG = "AnalyticsViewModel"

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AnalyticsFilter.LAST_WEEK)
    val selectedFilter: StateFlow<AnalyticsFilter> = _selectedFilter.asStateFlow()

    private val _metrics = MutableStateFlow<DashboardMetrics?>(null)
    val metrics: StateFlow<DashboardMetrics?> = _metrics.asStateFlow()

    init {
        loadAnalytics()
    }

    fun updateFilter(filter: AnalyticsFilter) {
        _selectedFilter.value = filter
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.value = AnalyticsUiState.Loading
            try {
                val filter = _selectedFilter.value
                DebugLogger.d(TAG, "Loading analytics for filter: ${filter.name}")

                // 1. Get all employees (excluding Admins usually, matching iOS 'includeAdmins: false')
                val employees = firestoreService.getAllEmployees().filter { it.role != EmployeeRole.ADMIN }
                val totalEmployees = employees.size
                val validEmployeeIds = employees.map { it.employeeId }

                // 2. Get attendance records based on filter
                val records = firestoreService.getAllAttendanceRecords(filter.startDate, filter.endDate)

                // 3. Calculate metrics
                
                // Active Today
                val activeLocations = firestoreService.getActiveLocations()
                // Filter to only include valid employees (non-admins)
                val activeToday = activeLocations.count { validEmployeeIds.contains(it.first.employeeId) }

                // Average Work Hours
                val avgWorkHours = calculateAverageWorkHours(records)

                // Attendance Rate
                val attendanceRate = calculateAttendanceRate(records, totalEmployees, filter)

                // Top Locations
                val topLocations = calculateTopLocations(records)

                // Hourly Activity
                val hourlyActivity = calculateHourlyActivity(records)

                // Daily Trends
                val dailyTrends = calculateDailyTrends(records, filter)

                val newMetrics = DashboardMetrics(
                    totalEmployees = totalEmployees,
                    activeToday = activeToday,
                    averageWorkHours = avgWorkHours,
                    attendanceRate = attendanceRate,
                    topLocations = topLocations,
                    hourlyActivity = hourlyActivity,
                    dailyTrends = dailyTrends
                )

                _metrics.value = newMetrics
                _uiState.value = AnalyticsUiState.Success
                DebugLogger.d(TAG, "Analytics loaded successfully")

            } catch (e: Exception) {
                DebugLogger.e(TAG, "Error loading analytics", e)
                _uiState.value = AnalyticsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun refreshData() {
        loadAnalytics()
    }

    private fun calculateAverageWorkHours(records: List<AttendanceRecord>): Double {
        val completedRecords = records.filter { it.totalDuration != null }
        val totalSeconds = completedRecords.sumOf { it.totalDuration ?: 0.0 }
        
        return if (completedRecords.isNotEmpty()) {
            (totalSeconds / 3600.0) / completedRecords.size
        } else {
            0.0
        }
    }

    private fun calculateAttendanceRate(records: List<AttendanceRecord>, totalEmployees: Int, filter: AnalyticsFilter): Double {
        if (totalEmployees == 0) return 0.0
        
        val actualCheckIns = records.size
        // Ensure at least 1 day to avoid division by zero or weird logic
        val days = filter.days.coerceAtLeast(1)
        val expectedCheckIns = totalEmployees * days
        
        return if (expectedCheckIns > 0) {
            (actualCheckIns.toDouble() / expectedCheckIns.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    private fun calculateTopLocations(records: List<AttendanceRecord>): List<LocationStats> {
        // Group by place name
        val locationCounts = records
            .mapNotNull { it.checkInPlaceName?.ifEmpty { null } } // Filter null or empty names
            .groupingBy { it }
            .eachCount()
        
        val totalRecords = records.size
        
        return locationCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (name, count) ->
                LocationStats(
                    id = name,
                    locationName = name,
                    checkInCount = count,
                    percentage = if (totalRecords > 0) (count.toDouble() / totalRecords) * 100.0 else 0.0
                )
            }
    }

    private fun calculateHourlyActivity(records: List<AttendanceRecord>): List<HourlyStats> {
        val calendar = Calendar.getInstance()
        
        val hourCounts = records.groupingBy { record ->
            record.checkInTime?.toDate()?.let { date ->
                calendar.time = date
                calendar.get(Calendar.HOUR_OF_DAY)
            } ?: 0
        }.eachCount()
        
        // Return list for all 24 hours (0..23)
        return (0..23).map { hour ->
            HourlyStats(hour, hourCounts[hour] ?: 0)
        }
    }

    private fun calculateDailyTrends(records: List<AttendanceRecord>, filter: AnalyticsFilter): List<DailyTrend> {
        val calendar = Calendar.getInstance()
        
        // Group by Day (reset time to midnight)
        val dailyRecords = records.groupBy { record ->
            record.checkInTime?.toDate()?.let { date ->
                calendar.time = date
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.time
            } ?: Date(0) // Should not happen with current data, but fallback
        }
        
        // Transform to DailyTrend
        return dailyRecords.map { (date, dayRecords) ->
            val checkIns = dayRecords.size
            
            val completedRecs = dayRecords.filter { it.totalDuration != null }
            val totalSeconds = completedRecs.sumOf { it.totalDuration ?: 0.0 }
            val avgHours = if (completedRecs.isNotEmpty()) (totalSeconds / 3600.0) / completedRecs.size else 0.0
            
            DailyTrend(
                date = date,
                checkIns = checkIns,
                avgHours = avgHours,
                attendanceRate = 100.0 // Simplified as per iOS logic comment
            )
        }.sortedBy { it.date }
    }
}

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    object Success : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}
