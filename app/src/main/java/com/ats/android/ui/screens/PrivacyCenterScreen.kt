package com.ats.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ats.android.R
import com.ats.android.ui.components.SettingsGroupCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyCenterScreen(
    onNavigateBack: () -> Unit,
    onChangePasswordClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_center)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.navigate))
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
                    text = "Control your privacy and security settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                SettingsGroupCard(title = stringResource(R.string.security)) {
                    PrivacyOption(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.change_password),
                        onClick = onChangePasswordClick
                    )
                    Divider()
                    PrivacyOption(
                        icon = Icons.Default.Fingerprint,
                        title = stringResource(R.string.biometric_authentication),
                        subtitle = stringResource(R.string.biometric_subtitle),
                        onClick = { /* TODO */ }
                    )
                }
            }
            
            item {
                SettingsGroupCard(title = stringResource(R.string.data_privacy)) {
                    PrivacyOption(
                        icon = Icons.Default.Visibility,
                        title = "Profile Visibility",
                        subtitle = "Who can see your profile details",
                        onClick = { /* TODO */ }
                    )
                    Divider()
                    PrivacyOption(
                        icon = Icons.Default.History,
                        title = "Activity History",
                        subtitle = "Manage your activity logs",
                        onClick = { /* TODO */ }
                    )
                }
            }
            
            item {
                SettingsGroupCard(title = "Legal") {
                    PrivacyOption(
                        icon = Icons.Default.Description,
                        title = "Privacy Policy",
                        onClick = { /* TODO: Open URL */ }
                    )
                    Divider()
                    PrivacyOption(
                        icon = Icons.Default.Gavel,
                        title = "Terms of Service",
                        onClick = { /* TODO: Open URL */ }
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyOption(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
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
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
