package com.ats.android.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ats.android.ui.theme.CornerRadius

/**
 * Glass morphism card matching iOS .ultraThinMaterial effect
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CornerRadius.medium,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(cornerRadius),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp // Flat like iOS
        ),
        content = content
    )
}

/**
 * Outlined glass card (for secondary content)
 */
@Composable
fun OutlinedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CornerRadius.medium,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(cornerRadius),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        content = content
    )
}
