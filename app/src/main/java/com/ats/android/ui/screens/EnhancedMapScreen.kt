package com.ats.android.ui.screens

import android.location.Location
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import android.util.Log
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ats.android.R
import com.ats.android.ui.components.*
import com.ats.android.ui.theme.*
import com.ats.android.viewmodels.EmployeeLocation
import com.ats.android.viewmodels.MapViewModel
import com.ats.android.viewmodels.MapUiState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import com.google.android.gms.maps.model.MapStyleOptions
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.combinedClickable

/**
 * Enhanced Map Screen matching iOS ModernAdminMapView with Material 3 Expressive Design
 * 
 * Features:
 * - Expandable search bar with Google Places integration
 * - Expandable employee list showing online employees with distances
 * - Employee pinning on click with camera animation
 * - Nearest employee highlighting in km
 * - Stacked avatars in compact button (iOS style)
 * - Material 3 Expressive design with glass morphism
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedMapScreen(
    selectedEmployeeId: String? = null
) {
    val context = LocalContext.current
    val viewModel: MapViewModel = viewModel(
        factory = com.ats.android.viewmodels.MapViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()
    val employeeLocations by viewModel.employeeLocations.collectAsState()
    val searchLocation by viewModel.searchLocation.collectAsState()
    val nearestEmployee by viewModel.nearestEmployee.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val selectedPlaceDetails by viewModel.selectedPlaceDetails.collectAsState()
    
    var expandedSearch by remember { mutableStateOf(false) }
    var expandedEmployeeList by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    // Initialize with passed ID if present, otherwise null
    var currentSelectedEmployeeId by remember(selectedEmployeeId) { mutableStateOf(selectedEmployeeId) }
    var showForceCheckoutDialog by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val locationService = remember { com.ats.android.services.LocationService(context) }
    
    // Dark Mode Map Style
    val isDarkTheme = isSystemInDarkTheme()
    val mapStyleOptions = remember(isDarkTheme) {
        if (isDarkTheme) {
            MapStyleOptions.loadRawResourceStyle(context, com.ats.android.R.raw.map_style_dark)
        } else {
            null
        }
    }
    
    // Get user's current location for map center
    var defaultCenter by remember { mutableStateOf<LatLng?>(null) }
    
    // Get user location on first load
    LaunchedEffect(Unit) {
        try {
            android.util.Log.d("EnhancedMapScreen", "Getting user location for map center...")
            val location = locationService.getCurrentLocation()
            if (location != null) {
                defaultCenter = LatLng(location.latitude, location.longitude)
                android.util.Log.d("EnhancedMapScreen", "✅ Map centered on user location: ${location.latitude}, ${location.longitude}")
            } else {
                android.util.Log.w("EnhancedMapScreen", "⚠️ No location available, using default")
                defaultCenter = LatLng(24.7136, 46.6753) // Fallback to Riyadh
            }
        } catch (e: Exception) {
            android.util.Log.e("EnhancedMapScreen", "❌ Error getting location: ${e.message}")
            defaultCenter = LatLng(24.7136, 46.6753) // Fallback to Riyadh
        }
    }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            defaultCenter ?: LatLng(24.7136, 46.6753), 
            12f
        )
    }
    
    // Update camera when location is loaded
    LaunchedEffect(defaultCenter) {
        defaultCenter?.let { center ->
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(center, 12f)
                ),
                durationMs = 1000
            )
        }
    }
    
    // Move camera to selected employee
    LaunchedEffect(currentSelectedEmployeeId) {
        currentSelectedEmployeeId?.let { id ->
            employeeLocations.find { it.employeeId == id }?.let { employee ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(employee.position, 15f)
                    ),
                    durationMs = 1000
                )
                
                // Auto-deselect after 5 seconds (matching iOS)
                delay(5000)
                currentSelectedEmployeeId = null
            }
        }
    }
    
    // Force Checkout Confirmation Dialog
    if (showForceCheckoutDialog != null) {
        AlertDialog(
            onDismissRequest = { showForceCheckoutDialog = null },
            title = { Text(stringResource(R.string.force_checkout)) },
            text = { Text(stringResource(R.string.force_checkout_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForceCheckoutDialog?.let { viewModel.forceCheckout(it) }
                        showForceCheckoutDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.force_checkout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showForceCheckoutDialog = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
    
    // Move camera to search location
    LaunchedEffect(searchLocation) {
        searchLocation?.let { location ->
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(location, 14f)
                ),
                durationMs = 1000
            )
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Google Map Layer
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = true,
                        mapType = MapType.NORMAL,
                        mapStyleOptions = mapStyleOptions
                    ),       uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true
            )
        ) {
            // Employee markers
            for (location in employeeLocations) {
                val isSelected = location.employeeId == currentSelectedEmployeeId
                val isNearest = location.employeeId == nearestEmployee?.employeeId
                
                // Manually load bitmap to avoid AsyncImage crash in MarkerComposable
                var avatarBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
                val context = LocalContext.current
                
                LaunchedEffect(location.avatarUrl) {
                    if (location.avatarUrl != null) {
                        try {
                            val request = coil.request.ImageRequest.Builder(context)
                                .data(location.avatarUrl)
                                .allowHardware(false) // Important for Map markers
                                .size(100, 100)
                                .build()
                            
                            val result = coil.ImageLoader(context).execute(request)
                            if (result is coil.request.SuccessResult) {
                                avatarBitmap = result.drawable.toBitmap().asImageBitmap()
                            } else if (result is coil.request.ErrorResult) {
                                Log.e("EnhancedMapScreen", "Error loading avatar: ${result.throwable.message}")
                            }
                        } catch (e: Exception) {
                            Log.e("EnhancedMapScreen", "Exception loading avatar", e)
                        }
                    }
                }

                MarkerComposable(
                    state = MarkerState(position = location.position),
                    keys = arrayOf(location.employeeId, isSelected, isNearest, avatarBitmap ?: "no_avatar"), // Ensure non-null
                    title = location.employeeName,
                    snippet = buildString {
                        append("${location.role}")
                        location.placeName?.let { append(" • $it") }
                        if (isNearest) append(" • NEAREST")
                    },
                    tag = location.employeeId,
                    onClick = {
                        currentSelectedEmployeeId = location.employeeId
                        true
                    },
                    zIndex = if (isSelected) 1.0f else 0.0f
                ) {
                    val roleColor = getRoleColor(location.role)
                    val borderColor = if (isNearest) ATSColors.ActiveNowGreen else Color.White
                    
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 60.dp else 48.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Avatar
                        if (avatarBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = avatarBitmap!!,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected || isNearest) 3.dp else 2.dp,
                                        color = borderColor,
                                        shape = CircleShape
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Fallback with initials
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                roleColor.copy(alpha = 0.8f),
                                                roleColor
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = if (isSelected || isNearest) 3.dp else 2.dp,
                                        color = borderColor,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = location.employeeName.firstOrNull()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontSize = if (isSelected) 24.sp else 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Active/Nearest Indicator Dot
                        if (isNearest || isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-2).dp, y = (-2).dp)
                                    .size(12.dp)
                                    .background(ATSColors.ActiveNowGreen, CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }
            }
            
            // Search location marker
            searchLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "Search Location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }
        }
        
        // Top Search Bar (iOS style with glass effect)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
                .padding(top = 48.dp)
        ) {
            AnimatedContent(
                targetState = expandedSearch,
                transitionSpec = {
                    (fadeIn() + expandVertically()) togetherWith (fadeOut() + shrinkVertically())
                },
                label = "search_animation"
            ) { expanded ->
                if (expanded) {
                    ExpandedSearchView(
                        searchText = searchText,
                        onSearchTextChange = { searchText = it },
                        onSearch = { viewModel.searchPlaces(it) },
                        onCancel = {
                            expandedSearch = false
                            searchText = ""
                            viewModel.clearSearch()
                        },
                        searchResults = searchResults,
                        isSearching = isSearching,
                        searchError = searchError,
                        employeeLocations = employeeLocations,
                        nearestEmployee = nearestEmployee,
                        selectedPlaceDetails = selectedPlaceDetails,
                        onPlaceSelect = { placeId ->
                            viewModel.selectPlace(placeId)
                        },
                        onEmployeeClick = { employeeId ->
                            currentSelectedEmployeeId = employeeId
                            expandedEmployeeList = false
                            expandedSearch = false
                        }
                    )
                } else {
                    CompactSearchBarView(
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
                .zIndex(9f)
        ) {
            if (expandedEmployeeList) {
                ExpandedEmployeeListView(
                    employees = if (searchLocation != null) {
                        viewModel.getSortedEmployeesByDistance()
                    } else {
                        employeeLocations
                    },
                    searchLocation = searchLocation,
                    selectedEmployeeId = selectedEmployeeId,
                    nearestEmployeeId = nearestEmployee?.employeeId,
                    onEmployeeClick = { employeeId ->
                        currentSelectedEmployeeId = employeeId
                        expandedEmployeeList = false
                    },
                    onEmployeeLongClick = { employeeId ->
                        showForceCheckoutDialog = employeeId
                    },
                    onDismiss = {
                        expandedEmployeeList = false
                    }
                )
            } else if (employeeLocations.isNotEmpty() && !expandedSearch) {
                CompactEmployeeButtonView(
                    employeeCount = employeeLocations.size,
                    employees = employeeLocations.take(3),
                    onClick = {
                        expandedEmployeeList = true
                        expandedSearch = false
                    }
                )
            }
        }
        
        // Loading overlay
        if (uiState is MapUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                GlassCard {
                    Row(
                        modifier = Modifier.padding(Spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Loading locations...")
                    }
                }
            }
        }
    }
}

/**
 * Compact Search Bar (iOS style with glass morphism)
 */
