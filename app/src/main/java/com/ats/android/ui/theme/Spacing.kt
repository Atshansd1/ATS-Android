package com.ats.android.ui.theme

import androidx.compose.ui.unit.dp

/**
 * iOS-matched spacing system
 */
object Spacing {
    val xs = 4.dp    // Extra small - tight spacing
    val sm = 8.dp    // Small - compact spacing
    val md = 12.dp   // Medium - default card spacing
    val lg = 16.dp   // Large - card padding, list item padding
    val xl = 20.dp   // Extra large - section spacing (iOS default)
    val xxl = 24.dp  // Extra extra large - major sections
    val xxxl = 32.dp // Huge - special spacing
}

/**
 * Corner radii matching iOS
 */
object CornerRadius {
    val small = 8.dp   // Small elements, chips
    val medium = 12.dp // Cards, buttons (iOS default)
    val large = 16.dp  // Search bars, large buttons
    val xlarge = 24.dp // Bottom sheets, modals
}

/**
 * Avatar sizes
 */
object AvatarSize {
    val small = 32.dp
    val medium = 40.dp
    val large = 50.dp  // iOS default for lists
    val xlarge = 56.dp
}
