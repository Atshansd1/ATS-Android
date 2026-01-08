package com.ats.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.ats.android.models.GeoPointData
import com.ats.android.models.AttendanceCenter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.R

import com.ats.android.ui.components.GlassCard
import com.ats.android.ui.theme.Spacing
import com.ats.android.viewmodels.AttendanceCentersViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCenterScreen(
    viewModel: AttendanceCentersViewModel = viewModel(),
    centerId: String? = null,
    onNavigateBack: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val allEmployees by viewModel.allEmployees.collectAsState()
    val centerToEdit by viewModel.selectedCenter.collectAsState()

    // Load data if editing
    LaunchedEffect(centerId) {
        if (centerId != null) {
            viewModel.loadCenter(centerId)
        }
    }

    // Form State
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var radiusMeters by remember { mutableFloatStateOf(200f) }
    var selectedEmployeeIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isActive by remember { mutableStateOf(true) }
    
    // Bilingual Support
    var nameEn by remember { mutableStateOf("") }
    var nameAr by remember { mutableStateOf("") }
    var addressEn by remember { mutableStateOf("") }
    var addressAr by remember { mutableStateOf("") }
    
    // Location
    var latitude by remember { mutableDoubleStateOf(25.2048) } // Default Dubai
    var longitude by remember { mutableDoubleStateOf(55.2708) }

    // Populate form when data is loaded
    LaunchedEffect(centerToEdit) {
        centerToEdit?.let { center ->
            name = center.name
            address = center.address
            radiusMeters = center.radiusMeters.toFloat()
            selectedEmployeeIds = center.assignedEmployeeIds.toSet()
            isActive = center.isActive
            nameEn = center.nameEn ?: ""
            nameAr = center.nameAr ?: ""
            addressEn = center.addressEn ?: ""
            addressAr = center.addressAr ?: ""
            latitude = center.coordinate.latitude
            longitude = center.coordinate.longitude
        }
    }

    var showEmployeeSelection by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    
    // Handle Success Navigation
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (centerId == null) stringResource(R.string.new_location) 
                        else stringResource(R.string.edit_location)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, stringResource(R.string.cancel))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val center = if (centerId != null && centerToEdit != null) {
                                centerToEdit!!.copy(
                                    name = name,
                                    address = address,
                                    coordinate = GeoPointData(latitude, longitude),
                                    radiusMeters = radiusMeters.toDouble(),
                                    assignedEmployeeIds = selectedEmployeeIds.toList(),
                                    isActive = isActive,
                                    nameEn = nameEn.takeIf { it.isNotBlank() },
                                    nameAr = nameAr.takeIf { it.isNotBlank() },
                                    addressEn = addressEn.takeIf { it.isNotBlank() },
                                    addressAr = addressAr.takeIf { it.isNotBlank() },
                                )
                            } else {
                                AttendanceCenter(
                                    name = name,
                                    address = address,
                                    coordinate = GeoPointData(latitude, longitude),
                                    radiusMeters = radiusMeters.toDouble(),
                                    assignedEmployeeIds = selectedEmployeeIds.toList(),
                                    isActive = isActive,
                                    nameEn = nameEn.takeIf { it.isNotBlank() },
                                    nameAr = nameAr.takeIf { it.isNotBlank() },
                                    addressEn = addressEn.takeIf { it.isNotBlank() },
                                    addressAr = addressAr.takeIf { it.isNotBlank() }
                                )
                            }
                            
                            if (centerId == null) {
                                viewModel.createCenter(center)
                            } else {
                                viewModel.updateCenter(center)
                            }
                        },
                        enabled = name.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (centerId == null) stringResource(R.string.create) 
                                else stringResource(R.string.save),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            // Location Details Section
            SectionHeader(stringResource(R.string.location_details))
            GlassCard {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.location_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            
            // Bilingual Details Section
            SectionHeader(stringResource(R.string.bilingual_details))
            GlassCard {
                Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text(stringResource(R.string.name_en)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nameAr,
                        onValueChange = { nameAr = it },
                        label = { Text(stringResource(R.string.name_ar)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl)
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address (Default)") }, // Fallback
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addressEn,
                        onValueChange = { addressEn = it },
                        label = { Text(stringResource(R.string.address_en)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addressAr,
                        onValueChange = { addressAr = it },
                        label = { Text(stringResource(R.string.address_ar)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrRtl)
                    )
                }
            }

            // Location Picker Section
            SectionHeader(stringResource(R.string.location))
            GlassCard {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (address.isNotBlank()) address else "Select Location",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$latitude, $longitude",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showMapPicker = true }) {
                            Icon(Icons.Default.EditLocation, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = Spacing.sm))
                    
                    // Coordinate Input Section
                    Text(
                        text = stringResource(R.string.enter_coordinates),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    var latitudeText by remember { mutableStateOf(latitude.toString()) }
                    var longitudeText by remember { mutableStateOf(longitude.toString()) }
                    
                    // Update text fields when coordinates change from map
                    LaunchedEffect(latitude, longitude) {
                        latitudeText = String.format("%.6f", latitude)
                        longitudeText = String.format("%.6f", longitude)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        OutlinedTextField(
                            value = latitudeText,
                            onValueChange = { text ->
                                latitudeText = text
                                text.toDoubleOrNull()?.let { 
                                    if (it >= -90 && it <= 90) latitude = it 
                                }
                            },
                            label = { Text("Lat") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = longitudeText,
                            onValueChange = { text ->
                                longitudeText = text
                                text.toDoubleOrNull()?.let { 
                                    if (it >= -180 && it <= 180) longitude = it 
                                }
                            },
                            label = { Text("Lng") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = Spacing.md))
                    
                    // Radius Slider
                    Text(stringResource(R.string.radius))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("50m", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = radiusMeters,
                            onValueChange = { radiusMeters = it },
                            valueRange = 50f..1000f,
                            steps = 19, // (1000-50)/50 = 19 steps
                            modifier = Modifier.weight(1f).padding(horizontal = Spacing.sm)
                        )
                        Text("1km", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        "${radiusMeters.toInt()} meters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
            
            // Employees Section
            SectionHeader(stringResource(R.string.assigned_employees))
            GlassCard(modifier = Modifier.clickable { showEmployeeSelection = true }) {
                Row(
                    modifier = Modifier.padding(Spacing.md).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.employees))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         if (selectedEmployeeIds.isEmpty()) {
                            Text(stringResource(R.string.all_employees), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("${selectedEmployeeIds.size}", color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            // Active Toggle
            GlassCard {
                 Row(
                    modifier = Modifier.padding(Spacing.md).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.active_location))
                        Text(
                            stringResource(R.string.active_location_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        }
    }
    
    // Map Picker Dialog
    if (showMapPicker) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), 15f)
        }
        var pickerCenter by remember { mutableStateOf(LatLng(latitude, longitude)) }
        
        AlertDialog(
            onDismissRequest = { showMapPicker = false },
            title = { Text("Pick Location") },
            text = {
                Box(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState
                    ) {
                        Marker(
                            state = MarkerState(position = pickerCenter),
                            title = "Selected Location"
                        )
                    }
                    // Center indicator
                    Icon(
                        Icons.Default.LocationOn, 
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    // Update picker center as map moves
                    LaunchedEffect(cameraPositionState.isMoving) {
                        if (!cameraPositionState.isMoving) {
                            pickerCenter = cameraPositionState.position.target
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        latitude = pickerCenter.latitude
                        longitude = pickerCenter.longitude
                        showMapPicker = false
                    }
                ) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMapPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Employee Selection Dialog (Simple version for now, reusing existing ViewModel logic if possible or local)
    if (showEmployeeSelection) {
        AlertDialog(
            onDismissRequest = { showEmployeeSelection = false },
            title = { Text(stringResource(R.string.select_employees)) },
            text = {
                // Determine display list
                // For simplicity, we filter locally or use ViewModel
                val employees = allEmployees
                
                LazyColumn(modifier = Modifier.height(400.dp)) {
                     // "Select All" / "Deselect All" Logic could go here
                     item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedEmployeeIds.size == employees.size) {
                                        selectedEmployeeIds = emptySet()
                                    } else {
                                        selectedEmployeeIds = employees.map { it.employeeId }.toSet()
                                    }
                                }
                                .padding(8.dp),
                             horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                if (selectedEmployeeIds.size == employees.size) stringResource(R.string.deselect_all) 
                                else stringResource(R.string.select_all),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                     }
                     
                    items(employees) { employee ->
                        val isSelected = selectedEmployeeIds.contains(employee.employeeId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedEmployeeIds = if (isSelected) {
                                        selectedEmployeeIds - employee.employeeId
                                    } else {
                                        selectedEmployeeIds + employee.employeeId
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { 
                                     selectedEmployeeIds = if (it) {
                                        selectedEmployeeIds + employee.employeeId
                                    } else {
                                        selectedEmployeeIds - employee.employeeId
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(employee.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmployeeSelection = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs)
    )
}
