package com.ats.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ats.android.R
import com.ats.android.ui.components.SettingsGroupCard
import com.ats.android.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: com.ats.android.viewmodels.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // Collect states
    val pushEnabled by viewModel.pushEnabled.collectAsState()
    val emailEnabled by viewModel.emailEnabled.collectAsState()
    val checkInReminders by viewModel.checkInReminders.collectAsState()
    val leaveUpdates by viewModel.leaveUpdates.collectAsState()
    val announcementAlerts by viewModel.announcementAlerts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.navigate)) // generic back
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
            item {
                Text(
                    text = "Manage how you receive notifications.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                SettingsGroupCard(title = "General") {
                    NotificationToggle(
                        title = "Push Notifications",
                        checked = pushEnabled,
                        onCheckedChange = { viewModel.updateNotificationSetting("push", it) }
                    )
                    Divider()
                    NotificationToggle(
                        title = "Email Notifications",
                        checked = emailEnabled,
                        onCheckedChange = { viewModel.updateNotificationSetting("email", it) }
                    )
                }
            }
            
            item {
                SettingsGroupCard(title = "Alerts") {
                    NotificationToggle(
                        title = "Check-In Reminders",
                        subtitle = "Get reminded if you forget to check in",
                        checked = checkInReminders,
                        onCheckedChange = { viewModel.updateNotificationSetting("reminders", it) }
                    )
                    Divider()
                    NotificationToggle(
                        title = "Leave Request Updates",
                        subtitle = "Status changes for your leave requests",
                        checked = leaveUpdates,
                        onCheckedChange = { viewModel.updateNotificationSetting("leave", it) }
                    )
                    Divider()
                    NotificationToggle(
                        title = "Announcements",
                        subtitle = "Company wide announcements",
                        checked = announcementAlerts,
                        onCheckedChange = { viewModel.updateNotificationSetting("announcements", it) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationToggle(
    title: String,
    subtitle: String? = null,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
