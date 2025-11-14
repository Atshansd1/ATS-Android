package com.ats.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ats.android.models.AttendanceRecord
import com.ats.android.models.AttendanceStatus
import com.ats.android.models.Employee
import com.ats.android.models.EmployeeRole
import com.ats.android.ui.components.*
import com.ats.android.ui.theme.*
import com.ats.android.viewmodels.EmployeeDetailViewModel
import com.ats.android.viewmodels.EmployeeDetailViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

/**
 * Employee Detail Screen matching iOS implementation
 * Features:
 * - Avatar display and upload
 * - Complete employee information
 * - Edit functionality
 * - Attendance history (last 30 days)
 * - Firebase real-time sync
 * - Material 3 Expressive design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailScreen(
    employee: Employee,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: EmployeeDetailViewModel = viewModel(
        key = employee.uid,
        factory = EmployeeDetailViewModelFactory(context, employee)
    )
    
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsState()
    val currentAvatarUrl by viewModel.currentAvatarUrl.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var showEditDialog by remember { mutableStateOf(false) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadAvatar(context, it)
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadAttendanceHistory()
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Employee Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            // Avatar Section
            item {
                AvatarSection(
                    avatarUrl = currentAvatarUrl,
                    employeeName = employee.displayName,
                    roleColor = getRoleColor(employee.role),
                    isUploading = isUploadingAvatar,
                    onUploadClick = { imagePickerLauncher.launch("image/*") }
                )
            }
            
            // Basic Info Section
            item {
                BasicInfoSection(employee = employee)
            }
            
            // Attendance History Section
            item {
                AttendanceHistorySection(
                    records = attendanceRecords,
                    isLoading = isLoading
                )
            }
            
            // Error message
            if (errorMessage != null) {
                item {
                    ErrorMessage(message = errorMessage!!)
                }
            }
        }
    }
    
    // Edit Dialog
    if (showEditDialog) {
        EditEmployeeDialog(
            employee = employee,
            onDismiss = { showEditDialog = false },
            onSave = { updatedEmployee ->
                viewModel.updateEmployee(updatedEmployee)
                showEditDialog = false
                onUpdate()
            }
        )
    }
}

/**
 * Avatar Section (iOS style)
 */
@Composable
fun AvatarSection(
    avatarUrl: String?,
    employeeName: String,
    roleColor: Color,
    isUploading: Boolean,
    onUploadClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            // Avatar
            if (!avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = employeeName,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Default gradient avatar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(roleColor, CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employeeName.first().uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
            
            // Camera button
            FloatingActionButton(
                onClick = onUploadClick,
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Upload Photo",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        if (isUploading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    "Uploading...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Basic Info Section (iOS style)
 */
@Composable
fun BasicInfoSection(employee: Employee) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Name
            Text(
                text = employee.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            Divider()
            
            // Employee Details
            InfoDetailRow(
                icon = Icons.Default.Badge,
                label = "Employee ID",
                value = employee.employeeId
            )
            
            employee.email?.let { email ->
                InfoDetailRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = email
                )
            }
            
            employee.phoneNumber?.let { phone ->
                InfoDetailRow(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = phone
                )
            }
            
            InfoDetailRow(
                icon = Icons.Default.Business,
                label = "Department",
                value = employee.team
            )
            
            InfoDetailRow(
                icon = Icons.Default.AdminPanelSettings,
                label = "Role",
                value = when (employee.role) {
                    EmployeeRole.ADMIN -> "Admin"
                    EmployeeRole.SUPERVISOR -> "Supervisor"
                    EmployeeRole.EMPLOYEE -> "Employee"
                }
            )
            
            // Status
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (employee.isActive) ATSColors.ActiveDot else ATSColors.InactiveDot
                )
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (employee.isActive) "Active" else "Inactive",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Info Detail Row
 */
@Composable
fun InfoDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Attendance History Section - Compact Design
 */
@Composable
fun AttendanceHistorySection(
    records: List<AttendanceRecord>,
    isLoading: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Recent Attendance (30 days)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            records.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.EventBusy,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No records yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    records.take(5).forEach { record ->
                        CompactAttendanceCard(record = record)
                    }
                    
                    if (records.size > 5) {
                        Text(
                            text = "Showing last 5 of ${records.size} records",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact Attendance Card - Space-efficient design
 */
@Composable
fun CompactAttendanceCard(record: AttendanceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date and times column
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = SimpleDateFormat("MMM dd", java.util.Locale.US)
                        .format((record.date ?: record.checkInTime).toDate()),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SimpleDateFormat("hh:mm a", java.util.Locale.US)
                            .format(record.checkInTime.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (record.checkOutTime != null) {
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SimpleDateFormat("hh:mm a", java.util.Locale.US)
                                .format(record.checkOutTime!!.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Status badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = when (record.status) {
                    AttendanceStatus.CHECKED_IN -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                    AttendanceStatus.CHECKED_OUT -> Color(0xFF2196F3).copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = when (record.status) {
                        AttendanceStatus.CHECKED_IN -> "Active"
                        AttendanceStatus.CHECKED_OUT -> "Done"
                        else -> record.status.value
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = when (record.status) {
                        AttendanceStatus.CHECKED_IN -> Color(0xFF4CAF50)
                        AttendanceStatus.CHECKED_OUT -> Color(0xFF2196F3)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * Attendance Record Card (iOS style) - DEPRECATED, use CompactAttendanceCard
 */
@Composable
fun AttendanceRecordCard(record: AttendanceRecord) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
                        .format((record.date ?: record.checkInTime).toDate()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Status badge
                StatusBadge(status = record.status)
            }
            
            // Check-in time
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Login,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Green
                )
                Text(
                    text = "In: ${SimpleDateFormat("hh:mm a", java.util.Locale.US)
                        .format(record.checkInTime.toDate())}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Check-out time
            record.checkOutTime?.let { checkOut ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Logout,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFF9800)
                    )
                    Text(
                        text = "Out: ${SimpleDateFormat("hh:mm a", java.util.Locale.US)
                            .format(checkOut.toDate())}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Duration
            if (record.totalDuration != null || record.duration != null) {
                val duration = (record.totalDuration ?: record.duration?.toDouble() ?: 0.0).toLong()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Status Badge
 */
@Composable
fun StatusBadge(status: AttendanceStatus) {
    val (color, text) = when (status) {
        AttendanceStatus.CHECKED_IN -> Color.Green to "Checked In"
        AttendanceStatus.CHECKED_OUT -> Color(0xFF2196F3) to "Checked Out"
        AttendanceStatus.ON_LEAVE -> Color(0xFFFF9800) to "On Leave"
        AttendanceStatus.ABSENT -> Color(0xFFF44336) to "Absent"
    }
    
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/**
 * Error Message
 */
@Composable
fun ErrorMessage(message: String) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Format duration
 */
fun formatDuration(durationSeconds: Long): String {
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    return "${hours}h ${minutes}m"
}
