package com.ats.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.models.Employee
import com.ats.android.models.EmployeeRole
import com.ats.android.ui.components.*
import com.ats.android.ui.theme.*
import com.ats.android.viewmodels.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource


/**
 * Dashboard matching iOS design exactly
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IOSDashboardScreen(
    employee: Employee?,
    viewModel: DashboardViewModel = viewModel()
) {
    val activeEmployeeItems by viewModel.activeEmployeeItems.collectAsState()
    val activityItems by viewModel.activityItems.collectAsState()
    val stats by viewModel.stats.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(com.ats.android.R.string.dashboard),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            // Summary Cards Section (2x2 Grid)
            item {
                SummaryCardsSection(
                    activeCount = stats.activeNow,
                    totalEmployees = stats.totalEmployees,
                    onLeaveCount = stats.onLeave,
                    todayCheckIns = stats.checkedInToday
                )
            }
            
            // Live Activity Feed
            item {
                LiveActivitySection(activities = activityItems)
            }
            
            // Active Employees Section
            item {
                ActiveEmployeesSection(employees = activeEmployeeItems)
            }
        }
    }
}

/**
 * 2x2 Grid of Summary Cards (iOS style)
 */
@Composable
fun SummaryCardsSection(
    activeCount: Int,
    totalEmployees: Int,
    onLeaveCount: Int,
    todayCheckIns: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        // First Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = stringResource(com.ats.android.R.string.active_now),
                value = activeCount.toString(),
                icon = Icons.Default.PersonAdd,
                iconColor = ATSColors.ActiveNowGreen
            )
            
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = stringResource(com.ats.android.R.string.total_employees),
                value = totalEmployees.toString(),
                icon = Icons.Default.People,
                iconColor = ATSColors.TotalEmployeesBlue
            )
        }
        
        // Second Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = stringResource(com.ats.android.R.string.on_leave),
                value = onLeaveCount.toString(),
                icon = Icons.Default.CalendarToday,
                iconColor = ATSColors.OnLeaveOrange
            )
            
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = stringResource(com.ats.android.R.string.todays_checkins),
                value = todayCheckIns.toString(),
                icon = Icons.Default.AccessTime,
                iconColor = ATSColors.TodayCheckInsPurple
            )
        }
    }
}

/**
 * Individual Summary Card (iOS .ultraThinMaterial style)
 */
@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = CornerRadius.medium
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = value,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Live Activity Feed Section
 */
@Composable
fun LiveActivitySection(activities: List<ActivityItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = stringResource(com.ats.android.R.string.live_activity),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        if (activities.isEmpty()) {
            GlassCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = stringResource(com.ats.android.R.string.no_recent_activity),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(com.ats.android.R.string.employee_activity_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            GlassCard {
                Column {
                    activities.forEachIndexed { index, activity ->
                        ActivityRow(activity = activity)
                        if (index < activities.lastIndex) {
                            Divider(color = ATSColors.DividerColor)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Activity Row (iOS style)
 */
@Composable
fun ActivityRow(activity: ActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = activity.icon,
            contentDescription = null,
            tint = activity.iconColor,
            modifier = Modifier.size(24.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = activity.employeeName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = activity.action,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = activity.timeAgo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Active Employees Section
 */
@Composable
fun ActiveEmployeesSection(employees: List<ActiveEmployeeItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(com.ats.android.R.string.active_employees),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            TextButton(onClick = { /* Navigate to map */ }) {
                Text(stringResource(com.ats.android.R.string.view_map), fontSize = 14.sp)
            }
        }
        
        if (employees.isEmpty()) {
            GlassCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = stringResource(com.ats.android.R.string.no_active_employees),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(com.ats.android.R.string.active_employees_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                employees.forEach { employee ->
                    ActiveEmployeeCard(employee = employee)
                }
            }
        }
    }
}

/**
 * Active Employee Card (iOS style with green dot)
 */
@Composable
fun ActiveEmployeeCard(employee: ActiveEmployeeItem) {
    GlassCard(cornerRadius = CornerRadius.small) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Green active dot (8dp)
            ActiveStatusDot(isActive = true, size = 8.dp)
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = employee.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                employee.placeName?.let { place ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = ATSColors.SupervisorBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = place,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                } ?: Text(
                    text = employee.department,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = employee.checkInTime,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = employee.duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Data Classes
data class ActivityItem(
    val employeeName: String,
    val action: String,
    val timeAgo: String,
    val icon: ImageVector,
    val iconColor: Color
)

data class ActiveEmployeeItem(
    val id: String,
    val name: String,
    val department: String,
    val checkInTime: String,
    val duration: String,
    val placeName: String?,
    val avatarUrl: String? = null,
    val role: EmployeeRole = EmployeeRole.EMPLOYEE
)
