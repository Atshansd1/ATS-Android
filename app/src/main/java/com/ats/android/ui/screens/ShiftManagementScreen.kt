package com.ats.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.models.DaySchedule
import com.ats.android.models.ShiftConfig
import com.ats.android.models.WorkDay
import com.ats.android.ui.components.GlassCard
import com.ats.android.ui.theme.ATSColors
import com.ats.android.ui.theme.Spacing
import com.ats.android.viewmodels.ShiftManagementViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftManagementScreen(
    viewModel: ShiftManagementViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val shiftConfig by viewModel.shiftConfig.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var showTimePicker by remember { mutableStateOf<Pair<WorkDay, Boolean>?>(null) } // Day, isStartTime
    
    LaunchedEffect(Unit) {
        viewModel.loadShiftConfig()
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Shift Management",
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = Spacing.md),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.saveShiftConfig() }
                        ) {
                            Icon(Icons.Default.Save, "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // Header info
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Configure Work Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "Set working days and hours for your organization",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            // Work Days Section
            item {
                Text(
                    text = "Work Days",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // Day schedules
            items(
                items = WorkDay.values().toList(),
                key = { it.name }
            ) { day ->
                val schedule = shiftConfig?.schedules?.get(day.name) ?: DaySchedule()
                val isWorkDay = shiftConfig?.workDays?.contains(day) ?: false
                
                DayScheduleCard(
                    day = day,
                    schedule = schedule,
                    isWorkDay = isWorkDay,
                    onToggleWorkDay = { enabled ->
                        viewModel.toggleWorkDay(day, enabled)
                    },
                    onEditStartTime = {
                        showTimePicker = Pair(day, true)
                    },
                    onEditEndTime = {
                        showTimePicker = Pair(day, false)
                    }
                )
            }
            
            // Save button at bottom
            item {
                Spacer(modifier = Modifier.height(Spacing.md))
                Button(
                    onClick = { viewModel.saveShiftConfig() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("Save Changes")
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xl))
            }
        }
    }
    
    // Time Picker Dialog
    showTimePicker?.let { (day, isStartTime) ->
        val schedule = shiftConfig?.schedules?.get(day.name) ?: DaySchedule()
        val currentTime = if (isStartTime) schedule.startTime else schedule.endTime
        val (hour, minute) = currentTime.split(":").map { it.toInt() }
        
        TimePickerDialog(
            title = "${if (isStartTime) "Start" else "End"} Time - ${day.displayName}",
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { selectedHour, selectedMinute ->
                val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
                if (isStartTime) {
                    viewModel.updateStartTime(day, timeString)
                } else {
                    viewModel.updateEndTime(day, timeString)
                }
                showTimePicker = null
            },
            onDismiss = { showTimePicker = null }
        )
    }
}

@Composable
fun DayScheduleCard(
    day: WorkDay,
    schedule: DaySchedule,
    isWorkDay: Boolean,
    onToggleWorkDay: (Boolean) -> Unit,
    onEditStartTime: () -> Unit,
    onEditEndTime: () -> Unit
) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            // Day name and toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = day.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isWorkDay) "Work Day" else "Day Off",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isWorkDay) ATSColors.CheckInGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = isWorkDay,
                    onCheckedChange = onToggleWorkDay,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = ATSColors.CheckInGreen,
                        checkedThumbColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
            
            // Time selection (only if work day)
            if (isWorkDay) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Divider(color = ATSColors.DividerColor)
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Start time
                    TimeButton(
                        label = "Start Time",
                        time = schedule.startTime,
                        onClick = onEditStartTime,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // End time
                    TimeButton(
                        label = "End Time",
                        time = schedule.endTime,
                        onClick = onEditEndTime,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Duration display
                Spacer(modifier = Modifier.height(Spacing.sm))
                val duration = calculateDuration(schedule.startTime, schedule.endTime)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = "$duration hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TimeButton(
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = formatTime12Hour(time),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedHour = (selectedHour - 1 + 24) % 24 }) {
                                Icon(Icons.Default.KeyboardArrowUp, null)
                            }
                        }
                        Text(
                            text = String.format("%02d", selectedHour),
                            style = MaterialTheme.typography.displaySmall
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) {
                                Icon(Icons.Default.KeyboardArrowDown, null)
                            }
                        }
                    }
                    
                    Text(":", style = MaterialTheme.typography.displaySmall)
                    
                    // Minute picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedMinute = (selectedMinute - 15 + 60) % 60 }) {
                                Icon(Icons.Default.KeyboardArrowUp, null)
                            }
                        }
                        Text(
                            text = String.format("%02d", selectedMinute),
                            style = MaterialTheme.typography.displaySmall
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedMinute = (selectedMinute + 15) % 60 }) {
                                Icon(Icons.Default.KeyboardArrowDown, null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHour, selectedMinute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime12Hour(time24: String): String {
    val (hour, minute) = time24.split(":").map { it.toInt() }
    val period = if (hour < 12) "AM" else "PM"
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$hour12:${String.format("%02d", minute)} $period"
}

private fun calculateDuration(startTime: String, endTime: String): String {
    val (startHour, startMin) = startTime.split(":").map { it.toInt() }
    val (endHour, endMin) = endTime.split(":").map { it.toInt() }
    
    val startMinutes = startHour * 60 + startMin
    val endMinutes = endHour * 60 + endMin
    
    val durationMinutes = if (endMinutes > startMinutes) {
        endMinutes - startMinutes
    } else {
        (24 * 60) - startMinutes + endMinutes
    }
    
    val hours = durationMinutes / 60
    val minutes = durationMinutes % 60
    
    return if (minutes == 0) {
        hours.toString()
    } else {
        "$hours.${(minutes * 100) / 60}"
    }
}
