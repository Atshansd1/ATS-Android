package com.ats.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ats.android.R
import com.ats.android.models.LocationMovement
import com.ats.android.utils.NumberFormatter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeRouteMapScreen(
    movement: LocationMovement,
    onBack: () -> Unit = {}
) {
    // Create route points
    val fromPoint = LatLng(movement.fromLatitude, movement.fromLongitude)
    val toPoint = LatLng(movement.toLatitude, movement.toLongitude)
    val checkInPoint = LatLng(movement.checkInLatitude, movement.checkInLongitude)
    
    // Calculate bounds to show all points
    val boundsBuilder = LatLngBounds.builder()
        .include(fromPoint)
        .include(toPoint)
        .include(checkInPoint)
    val bounds = boundsBuilder.build()
    
    // Center position (midpoint between from and to)
    val centerLat = (movement.fromLatitude + movement.toLatitude) / 2
    val centerLng = (movement.fromLongitude + movement.toLongitude) / 2
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(centerLat, centerLng),
            14f
        )
    }
    
    // Adjust camera to show all points when screen loads
    LaunchedEffect(bounds) {
        try {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 100)
            )
        } catch (e: Exception) {
            // Ignore camera animation errors
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(stringResource(R.string.employee_route))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                )
            ) {
                // Check-in marker (start point)
                Marker(
                    state = MarkerState(position = checkInPoint),
                    title = stringResource(R.string.check_in),
                    snippet = movement.fromAddress ?: "",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                        .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN)
                )
                
                // From location marker
                if (movement.fromLatitude != movement.checkInLatitude || 
                    movement.fromLongitude != movement.checkInLongitude) {
                    Marker(
                        state = MarkerState(position = fromPoint),
                        title = stringResource(R.string.movement_from),
                        snippet = movement.fromAddress ?: "",
                        icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                            .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE)
                    )
                }
                
                // To location marker (current position)
                Marker(
                    state = MarkerState(position = toPoint),
                    title = stringResource(R.string.movement_to),
                    snippet = movement.toAddress ?: "",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                        .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED)
                )
                
                // Draw polyline from check-in to from location (if different)
                if (movement.fromLatitude != movement.checkInLatitude || 
                    movement.fromLongitude != movement.checkInLongitude) {
                    Polyline(
                        points = listOf(checkInPoint, fromPoint),
                        color = Color(0xFF4CAF50), // Green
                        width = 8f
                    )
                }
                
                // Draw polyline from 'from' to 'to' location
                Polyline(
                    points = listOf(fromPoint, toPoint),
                    color = Color(0xFF2196F3), // Blue
                    width = 10f
                )
                
                // Draw dashed line from check-in to current (total distance)
                Polyline(
                    points = listOf(checkInPoint, toPoint),
                    color = Color(0xFFFF9800), // Orange
                    width = 4f,
                    pattern = listOf(
                        com.google.android.gms.maps.model.Dot(),
                        com.google.android.gms.maps.model.Gap(10f)
                    )
                )
            }
            
            // Route Info Card (bottom overlay)
            RouteInfoCard(
                movement = movement,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun RouteInfoCard(
    movement: LocationMovement,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Employee name
            Text(
                text = movement.employeeName.ifEmpty { stringResource(R.string.unknown) },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Movement type
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingFlat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = getLocalizedMovementType(movement.movementType),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Divider()
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Distance moved
                RouteStatItem(
                    icon = Icons.Default.Straighten,
                    label = stringResource(R.string.distance),
                    value = NumberFormatter.formatDistance(movement.distance),
                    color = Color(0xFF2196F3)
                )
                
                // Duration
                if (movement.duration != null && movement.duration > 0) {
                    RouteStatItem(
                        icon = Icons.Default.Timer,
                        label = stringResource(R.string.movement_duration),
                        value = NumberFormatter.formatDuration(movement.duration),
                        color = Color(0xFF4CAF50)
                    )
                }
                
                // Distance from check-in
                RouteStatItem(
                    icon = Icons.Default.MyLocation,
                    label = stringResource(R.string.from_checkin),
                    value = NumberFormatter.formatDistance(movement.distanceFromCheckIn()),
                    color = Color(0xFFFF9800)
                )
            }
            
            Divider()
            
            // Location details
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // From location
                movement.fromAddress?.let { address ->
                    RouteLocationItem(
                        icon = Icons.Default.Flag,
                        label = stringResource(R.string.movement_from),
                        address = address,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                // To location
                movement.toAddress?.let { address ->
                    RouteLocationItem(
                        icon = Icons.Default.LocationOn,
                        label = stringResource(R.string.movement_to),
                        address = address,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
fun RouteStatItem(
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RouteLocationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    address: String,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
