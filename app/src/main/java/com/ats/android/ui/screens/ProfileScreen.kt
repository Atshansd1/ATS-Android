package com.ats.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.res.stringResource
import com.ats.android.R
import com.ats.android.models.Employee
import com.ats.android.models.EmployeeRole
import com.ats.android.ui.components.GlassCard
import com.ats.android.ui.theme.ATSColors
import com.ats.android.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    employee: Employee,
    onNavigateBack: () -> Unit,
    onEditProfile: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.navigate))
                    }
                },
                actions = {
                    IconButton(onClick = onEditProfile) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Gradient Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    )
            ) {
                // Avatar overlapping bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 50.dp)
                ) {
                    if (!employee.avatarURL.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(employee.avatarURL)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = employee.displayName.firstOrNull()?.toString()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Edit Icon
                    IconButton(
                        onClick = onEditProfile,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(60.dp))
            
            // Name and Role
            Text(
                text = employee.displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                color = when(employee.role) {
                    EmployeeRole.ADMIN -> ATSColors.AdminPurple.copy(alpha = 0.1f)
                    EmployeeRole.SUPERVISOR -> ATSColors.SupervisorBlue.copy(alpha = 0.1f)
                    EmployeeRole.EMPLOYEE -> ATSColors.EmployeeGreen.copy(alpha = 0.1f)
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = employee.role.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = when(employee.role) {
                        EmployeeRole.ADMIN -> ATSColors.AdminPurple
                        EmployeeRole.SUPERVISOR -> ATSColors.SupervisorBlue
                        EmployeeRole.EMPLOYEE -> ATSColors.EmployeeGreen
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Info Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Contact Info
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.contact_information),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ProfileInfoRow(Icons.Default.Email, stringResource(R.string.email_label), employee.email ?: stringResource(R.string.not_set))
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        ProfileInfoRow(Icons.Default.Phone, stringResource(R.string.phone), employee.phoneNumber ?: stringResource(R.string.not_set))
                    }
                }
                
                // Employment Details
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.employment_details),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ProfileInfoRow(Icons.Default.Badge, stringResource(R.string.employee_id_label), employee.employeeId)
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        ProfileInfoRow(Icons.Default.Group, stringResource(R.string.team), employee.team)
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        ProfileInfoRow(Icons.Default.LocationOn, stringResource(R.string.work_location), "Main Office") 
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
