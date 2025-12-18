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
import androidx.compose.ui.res.stringResource
import com.ats.android.R

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
                        stringResource(R.string.shift_management),
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.navigate_back))
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
                            Icon(Icons.Default.Save, stringResource(R.string.save))
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
                    text = stringResource(R.string.configure_work_schedule),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.set_working_days_hours),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Work Days Section
            item {
                Text(
                    text = stringResource(R.string.work_days),
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
                    onToggleWorkDay = { viewModel.toggleWorkDay(day, it) },
                    onEditStartTime = { showTimePicker = day to true },
                    onEditEndTime = { showTimePicker = day to false }
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
                        Text(stringResource(R.string.save))
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
            title = stringResource(
                R.string.time_picker_title, 
                stringResource(if (isStartTime) R.string.start_time else R.string.end_time), 
                stringResource(day.labelResId)
            ),
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
                        text = stringResource(day.labelResId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(if (isWorkDay) R.string.work_day else R.string.day_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isWorkDay) ATSColors.CheckInGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = isWorkDay,
                    onCheckedChange = onToggleWorkDay,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ATSColors.CheckInGreen,
                        checkedTrackColor = ATSColors.CheckInGreen.copy(alpha = 0.5f)
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
                        label = stringResource(R.string.start_time),
                        time = schedule.startTime,
                        onClick = onEditStartTime,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // End time
                    TimeButton(
                        label = stringResource(R.string.end_time),
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
                        text = stringResource(R.string.duration_hours, duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
                        Text(stringResource(R.string.hour_label), style = MaterialTheme.typography.labelSmall)
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
                        Text(stringResource(R.string.minute_label), style = MaterialTheme.typography.labelSmall)
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
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun TimeButton(
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs)
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

fun calculateDuration(startTime: String, endTime: String): String {
    try {
        val startParts = startTime.split(":").map { it.toInt() }
        val endParts = endTime.split(":").map { it.toInt() }
        
        val startMinutes = startParts[0] * 60 + startParts[1]
        val endMinutes = endParts[0] * 60 + endParts[1]
        
        var diffMinutes = endMinutes - startMinutes
        if (diffMinutes < 0) diffMinutes += 24 * 60
        
        val hours = diffMinutes / 60
        val minutes = diffMinutes % 60
        
        return if (minutes > 0) {
            String.format("%dh %02dm", hours, minutes)
        } else {
            String.format("%dh", hours)
        }
    } catch (e: Exception) {
        return "0h"
    }
}
