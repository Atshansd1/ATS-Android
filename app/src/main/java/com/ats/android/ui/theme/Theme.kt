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
 * Professional Light Color Scheme (iOS-style)
 */
private val ProfessionalLightColorScheme = lightColorScheme(
    // Primary - iOS Blue (minimal)
    primary = ProfessionalLightColors.Primary,
    onPrimary = ProfessionalLightColors.OnPrimary,
    primaryContainer = ProfessionalLightColors.PrimaryContainer,
    onPrimaryContainer = ProfessionalLightColors.OnPrimaryContainer,
    
    // Secondary - Neutral Gray
    secondary = ProfessionalLightColors.Secondary,
    onSecondary = ProfessionalLightColors.OnSecondary,
    secondaryContainer = ProfessionalLightColors.SecondaryContainer,
    onSecondaryContainer = ProfessionalLightColors.OnSecondaryContainer,
    
    // Tertiary - Minimal (rarely used)
    tertiary = ProfessionalLightColors.Tertiary,
    onTertiary = ProfessionalLightColors.OnTertiary,
    tertiaryContainer = ProfessionalLightColors.TertiaryContainer,
    onTertiaryContainer = ProfessionalLightColors.OnTertiaryContainer,
    
    // Error
    error = ProfessionalLightColors.Error,
    errorContainer = ProfessionalLightColors.ErrorContainer,
    onError = ProfessionalLightColors.OnError,
    onErrorContainer = ProfessionalLightColors.OnErrorContainer,
    
    // Background & Surface - Clean neutrals
    background = ProfessionalLightColors.Background,
    onBackground = ProfessionalLightColors.OnBackground,
    surface = ProfessionalLightColors.Surface,
    onSurface = ProfessionalLightColors.OnSurface,
    surfaceVariant = ProfessionalLightColors.SurfaceVariant,
    onSurfaceVariant = ProfessionalLightColors.OnSurfaceVariant,
    surfaceTint = ProfessionalLightColors.Primary,
    
    // Outline - Subtle
    outline = ProfessionalLightColors.Outline,
    outlineVariant = ProfessionalLightColors.OutlineVariant,
    
    // Inverse
    inverseSurface = ProfessionalDarkColors.Surface,
    inverseOnSurface = ProfessionalDarkColors.OnSurface,
    inversePrimary = ProfessionalDarkColors.Primary,
    
    // Scrim
    scrim = Color(0xFF000000)
)

/**
 * Professional Dark Color Scheme (iOS-style)
 */
private val ProfessionalDarkColorScheme = darkColorScheme(
    // Primary - Lighter Blue for dark theme
    primary = ProfessionalDarkColors.Primary,
    onPrimary = ProfessionalDarkColors.OnPrimary,
    primaryContainer = ProfessionalDarkColors.PrimaryContainer,
    onPrimaryContainer = ProfessionalDarkColors.OnPrimaryContainer,
    
    // Secondary - Light Gray
    secondary = ProfessionalDarkColors.Secondary,
    onSecondary = ProfessionalDarkColors.OnSecondary,
    secondaryContainer = ProfessionalDarkColors.SecondaryContainer,
    onSecondaryContainer = ProfessionalDarkColors.OnSecondaryContainer,
    
    // Tertiary - Subtle
    tertiary = ProfessionalDarkColors.Tertiary,
    onTertiary = ProfessionalDarkColors.OnTertiary,
    tertiaryContainer = ProfessionalDarkColors.TertiaryContainer,
    onTertiaryContainer = ProfessionalDarkColors.OnTertiaryContainer,
    
    // Error
    error = ProfessionalDarkColors.Error,
    errorContainer = ProfessionalDarkColors.ErrorContainer,
    onError = ProfessionalDarkColors.OnError,
    onErrorContainer = ProfessionalDarkColors.OnErrorContainer,
    
    // Background & Surface - iOS dark style
    background = ProfessionalDarkColors.Background,
    onBackground = ProfessionalDarkColors.OnBackground,
    surface = ProfessionalDarkColors.Surface,
    onSurface = ProfessionalDarkColors.OnSurface,
    surfaceVariant = ProfessionalDarkColors.SurfaceVariant,
    onSurfaceVariant = ProfessionalDarkColors.OnSurfaceVariant,
    surfaceTint = ProfessionalDarkColors.Primary,
    
    // Outline - Subtle
    outline = ProfessionalDarkColors.Outline,
    outlineVariant = ProfessionalDarkColors.OutlineVariant,
    
    // Inverse
    inverseSurface = ProfessionalLightColors.Surface,
    inverseOnSurface = ProfessionalLightColors.OnSurface,
    inversePrimary = ProfessionalLightColors.Primary,
    
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
        darkTheme -> ProfessionalDarkColorScheme
        else -> ProfessionalLightColorScheme
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
        shapes = ProfessionalShapes,
        content = content
    )
}
