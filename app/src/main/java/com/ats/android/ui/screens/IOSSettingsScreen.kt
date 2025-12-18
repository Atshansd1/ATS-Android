package com.ats.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.models.Employee
import com.ats.android.ui.components.*
import com.ats.android.ui.theme.*
import com.ats.android.utils.LocaleManager
import com.ats.android.utils.TestDataHelper
import com.ats.android.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import com.ats.android.services.UpdateManager


/**
 * Settings Screen with Material 3 Design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IOSSettingsScreen(
    currentEmployee: Employee?,
    onNavigateToAttendanceManagement: () -> Unit = {},
    onNavigateToLanguageSettings: () -> Unit = {},
    onNavigateToDebugLogs: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToPrivacyCenter: () -> Unit = {},
    onNavigateToLocationSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToLeaveApproval: () -> Unit = {},
    onNavigateToLeaves: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToShiftManagement: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var showMessage by remember { mutableStateOf<String?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    
    // Auto-cleanup test entities on first load of Settings
    LaunchedEffect(Unit) {
        // Run silently in background
        launch {
            try {
                TestDataHelper.cleanupTestEmployees()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    // Get ViewModel
    val settingsViewModel: SettingsViewModel = viewModel()
    val updateManager = remember { UpdateManager(context) }
    val versionInfo by updateManager.versionInfo.collectAsState()
    val downloadProgress by updateManager.downloadProgress.collectAsState()
    val currentAppVersion = updateManager.getCurrentVersion()
    
    // UI State
    val language by settingsViewModel.language.collectAsState()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(com.ats.android.R.string.settings_title), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Section
            item {
                currentEmployee?.let { employee ->
                    SettingsGroupCard(title = stringResource(com.ats.android.R.string.profile_title)) { // Ensure string resource exists or use "Profile"
                        // Name
                        IOSSettingsRow(
                            title = stringResource(com.ats.android.R.string.name_label),
                            value = employee.displayName,
                            showChevron = false
                        )
                        
                        Divider(color = ATSColors.DividerColor)
                        
                        // Employee ID
                        IOSSettingsRow(
                            title = stringResource(com.ats.android.R.string.employee_id_label),
                            value = employee.employeeId,
                            showChevron = false
                        )
                        
                        Divider(color = ATSColors.DividerColor)
                        
                        // Role
                        IOSSettingsRow(
                            title = stringResource(com.ats.android.R.string.role_label),
                            value = employee.role.toString().lowercase(),
                            showChevron = false
                        )
                        
                        Divider(color = ATSColors.DividerColor)
                        
                        // Team
                        IOSSettingsRow(
                            title = stringResource(com.ats.android.R.string.team_label),
                            value = employee.team,
                            showChevron = false
                        )
                        
                        Divider(color = ATSColors.DividerColor)
                        
                        // Change Password
                        IOSSettingsRow(
                            icon = Icons.Default.Lock, // Use Lock icon for password
                            title = stringResource(com.ats.android.R.string.change_password),
                            onClick = onNavigateToChangePassword
                        )
                    }
                }
            }

            // Preferences Section
            item {
                SettingsGroupCard(title = stringResource(com.ats.android.R.string.preferences)) {
                    IOSSettingsRow(
                        icon = Icons.Default.Language,
                        title = stringResource(com.ats.android.R.string.language),
                        value = if (language == "ar") "Arabic" else "English",
                        onClick = onNavigateToLanguageSettings
                    )
                    
                    Divider(color = ATSColors.DividerColor)
                    
                    IOSSettingsRow(
                        icon = Icons.Default.Notifications,
                        title = stringResource(com.ats.android.R.string.notifications),
                        value = null,
                        onClick = onNavigateToNotificationSettings
                    )
                }
            }

            // Privacy & Security Section
            item {
                SettingsGroupCard(title = stringResource(com.ats.android.R.string.privacy_security)) { // Need to ensure this string exists
                     IOSSettingsRow(
                        icon = Icons.Default.Security,
                        title = stringResource(com.ats.android.R.string.privacy_center),
                        onClick = onNavigateToPrivacyCenter
                    )
                    
                    Divider(color = ATSColors.DividerColor)
                    
                    IOSSettingsRow(
                        icon = Icons.Default.LocationOn,
                        title = "Location Settings",
                        value = stringResource(com.ats.android.R.string.enabled),
                        showChevron = true,
                        onClick = onNavigateToLocationSettings
                    )
                }
            }
            
            // Management Section (Admin only)
            if (currentEmployee?.role == com.ats.android.models.EmployeeRole.ADMIN) {
                item {
                    SettingsGroupCard(title = stringResource(com.ats.android.R.string.management_title)) {
                        IOSSettingsRow(
                            icon = Icons.Default.LocationOn,
                            title = stringResource(com.ats.android.R.string.attendance_locations),
                            subtitle = stringResource(com.ats.android.R.string.attendance_locations_desc),
                            onClick = onNavigateToAttendanceManagement
                        )
                        
                        Divider(color = ATSColors.DividerColor)
                        
                        IOSSettingsRow(
                            icon = Icons.Default.EventNote,
                            title = stringResource(com.ats.android.R.string.leave_approval),
                            subtitle = stringResource(com.ats.android.R.string.review_leave_requests),
                            onClick = onNavigateToLeaveApproval
                        )
                        
                        Divider(color = ATSColors.DividerColor)
                        
                        IOSSettingsRow(
                            icon = Icons.Default.BarChart,
                            title = stringResource(com.ats.android.R.string.analytics),
                            subtitle = stringResource(com.ats.android.R.string.analytics_desc),
                            onClick = onNavigateToAnalytics
                        )
                        
                        Divider(color = ATSColors.DividerColor)
                        
                        IOSSettingsRow(
                            icon = Icons.Default.Schedule,
                            title = stringResource(com.ats.android.R.string.shift_management),
                            subtitle = stringResource(com.ats.android.R.string.shift_management_desc),
                            onClick = onNavigateToShiftManagement
                        )
                    }
                }
            }
            
            // Update Section (Only show if new version available)
            versionInfo?.let { info ->
                if (info.isUpdateAvailable) {
                    item {
                        ExpressiveUpdateCard(
                            currentVersion = currentAppVersion,
                            newVersion = info.latestVersion,
                            downloadProgress = downloadProgress,
                            onUpdateClick = {
                                updateManager.downloadAndInstallUpdate(info.downloadUrl)
                            }
                        )
                    }
                }
            }
                
                // About Section
                item {
                    SettingsGroupCard(title = stringResource(com.ats.android.R.string.about)) {
                        IOSSettingsRow(
                            icon = Icons.Default.Info,
                            title = stringResource(com.ats.android.R.string.version),
                            value = currentAppVersion,
                            showChevron = false,
                            onClick = { 
                                showMessage = "ATS Android v$currentAppVersion"
                            }
                        )
                        
                        Divider(color = ATSColors.DividerColor)
                        
                        IOSSettingsRow(
                            icon = Icons.Default.Code,
                            title = stringResource(com.ats.android.R.string.app_info),
                            value = null,
                            onClick = { 
                                showMessage = context.getString(com.ats.android.R.string.app_info_details)
                            }
                        )
                    }
                }
                
                // Sign Out Button
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSignOutDialog = true },
                        cornerRadius = CornerRadius.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = stringResource(com.ats.android.R.string.sign_out),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(Spacing.lg))
                }
            }
            
            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

    
    // Show message snackbar
    showMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(if (message.length > 50) 4000 else 3000)
            showMessage = null
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg),
            contentAlignment = Alignment.BottomCenter
        ) {
            Snackbar(
                containerColor = if (message.contains("Error") || message.contains("error")) {
                    MaterialTheme.colorScheme.errorContainer
                } else if (message.contains("Success") || message.contains("success")) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
    
    // Sign Out Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(com.ats.android.R.string.sign_out)) },
            text = { Text(stringResource(com.ats.android.R.string.sign_out_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    }
                ) {
                    Text(stringResource(com.ats.android.R.string.sign_out), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(com.ats.android.R.string.cancel))
                }
            }
        )
    }
}

/**
 * Profile Section (iOS style)
 */
@Composable
fun ProfileSection(
    employee: Employee,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = CornerRadius.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Avatar and Name
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmployeeAvatar(
                    employee = employee,
                    size = AvatarSize.xlarge
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = employee.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoleBadge(role = employee.role)
                        
                        if (employee.isActive) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ActiveStatusDot(isActive = true, size = 6.dp)
                                Text(
                                    text = stringResource(com.ats.android.R.string.active),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ATSColors.ActiveDot
                                )
                            }
                        }
                    }
                }
            }
            
            Divider(color = ATSColors.DividerColor)
            
            // Profile Details
            ProfileDetailRow(label = stringResource(com.ats.android.R.string.employee_id_label), value = employee.employeeId)
            ProfileDetailRow(label = stringResource(com.ats.android.R.string.team_label), value = employee.team)
            employee.email?.let { email ->
                ProfileDetailRow(label = stringResource(com.ats.android.R.string.email_label), value = email)
            }
        }
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

