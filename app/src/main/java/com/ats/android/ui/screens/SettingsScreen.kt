package com.ats.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ats.android.utils.TestDataHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSignOut: () -> Unit,
    onNavigateToLanguageSettings: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showSignOutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Test Data Section (Development)
            item {
                SettingsSection(title = "Test Data (Development)")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Add Test Employees",
                    subtitle = "Add 4 sample employees to database",
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val result = TestDataHelper.addTestEmployees()
                            isLoading = false
                            showMessage = result.getOrNull() ?: "Error: ${result.exceptionOrNull()?.message}"
                        }
                    }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.LocationOn,
                    title = "Add Test Locations",
                    subtitle = "Add 3 active locations to map",
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val result = TestDataHelper.addTestLocations()
                            isLoading = false
                            showMessage = result.getOrNull() ?: "Error: ${result.exceptionOrNull()?.message}"
                        }
                    }
                )
            }
            
            // Language Section
            item {
                SettingsSection(title = "Preferences")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = "English / العربية",
                    onClick = onNavigateToLanguageSettings
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Manage notification preferences",
                    onClick = { /* TODO: Notifications */ }
                )
            }
            
            // Account Section
            item {
                SettingsSection(title = "Account")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Profile",
                    subtitle = "View and edit your profile",
                    onClick = { /* TODO: Profile */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Privacy",
                    subtitle = "Privacy and security settings",
                    onClick = { /* TODO: Privacy */ }
                )
            }
            
            // About Section
            item {
                SettingsSection(title = "About")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.SystemUpdate,
                    title = "Check for Updates",
                    subtitle = "Download latest version",
                    onClick = {
                        showMessage = "Opening update page..."
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/mohanadsd/ATS-Android/releases/latest")
                        )
                        context.startActivity(intent)
                    }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About ATS",
                    subtitle = "Version 1.1.0",
                    onClick = { /* TODO: About */ }
                )
            }
            
            // Sign Out
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { showSignOutDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Sign Out",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Show message snackbar
    if (showMessage != null) {
        LaunchedEffect(showMessage) {
            kotlinx.coroutines.delay(3000)
            showMessage = null
        }
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(showMessage!!)
        }
    }
    
    // Loading indicator
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    
    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
