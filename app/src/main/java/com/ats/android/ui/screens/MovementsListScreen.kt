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
import com.ats.android.viewmodels.MovementsViewModel
import com.ats.android.viewmodels.MovementsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsListScreen(
    viewModel: MovementsViewModel = viewModel()
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
                            MovementCard(movement = movement)
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
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Movement type icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                    
                    Column {
                        Text(
                            text = movement.employeeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = movement.movementType.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = getMovementColor(movement.movementType)
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = movement.timeAgo(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (movement.distance > 0) {
                        Text(
                            text = movement.formattedDistance(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Location details
            if (movement.fromAddress != null || movement.toAddress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                movement.fromAddress?.let { address ->
                    LocationRow(
                        icon = Icons.Default.North,
                        label = "From",
                        address = address,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                movement.toAddress?.let { address ->
                    Spacer(modifier = Modifier.height(8.dp))
                    LocationRow(
                        icon = Icons.Default.South,
                        label = "To",
                        address = address,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Additional info
            if (movement.movementType == MovementType.STATIONARY_STAY && movement.duration != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.stayed_for, movement.formattedDuration() ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Distance from check-in
            if (movement.distanceFromCheckIn() > 1.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = String.format("%.2f km from check-in", movement.distanceFromCheckIn()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // View on map button
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { /* TODO: Navigate to map */ },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("View on Map")
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