@Composable
fun CompactSearchBarView(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
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
                    text = stringResource(R.string.search_location),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        GlassCard(
            modifier = Modifier.clickable(onClick = onFilterClick),
            cornerRadius = 50.dp
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter",
                modifier = Modifier.padding(Spacing.md),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Expanded Search View (iOS style with results and nearby employees)
 */
@Composable
fun ExpandedSearchView(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onCancel: () -> Unit,
    searchResults: List<com.ats.android.models.GooglePlacePrediction>,
    isSearching: Boolean,
    searchError: String?,
    employeeLocations: List<EmployeeLocation>,
    nearestEmployee: EmployeeLocation?,
    selectedPlaceDetails: com.ats.android.models.GooglePlaceDetails?,
    onPlaceSelect: (String) -> Unit,
    onEmployeeClick: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        cornerRadius = CornerRadius.large
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Search input
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
                    onValueChange = {
                        onSearchTextChange(it)
                        onSearch(it)
                    },
                    placeholder = { Text(stringResource(R.string.search_places)) },
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
                    IconButton(onClick = {
                        onSearchTextChange("")
                        onSearch("")
                    }) {
                        Icon(Icons.Default.Clear, stringResource(R.string.clear))
                    }
                }
                
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel), color = ATSColors.SupervisorBlue)
                }
            }
            
            // Results
            if (selectedPlaceDetails != null) {
                // Show selected place with nearby employees (iOS style)
                Divider()
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 500.dp)
                ) {
                    item {
                        SearchResultItemWithEmployees(
                            place = selectedPlaceDetails,
                            employeeLocations = employeeLocations,
                            onPlaceSelect = { /* Already selected */ },
                            onEmployeeClick = onEmployeeClick
                        )
                    }
                }
            } else if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (searchError != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        Icons.Default.Error,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        stringResource(R.string.search_error),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        searchError, // Keep exact error message from ViewModel
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (searchResults.isNotEmpty()) {
                Divider()
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(searchResults) { place ->
                        SearchResultItem(
                            place = place,
                            employeeLocations = employeeLocations,
                            nearestEmployee = nearestEmployee,
                            onPlaceSelect = { onPlaceSelect(place.placeId) },
                            onEmployeeClick = onEmployeeClick
                        )
                        Divider()
                    }
                }
            } else if (searchText.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        Icons.Default.SearchOff,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        stringResource(R.string.no_results_found),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.try_different_search),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Search Result Item with nearby employees (iOS style)
 * Shows nearest employees with distances, matching iOS implementation
 */
