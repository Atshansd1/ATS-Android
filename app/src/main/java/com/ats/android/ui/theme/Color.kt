package com.ats.android.ui.theme

import androidx.compose.ui.graphics.Color

// Expressive Material Design 3 - ATS Brand Colors (Light)
val ExpressiveLightPrimary = Color(0xFF2563EB) // Vibrant blue for primary actions
val ExpressiveLightOnPrimary = Color(0xFFFFFFFF)
val ExpressiveLightPrimaryContainer = Color(0xFFDDE8FF)
val ExpressiveLightOnPrimaryContainer = Color(0xFF001A41)

val ExpressiveLightSecondary = Color(0xFF7C3AED) // Vibrant purple for secondary actions
val ExpressiveLightOnSecondary = Color(0xFFFFFFFF)
val ExpressiveLightSecondaryContainer = Color(0xFFE9D5FF)
val ExpressiveLightOnSecondaryContainer = Color(0xFF2D1064)

val ExpressiveLightTertiary = Color(0xFFEC4899) // Vibrant pink for tertiary accents
val ExpressiveLightOnTertiary = Color(0xFFFFFFFF)
val ExpressiveLightTertiaryContainer = Color(0xFFFFD4E6)
val ExpressiveLightOnTertiaryContainer = Color(0xFF4A0722)

// Expressive Surface Colors
val ExpressiveLightSurface = Color(0xFFFAFBFC)
val ExpressiveLightOnSurface = Color(0xFF1C1B1F)
val ExpressiveLightSurfaceVariant = Color(0xFFE7E0EC)
val ExpressiveLightOnSurfaceVariant = Color(0xFF49454F)
val ExpressiveLightSurfaceTint = ExpressiveLightPrimary
val ExpressiveLightOutlineVariant = Color(0xFFCAC4D0)

// Expressive Material Design 3 - ATS Brand Colors (Dark)
val ExpressiveDarkPrimary = Color(0xFF93C5FD) // Soft blue for dark theme
val ExpressiveDarkOnPrimary = Color(0xFF002D6B)
val ExpressiveDarkPrimaryContainer = Color(0xFF004197)
val ExpressiveDarkOnPrimaryContainer = Color(0xFFDDE8FF)

val ExpressiveDarkSecondary = Color(0xFFD8B4FE) // Soft purple for dark theme
val ExpressiveDarkOnSecondary = Color(0xFF4A1461)
val ExpressiveDarkSecondaryContainer = Color(0xFF6330A5)
val ExpressiveDarkOnSecondaryContainer = Color(0xFFE9D5FF)

val ExpressiveDarkTertiary = Color(0xFFF9A8D4) // Soft pink for dark theme
val ExpressiveDarkOnTertiary = Color(0xFF641039)
val ExpressiveDarkTertiaryContainer = Color(0xFF8E1C5D)
val ExpressiveDarkOnTertiaryContainer = Color(0xFFFFD4E6)

// Expressive Surface Colors (Dark)
val ExpressiveDarkSurface = Color(0xFF121212)
val ExpressiveDarkOnSurface = Color(0xFFE6E0E9)
val ExpressiveDarkSurfaceVariant = Color(0xFF49454F)
val ExpressiveDarkOnSurfaceVariant = Color(0xFFCAC4D0)
val ExpressiveDarkSurfaceTint = ExpressiveDarkPrimary
val ExpressiveDarkOutlineVariant = Color(0xFF49454F)

// Expressive Semantic Colors for ATS Features
val ExpressiveCheckIn = Color(0xFF10B981) // Vibrant emerald green for check-in
val ExpressiveOnCheckIn = Color(0xFFFFFFFF)
val ExpressiveCheckInContainer = Color(0xFFD1FAE5)
val ExpressiveOnCheckInContainer = Color(0xFF064E3B)

val ExpressiveLocation = Color(0xFF0EA5E9) // Sky blue for location features
val ExpressiveOnLocation = Color(0xFFFFFFFF)
val ExpressiveLocationContainer = Color(0xFFE0F2FE)
val ExpressiveOnLocationContainer = Color(0xFF075985)

val ExpressiveWarning = Color(0xFFF59E0B) // Amber for warnings
val ExpressiveOnWarning = Color(0xFFFFFFFF)
val ExpressiveWarningContainer = Color(0xFFFEF3C7)
val ExpressiveOnWarningContainer = Color(0xFF92400E)

val ExpressiveError = Color(0xFFEF4444) // Red for errors
val ExpressiveOnError = Color(0xFFFFFFFF)
val ExpressiveErrorContainer = Color(0xFFFEE2E2)
val ExpressiveOnErrorContainer = Color(0xFF991B1B)

val ExpressiveSuccess = Color(0xFF22C55E) // Green for success
val ExpressiveOnSuccess = Color(0xFFFFFFFF)
val ExpressiveSuccessContainer = Color(0xFFDCFCE7)
val ExpressiveOnSuccessContainer = Color(0xFF166534)

// Expressive Status Colors
val ExpressiveOnline = Color(0xFF22C55E)
val ExpressiveOffline = Color(0xFF6B7280)
val ExpressiveAway = Color(0xFFF59E0B)

