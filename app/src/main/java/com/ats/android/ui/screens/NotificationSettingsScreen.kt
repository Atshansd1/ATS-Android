package com.ats.android.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ats.android.R
import com.ats.android.services.LocalNotificationManager
import com.ats.android.ui.components.SettingsGroupCard

/**
 * NotificationSettingsScreen - Matches iOS NotificationSettingsView
 * 
 * Features:
 * - Daily Reminders: toggle + time picker
 * - Checkout Reminders: toggle + hours stepper
 * - Geofence Notifications: toggle
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
    var checkoutHours by remember { mutableIntStateOf(notificationManager.getCheckoutHours().toInt()) }
    
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
            TopAppBar(
                title = { Text(stringResource(R.string.notifications)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Daily Reminders Section
            item {
                SettingsGroupCard(title = stringResource(R.string.daily_reminders)) {
                    // Enable Check-In Reminders Toggle
                    NotificationToggleRow(
                        title = stringResource(R.string.enable_checkin_reminders),
                        checked = checkInRemindersEnabled,
                        onCheckedChange = { enabled ->
                            checkInRemindersEnabled = enabled
                            notificationManager.setDailyReminderEnabled(enabled)
                            if (enabled) {
                                notificationManager.scheduleDailyCheckInReminder(reminderHour, reminderMinute)
                            } else {
                                notificationManager.cancelDailyCheckInReminder()
                            }
                        }
                    )
                    
                    // Reminder Time (only visible when enabled)
                    AnimatedVisibility(
                        visible = checkInRemindersEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Divider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reminder Time",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                
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
                    
                    // Footer description
                    Text(
                        text = stringResource(R.string.checkin_reminder_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            
            // Checkout Reminders Section
            item {
                SettingsGroupCard(title = stringResource(R.string.check_out_reminders)) {
                    // Checkout Reminders Toggle
                    NotificationToggleRow(
                        title = stringResource(R.string.check_out_reminders),
                        checked = checkOutRemindersEnabled,
                        onCheckedChange = { enabled ->
                            checkOutRemindersEnabled = enabled
                            notificationManager.setCheckoutReminderEnabled(enabled)
                            if (!enabled) {
                                notificationManager.cancelCheckOutReminder()
                            }
                        }
                    )
                    
                    // Hours Stepper (only visible when enabled)
                    AnimatedVisibility(
                        visible = checkOutRemindersEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Divider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "After $checkoutHours hours",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                
                                // iOS-style Stepper
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Decrease button
                                    Surface(
                                        onClick = { 
                                            if (checkoutHours > 4) {
                                                checkoutHours -= 1
                                                notificationManager.setCheckoutHours(checkoutHours)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (checkoutHours > 4) 
                                            MaterialTheme.colorScheme.secondaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "−",
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (checkoutHours > 4)
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    
                                    // Increase button
                                    Surface(
                                        onClick = { 
                                            if (checkoutHours < 12) {
                                                checkoutHours += 1
                                                notificationManager.setCheckoutHours(checkoutHours)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (checkoutHours < 12) 
                                            MaterialTheme.colorScheme.secondaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "+",
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (checkoutHours < 12)
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Footer description
                    Text(
                        text = stringResource(R.string.checkout_reminder_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            
            // Geofence Notifications Section
            item {
                SettingsGroupCard(title = stringResource(R.string.geofence_notifications)) {
                    NotificationToggleRow(
                        title = stringResource(R.string.location_based_reminders),
                        checked = geofenceEnabled,
                        onCheckedChange = { enabled ->
                            geofenceEnabled = enabled
                            notificationManager.setGeofenceEnabled(enabled)
                        }
                    )
                    
                    // Footer description
                    Text(
                        text = stringResource(R.string.geofence_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialogContent(
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
private fun NotificationToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogContent(
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
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}
