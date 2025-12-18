package com.ats.android.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.R
import com.ats.android.models.*
import com.ats.android.ui.components.GlassCard
import com.ats.android.ui.theme.ATSColors
import com.ats.android.ui.theme.CornerRadius
import com.ats.android.ui.theme.Spacing
import com.ats.android.viewmodels.AnalyticsUiState
import com.ats.android.viewmodels.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    
    // Refresh data when filter changes or on enter
    LaunchedEffect(Unit) {
        viewModel.loadAnalytics()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.analytics_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Filter Section
            FilterSection(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.updateFilter(it) }
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // 2. Content based on state
            when (uiState) {
                is AnalyticsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AnalyticsUiState.Error -> {
                     Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = (uiState as AnalyticsUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is AnalyticsUiState.Success -> {
                    if (metrics != null) {
                        val data = metrics!!
                        
                        // Summary Cards
                        SummaryCardsGrid(metrics = data)
                        
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        
                        // Attendance Trend Chart
                        AttendanceTrendChart(trends = data.dailyTrends)
                        
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        
                        // Hourly Activity Chart
                        HourlyActivityChart(activity = data.hourlyActivity)
                        
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        
                        // Top Locations
                        TopLocationsSection(locations = data.topLocations)
                        
                        Spacer(modifier = Modifier.height(Spacing.xl)) // Bottom padding
                        
                    } else {
                        EmptyState()
                    }
                }
            }
        }
    }
}

// MARK: - Components

@Composable
fun FilterSection(
    selectedFilter: AnalyticsFilter,
    onFilterSelected: (AnalyticsFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        FilterButton(
            title = stringResource(R.string.analytics_week),
            isSelected = selectedFilter == AnalyticsFilter.LAST_WEEK,
            onClick = { onFilterSelected(AnalyticsFilter.LAST_WEEK) }
        )
        FilterButton(
            title = stringResource(R.string.analytics_month),
            isSelected = selectedFilter == AnalyticsFilter.LAST_MONTH,
            onClick = { onFilterSelected(AnalyticsFilter.LAST_MONTH) }
        )
        FilterButton(
            title = stringResource(R.string.analytics_3_months),
            isSelected = selectedFilter == AnalyticsFilter.LAST_3_MONTHS,
            onClick = { onFilterSelected(AnalyticsFilter.LAST_3_MONTHS) }
        )
    }
}

@Composable
fun RowScope.FilterButton(
    title: String,
    isSelected: Bool,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
    }
}

// Typealias for boolean to match Swift if copied (Kotlin uses Boolean)
typealias Bool = Boolean

@Composable
fun SummaryCardsGrid(metrics: DashboardMetrics) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetricCard(
                title = stringResource(R.string.analytics_total_employees),
                value = metrics.totalEmployees.toString(),
                icon = Icons.Default.Person, // person.3.fill approx
                color = Color.Blue,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.analytics_active_today),
                value = metrics.activeToday.toString(),
                icon = Icons.Default.CheckCircle, // checkmark.circle.fill
                color = Color(0xFF4CAF50), // Green
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetricCard(
                title = stringResource(R.string.analytics_avg_work_hours),
                value = String.format("%.1fh", metrics.averageWorkHours),
                icon = Icons.Default.AccessTime, // clock.fill
                color = Color(0xFFFF9800), // Orange
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.analytics_attendance_rate),
                value = String.format("%.0f%%", metrics.attendanceRate),
                icon = Icons.Default.BarChart, // chart.bar.fill
                color = Color(0xFF9C27B0), // Purple
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = CornerRadius.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AttendanceTrendChart(trends: List<DailyTrend>) {
    Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
        Text(
            text = stringResource(R.string.analytics_attendance_trend),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            cornerRadius = CornerRadius.medium
        ) {
            if (trends.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.analytics_no_data), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                // Custom Line Chart
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)) {
                    
                    val maxVal = trends.maxOfOrNull { it.checkIns }?.coerceAtLeast(1) ?: 1
                    val points = trends.map { it.checkIns.toFloat() }
                    
                    val width = size.width
                    val height = size.height
                    // Leave space for labels
                    val chartHeight = height * 0.85f
                    val chartWidth = width 
                    
                    val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)
                    val scaleY = chartHeight / maxVal
                    
                    val path = Path()
                    var firstPoint = Offset(0f, 0f)
                    
                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = chartHeight - (value * scaleY)
                        if (index == 0) {
                            path.moveTo(x, y)
                            firstPoint = Offset(x, y)
                        } else {
                            // Smooth curve (simple bezier or straight line)
                            // Using straight lines for simplicity and robustness
                            path.lineTo(x, y)
                        }
                    }
                    
                    // Draw Line
                    drawPath(
                        path = path,
                        color = Color.Blue,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    
                    // Draw Gradient Fill
                    val fillPath = Path()
                    fillPath.addPath(path)
                    fillPath.lineTo(points.lastIndex * stepX, chartHeight)
                    fillPath.lineTo(0f, chartHeight)
                    fillPath.close()
                    
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Blue.copy(alpha = 0.3f), Color.Transparent),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )
                    
                    // X-Axis Labels (Simple)
                    // Draw simplified X axis labels (e.g., first and last, or every Nth)
                    if (trends.isNotEmpty()) {
                         val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                         
                         // Draw start date
                         /* drawContext.canvas.nativeCanvas.drawText(
                            dateFormat.format(trends.first().date),
                            0f,
                            height,
                            android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 30f }
                         )
                         // Not doing native canvas text drawing to keep it simple and safe compatible with Compose
                         */
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyActivityChart(activity: List<HourlyStats>) {
    Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
        Text(
            text = stringResource(R.string.analytics_hourly_activity),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            cornerRadius = CornerRadius.medium
        ) {
             Canvas(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)) {
                
                val maxVal = activity.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                val width = size.width
                val height = size.height
                val barWidth = (width / activity.size) * 0.7f // 70% width, 30% spacing
                val spacing = (width / activity.size) * 0.3f
                
                activity.forEachIndexed { index, stat ->
                     val x = index * (barWidth + spacing)
                     val barHeight = (stat.count.toFloat() / maxVal.toFloat()) * height
                     val y = height - barHeight
                     
                     drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784)) // Green gradient
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight)
                     )
                }
            }
        }
    }
}

@Composable
fun TopLocationsSection(locations: List<LocationStats>) {
    Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
        Text(
            text = stringResource(R.string.analytics_top_locations),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = CornerRadius.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (locations.isEmpty()) {
                     Text(stringResource(R.string.analytics_no_data), style = MaterialTheme.typography.bodySmall)
                } else {
                    locations.forEach { location ->
                        LocationStatsRow(location = location)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationStatsRow(location: LocationStats) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = location.locationName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = location.checkInCount.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Progress Bar custom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (location.percentage / 100).toFloat().coerceIn(0f, 1f))
                    .background(Color.Blue, CircleShape)
            )
        }
        
        Text(
            text = String.format("%.1f%%", location.percentage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.BarChart,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = Color.Gray
        )
        Text(
            text = stringResource(R.string.analytics_no_data),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.analytics_check_back),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
