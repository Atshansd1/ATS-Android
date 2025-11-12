package com.ats.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Expressive Elevation Levels
data class ExpressiveElevation(
    val none: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp,
    val level4: Dp = 8.dp,
    val level5: Dp = 12.dp,
    val level6: Dp = 16.dp,
    val level7: Dp = 20.dp,
)

// Expressive elevation values
val expressiveElevation = ExpressiveElevation()

// Expressive Shadow Colors
data class ExpressiveShadowColors(
    val shadowPrimary: Color = Color(0x1A000000),
    val shadowSecondary: Color = Color(0x0D000000),
    val shadowTertiary: Color = Color(0x08000000),
)

// Expressive shadow configurations
data class ExpressiveShadowConfig(
    val elevation: Dp,
    val shape: Shape,
    val spotColor: Color,
    val ambientColor: Color,
    val defaultElevation: Dp = 0.dp
)

/**
 * Expressive Card with proper elevation and state layers
 */


/**
 * Expressive Elevated Button with custom shadow and state layers
 */
@Composable
fun ExpressiveElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    elevation: ExpressiveShadowConfig = ExpressiveShadowConfig(
        elevation = expressiveElevation.level3,
        shape = shape,
        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ),
    content: @Composable () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.shadow(
            elevation = elevation.elevation,
            shape = shape,
            spotColor = elevation.spotColor,
            ambientColor = elevation.ambientColor
        ),
        shape = shape
    ) {
        content()
    }
}

/**
 * Expressive FAB with enhanced shadows and elevation
 */
@Composable
fun ExpressiveFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    elevation: ExpressiveShadowConfig = ExpressiveShadowConfig(
        elevation = expressiveElevation.level6,
        shape = shape,
        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    ),
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.shadow(
            elevation = elevation.elevation,
            shape = shape,
            spotColor = elevation.spotColor,
            ambientColor = elevation.ambientColor
        ),
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = elevation.defaultElevation
        )
    ) {
        content()
    }
}

/**
 * Expressive Large FAB
 */
@Composable
fun ExpressiveLargeFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    elevation: ExpressiveShadowConfig = ExpressiveShadowConfig(
        elevation = expressiveElevation.level7,
        shape = shape,
        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    ),
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.shadow(
            elevation = elevation.elevation,
            shape = shape,
            spotColor = elevation.spotColor,
            ambientColor = elevation.ambientColor
        ),
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = elevation.defaultElevation
        )
    ) {
        content()
    }
}

/**
 * Expressive Surface with enhanced elevation
 */
@Composable
fun ExpressiveSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: ExpressiveShadowConfig = ExpressiveShadowConfig(
        elevation = expressiveElevation.level2,
        shape = shape,
        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        ambientColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.1f)
    ),
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.shadow(
            elevation = elevation.elevation,
            shape = shape,
            spotColor = elevation.spotColor,
            ambientColor = elevation.ambientColor
        ),
        shape = shape,
        color = color,
        contentColor = contentColor,
        tonalElevation = elevation.defaultElevation
    ) {
        content()
    }
}

/**
 * Utility function to get shadow config for different elevation levels
 */
object ExpressiveShadowPresets {
    @Composable
    fun low(shape: Shape = RoundedCornerShape(12.dp)) = ExpressiveShadowConfig(
        elevation = expressiveElevation.level1,
        shape = shape,
        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        ambientColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.05f)
    )
    
    @Composable
    fun medium(shape: Shape = RoundedCornerShape(16.dp)) = ExpressiveShadowConfig(
        elevation = expressiveElevation.level3,
        shape = shape,
        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        ambientColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.1f)
    )
    
    @Composable
    fun high(shape: Shape = RoundedCornerShape(20.dp)) = ExpressiveShadowConfig(
        elevation = expressiveElevation.level5,
        shape = shape,
        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        ambientColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.15f)
    )
    
    @Composable
    fun extraHigh(shape: Shape = RoundedCornerShape(24.dp)) = ExpressiveShadowConfig(
        elevation = expressiveElevation.level7,
        shape = shape,
        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        ambientColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f)
    )
}
