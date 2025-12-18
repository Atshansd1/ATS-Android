package com.ats.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.models.Employee
import com.ats.android.models.ActivityType
import com.ats.android.viewmodels.DashboardViewModel
import com.ats.android.viewmodels.DashboardUiState
import androidx.compose.ui.res.stringResource
import com.ats.android.R
import com.ats.android.ui.theme.ATSColors
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentEmployee: Employee?,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val activeEmployees by viewModel.activeEmployees.collectAsState()
    val recentActivity by viewModel.recentActivity.collectAsState()
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            stringResource(R.string.dashboard),
                            style = MaterialTheme.typography.titleLarge
                        )
                        currentEmployee?.let {
                            Text(
                                text = it.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.refresh))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is DashboardUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is DashboardUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (uiState as DashboardUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is DashboardUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding() + 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Stats Section - Minimal Cards
                    item {
                        MinimalStatsSection(stats = stats)
                    }
                    
                    // Activity Feed - Clean List
                    if (recentActivity.isNotEmpty()) {
                        item {
                            MinimalActivitySection(activities = recentActivity.take(5))
                        }
                    }
                    
                    // Active Employees - Simple List
                    if (activeEmployees.isNotEmpty()) {
                        item {
                            MinimalActiveEmployeesSection(
                                activeEmployees = activeEmployees.take(5)
                            )
                        }
                    } else {
                        item {
                            MinimalEmptyState(
                                icon = Icons.Default.LocationOff,
                                title = stringResource(R.string.no_active_employees),
                                description = stringResource(R.string.no_active_employees_desc)
                            )
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Minimal Native Android Components

@Composable
fun MinimalStatsSection(stats: com.ats.android.models.DashboardStats) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary Large Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.active_now),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stats.activeNow.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
            }
        }
        
        // Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MinimalStatCard(
                title = stringResource(R.string.total),
                value = stats.totalEmployees.toString(),
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f)
            )
            
            MinimalStatCard(
                title = stringResource(R.string.today),
                value = stats.checkedInToday.toString(),
                icon = Icons.Default.Login,
                modifier = Modifier.weight(1f)
            )
            
            MinimalStatCard(
                title = stringResource(R.string.on_leave),
                value = stats.onLeave.toString(),
                icon = Icons.Default.PersonOff,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MinimalStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier = modifier.height(90.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MinimalActivitySection(activities: List<com.ats.android.models.EmployeeActivity>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.recent_activity),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        
        Card {
            Column {
                activities.forEachIndexed { index, activity ->
                    MinimalActivityItem(activity = activity)
                    if (index < activities.size - 1) {
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalActivityItem(activity: com.ats.android.models.EmployeeActivity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when(activity.type) {
                    ActivityType.CHECK_IN -> Icons.Default.Login
                    ActivityType.CHECK_OUT -> Icons.Default.Logout
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = when(activity.type) {
                    ActivityType.CHECK_IN -> ATSColors.CheckInGreen
                    ActivityType.CHECK_OUT -> ATSColors.CheckOutBlue
                    else -> MaterialTheme.colorScheme.primary
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(activity.employeeName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(
                        when(activity.type) {
                            ActivityType.CHECK_IN -> R.string.checked_in
                            ActivityType.CHECK_OUT -> R.string.checked_out
                            else -> R.string.status_update
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
             SimpleDateFormat("HH:mm", Locale.getDefault()).format(activity.timestamp.toDate()),
             style = MaterialTheme.typography.labelSmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MinimalActiveEmployeesSection(activeEmployees: List<com.ats.android.models.ActiveEmployeeInfo>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.active_employees)} (${activeEmployees.size})",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = { /* Navigate to full list */ }) {
                Text(stringResource(R.string.view_all))
            }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activeEmployees.forEach { employee ->
                MinimalEmployeeCard(employee = employee)
            }
        }
    }
}

@Composable
fun MinimalEmployeeCard(employee: com.ats.android.models.ActiveEmployeeInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = employee.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(employee.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(employee.department, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MinimalEmptyState(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
