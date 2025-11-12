package com.ats.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ats.android.ui.theme.ComponentShapes

/**
 * Material 3 Expressive Gradient Card
 * Large corner radius, gradient background, elevated shadow
 */
@Composable
fun ExpressiveGradientCard(
    modifier: Modifier = Modifier,
    gradient: List<Color>,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerModifier = if (onClick != null) {
        modifier.then(
            Modifier
                .fillMaxWidth()
                .clip(ComponentShapes.Card)
        )
    } else {
        modifier.then(
            Modifier
                .fillMaxWidth()
                .clip(ComponentShapes.Card)
        )
    }

    Surface(
        modifier = containerModifier,
        shape = ComponentShapes.Card,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(gradient)
                )
                .padding(24.dp),
            content = content
        )
    }
}

/**
 * Material 3 Expressive Surface Card
 * Uses surface colors with expressive elevation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = ComponentShapes.Card,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp,
                hoveredElevation = 6.dp
            ),
            onClick = onClick
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = ComponentShapes.Card,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                content = content
            )
        }
    }
}

/**
 * Material 3 Expressive Button
 * Large corner radius, prominent elevation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = ComponentShapes.Button,
        colors = colors,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        content = content
    )
}

/**
 * Material 3 Expressive Outlined Button
 */
@Composable
fun ExpressiveOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = ComponentShapes.Button,
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        content = content
    )
}

/**
 * Material 3 Expressive Text Button
 */
@Composable
fun ExpressiveTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = ComponentShapes.Button,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        content = content
    )
}

/**
 * Material 3 Expressive FAB
 * Large corner radius, prominent elevation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveFAB(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    expanded: Boolean = false,
    text: String? = null
) {
    if (expanded && text != null) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            shape = ComponentShapes.FAB,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp,
                hoveredElevation = 8.dp
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            shape = ComponentShapes.FAB,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp,
                hoveredElevation = 8.dp
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text
            )
        }
    }
}

/**
 * Material 3 Expressive Chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick ?: {},
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        modifier = modifier,
        shape = ComponentShapes.Chip,
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null
    )
}

/**
 * Material 3 Expressive Statistic Card with animated value
 */
@Composable
fun ExpressiveStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    gradient: List<Color>,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    var animatedScale by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        animatedScale = 1f
    }
    
    val scale by animateFloatAsState(
        targetValue = animatedScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    ExpressiveGradientCard(
        modifier = modifier.scale(scale),
        gradient = gradient,
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .padding(8.dp),
            tint = Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Material 3 Expressive Loading Indicator
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            strokeWidth = 6.dp,
            color = MaterialTheme.colorScheme.primary
        )
        
        if (message != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/**
 * Material 3 Expressive Empty State
 */
@Composable
fun ExpressiveEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(32.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(32.dp))
            ExpressiveButton(
                onClick = onAction
            ) {
                Text(text = actionLabel)
            }
        }
    }
}