@Composable
fun SearchResultItem(
    place: com.ats.android.models.GooglePlacePrediction,
    employeeLocations: List<EmployeeLocation>,
    nearestEmployee: EmployeeLocation?,
    onPlaceSelect: () -> Unit,
    onEmployeeClick: (String) -> Unit
) {
    // Note: We need to fetch place details to get coordinates
    // For now, we'll show this is a placeholder until place is selected
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlaceSelect)
            .padding(Spacing.lg)
    ) {
        // Place info
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.mainText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (place.secondaryText.isNotEmpty()) {
                    Text(
                        text = place.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Show nearby employees count (will calculate after selection)
        if (employeeLocations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.active_employees_nearby_fmt, employeeLocations.size),
                style = MaterialTheme.typography.labelSmall,
                color = ATSColors.SupervisorBlue,
                modifier = Modifier.padding(start = 36.dp)
            )
        }
    }
}

/**
 * Search Result Item with Place Details (shows actual employees with distances)
 * This is shown after place details are fetched - matching iOS
 */
@Composable
fun SearchResultItemWithEmployees(
    place: com.ats.android.models.GooglePlaceDetails,
    employeeLocations: List<EmployeeLocation>,
    onPlaceSelect: () -> Unit,
    onEmployeeClick: (String) -> Unit
) {
    // Calculate distances and find nearest
    val employeesWithDistances = remember(place, employeeLocations) {
        val placeLatLng = LatLng(place.latitude, place.longitude)
        employeeLocations.map { employee ->
            val distance = calculateDistanceInKm(placeLatLng, employee.position)
            Triple(employee, distance, false) // Will mark nearest after sorting
        }.sortedBy { it.second }.mapIndexed { index, triple ->
            // First one is nearest
            Triple(triple.first, triple.second, index == 0)
        }
    }
    
    val nearestEmployees = employeesWithDistances.take(5)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlaceSelect)
            .padding(Spacing.lg)
    ) {
        // Place info
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.LocationOn,
                null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = place.formattedAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Nearby employees section (iOS style)
        if (nearestEmployees.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Column(
                modifier = Modifier.padding(start = 36.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "NEARBY EMPLOYEES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                nearestEmployees.forEach { (employee, distance, isNearest) ->
                    NearbyEmployeeItem(
                        employee = employee,
                        distance = distance,
                        isNearest = isNearest,
                        onClick = { onEmployeeClick(employee.employeeId) }
                    )
                }
            }
        }
    }
}

