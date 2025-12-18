package com.ats.android.ui.screens

import android.location.Location
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.ui.components.*
import com.ats.android.ui.theme.*
import com.ats.android.viewmodels.EmployeeLocation
import com.ats.android.viewmodels.MapViewModel
import com.ats.android.viewmodels.MapUiState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.google.android.gms.maps.model.MapStyleOptions
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext


/**
 * Map Screen matching iOS ModernAdminMapView design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IOSMapScreen(
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val employeeLocations by viewModel.employeeLocations.collectAsState()
    val mapCenter by viewModel.mapCenter.collectAsState()
    
    // Dark Mode Map Style
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val mapStyleOptions = remember(isDarkTheme) {
        if (isDarkTheme) {
            MapStyleOptions.loadRawResourceStyle(context, com.ats.android.R.raw.map_style_dark)
        } else {
            null
        }
    }
    
    // Log state for debugging
    LaunchedEffect(uiState, employeeLocations.size) {
        Log.d("IOSMapScreen", "🗺️ UI State: $uiState, Locations: ${employeeLocations.size}")
    }
    
    var expandedSearch by remember { mutableStateOf(false) }
    var expandedEmployeeList by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var selectedEmployeeId by remember { mutableStateOf<String?>(null) }
    var searchLocation by remember { mutableStateOf<LatLng?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Default map center (Riyadh, Saudi Arabia)
    val defaultCenter = LatLng(24.7136, 46.6753)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(mapCenter ?: defaultCenter, 12f)
    }
    
    // Update camera when map center changes
    LaunchedEffect(mapCenter) {
        mapCenter?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(it, 14f)
                )
            )
        }
    }
    
    // Move camera to selected employee
    LaunchedEffect(selectedEmployeeId) {
        selectedEmployeeId?.let { id ->
            employeeLocations.find { it.employeeId == id }?.let { employee ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(employee.position, 15f)
                    ),
                    durationMs = 1000
                )
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Full-screen Google Map (always show, even while loading)
        when (uiState) {
            is MapUiState.Loading -> {
                // Show map immediately with loading indicator
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        mapType = MapType.NORMAL,
                        mapStyleOptions = mapStyleOptions
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        compassEnabled = true
                    )
                )
                
                // Loading indicator overlay
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard {
                        Row(
                            modifier = Modifier.padding(Spacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text( stringResource(com.ats.android.R.string.loading_locations))
                        }
                    }
                }
            }
            is MapUiState.Error -> {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        mapType = MapType.NORMAL,
                        mapStyleOptions = mapStyleOptions
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        compassEnabled = true
                    )
                )
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(modifier = Modifier.padding(Spacing.lg)) {
                        Column(
                            modifier = Modifier.padding(Spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                (uiState as MapUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.refresh() }) {
                                Text( stringResource(com.ats.android.R.string.retry))
                            }
                        }
                    }
                }
            }
            is MapUiState.Success -> {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        mapType = MapType.NORMAL,
                        mapStyleOptions = mapStyleOptions
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        compassEnabled = true
                    )
                ) {
                    // Employee markers
                    employeeLocations.forEach { location ->
                        Marker(
                            state = MarkerState(position = location.position),
                            title = location.employeeName,
                            snippet = "${location.role} • ${location.placeName ?: stringResource(com.ats.android.R.string.unknown)}",
                            tag = location.employeeId,
                            onClick = {
                                selectedEmployeeId = location.employeeId
                                true
                            }
                        )
                    }
                    
                    // Search location marker (if searching)
                    searchLocation?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = stringResource(com.ats.android.R.string.search_location),
                            icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                                .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }
                }
            }
        }
        
        // Debug info and refresh button (top right)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Debug info
            GlassCard {
                Text(
                    text = "${employeeLocations.size} locations",
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            // Refresh button
            GlassCard(
                modifier = Modifier.clickable { viewModel.refresh() }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(com.ats.android.R.string.refresh),
                    modifier = Modifier.padding(Spacing.sm),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Top Search Bar (iOS style)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = expandedSearch,
                transitionSpec = {
                    fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically()
                },
                label = "search_animation"
            ) { expanded ->
                if (expanded) {
                    ExpandedSearchBar(
                        searchText = searchText,
                        onSearchTextChange = { searchText = it },
                        onCancel = {
                            expandedSearch = false
                            searchText = ""
                            searchLocation = null
                        },
                        onSearch = { query ->
                            // Simulate search - in production, use Google Places API
                            // For now, just move to a location near Riyadh
                            searchLocation = LatLng(
                                24.7136 + (Math.random() * 0.1 - 0.05),
                                46.6753 + (Math.random() * 0.1 - 0.05)
                            )
                            scope.launch {
                                searchLocation?.let {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition.fromLatLngZoom(it, 14f)
                                        )
                                    )
                                }
                            }
                        }
                    )
                } else {
                    CompactSearchBar(
                        onSearchClick = {
                            expandedSearch = true
                            expandedEmployeeList = false
                        },
                        onFilterClick = { /* TODO: Show filters */ }
                    )
                }
            }
        }
        
        // Bottom Employee List (iOS style)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            if (expandedEmployeeList) {
                EmployeeListBottomSheet(
                    employees = employeeLocations,
                    searchLocation = searchLocation,
                    selectedEmployeeId = selectedEmployeeId,
                    onEmployeeClick = { employeeId ->
                        selectedEmployeeId = employeeId
                    },
                    onDismiss = {
                        expandedEmployeeList = false
                    }
                )
            } else if (employeeLocations.isNotEmpty() && !expandedSearch) {
                CompactEmployeeButton(
                    employeeCount = employeeLocations.size,
                    onClick = {
                        expandedEmployeeList = true
                        expandedSearch = false
                    }
                )
            }
        }
    }
}

