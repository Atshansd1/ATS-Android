package com.ats.android.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.MainActivity
import com.ats.android.R
import com.ats.android.ui.components.GlassCard
import com.ats.android.ui.theme.Spacing
import com.ats.android.utils.LocaleManager
import com.ats.android.viewmodels.SettingsViewModel

/**
 * Language Settings Screen
 * Allows users to switch between English and Arabic with RTL support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(context) as T
            }
        }
    )
    
    val currentLanguage by settingsViewModel.language.collectAsState()
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    var showRestartDialog by remember { mutableStateOf(false) }
    
    // Update selectedLanguage when currentLanguage changes
    LaunchedEffect(currentLanguage) {
        selectedLanguage = currentLanguage
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        context.getString(R.string.language),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg)
            ) {
                Text(
                    text = context.getString(R.string.change_language),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(Spacing.xs))
                
                Text(
                    text = context.getString(R.string.language_change_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider()
            
            // Language Options
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(LocaleManager.Language.values()) { language ->
                    LanguageOption(
                        language = language,
                        isSelected = selectedLanguage == language.code,
                        onClick = {
                            if (selectedLanguage != language.code) {
                                selectedLanguage = language.code
                                showRestartDialog = true
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Restart Dialog
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { 
                showRestartDialog = false
                selectedLanguage = currentLanguage // Reset selection
            },
            icon = {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(context.getString(R.string.restart_app))
            },
            text = {
                Text(context.getString(R.string.language_restart_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Save language using ViewModel (syncs to both stores)
                        settingsViewModel.setLanguage(selectedLanguage)
                        
                        // Restart app
                        restartApp(context)
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(context.getString(R.string.restart_app))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestartDialog = false
                    selectedLanguage = currentLanguage // Reset selection
                }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun LanguageOption(
    language: LocaleManager.Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Icon
                Icon(
                    imageVector = if (language == LocaleManager.Language.ARABIC) {
                        Icons.Default.Person // Could use a better icon
                    } else {
                        Icons.Default.Person
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Column {
                    Text(
                        text = language.nativeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    Text(
                        text = language.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Selection Indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = "Not selected",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/**
 * Restart the app to apply language changes
 */
private fun restartApp(context: Context) {
    android.util.Log.d("LanguageSettings", "🔄 Restarting app...")
    
    val activity = context as? Activity
    activity?.let {
        // Give time for preferences to be written to disk
        Thread.sleep(100)
        
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        
        // Kill the process to ensure clean restart
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