/**
 * Nearby Employee Item (iOS style)
 */
@Composable
fun NearbyEmployeeItem(
    employee: EmployeeLocation,
    distance: Double,
    isNearest: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                if (isNearest) Color.Green.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (employee.avatarUrl != null) {
            AsyncImage(
                model = employee.avatarUrl,
                contentDescription = employee.employeeName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isNearest) Color.Green else getRoleColor(employee.role)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = employee.employeeName.first().uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        
        // Employee info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = employee.employeeName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(employee.role.labelResId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Distance
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDistance(distance),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isNearest) Color.Green else ATSColors.SupervisorBlue
            )
            
            if (isNearest) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.nearest),
                    tint = Color.Green,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Calculate distance between two LatLng points in kilometers
 */
fun calculateDistanceInKm(from: LatLng, to: LatLng): Double {
    val results = FloatArray(1)
    Location.distanceBetween(
        from.latitude, from.longitude,
        to.latitude, to.longitude,
        results
    )
    return (results[0] / 1000.0) // Convert to km
}

/**
 * Format distance (km or m)
 */
fun formatDistance(distanceKm: Double): String {
    return if (distanceKm < 1) {
        "${(distanceKm * 1000).toInt()} m"
    } else {
        "%.1f km".format(distanceKm)
    }
}

