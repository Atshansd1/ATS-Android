package com.ats.android.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Shape System (May 2025 Official Update)
 * Source: https://m3.material.io/styles/shape/corner-radius-scale
 * 
 * Official M3 Expressive Corner Radii:
 * - Extra Small: 4dp
 * - Small: 8dp
 * - Medium: 12dp
 * - Large: 20dp (INCREASED in M3 Expressive)
 * - Extra Large: 32dp (INCREASED in M3 Expressive)
 * - Extra Extra Large: 48dp (NEW in M3 Expressive)
 * - Full: CircleShape or 50% (fully rounded)
 */
/**
 * Professional Shapes (iOS-style)
 * Consistent rounded corners for a clean, professional look
 */
val ProfessionalShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // Small elements
    small = RoundedCornerShape(12.dp),       // Buttons, chips
    medium = RoundedCornerShape(16.dp),      // Cards (iOS standard)
    large = RoundedCornerShape(20.dp),       // Sheets, dialogs
    extraLarge = RoundedCornerShape(28.dp)   // Large cards
)

// Keep old name for compatibility
val ExpressiveShapes = ProfessionalShapes

/**
 * Component-specific shapes following M3 Expressive guidelines
 * "Be bold and dare to embrace tension" - mix round and square shapes
 */
object ComponentShapes {
    // Buttons - Large (20dp) for expressive prominence
    val Button = RoundedCornerShape(20.dp)         // Large
    val SmallButton = RoundedCornerShape(12.dp)    // Medium
    val Chip = RoundedCornerShape(8.dp)            // Small
    
    // Cards - Large to Extra Extra Large for hero moments
    val Card = RoundedCornerShape(20.dp)           // Large - Standard cards
    val LargeCard = RoundedCornerShape(32.dp)      // Extra Large - Important content
    val HeroCard = RoundedCornerShape(48.dp)       // Extra Extra Large - Hero moments
    
    // Navigation - Extra Large (32dp) per M3 Expressive update
    val NavigationBar = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)  // Extra Large
    val NavigationItem = RoundedCornerShape(20.dp) // Large
    val NavigationDrawer = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
    
    // FABs - Full (CircleShape) or Large (20dp)
    val FAB = CircleShape                          // Full - Fully rounded
    val ExtendedFAB = RoundedCornerShape(20.dp)    // Large
    
    // Text Fields - Medium (12dp)
    val TextField = RoundedCornerShape(12.dp)      // Medium
    
    // Dialogs - Extra Large (32dp)
    val Dialog = RoundedCornerShape(32.dp)         // Extra Large
    val BottomSheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    
    // App Bars - Extra Large (32dp) for expressive feel
    val TopAppBar = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    
    // Icons - Medium to Large
    val IconContainer = RoundedCornerShape(12.dp)  // Medium
    val LargeIconContainer = RoundedCornerShape(20.dp) // Large
}
