package com.ats.android.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ats.android.R
import com.ats.android.services.LocalNotificationManager

/**
 * NotificationSettingsScreen - iOS-matching notification settings UI
 * 
 * Features:
 * - Daily check-in reminders toggle with time picker
 * - Check-out reminders toggle with hours slider
 * - Location-based (geofence) reminders toggle
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val notificationManager = remember { LocalNotificationManager.getInstance(context) }
    
    // State
    var checkInRemindersEnabled by remember { mutableStateOf(notificationManager.isDailyReminderEnabled()) }
    var checkOutRemindersEnabled by remember { mutableStateOf(notificationManager.isCheckoutReminderEnabled()) }
    var geofenceEnabled by remember { mutableStateOf(notificationManager.isGeofenceEnabled()) }
    
    val (savedHour, savedMinute) = notificationManager.getDailyReminderTime()
    var reminderHour by remember { mutableIntStateOf(savedHour) }
    var reminderMinute by remember { mutableIntStateOf(savedMinute) }
    var checkoutHours by remember { mutableFloatStateOf(notificationManager.getCheckoutHours()) }
    
    var showTimePicker by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(notificationManager.hasNotificationPermission()) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }
    
    // Request permission on Android 13+
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notifications),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section Header Description
            Text(
                text = stringResource(R.string.notifications_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Daily Reminders Section
            SettingsSection(
                title = stringResource(R.string.check_in_reminders),
                icon = Icons.Default.NotificationsActive
            ) {
                // Main Toggle
                SettingsToggleRow(
                    title = stringResource(R.string.check_in_reminders),
                    subtitle = stringResource(R.string.check_in_reminders_desc),
                    checked = checkInRemindersEnabled,
                    onCheckedChange = { enabled ->
                        checkInRemindersEnabled = enabled
                        if (enabled) {
                            notificationManager.scheduleDailyCheckInReminder(reminderHour, reminderMinute)
                        } else {
                            notificationManager.cancelDailyCheckInReminder()
                        }
                    }
                )
                
                // Time Picker (only if enabled)
                AnimatedVisibility(
                    visible = checkInRemindersEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reminder Time",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            Surface(
                                onClick = { showTimePicker = true },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = String.format("%02d:%02d", reminderHour, reminderMinute),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            // Check-Out Reminders Section
            SettingsSection(
                title = stringResource(R.string.check_out_reminders),
                icon = Icons.Default.Timer
            ) {
                SettingsToggleRow(
                    title = stringResource(R.string.check_out_reminders),
                    subtitle = stringResource(R.string.check_out_reminder_body),
                    checked = checkOutRemindersEnabled,
                    onCheckedChange = { enabled ->
                        checkOutRemindersEnabled = enabled
                        if (!enabled) {
                            notificationManager.cancelCheckOutReminder()
                        }
                    }
                )
                
                AnimatedVisibility(
                    visible = checkOutRemindersEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        
                        // iOS-style Stepper
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "After ${checkoutHours.toInt()} hours",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Decrease button
                                Surface(
                                    onClick = { 
                                        if (checkoutHours > 4f) checkoutHours -= 1f
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (checkoutHours > 4f) 
                                        MaterialTheme.colorScheme.secondaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "−",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (checkoutHours > 4f)
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                
                                // Value display
                                Text(
                                    text = "${checkoutHours.toInt()}h",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.width(40.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                
                                // Increase button
                                Surface(
                                    onClick = { 
                                        if (checkoutHours < 12f) checkoutHours += 1f
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (checkoutHours < 12f) 
                                        MaterialTheme.colorScheme.secondaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "+",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (checkoutHours < 12f)
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Location-Based Reminders Section
            SettingsSection(
                title = stringResource(R.string.location_based_reminders),
                icon = Icons.Default.LocationOn
            ) {
                SettingsToggleRow(
                    title = stringResource(R.string.location_based_reminders),
                    subtitle = "Get notified when you arrive at your work location",
                    checked = geofenceEnabled,
                    onCheckedChange = { enabled ->
                        geofenceEnabled = enabled
                        notificationManager.setGeofenceEnabled(enabled)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                reminderHour = hour
                reminderMinute = minute
                if (checkInRemindersEnabled) {
                    notificationManager.scheduleDailyCheckInReminder(hour, minute)
                }
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Reminder Time",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TimePicker(state = timePickerState)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Surface(
                        onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = stringResource(R.string.confirm),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
