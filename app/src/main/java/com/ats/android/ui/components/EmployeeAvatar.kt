package com.ats.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ats.android.models.Employee
import com.ats.android.ui.theme.ATSColors
import com.ats.android.ui.theme.AvatarSize
import com.ats.android.ui.theme.getRoleColor

/**
 * Employee avatar matching iOS design
 * - 50dp circle by default (iOS standard)
 * - Shows image or gradient circle with initial
 * - Role-colored gradient for fallback
 */
@Composable
fun EmployeeAvatar(
    employee: Employee,
    size: Dp = AvatarSize.large,
    showBorder: Boolean = false,
    borderColor: Color = Color.White,
    borderWidth: Dp = 2.dp
) {
    val roleColor = getRoleColor(employee.role)
    
    if (!employee.avatarURL.isNullOrEmpty()) {
        // Show image with AsyncImage
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .then(
                    if (showBorder) {
                        Modifier.border(borderWidth, borderColor, CircleShape)
                    } else Modifier
                )
        ) {
            AsyncImage(
                model = employee.avatarURL,
                contentDescription = employee.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        // Show gradient circle with initial (iOS style)
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            roleColor.copy(alpha = 0.6f),
                            roleColor.copy(alpha = 0.3f)
                        )
                    ),
                    shape = CircleShape
                )
                .then(
                    if (showBorder) {
                        Modifier.border(borderWidth, borderColor, CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = employee.displayName.firstOrNull()?.uppercase() ?: "?",
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

/**
 * Employee avatar with URL and name parameters
 */
@Composable
fun EmployeeAvatar(
    avatarUrl: String?,
    employeeName: String,
    size: Dp = AvatarSize.large,
    showBorder: Boolean = false,
    borderColor: Color = Color.White,
    borderWidth: Dp = 2.dp
) {
    if (!avatarUrl.isNullOrEmpty()) {
        // Show image with AsyncImage
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .then(
                    if (showBorder) {
                        Modifier.border(borderWidth, borderColor, CircleShape)
                    } else Modifier
                )
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = employeeName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        // Show gradient circle with initial
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2196F3).copy(alpha = 0.6f),
                            Color(0xFF2196F3).copy(alpha = 0.3f)
                        )
                    ),
                    shape = CircleShape
                )
                .then(
                    if (showBorder) {
                        Modifier.border(borderWidth, borderColor, CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = employeeName.firstOrNull()?.uppercase() ?: "?",
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

/**
 * Simple avatar for compact displays
 */
@Composable
fun SimpleAvatar(
    avatarUrl: String?,
    name: String,
    roleColor: Color,
    size: Dp = AvatarSize.medium
) {
    if (!avatarUrl.isNullOrEmpty()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = name,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            roleColor.copy(alpha = 0.6f),
                            roleColor.copy(alpha = 0.3f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
