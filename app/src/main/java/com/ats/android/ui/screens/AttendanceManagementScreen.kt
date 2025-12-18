package com.ats.android.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.models.*
import com.ats.android.ui.components.GlassCard
import com.ats.android.ui.theme.Spacing
import com.ats.android.viewmodels.LocationRestrictionsViewModel
import androidx.compose.ui.res.stringResource
import com.ats.android.R
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

/**
 * Attendance Management Screen combining Shifts and Location Restrictions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun AttendanceManagementScreen(
    viewModel: LocationRestrictionsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val restrictionType by viewModel.restrictionType.collectAsState()
    val allowedLocations by viewModel.allowedLocations.collectAsState()
    val appliesToAllEmployees by viewModel.appliesToAllEmployees.collectAsState()
    val selectedEmployeeIds by viewModel.selectedEmployeeIds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    var showLocationSearch by remember { mutableStateOf(false) }
    var showEmployeeSelection by remember { mutableStateOf(false) }
    var selectedLocationForMap by remember { mutableStateOf<AllowedLocation?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadConfiguration()
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.attendance_locations),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLocationSearch = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_location),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
             // Main content
             LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Main Header content extracted from original LocationRestrictionsTab
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.sm)
                    ) {
                        Text(
                            text = stringResource(R.string.attendance_locations_desc),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Error message
                errorMessage?.let { error ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                // Success message
                successMessage?.let { success ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Text(
                                    text = success,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }
                }
                
                // Restriction Type Section
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(Spacing.lg)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = Spacing.md)
                            ) {
                                Icon(
                                    Icons.Default.Policy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    text = stringResource(R.string.check_in_policy),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = Spacing.sm))
                            
                            LocationRestrictionType.values().forEach { type ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (restrictionType == type) 
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                                    else 
                                        Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clickable { viewModel.updateRestrictionType(type) }
                                            .padding(Spacing.md),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = restrictionType == type,
                                            onClick = { viewModel.updateRestrictionType(type) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary,
                                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.md))
                                        Column {
                                            Text(
                                                text = when(type) {
                                                    LocationRestrictionType.ANYWHERE -> stringResource(R.string.restriction_anywhere)
                                                    LocationRestrictionType.SPECIFIC -> stringResource(R.string.restriction_specific)
                                                    LocationRestrictionType.MULTIPLE -> stringResource(R.string.restriction_multiple)
                                                },
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (restrictionType == type) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (restrictionType == type) 
                                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                                else 
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                            if (restrictionType == type) {
                                                Text(
                                                    text = when(type) {
                                                        LocationRestrictionType.ANYWHERE -> stringResource(R.string.desc_anywhere)
                                                        LocationRestrictionType.SPECIFIC -> stringResource(R.string.desc_specific)
                                                        LocationRestrictionType.MULTIPLE -> stringResource(R.string.desc_multiple)
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Allowed Locations Section
                if (restrictionType != LocationRestrictionType.ANYWHERE) {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(Spacing.md)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.allowed_locations),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                
                                if (allowedLocations.isEmpty()) {
                                    Spacer(modifier = Modifier.height(Spacing.sm))
                                    Text(
                                        text = stringResource(R.string.no_locations_added),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    // Location items
                    items(allowedLocations) { location ->
                        AllowedLocationCard(
                            location = location,
                            onRemove = { viewModel.removeLocation(location.id) },
                            onShowMap = { selectedLocationForMap = location },
                            onRadiusChange = { newRadius ->
                                viewModel.updateLocationRadius(location.id, newRadius)
                            }
                        )
                    }
                }
                
                // Employee Selection Section
                item {
                    Card {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(
                                text = stringResource(R.string.apply_to),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleAppliesToAllEmployees() }
                                    .padding(vertical = Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = appliesToAllEmployees,
                                    onClick = { viewModel.toggleAppliesToAllEmployees() }
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    text = stringResource(R.string.all_employees),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (appliesToAllEmployees) {
                                            viewModel.toggleAppliesToAllEmployees()
                                        }
                                        showEmployeeSelection = true
                                    }
                                    .padding(vertical = Spacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = !appliesToAllEmployees,
                                        onClick = {
                                            if (appliesToAllEmployees) {
                                                viewModel.toggleAppliesToAllEmployees()
                                            }
                                            showEmployeeSelection = true
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.sm))
                                    Text(
                                        text = stringResource(R.string.specific_employees),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                
                                if (!appliesToAllEmployees) {
                                    Text(
                                        text = stringResource(R.string.selected_count, selectedEmployeeIds.size),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            } // End LazyColumn
            
            // Enhanced Save Button at bottom
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    Divider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                    ) {
                        if (isLoading) {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.md))
                                    Text(
                                        stringResource(R.string.saving_configuration),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.saveConfiguration() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 8.dp
                                )
                            ) {
                                Icon(
                                    Icons.Default.Check, 
                                    stringResource(R.string.save_configuration),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    stringResource(R.string.save_configuration),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Dialogs code remains the same...
    if (showLocationSearch) {
        LocationSearchDialog(
            onDismiss = { showLocationSearch = false },
            onLocationSelected = { location ->
                viewModel.addLocation(location)
                showLocationSearch = false
            }
        )
    }
    
    if (showEmployeeSelection) {
        EmployeeSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showEmployeeSelection = false }
        )
    }
    
    selectedLocationForMap?.let { location ->
        LocationMapDialog(
            location = location,
            onDismiss = { selectedLocationForMap = null }
        )
    }
}

// MARK: - Allowed Location Card
@Composable
fun AllowedLocationCard(
    location: AllowedLocation,
    onRemove: () -> Unit,
    onShowMap: () -> Unit,
    onRadiusChange: (Double) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = location.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = location.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledIconButton(
                        onClick = onShowMap,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Map, stringResource(R.string.show_on_map))
                    }
                    FilledIconButton(
                        onClick = onRemove,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Delete, stringResource(R.string.remove))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            Divider()
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.RadioButtonChecked,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = stringResource(R.string.radius_meters, location.radius.toInt()),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

// MARK: - Location Search Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (AllowedLocation) -> Unit
) {
    // Temporary placeholder - Location search will be re-enabled
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_location)) },
        text = { Text(stringResource(R.string.location_search_disabled)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        }
    )
}

// MARK: - Employee Selection Dialog
@Composable
fun EmployeeSelectionDialog(
    viewModel: LocationRestrictionsViewModel,
    onDismiss: () -> Unit
) {
    val employees by viewModel.employees.collectAsState()
    val selectedIds by viewModel.selectedEmployeeIds.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_employees)) },
        text = {
            LazyColumn {
                items(employees) { employee ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.toggleEmployeeSelection(employee.employeeId)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedIds.contains(employee.employeeId),
                            onCheckedChange = {
                                viewModel.toggleEmployeeSelection(employee.employeeId)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(employee.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        }
    )
}

// MARK: - Location Map Dialog
@Composable
fun LocationMapDialog(
    location: AllowedLocation,
    onDismiss: () -> Unit
) {
    val position = LatLng(location.latitude, location.longitude)
    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, 15f)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(location.name) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = position),
                        title = location.name,
                        snippet = location.address
                    )
                    Circle(
                        center = position,
                        radius = location.radius,
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        strokeColor = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2f
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