/**
 * Compact Employee Button (iOS style with stacked avatars)
 */
@Composable
fun CompactEmployeeButtonView(
    employeeCount: Int,
    employees: List<EmployeeLocation>,
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
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stacked avatars (iOS style)
                Box(modifier = Modifier.width(80.dp)) {
                    employees.forEachIndexed { index, employee ->
                        Box(
                            modifier = Modifier
                                .offset(x = (index * 20).dp)
                                .size(40.dp)
                                .zIndex((employees.size - index).toFloat())
                        ) {
                            if (employee.avatarUrl != null) {
                                AsyncImage(
                                    model = employee.avatarUrl,
                                    contentDescription = employee.employeeName,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color.White, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color.White, CircleShape)
                                        .background(getRoleColor(employee.role), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = employee.employeeName.first().uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                
                Column {
                    Text(
                        text = stringResource(R.string.active_count, employeeCount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.tap_to_view_all),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ExpandLess,
                contentDescription = "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Expanded Employee List View (iOS style bottom sheet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedEmployeeListView(
    employees: List<EmployeeLocation>,
    searchLocation: LatLng?,
    selectedEmployeeId: String?,
    nearestEmployeeId: String?,
    onEmployeeClick: (String) -> Unit,
    onEmployeeLongClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = CornerRadius.xlarge,
            topEnd = CornerRadius.xlarge
        ),
        dragHandle = {
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
                .heightIn(max = 600.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.active_employees),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.checked_in_count, employees.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider()
            
            // Employee list
            LazyColumn {
                items(employees, key = { it.employeeId }) { employee ->
                    EmployeeListItem(
                        employee = employee,
                        searchLocation = searchLocation,
                        isSelected = employee.employeeId == selectedEmployeeId,
                        isNearest = employee.employeeId == nearestEmployeeId,
                        onClick = { onEmployeeClick(employee.employeeId) },
                        onLongClick = { onEmployeeLongClick(employee.employeeId) }
                    )
                    Divider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

/**
 * Employee List Item (iOS style with distance)
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EmployeeListItem(
    employee: EmployeeLocation,
    searchLocation: LatLng?,
    isSelected: Boolean,
    isNearest: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val distance = searchLocation?.let { search ->
        val results = FloatArray(1)
        Location.distanceBetween(
            search.latitude, search.longitude,
            employee.position.latitude, employee.position.longitude,
            results
        )
        val distanceKm = results[0] / 1000
        if (distanceKm < 1) "${(distanceKm * 1000).toInt()}m" else "%.1f km".format(distanceKm)
    }
    
    ListItem(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (isSelected) {
                    Modifier.background(ATSColors.SupervisorBlue.copy(alpha = 0.1f))
                } else Modifier
            ),
        leadingContent = {
            Box {
                if (employee.avatarUrl != null) {
                    AsyncImage(
                        model = employee.avatarUrl,
                        contentDescription = employee.employeeName,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier.border(3.dp, Color.Green, CircleShape)
                                } else Modifier
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(getRoleColor(employee.role))
                            .then(
                                if (isSelected) {
                                    Modifier.border(3.dp, Color.Green, CircleShape)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = employee.employeeName.first().uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        },
        headlineContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = employee.employeeName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (isNearest) {
                    Icon(
                        Icons.Default.CheckCircle,
                        "Nearest",
                        tint = Color.Green,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RoleBadge(role = employee.role)
                
                employee.placeName?.let { place ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
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
                
                employee.checkInTime?.let { time ->
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Text(
                        text = context.getString(com.ats.android.R.string.since, android.text.format.DateFormat.getTimeFormat(context).format(time)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                distance?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isNearest) Color.Green else ATSColors.SupervisorBlue
                    )
                }
                
                Icon(
                    imageVector = if (isSelected) Icons.Default.LocationOn else Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isSelected) Color.Green else ATSColors.SupervisorBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}
