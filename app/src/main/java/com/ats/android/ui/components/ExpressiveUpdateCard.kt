package com.ats.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ats.android.services.UpdateManager

/**
 * M3 Expressive Update Card - Only shown when update is available
 * Applies expressive design principles: shape variety, rich colors, emphasized typography
 */
@Composable
fun ExpressiveUpdateCard(
    currentVersion: String,
    newVersion: String,
    downloadProgress: UpdateManager.DownloadProgress,
    onUpdateClick: () -> Unit
) {
    // M3 Expressive: Mix of shapes - rounded card with contrasting elements
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge, // Expressive: Extra large corners
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer // Expressive: Rich color
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp // Expressive: Elevated for emphasis
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp), // Expressive: Generous padding
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Moment: Emphasized header with version info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Expressive Typography: Bold, large title
                    Text(
                        text = stringResource(com.ats.android.R.string.new_version_available),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold, // Emphasized
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Version progression
                    Text(
                        text = stringResource(
                            com.ats.android.R.string.version_info,
                            currentVersion,
                            newVersion
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary // Expressive: Color contrast
                    )
                }
                
                // Expressive: Circular icon badge with contrasting shape
                Box(
                    modifier = Modifier
                        .size(56.dp) // Larger for emphasis
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // Update Status/Progress
            when (downloadProgress) {
                is UpdateManager.DownloadProgress.Idle -> {
                    // Expressive: Prominent action button
                    Button(
                        onClick = onUpdateClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp), // Expressive: Taller button for emphasis
                        shape = MaterialTheme.shapes.large, // Mix of shapes
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Expressive: Emphasized button text
                        Text(
                            text = stringResource(com.ats.android.R.string.install_now),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = stringResource(com.ats.android.R.string.tap_to_install),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                is UpdateManager.DownloadProgress.Downloading -> {
                    // Expressive: Animated progress with emphasized text
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Bold progress bar
                        LinearProgressIndicator(
                            progress = downloadProgress.progress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp), // Thicker for visibility
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(com.ats.android.R.string.downloading_update),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            // Expressive: Large, bold percentage
                            Text(
                                text = stringResource(com.ats.android.R.string.download_progress, downloadProgress.progress),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                is UpdateManager.DownloadProgress.Completed -> {
                    // Expressive: Success state with large icon
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp) // Large for emphasis
                        )
                        Text(
                            text = stringResource(com.ats.android.R.string.update_downloaded),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(com.ats.android.R.string.install_update),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                is UpdateManager.DownloadProgress.Error -> {
                    // Expressive: Clear error state
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(com.ats.android.R.string.update_failed),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = downloadProgress.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
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
}
