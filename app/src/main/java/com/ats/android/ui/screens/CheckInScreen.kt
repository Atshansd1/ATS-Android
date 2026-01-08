package com.ats.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.R
import com.ats.android.models.AttendanceRecord
import com.ats.android.models.Employee
import com.ats.android.ui.theme.CornerRadius
import com.ats.android.ui.components.EmployeeAvatar
import com.ats.android.ui.components.GlassCard
import com.ats.android.ui.theme.ATSColors
import com.ats.android.viewmodels.CheckInViewModel
import com.ats.android.viewmodels.CheckInUiState
import com.ats.android.viewmodels.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    currentEmployee: Employee?,
    viewModel: CheckInViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    ),
    historyViewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isCheckedIn by viewModel.isCheckedIn.collectAsState()
    val placeName by viewModel.placeName.collectAsState()
    val activeRecord by viewModel.activeRecord.collectAsState()
    
    // Permission States
    val showPermissionRequired by viewModel.showBackgroundPermissionRequired.collectAsState()
    val showPermissionDowngrade by viewModel.showPermissionDowngradeAlert.collectAsState()
    val countdown by viewModel.permissionCountdown.collectAsState()
    
    val attendanceRecords by historyViewModel.attendanceRecords.collectAsState()
    val context = LocalContext.current
    
    // Lifecycle Observer for Resume Check
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                currentEmployee?.let { viewModel.checkBackgroundPermissionForActiveSession(it) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(currentEmployee) {
        if (currentEmployee != null) {
            viewModel.initialize(currentEmployee)
            historyViewModel.loadHistory(currentEmployee)
        }
    }
    
    // Permission Required Alert
    if (showPermissionRequired) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBackgroundPermissionDialog() },
            title = { Text(stringResource(R.string.background_location_title)) },
            text = { Text(stringResource(R.string.background_location_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    }
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBackgroundPermissionDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Permission Downgrade Countdown Alert
    if (showPermissionDowngrade) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss */ },
            title = { Text(stringResource(R.string.permission_downgrade_title)) },
            text = { Text(stringResource(R.string.permission_downgrade_message, countdown)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    }
                ) {
                    Text(stringResource(R.string.restore_permission))
                }
            }
        )
    }
    
    // Status Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val statusColor by animateColorAsState(
        targetValue = if (isCheckedIn) ATSColors.CheckInGreen else MaterialTheme.colorScheme.primary,
        label = "statusColor"
    )
    
    // Snackbar for error messages
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error in snackbar when uiState is Error
    LaunchedEffect(uiState) {
        if (uiState is CheckInUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as CheckInUiState.Error).message,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Expressive Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            statusColor.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            if (isCheckedIn) stringResource(R.string.check_out_title) else stringResource(R.string.check_in_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            // Calculate stats
            val totalDays = attendanceRecords.size
            val totalHours = attendanceRecords.sumOf { record ->
                record.durationHours * 3600
            } / 3600.0
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Employee Profile Greeting
                item {
                    currentEmployee?.let { employee ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = CornerRadius.large
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                EmployeeAvatar(
                                    avatarUrl = employee.avatarURL,
                                    employeeName = employee.displayName,
                                    size = 56.dp
                                )
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = stringResource(R.string.hello_greeting, employee.displayName.split(" ").firstOrNull() ?: ""),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = employee.role.value.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Active Status Card (The "Big Button" Area)
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = CornerRadius.large,
                        backgroundColor = if (isCheckedIn) ATSColors.CheckInGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Pulse Status Indicator
                            Box(contentAlignment = Alignment.Center) {
                                if (isCheckedIn) {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .scale(pulseScale)
                                            .background(ATSColors.CheckInGreen.copy(alpha = 0.2f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .scale(pulseScale)
                                            .background(ATSColors.CheckInGreen.copy(alpha = 0.3f), CircleShape)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(statusColor, CircleShape)
                                        .shadow(10.dp, CircleShape, spotColor = statusColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCheckedIn) Icons.Default.Check else Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isCheckedIn) stringResource(R.string.currently_checked_in) else stringResource(R.string.ready_to_check_in),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCheckedIn) ATSColors.CheckInGreen else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Location Pill
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(50),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = when (placeName) {
                                                null -> stringResource(R.string.loading_location)
                                                "permission_required" -> stringResource(R.string.location_required)
                                                "location_unavailable" -> stringResource(R.string.unable_to_get_location)
                                                else -> placeName ?: stringResource(R.string.loading_location)
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            // Check In/Out Button (Main Action)
                            Button(
                                onClick = {
                                    if (currentEmployee != null) {
                                        if (isCheckedIn) viewModel.checkOut(currentEmployee) else viewModel.checkIn(currentEmployee)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = currentEmployee != null && uiState !is CheckInUiState.Processing,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = statusColor
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp)
                            ) {
                                if (uiState is CheckInUiState.Processing) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                } else {
                                    Text(
                                        text = if (isCheckedIn) stringResource(R.string.check_out_button) else stringResource(R.string.check_in_button),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // History Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.recent_activity),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Mini Stats
                        if (totalDays > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.stats_summary_fmt, totalHours, totalDays),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                // Recent Items
                if (attendanceRecords.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            cornerRadius = CornerRadius.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                                Text(stringResource(R.string.no_recent_activity), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(attendanceRecords.take(5)) { record ->
                        CheckInHistoryRecordCard(record)
                    }
                }
            }
        }
    }
}

@Composable
fun CheckInHistoryRecordCard(record: AttendanceRecord) {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val isActive = record.checkOutTime == null
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = CornerRadius.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                // Date Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isActive) ATSColors.CheckInGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .width(50.dp)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dateFormat.format(record.checkInTime.toDate()).split(" ").getOrElse(0) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(record.checkInTime.toDate()).split(" ").getOrElse(1) { "" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) ATSColors.CheckInGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Times
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isActive) stringResource(R.string.currently_active) else "${timeFormat.format(record.checkInTime.toDate())} - ${record.checkOutTime?.let { timeFormat.format(it.toDate()) } ?: "..."}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) ATSColors.CheckInGreen else MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (record.checkInPlaceName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                             Icon(Icons.Default.Place, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                             Text(
                                text = record.checkInPlaceName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            
            // Duration
            if (!isActive) {
                 val totalSeconds = (record.totalDuration ?: record.duration?.toDouble() ?: 0.0).toLong()
                 val hours = TimeUnit.SECONDS.toHours(totalSeconds)
                 val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
                 
                 Text(
                     text = String.format("%dh %dm", hours, minutes),
                     style = MaterialTheme.typography.labelSmall,
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
            }
        }
    }
}
