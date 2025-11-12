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
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val updateManager = remember { UpdateManager(context) }
    val downloadProgress by updateManager.downloadProgress.collectAsState()
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(context) as T
            }
        }
    )
    
    val currentLanguage by settingsViewModel.language.collectAsState()
    val languageDisplayName = when (currentLanguage) {
        "ar" -> "العربية"
        else -> "English"
    }
    
    var showSignOutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(com.ats.android.R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl)
            ) {
                // App Update Section (Prominent at top)
                item {
                    UpdateSection(
                        downloadProgress = downloadProgress,
                        onUpdateClick = {
                            updateManager.downloadAndInstallUpdate()
                        }
                    )
                }
                
                // Profile Section (if employee exists)
                currentEmployee?.let { employee ->
                    item {
                        ProfileSection(employee = employee)
                    }
                }
                
                // Preferences Section
                item {
                    SettingsGroupCard(title = stringResource(com.ats.android.R.string.preferences)) {
                        IOSSettingsRow(
                            icon = Icons.Default.Language,
                            title = stringResource(com.ats.android.R.string.language),
                            value = languageDisplayName,
                            onClick = onNavigateToLanguageSettings
                        )
                        
                        Divider(color = ATSColors.DividerColor)
                        
                        IOSSettingsRow(
                            icon = Icons.Default.Notifications,
                            title = stringResource(com.ats.android.R.string.notifications),
                            value = null,
                            onClick = { 
                                showMessage = context.getString(com.ats.android.R.string.notification_settings)
                            }
                        )
                    }
                }
                
                // Attendance Management Section (Admin only)
                if (currentEmployee?.role == com.ats.android.models.EmployeeRole.ADMIN) {
                    item {
                        SettingsGroupCard(title = stringResource(com.ats.android.R.string.administration)) {
                            IOSSettingsRow(
                                icon = Icons.Default.Schedule,
                                title = stringResource(com.ats.android.R.string.attendance_management),
                                subtitle = stringResource(com.ats.android.R.string.configure_shifts_locations),
                                value = null,
                                onClick = { 
                                    onNavigateToAttendanceManagement()
                                }
                            )
                        }
                    }
                }
                
                // Privacy & Permissions Section
                item {
                    SettingsGroupCard(title = stringResource(com.ats.android.R.string.privacy)) {
                        IOSSettingsRow(
                            icon = Icons.Default.LocationOn,
                            title = stringResource(com.ats.android.R.string.location_permissions),
                            subtitle = stringResource(com.ats.android.R.string.location_always_enabled),
                            value = stringResource(com.ats.android.R.string.enabled),
                            showChevron = true,
                            onClick = {
                                // Open app settings
                                try {
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        android.net.Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    showMessage = "Unable to open settings"
                                }
                            }
                        )
                    }
                }
                
                // Test Data Section (Admin only - Development)
                if (currentEmployee?.role == com.ats.android.models.EmployeeRole.ADMIN) {
                    item {
                        SettingsGroupCard(title = stringResource(com.ats.android.R.string.test_data_development)) {
                            IOSSettingsRow(
                                icon = Icons.Default.Person,
                                title = stringResource(com.ats.android.R.string.add_test_employees),
                                subtitle = stringResource(com.ats.android.R.string.add_4_sample_employees),
                                value = null,
                                showChevron = false,
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        val result = TestDataHelper.addTestEmployees()
                                        isLoading = false
                                        showMessage = result.getOrNull() 
                                            ?: "${context.getString(com.ats.android.R.string.error)}: ${result.exceptionOrNull()?.message}"
                                    }
                                }
                            )
                            
                            Divider(color = ATSColors.DividerColor)
                            
                            IOSSettingsRow(
                                icon = Icons.Default.LocationOn,
                                title = stringResource(com.ats.android.R.string.add_test_locations),
                                subtitle = stringResource(com.ats.android.R.string.add_3_active_locations),
                                value = null,
                                showChevron = false,
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        val result = TestDataHelper.addTestLocations()
                                        isLoading = false
                                        showMessage = result.getOrNull() 
                                            ?: "${context.getString(com.ats.android.R.string.error)}: ${result.exceptionOrNull()?.message}"
                                    }
                                }
                            )
                            
                            Divider(color = ATSColors.DividerColor)
                            
                            IOSSettingsRow(
                                icon = Icons.Default.Delete,
                                title = stringResource(com.ats.android.R.string.clean_up_old_locations),
                                subtitle = stringResource(com.ats.android.R.string.remove_locations_older_24h),
                                value = null,
                                showChevron = false,
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        val result = com.ats.android.utils.CleanupHelper.cleanupOldActiveLocations(24)
                                        isLoading = false
                                        showMessage = result.getOrNull() 
                                            ?: "${context.getString(com.ats.android.R.string.error)}: ${result.exceptionOrNull()?.message}"
                                    }
                                }
                            )
                            
                            Divider(color = ATSColors.DividerColor)
                            
                            IOSSettingsRow(
                                icon = Icons.Default.DeleteForever,
                                title = stringResource(com.ats.android.R.string.clear_all_active_locations),
                                subtitle = stringResource(com.ats.android.R.string.remove_all_active_locations),
                                value = null,
                                showChevron = false,
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        val result = com.ats.android.utils.CleanupHelper.clearAllActiveLocations()
                                        isLoading = false
                                        showMessage = result.getOrNull() 
                                            ?: "${context.getString(com.ats.android.R.string.error)}: ${result.exceptionOrNull()?.message}"
                                    }
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
                            value = "1.3.0",
                            showChevron = false,
                            onClick = { 
                                showMessage = "ATS Android v1.3.0 - Material Design 3"
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
fun ProfileSection(employee: Employee) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
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

/**
 * Settings Group Card (iOS style)
 */
@Composable
fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.xs)
        )
        
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = CornerRadius.medium
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }
    }
}

/**
 * iOS-style Settings Row
 */
@Composable
fun IOSSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with colored background
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        
        // Title and subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Value and/or chevron
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Update Section Component - Material 3 Design
 */
@Composable
fun UpdateSection(
    downloadProgress: UpdateManager.DownloadProgress,
    onUpdateClick: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(com.ats.android.R.string.app_updates),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(com.ats.android.R.string.update_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Update Status/Progress
            when (downloadProgress) {
                is UpdateManager.DownloadProgress.Idle -> {
                    // Update Button
                    Button(
                        onClick = onUpdateClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(com.ats.android.R.string.download_and_install),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Text(
                        text = stringResource(com.ats.android.R.string.tap_to_download),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                is UpdateManager.DownloadProgress.Downloading -> {
                    // Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = downloadProgress.progress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(com.ats.android.R.string.downloading_update),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(com.ats.android.R.string.download_progress, downloadProgress.progress),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                is UpdateManager.DownloadProgress.Completed -> {
                    // Install prompt
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = stringResource(com.ats.android.R.string.update_downloaded),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(com.ats.android.R.string.install_update),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                is UpdateManager.DownloadProgress.Error -> {
                    // Error message
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text(
                                text = stringResource(com.ats.android.R.string.update_failed),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = downloadProgress.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    // Retry button
                    OutlinedButton(
                        onClick = onUpdateClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        }
    }
}
