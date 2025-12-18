package com.ats.android.models

import java.util.Date

/**
 * Filter for Analytics Data
 */
enum class AnalyticsFilter(val days: Int) {
    LAST_WEEK(7),
    LAST_MONTH(30),
    LAST_3_MONTHS(90);
    
    val startDate: Date
        get() {
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
            return calendar.time
        }
        
    val endDate: Date
        get() = Date()
}

/**
 * Aggregated Dashboard Metrics
 */
data class DashboardMetrics(
    val totalEmployees: Int,
    val activeToday: Int,
    val averageWorkHours: Double,
    val attendanceRate: Double,
    val topLocations: List<LocationStats>,
    val hourlyActivity: List<HourlyStats>,
    val dailyTrends: List<DailyTrend>
)

data class LocationStats(
    val id: String,
    val locationName: String,
    val checkInCount: Int,
    val percentage: Double
)

data class HourlyStats(
    val hour: Int,
    val count: Int
)

data class DailyTrend(
    val date: Date,
    val checkIns: Int,
    val avgHours: Double,
    val attendanceRate: Double
)
