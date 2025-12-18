package com.ats.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.viewmodels.MapViewModel
import com.ats.android.viewmodels.MapUiState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.compose.ui.res.stringResource
import com.ats.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val employeeLocations by viewModel.employeeLocations.collectAsState()
    val filteredLocations by viewModel.filteredEmployeeLocations.collectAsState()
    val mapCenter by viewModel.mapCenter.collectAsState()
    val nearestEmployee by viewModel.nearestEmployee.collectAsState()
    
    // Use filtered locations if available, otherwise all locations
    val displayLocations = filteredLocations.ifEmpty { employeeLocations }
    
    var showFilterSheet by remember { mutableStateOf(false) }
    
    // Default map center (Riyadh, Saudi Arabia)
    val defaultCenter = LatLng(24.7136, 46.6753)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(mapCenter ?: defaultCenter, 12f)
    }
    
    // Update camera when map center changes
    LaunchedEffect(mapCenter) {
        mapCenter?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 14f)
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (uiState) {
            is MapUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is MapUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (uiState as MapUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is MapUiState.Success -> {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        mapType = MapType.NORMAL
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        compassEnabled = true
                    )
                ) {
                    // Show filtered or all employee locations
                    displayLocations.forEach { location ->
                        Marker(
                            state = MarkerState(position = location.position),
                            title = location.employeeName,
                            snippet = "${location.role} • ${location.placeName ?: stringResource(R.string.unknown_location)}",
                            tag = location.employeeId
                        )
                    }
                }
                
                // Filter button overlay (top right)
                FloatingActionButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .offset(y = 60.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.FilterList, stringResource(R.string.filters))
                }
                
                // Employee count badge (bottom left)
                if (displayLocations.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .padding(bottom = 96.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.active_count, displayLocations.size),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        
        // Filter bottom sheet
        if (showFilterSheet) {
            MapFilterSheet(
                viewModel = viewModel,
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapFilterSheet(
    viewModel: MapViewModel,
    onDismiss: () -> Unit
) {
    val selectedTeam by viewModel.selectedTeamFilter.collectAsState()
    val selectedRole by viewModel.selectedRoleFilter.collectAsState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.filter_employees),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Role filter
            Text(
                text = stringResource(R.string.role),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.ats.android.models.EmployeeRole.values().forEach { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = {
                            viewModel.setRoleFilter(if (selectedRole == role) null else role)
                        },
                        label = { Text(role.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Clear filters button
            if (selectedTeam != null || selectedRole != null) {
                OutlinedButton(
                    onClick = {
                        viewModel.clearFilters()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.clear_all_filters))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.apply))
            }
        }
    }
}
