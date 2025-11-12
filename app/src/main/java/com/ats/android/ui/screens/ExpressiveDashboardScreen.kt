package com.ats.android.ui.screens

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
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ats.android.R
import com.ats.android.models.Employee
import com.ats.android.models.EmployeeRole
import com.ats.android.ui.components.*
import com.ats.android.ui.theme.*
import com.ats.android.viewmodels.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Material 3 Expressive Dashboard Screen
 * Complete redesign following https://m3.material.io/
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveDashboardScreen(
    employee: Employee?,
    onNavigateToMovements: () -> Unit = {},
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
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.dashboard),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardData() }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.refresh))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Welcome Header
            item {
                Text(
                    text = stringResource(R.string.welcome, employee?.displayName ?: stringResource(R.string.user)),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Stats Grid (2x2)
            item {
                ExpressiveSummaryCardsSection(
                    activeCount = stats.activeNow,
                    totalEmployees = stats.totalEmployees,
                    onLeaveCount = stats.onLeave,
                    todayCheckIns = stats.checkedInToday
                )
            }
            
            // Live Activity Feed
            item {
                ExpressiveLiveActivitySection(
                    activities = activityItems,
                    onMovementsClick = onNavigateToMovements
                )
            }
            
            // Active Employees
            item {
                ExpressiveActiveEmployeesSection(employees = activeEmployeeItems)
            }
        }
    }
}

/**
 * Expressive 2x2 Grid of Summary Cards with gradients and shadows
 */
@Composable
fun ExpressiveSummaryCardsSection(
    activeCount: Int,
    totalEmployees: Int,
    onLeaveCount: Int,
    todayCheckIns: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExpressiveSummaryCard(
                title = stringResource(R.string.active_now),
                value = activeCount.toString(),
                icon = Icons.Default.PersonPinCircle,
                gradient = ExpressiveSuccessGradient,
                iconColor = ExpressiveOnline,
                modifier = Modifier.weight(1f)
            )
            
            ExpressiveSummaryCard(
                title = stringResource(R.string.total_employees),
                value = totalEmployees.toString(),
                icon = Icons.Default.Groups,
                gradient = ExpressivePrimaryGradient,
                iconColor = ExpressiveLightPrimary,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Second Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExpressiveSummaryCard(
                title = stringResource(R.string.on_leave),
                value = onLeaveCount.toString(),
                icon = Icons.Default.CalendarToday,
                gradient = ExpressiveWarningGradient,
                iconColor = ExpressiveAway,
                modifier = Modifier.weight(1f)
            )
            
            ExpressiveSummaryCard(
                title = stringResource(R.string.todays_checkins),
                value = todayCheckIns.toString(),
                icon = Icons.Default.HowToReg,
                gradient = ExpressivePrimaryGradient,
                iconColor = ExpressiveLightPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * M3 Expressive Summary Card - Using proper M3 color roles and shapes
 */
@Composable
fun ExpressiveSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    gradient: List<Color>,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    // Determine container color based on card type
    val containerColor = when {
        title.contains("Active") -> MaterialTheme.colorScheme.primaryContainer
        title.contains("Total") -> MaterialTheme.colorScheme.secondaryContainer
        title.contains("Leave") -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    
    val contentColor = when {
        title.contains("Active") -> MaterialTheme.colorScheme.onPrimaryContainer
        title.contains("Total") -> MaterialTheme.colorScheme.onSecondaryContainer
        title.contains("Leave") -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )
    
    Card(
        modifier = modifier
            .height(100.dp)
            .scale(animatedScale),
        shape = ComponentShapes.Card,  // 20dp - Large
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(ComponentShapes.IconContainer)  // 12dp - Medium
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Value and title
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = contentColor,
                    maxLines = 1
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = contentColor.copy(alpha = 0.85f),
                    maxLines = 2
                )
            }
        }
    }
}



/**
 * M3 Expressive Live Activity Section - Compact & Scrollable (24 hours only)
 */
@Composable
fun ExpressiveLiveActivitySection(
    activities: List<ActivityItem>,
    onMovementsClick: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.live_activity_24h),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Pulsing live indicator
                val alpha by rememberInfiniteTransition(label = "live").animateFloat(
                    initialValue = 1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                )
            }
            
            // Link to full movements view (iOS style)
            TextButton(
                onClick = onMovementsClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.employee_movements),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        
        if (activities.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ComponentShapes.Card,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = stringResource(R.string.no_recent_activity),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.employee_activity_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            // Compact scrollable activity list
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp), // Fixed height for scrolling
                shape = ComponentShapes.Card,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(activities.size) { index ->
                        CompactActivityItem(activity = activities[index])
                        
                        if (index < activities.size - 1) {
                            Divider(
                                modifier = Modifier.padding(vertical = 2.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Activity Item for scrollable list
 */
@Composable
fun CompactActivityItem(activity: ActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(activity.iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = activity.icon,
                contentDescription = null,
                tint = activity.iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = activity.employeeName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = activity.action,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        
        Text(
            text = activity.timeAgo,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * M3 Expressive Activity Row
 */
@Composable
fun ExpressiveActivityRow(activity: ActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(ComponentShapes.IconContainer)
                .background(activity.iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = activity.icon,
                contentDescription = null,
                tint = activity.iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = activity.employeeName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = activity.action,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = activity.timeAgo,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * M3 Expressive Active Employees Section with real data
 */
@Composable
fun ExpressiveActiveEmployeesSection(
    employees: List<ActiveEmployeeItem>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.active_employees),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            TextButton(onClick = { /* Navigate to map */ }) {
                Text(
                    text = stringResource(R.string.view_map),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        
        if (employees.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ComponentShapes.Card,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = stringResource(R.string.no_active_employees),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.active_employees_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                employees.forEach { employee ->
                    ExpressiveActiveEmployeeCard(employee = employee)
                }
            }
        }
    }
}

/**
 * Get role-based color for avatars and badges
 */
fun getRoleColor(role: EmployeeRole): Color {
    return when (role) {
        EmployeeRole.ADMIN -> Color(0xFF9C27B0) // Purple
        EmployeeRole.SUPERVISOR -> Color(0xFF2196F3) // Blue
        EmployeeRole.EMPLOYEE -> Color(0xFF4CAF50) // Green
    }
}

/**
 * M3 Expressive Active Employee Card with avatar and green dot (iOS style)
 */
@Composable
fun ExpressiveActiveEmployeeCard(employee: ActiveEmployeeItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ComponentShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with active indicator
            Box {
                // Employee Avatar
                if (!employee.avatarUrl.isNullOrEmpty()) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(employee.avatarUrl)
                            .memoryCacheKey(employee.avatarUrl)
                            .diskCacheKey(employee.avatarUrl)
                            .crossfade(300)
                            .build(),
                        contentDescription = employee.name,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Default gradient avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        getRoleColor(employee.role).copy(alpha = 0.6f),
                                        getRoleColor(employee.role).copy(alpha = 0.3f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = employee.name.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
                
                // Green active dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = employee.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (employee.placeName != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = employee.placeName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                } else {
                    Text(
                        text = employee.department,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = employee.checkInTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = employee.duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