/**
 * Compact Search Bar (iOS style with glass effect)
 */
@Composable
fun CompactSearchBar(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Search button
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onSearchClick),
            cornerRadius = CornerRadius.large
        ) {
            Row(
                modifier = Modifier.padding(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(com.ats.android.R.string.search_location),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Filter button (circle)
        GlassCard(
            modifier = Modifier.clickable(onClick = onFilterClick),
            cornerRadius = 50.dp // Circle
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = stringResource(com.ats.android.R.string.filter),
                modifier = Modifier.padding(Spacing.md),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Expanded Search Bar (iOS style)
 */
@Composable
fun ExpandedSearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSearch: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        cornerRadius = CornerRadius.large
    ) {
        Column {
            // Search field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                TextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    placeholder = { Text(stringResource(com.ats.android.R.string.search_places)) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    textStyle = TextStyle(
                        textDirection = TextDirection.Content
                    )
                )
                
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchTextChange("") }) {
                        Icon(Icons.Default.Clear, stringResource(com.ats.android.R.string.clear))
                    }
                }
                
                TextButton(onClick = onCancel) {
                    Text(stringResource(com.ats.android.R.string.cancel), color = ATSColors.SupervisorBlue)
                }
            }
            
            // Search suggestions - will be populated by Google Places API when implemented
            if (searchText.isNotEmpty()) {
                Divider()
                
                ListItem(
                    modifier = Modifier.clickable {
                        onSearch(searchText)
                    },
                    headlineContent = { Text(stringResource(com.ats.android.R.string.search_for, searchText)) },
                    leadingContent = {
                        Icon(Icons.Default.Search, null)
                    }
                )
            }
        }
    }
}

/**
 * Compact Employee Button (iOS style)
 */
@Composable
fun CompactEmployeeButton(
    employeeCount: Int,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg)
            .clickable(onClick = onClick),
        cornerRadius = CornerRadius.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActiveStatusDot(isActive = true, size = 8.dp)
                Text(
                    text = stringResource(com.ats.android.R.string.active_employees_count, employeeCount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Icon(
                imageVector = Icons.Default.ExpandLess,
                contentDescription = stringResource(com.ats.android.R.string.expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Employee List Bottom Sheet (iOS style)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListBottomSheet(
    employees: List<EmployeeLocation>,
    searchLocation: LatLng?,
    selectedEmployeeId: String?,
    onEmployeeClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = CornerRadius.xlarge,
            topEnd = CornerRadius.xlarge
        ),
        dragHandle = {
            // Custom drag handle (iOS style)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = Spacing.sm)
                        .width(36.dp)
                        .height(5.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            // Header
            Text(
                text = stringResource(com.ats.android.R.string.active_employees_count, employees.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )
            
            Divider()
            
            // Employee list
            LazyColumn {
                items(
                    items = employees, 
                    key = { java.util.UUID.randomUUID().toString() }
                ) { location ->
                    EmployeeLocationRow(
                        location = location,
                        searchLocation = searchLocation,
                        isSelected = location.employeeId == selectedEmployeeId,
                        onClick = { onEmployeeClick(location.employeeId) }
                    )
                    Divider()
                }
            }
        }
    }
}

/**
 * Employee Location Row (iOS style)
 */
@Composable
fun EmployeeLocationRow(
    location: EmployeeLocation,
    searchLocation: LatLng?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val distance = searchLocation?.let { search ->
        calculateDistance(search, location.position)
    }
    
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.background(ATSColors.SupervisorBlue.copy(alpha = 0.1f))
                } else Modifier
            ),
        leadingContent = {
            // Use the EmployeeAvatar component (already created!)
            SimpleAvatar(
                avatarUrl = null, // TODO: Get from employee
                name = location.employeeName,
                roleColor = getRoleColor(location.role),
                size = AvatarSize.large
            )
        },
        headlineContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = location.employeeName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                RoleBadge(role = location.role)
            }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                location.placeName?.let { place ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = place,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                distance?.let { dist ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = ATSColors.SupervisorBlue
                        )
                        Text(
                            text = dist,
                            style = MaterialTheme.typography.labelSmall,
                            color = ATSColors.SupervisorBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/**
 * Calculate distance between two LatLng points
 */
fun calculateDistance(from: LatLng, to: LatLng): String {
    val results = FloatArray(1)
    Location.distanceBetween(
        from.latitude, from.longitude,
        to.latitude, to.longitude,
        results
    )
    
    val distanceKm = results[0] / 1000
    return if (distanceKm < 1) {
        "${(distanceKm * 1000).toInt()}m"
    } else {
        "%.1fkm".format(distanceKm)
    }
}
