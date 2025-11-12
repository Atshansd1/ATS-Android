package com.ats.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ats.android.ui.theme.ATSColors

/**
 * Active status dot matching iOS design
 * - 8dp circle (iOS standard)
 * - Green for active, gray for inactive
 */
@Composable
fun ActiveStatusDot(
    isActive: Boolean = true,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = if (isActive) ATSColors.ActiveDot else ATSColors.InactiveDot,
                shape = CircleShape
            )
    )
}
