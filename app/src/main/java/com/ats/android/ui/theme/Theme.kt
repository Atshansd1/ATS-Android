package com.ats.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material 3 Expressive Light Color Scheme
 */
private val ExpressiveLightColorScheme = lightColorScheme(
    // Primary - Vibrant Blue
    primary = ExpressiveLightPrimary,
    onPrimary = ExpressiveLightOnPrimary,
    primaryContainer = ExpressiveLightPrimaryContainer,
    onPrimaryContainer = ExpressiveLightOnPrimaryContainer,
    
    // Secondary - Vibrant Purple
    secondary = ExpressiveLightSecondary,
    onSecondary = ExpressiveLightOnSecondary,
    secondaryContainer = ExpressiveLightSecondaryContainer,
    onSecondaryContainer = ExpressiveLightOnSecondaryContainer,
    
    // Tertiary - Vibrant Pink
    tertiary = ExpressiveLightTertiary,
    onTertiary = ExpressiveLightOnTertiary,
    tertiaryContainer = ExpressiveLightTertiaryContainer,
    onTertiaryContainer = ExpressiveLightOnTertiaryContainer,
    
    // Error
    error = ExpressiveError,
    errorContainer = ExpressiveErrorContainer,
    onError = ExpressiveOnError,
    onErrorContainer = ExpressiveOnErrorContainer,
    
    // Background & Surface
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = ExpressiveLightSurface,
    onSurface = ExpressiveLightOnSurface,
    surfaceVariant = ExpressiveLightSurfaceVariant,
    onSurfaceVariant = ExpressiveLightOnSurfaceVariant,
    surfaceTint = ExpressiveLightSurfaceTint,
    
    // Outline
    outline = Color(0xFF74777F),
    outlineVariant = ExpressiveLightOutlineVariant,
    
    // Inverse
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = ExpressiveDarkPrimary,
    
    // Scrim
    scrim = Color(0xFF000000)
)

/**
 * Material 3 Expressive Dark Color Scheme
 */
private val ExpressiveDarkColorScheme = darkColorScheme(
    // Primary - Soft Blue
    primary = ExpressiveDarkPrimary,
    onPrimary = ExpressiveDarkOnPrimary,
    primaryContainer = ExpressiveDarkPrimaryContainer,
    onPrimaryContainer = ExpressiveDarkOnPrimaryContainer,
    
    // Secondary - Soft Purple
    secondary = ExpressiveDarkSecondary,
    onSecondary = ExpressiveDarkOnSecondary,
    secondaryContainer = ExpressiveDarkSecondaryContainer,
    onSecondaryContainer = ExpressiveDarkOnSecondaryContainer,
    
    // Tertiary - Soft Pink
    tertiary = ExpressiveDarkTertiary,
    onTertiary = ExpressiveDarkOnTertiary,
    tertiaryContainer = ExpressiveDarkTertiaryContainer,
    onTertiaryContainer = ExpressiveDarkOnTertiaryContainer,
    
    // Error
    error = ExpressiveError,
    errorContainer = Color(0xFF601410),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFF9DEDC),
    
    // Background & Surface
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    surface = ExpressiveDarkSurface,
    onSurface = ExpressiveDarkOnSurface,
    surfaceVariant = ExpressiveDarkSurfaceVariant,
    onSurfaceVariant = ExpressiveDarkOnSurfaceVariant,
    surfaceTint = ExpressiveDarkSurfaceTint,
    
    // Outline
    outline = Color(0xFF8E9099),
    outlineVariant = ExpressiveDarkOutlineVariant,
    
    // Inverse
    inverseSurface = Color(0xFFE3E2E6),
    inverseOnSurface = Color(0xFF2F3033),
    inversePrimary = ExpressiveLightPrimary,
    
    // Scrim
    scrim = Color(0xFF000000)
)

/**
 * ATS Material 3 Expressive Theme
 * Implements full Material 3 expressive design system
 */
@Composable
fun ATSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled by default for consistent branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ExpressiveDarkColorScheme
        else -> ExpressiveLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent for edge-to-edge
            window.statusBarColor = Color.Transparent.toArgb()
            // Update status bar appearance based on theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        shapes = ExpressiveShapes,
        content = content
    )
}
