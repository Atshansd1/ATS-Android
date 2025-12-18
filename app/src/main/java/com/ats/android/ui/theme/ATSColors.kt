package com.ats.android.ui.theme

import androidx.compose.ui.graphics.Color
import com.ats.android.models.EmployeeRole

/**
 * iOS-matched color palette for ATS Android
 * All colors taken directly from iOS app design
 */
object ATSColors {
    // Role Colors (matches iOS exactly)
    val AdminPurple = Color(0xFF9C27B0)
    val SupervisorBlue = Color(0xFF2196F3)
    val EmployeeGreen = Color(0xFF4CAF50)
    
    // Activity Type Colors
    val CheckInGreen = Color(0xFF4CAF50)
    val CheckOutBlue = Color(0xFF2196F3)
    val StatusChangeOrange = Color(0xFFFF9800)
    
    // Status Colors
    val ActiveDot = Color(0xFF4CAF50)
    val InactiveDot = Color(0xFF9E9E9E)
    
    // Summary Card Colors
    val ActiveNowGreen = Color(0xFF4CAF50)
    val TotalEmployeesBlue = Color(0xFF2196F3)
    val OnLeaveOrange = Color(0xFFFF9800)
    val TodayCheckInsPurple = Color(0xFF9C27B0)
    
    // UI Elements
    val GlassBackground = Color(0xFFFAFAFA).copy(alpha = 0.95f)
    val DividerColor = Color(0xFFE0E0E0)
    val TimelineRed = Color(0xFFF44336)
}

/**
 * Get role-specific color
 */
fun getRoleColor(role: EmployeeRole): Color {
    return when (role) {
        EmployeeRole.ADMIN -> ATSColors.AdminPurple
        EmployeeRole.SUPERVISOR -> ATSColors.SupervisorBlue
        EmployeeRole.EMPLOYEE -> ATSColors.EmployeeGreen
    }
}
