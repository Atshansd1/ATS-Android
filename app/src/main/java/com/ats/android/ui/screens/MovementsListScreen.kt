package com.ats.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.R
import com.ats.android.models.LocationMovement
import com.ats.android.models.MovementType
import com.ats.android.ui.components.GlassCard
import com.ats.android.ui.theme.ComponentShapes
import com.ats.android.ui.theme.CornerRadius
import com.ats.android.utils.NumberFormatter
import com.ats.android.viewmodels.MovementsViewModel
import com.ats.android.viewmodels.MovementsUiState
import java.util.Date
import com.ats.android.ui.components.EmployeeAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsListScreen(
    viewModel: MovementsViewModel = viewModel(),
    onViewRoute: (LocationMovement) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val movements by viewModel.movements.collectAsState()
    val employees by viewModel.employees.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            stringResource(R.string.recent_activity),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        ) 
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        IconButton(onClick = { /* Filter menu */ }) {
                            Icon(Icons.Default.FilterList, stringResource(R.string.filter))
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (uiState) {
                    is MovementsUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is MovementsUiState.Empty -> {
                        EmptyMovementsView(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is MovementsUiState.Error -> {
                        ErrorView(
                            message = (uiState as MovementsUiState.Error).message,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is MovementsUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(movements) { movement ->
                                MovementCard(
                                    movement = movement,
                                    employee = employees[movement.employeeId],
                                    onViewRoute = onViewRoute
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovementCard(
    movement: LocationMovement,
    employee: com.ats.android.models.Employee?,
    modifier: Modifier = Modifier,
    onViewRoute: (LocationMovement) -> Unit = {}
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = CornerRadius.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Avatar + Name + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                EmployeeAvatar(
                    avatarUrl = employee?.avatarURL,
                    employeeName = employee?.displayName ?: movement.employeeName,
                    size = 48.dp
                )
                  
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = employee?.displayName ?: movement.employeeName.ifEmpty { "Unknown Employee" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = employee?.role?.toString()?.uppercase() ?: "EMPLOYEE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Time Ago
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = LocalizedTimeAgo(movement.startTime.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
            
            // Movement Type Chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(getMovementColor(movement.getType()).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getMovementIcon(movement.getType()),
                        contentDescription = null,
                        tint = getMovementColor(movement.getType()),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = getLocalizedMovementType(movement.getType()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = getMovementColor(movement.getType())
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (movement.distance > 0) {
                     Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                     )
                     Spacer(modifier = Modifier.width(4.dp))
                     Text(
                        text = NumberFormatter.formatDistance(movement.distance),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                }
            }
            
            // Location Details (Timeline style)
            if (movement.fromAddress != null || movement.toAddress != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    if (movement.fromAddress != null) {
                        TimelineRow(
                            icon = Icons.Default.TripOrigin,
                            text = movement.fromAddress,
                            isLast = movement.toAddress == null,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    
                    if (movement.toAddress != null) {
                        TimelineRow(
                            icon = Icons.Default.Place,
                            text = movement.toAddress,
                            isLast = true,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Action Button
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onViewRoute(movement) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.view_route),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun TimelineRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String?,
    isLast: Boolean,
    color: Color
) {
    if (text == null) return
    
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp) // Fixed height for timeline segment
                        .background(color.copy(alpha = 0.3f))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            maxLines = 2,
            modifier = Modifier.padding(bottom = if (isLast) 0.dp else 12.dp)
        )
    }
}

@Composable
fun LocationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    address: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }
    }
}

@Composable
fun EmptyMovementsView(
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.padding(32.dp),
        cornerRadius = CornerRadius.large
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.no_movements_detected),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.movements_will_appear),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.padding(32.dp),
        cornerRadius = CornerRadius.large
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.error_loading_movements),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun getLocalizedMovementType(type: MovementType): String {
    return when (type) {
        MovementType.SIGNIFICANT_MOVE -> stringResource(R.string.movement_significant_move)
        MovementType.STATIONARY_STAY -> stringResource(R.string.movement_stationary_stay)
        MovementType.RETURNED_TO_CHECKIN -> stringResource(R.string.movement_returned_to_checkin)
        MovementType.LEFT_CHECKIN_AREA -> stringResource(R.string.movement_left_checkin_area)
    }
}

@Composable
fun LocalizedTimeAgo(date: Date): String {
    val now = System.currentTimeMillis()
    val then = date.time
    val diffSeconds = (now - then) / 1000
    
    return when {
        diffSeconds < 60 -> stringResource(R.string.time_just_now)
        diffSeconds < 3600 -> stringResource(R.string.time_ago_m, (diffSeconds / 60).toInt())
        diffSeconds < 86400 -> stringResource(R.string.time_ago_h, (diffSeconds / 3600).toInt())
        else -> stringResource(R.string.time_ago_d, (diffSeconds / 86400).toInt())
    }
}

private fun getMovementIcon(type: MovementType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        MovementType.SIGNIFICANT_MOVE -> Icons.Default.ArrowForward
        MovementType.STATIONARY_STAY -> Icons.Default.Place
        MovementType.RETURNED_TO_CHECKIN -> Icons.Default.Refresh
        MovementType.LEFT_CHECKIN_AREA -> Icons.Default.Warning
    }
}

private fun getMovementColor(type: MovementType): Color {
    return when (type) {
        MovementType.SIGNIFICANT_MOVE -> Color(0xFF2196F3) // Blue
        MovementType.STATIONARY_STAY -> Color(0xFF4CAF50) // Green
        MovementType.RETURNED_TO_CHECKIN -> Color(0xFFFF9800) // Orange
        MovementType.LEFT_CHECKIN_AREA -> Color(0xFFF44336) // Red
    }
}
