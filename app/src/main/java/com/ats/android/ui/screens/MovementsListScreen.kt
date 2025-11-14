package com.ats.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.R
import com.ats.android.models.LocationMovement
import com.ats.android.models.MovementType
import com.ats.android.ui.theme.ComponentShapes
import com.ats.android.utils.NumberFormatter
import com.ats.android.viewmodels.MovementsViewModel
import com.ats.android.viewmodels.MovementsUiState
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsListScreen(
    viewModel: MovementsViewModel = viewModel(),
    onViewRoute: (LocationMovement) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val movements by viewModel.movements.collectAsState()
    val selectedEmployeeId by viewModel.selectedEmployeeId.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recent_activity)) },
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(movements) { movement ->
                            MovementCard(
                                movement = movement,
                                onViewRoute = onViewRoute
                            )
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
    modifier: Modifier = Modifier,
    onViewRoute: (LocationMovement) -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ComponentShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Movement type icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(getMovementColor(movement.movementType).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getMovementIcon(movement.movementType),
                            contentDescription = null,
                            tint = getMovementColor(movement.movementType),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = movement.employeeName.ifEmpty { stringResource(R.string.unknown) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getLocalizedMovementType(movement.movementType),
                            style = MaterialTheme.typography.bodyMedium,
                            color = getMovementColor(movement.movementType),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Time ago with English digits
                    Text(
                        text = formatTimeAgoEnglish(movement.startTime.toDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Distance with English digits
                    if (movement.distance > 0) {
                        Text(
                            text = NumberFormatter.formatDistance(movement.distance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            // Location details
            Spacer(modifier = Modifier.height(12.dp))
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            movement.fromAddress?.let { address ->
                LocationRow(
                    icon = Icons.Default.LocationOn,
                    label = stringResource(R.string.movement_from),
                    address = address,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            if (movement.toAddress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LocationRow(
                    icon = Icons.Default.LocationOn,
                    label = stringResource(R.string.movement_to),
                    address = movement.toAddress!!,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Additional info with English digits
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Duration
                if (movement.duration != null && movement.duration > 0) {
                    InfoChip(
                        icon = Icons.Default.Timer,
                        label = stringResource(R.string.movement_duration),
                        value = NumberFormatter.formatDuration(movement.duration),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Distance from check-in
                if (movement.distanceFromCheckIn() > 0.1) {
                    InfoChip(
                        icon = Icons.Default.MyLocation,
                        label = stringResource(R.string.distance_from_checkin, ""),
                        value = NumberFormatter.formatDistance(movement.distanceFromCheckIn()),
                        color = if (movement.distanceFromCheckIn() > 1.0) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            // View route button
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { onViewRoute(movement) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.view_route),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
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
            modifier = Modifier.size(16.dp)
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
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = stringResource(R.string.no_movements_detected),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.movements_will_appear),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ErrorView(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = stringResource(R.string.error_loading_movements),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    Column(
        modifier = modifier,
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
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

fun formatTimeAgoEnglish(date: Date): String {
    val now = System.currentTimeMillis()
    val then = date.time
    val diffSeconds = (now - then) / 1000
    
    return when {
        diffSeconds < 60 -> "now"
        diffSeconds < 3600 -> "${diffSeconds / 60}m"
        diffSeconds < 86400 -> "${diffSeconds / 3600}h"
        else -> "${diffSeconds / 86400}d"
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
