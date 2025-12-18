package com.ats.android.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.R
import com.ats.android.models.AttendanceRecord
import com.ats.android.models.Employee
import com.ats.android.ui.theme.CornerRadius
import com.ats.android.ui.components.GlassCard
import com.ats.android.ui.theme.ATSColors
import com.ats.android.viewmodels.HistoryViewModel
import com.ats.android.viewmodels.HistoryUiState
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    currentEmployee: Employee?,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    
    LaunchedEffect(currentEmployee) {
        if (currentEmployee != null) {
            viewModel.loadHistory(currentEmployee)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Expressive Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.attendance_history), fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = { viewModel.refresh(currentEmployee) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            when (uiState) {
                is HistoryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is HistoryUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.error_loading_history), color = MaterialTheme.colorScheme.error)
                    }
                }
                is HistoryUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Summary Cards Row
                        item {
                            val totalDays = attendanceRecords.size
                            val totalHours = attendanceRecords.sumOf { it.durationHours }
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                GlassCard(modifier = Modifier.weight(1f), cornerRadius = CornerRadius.medium) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(totalDays.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(stringResource(R.string.days_present), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                
                                GlassCard(modifier = Modifier.weight(1f), cornerRadius = CornerRadius.medium) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(String.format("%.1f", totalHours), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(stringResource(R.string.total_hours), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        
                        if (attendanceRecords.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.no_records_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(attendanceRecords) { record ->
                                HistoryAttendanceRecordCard(record)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryAttendanceRecordCard(record: AttendanceRecord) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val isActive = record.checkOutTime == null
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = CornerRadius.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = dateFormat.format(record.checkInTime.toDate()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (isActive) {
                    Surface(
                        color = ATSColors.CheckInGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.active),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = ATSColors.CheckInGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                     val totalSeconds = (record.totalDuration ?: record.duration?.toDouble() ?: 0.0).toLong()
                     val hours = TimeUnit.SECONDS.toHours(totalSeconds)
                     val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
                     Text(
                         text = stringResource(R.string.duration_hm_fmt, hours, minutes),
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            // Times
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // In
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.check_in_verb), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(timeFormat.format(record.checkInTime.toDate()), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    if (record.checkInPlaceName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.Place, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(record.checkInPlaceName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
                
                // Out
                 Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.check_out_verb), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = record.checkOutTime?.let { timeFormat.format(it.toDate()) } ?: "--:--", 
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )
                     if (record.checkOutPlaceName != null) {
                        if (record.checkOutPlaceName == "Admin Force Checkout") {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.Warning, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.error)
                                Text(
                                    stringResource(R.string.admin_checkout_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.Place, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(record.checkOutPlaceName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
