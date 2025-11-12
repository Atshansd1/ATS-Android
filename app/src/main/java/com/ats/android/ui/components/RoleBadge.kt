package com.ats.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ats.android.models.EmployeeRole
import com.ats.android.ui.theme.getRoleColor

/**
 * Role badge matching iOS design
 * - Colored background with alpha
 * - Rounded corners (8dp)
 * - Role-specific colors
 */
@Composable
fun RoleBadge(
    role: EmployeeRole,
    modifier: Modifier = Modifier
) {
    val roleColor = getRoleColor(role)
    val roleName = when (role) {
        EmployeeRole.ADMIN -> "Admin"
        EmployeeRole.SUPERVISOR -> "Supervisor"
        EmployeeRole.EMPLOYEE -> "Employee"
    }
    
    Text(
        text = roleName,
        modifier = modifier
            .background(
                color = roleColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = roleColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
}

/**
 * Simple text badge with custom color
 */
@Composable
fun TextBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
}