// Expressive Role Colors
val ExpressiveAdmin = Color(0xFFA855F7) // Purple for admin
val ExpressiveSupervisor = Color(0xFF3B82F6) // Blue for supervisor
val ExpressiveEmployee = Color(0xFF10B981) // Green for employee

// Expressive Gradient Colors
val ExpressivePrimaryGradient = listOf(
    Color(0xFF3B82F6), // Blue
    Color(0xFF8B5CF6)  // Purple
)

val ExpressiveSuccessGradient = listOf(
    Color(0xFF10B981), // Green
    Color(0xFF34D399)  // Lighter green
)

val ExpressiveWarningGradient = listOf(
    Color(0xFFF59E0B), // Amber
    Color(0xFFFCD34D)  // Lighter amber
)

// Legacy color scheme compatibility (keep for reference)
val md_theme_light_primary = ExpressiveLightPrimary
val md_theme_light_onPrimary = ExpressiveLightOnPrimary
val md_theme_light_primaryContainer = ExpressiveLightPrimaryContainer
val md_theme_light_onPrimaryContainer = ExpressiveLightOnPrimaryContainer
val md_theme_light_secondary = ExpressiveLightSecondary
val md_theme_light_onSecondary = ExpressiveLightOnSecondary
val md_theme_light_secondaryContainer = ExpressiveLightSecondaryContainer
val md_theme_light_onSecondaryContainer = ExpressiveLightOnSecondaryContainer
val md_theme_light_tertiary = ExpressiveLightTertiary
val md_theme_light_onTertiary = ExpressiveLightOnTertiary
val md_theme_light_tertiaryContainer = ExpressiveLightTertiaryContainer
val md_theme_light_onTertiaryContainer = ExpressiveLightOnTertiaryContainer

val md_theme_dark_primary = ExpressiveDarkPrimary
val md_theme_dark_onPrimary = ExpressiveDarkOnPrimary
val md_theme_dark_primaryContainer = ExpressiveDarkPrimaryContainer
val md_theme_dark_onPrimaryContainer = ExpressiveDarkOnPrimaryContainer
val md_theme_dark_secondary = ExpressiveDarkSecondary
val md_theme_dark_onSecondary = ExpressiveDarkOnSecondary
val md_theme_dark_secondaryContainer = ExpressiveDarkSecondaryContainer
val md_theme_dark_onSecondaryContainer = ExpressiveDarkOnSecondaryContainer
val md_theme_dark_tertiary = ExpressiveDarkTertiary
val md_theme_dark_onTertiary = ExpressiveDarkOnTertiary
val md_theme_dark_tertiaryContainer = ExpressiveDarkTertiaryContainer
val md_theme_dark_onTertiaryContainer = ExpressiveDarkOnTertiaryContainer

val md_theme_light_error = ExpressiveError
val md_theme_light_errorContainer = ExpressiveErrorContainer
val md_theme_light_onError = ExpressiveOnError
val md_theme_light_onErrorContainer = ExpressiveOnErrorContainer
val md_theme_light_background = ExpressiveLightSurface
val md_theme_light_onBackground = ExpressiveLightOnSurface
val md_theme_light_surface = ExpressiveLightSurface
val md_theme_light_onSurface = ExpressiveLightOnSurface
val md_theme_light_surfaceVariant = ExpressiveLightSurfaceVariant
val md_theme_light_onSurfaceVariant = ExpressiveLightOnSurfaceVariant
val md_theme_light_outline = ExpressiveLightOutlineVariant
val md_theme_light_outlineVariant = ExpressiveLightOutlineVariant
val md_theme_light_scrim = Color(0xFF000000)
val md_theme_light_inverseSurface = ExpressiveDarkSurface
val md_theme_light_inverseOnSurface = ExpressiveDarkOnSurface
val md_theme_light_inversePrimary = ExpressiveDarkPrimary

val md_theme_dark_error = ExpressiveError
val md_theme_dark_errorContainer = ExpressiveErrorContainer
val md_theme_dark_onError = ExpressiveOnError
val md_theme_dark_onErrorContainer = ExpressiveOnErrorContainer
val md_theme_dark_background = ExpressiveDarkSurface
val md_theme_dark_onBackground = ExpressiveDarkOnSurface
val md_theme_dark_surface = ExpressiveDarkSurface
val md_theme_dark_onSurface = ExpressiveDarkOnSurface
val md_theme_dark_surfaceVariant = ExpressiveDarkSurfaceVariant
val md_theme_dark_onSurfaceVariant = ExpressiveDarkOnSurfaceVariant
val md_theme_dark_outline = ExpressiveDarkOutlineVariant
val md_theme_dark_outlineVariant = ExpressiveDarkOutlineVariant
val md_theme_dark_scrim = Color(0xFF000000)
val md_theme_dark_inverseSurface = ExpressiveLightSurface
val md_theme_dark_inverseOnSurface = ExpressiveLightOnSurface
val md_theme_dark_inversePrimary = ExpressiveLightPrimary
