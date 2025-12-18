package com.ats.android.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ats.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSettingsDetailScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.location_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.navigate))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(R.string.location_access_desc),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Text(
                text = stringResource(R.string.permissions_status),
                style = MaterialTheme.typography.titleMedium
            )
            
            // This is a static view for now since we don't have reactive permission state here
            // In a real app, use Accompanist Permissions or similar to show real status
            ListItem(
                headlineContent = { Text(stringResource(R.string.precise_location)) },
                supportingContent = { Text(stringResource(R.string.precise_location_desc)) },
                trailingContent = { Text(stringResource(R.string.enabled), color = MaterialTheme.colorScheme.primary) }
            )
            Divider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.background_location)) },
                supportingContent = { Text(stringResource(R.string.background_location_desc)) },
                trailingContent = { Text(stringResource(R.string.not_granted), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    try {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // fallback
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.open_system_settings))
            }
        }
    }
}
