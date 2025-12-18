package com.ats.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Professional iOS-style color scheme
 * Minimal, clean, and business-appropriate
 */

// Light Theme - Professional & Minimal
object ProfessionalLightColors {
    // Primary - iOS Blue (subtle, not vibrant)
    val Primary = Color(0xFF007AFF)  // iOS Blue
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFE5F1FF)
    val OnPrimaryContainer = Color(0xFF001D35)
    
    // Secondary - Subtle Gray-Blue
    val Secondary = Color(0xFF535E6C)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFE3E8EF)
    val OnSecondaryContainer = Color(0xFF101C2B)
    
    // Tertiary - Minimal Accent (rarely used)
    val Tertiary = Color(0xFF6B5D7E)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFF2E9FF)
    val OnTertiaryContainer = Color(0xFF251638)
    
    // Backgrounds - Clean & Neutral
    val Background = Color(0xFFFCFCFC)  // Almost white
    val OnBackground = Color(0xFF1A1C1E)
    val Surface = Color(0xFFFFFFFF)  // Pure white cards
    val OnSurface = Color(0xFF1A1C1E)
    val SurfaceVariant = Color(0xFFF5F5F5)  // Very light gray
    val OnSurfaceVariant = Color(0xFF43474E)
    
    // Outline - Subtle borders
    val Outline = Color(0xFFE0E0E0)  // Light gray
    val OutlineVariant = Color(0xFFF0F0F0)
    
    // Semantic Colors - Professional
    val Success = Color(0xFF34C759)  // iOS Green
    val OnSuccess = Color(0xFFFFFFFF)
    val SuccessContainer = Color(0xFFE8F7ED)
    val OnSuccessContainer = Color(0xFF00210A)
    
    val Warning = Color(0xFFFF9500)  // iOS Orange
    val OnWarning = Color(0xFFFFFFFF)
    val WarningContainer = Color(0xFFFFEDD5)
    val OnWarningContainer = Color(0xFF2E1500)
    
    val Error = Color(0xFFFF3B30)  // iOS Red
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF410002)
    
    // Information
    val Info = Color(0xFF007AFF)  // Same as primary
    val OnInfo = Color(0xFFFFFFFF)
    val InfoContainer = Color(0xFFE5F1FF)
    val OnInfoContainer = Color(0xFF001D35)
}

// Dark Theme - Professional & Minimal
object ProfessionalDarkColors {
    // Primary - Lighter Blue for dark theme
    val Primary = Color(0xFF409CFF)  // iOS Blue (lighter)
    val OnPrimary = Color(0xFF003A70)
    val PrimaryContainer = Color(0xFF00519D)
    val OnPrimaryContainer = Color(0xFFD5E8FF)
    
    // Secondary - Light Gray
    val Secondary = Color(0xFFC4CAD4)
    val OnSecondary = Color(0xFF2E3641)
    val SecondaryContainer = Color(0xFF444D58)
    val OnSecondaryContainer = Color(0xFFE0E7F0)
    
    // Tertiary - Subtle Purple
    val Tertiary = Color(0xFFDDC2F3)
    val OnTertiary = Color(0xFF3B2C4E)
    val TertiaryContainer = Color(0xFF524266)
    val OnTertiaryContainer = Color(0xFFF9E9FF)
    
    // Backgrounds - iOS Standard Dark Mode
    val Background = Color(0xFF000000)  // Pure Black (Standard iOS Background)
    val OnBackground = Color(0xFFFFFFFF)
    val Surface = Color(0xFF1C1C1E)  // iOS Secondary System Background (Cards)
    val OnSurface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFF2C2C2E)  // iOS Tertiary System Background
    val OnSurfaceVariant = Color(0xFF8E8E93)
    
    // Outline - Subtle in dark theme
    val Outline = Color(0xFF48484A)
    val OutlineVariant = Color(0xFF3A3A3C)
    
    // Semantic Colors - Adjusted for dark theme
    val Success = Color(0xFF32D74B)  // iOS Green (lighter)
    val OnSuccess = Color(0xFF003911)
    val SuccessContainer = Color(0xFF005219)
    val OnSuccessContainer = Color(0xFFB5F3C6)
    
    val Warning = Color(0xFFFF9F0A)  // iOS Orange (lighter)
    val OnWarning = Color(0xFF4D2800)
    val WarningContainer = Color(0xFF6B3A00)
    val OnWarningContainer = Color(0xFFFFDDB3)
    
    val Error = Color(0xFFFF453A)  // iOS Red (lighter)
    val OnError = Color(0xFF680003)
    val ErrorContainer = Color(0xFF930006)
    val OnErrorContainer = Color(0xFFFFDAD5)
    
    // Information
    val Info = Color(0xFF409CFF)
    val OnInfo = Color(0xFF003A70)
    val InfoContainer = Color(0xFF00519D)
    val OnInfoContainer = Color(0xFFD5E8FF)
}

// Role-specific colors (neutral)
object ProfessionalRoleColors {
    val Admin = Color(0xFF5856D6)  // iOS Purple
    val Supervisor = Color(0xFF007AFF)  // iOS Blue
    val Employee = Color(0xFF34C759)  // iOS Green
}

// Status colors (minimal)
object ProfessionalStatusColors {
    val Online = Color(0xFF34C759)  // Green
    val Offline = Color(0xFF8E8E93)  // Gray
    val Away = Color(0xFFFF9500)  // Orange
}
