package com.ats.android.ui.leaves

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.ats.android.R
import com.ats.android.models.*
import com.ats.android.viewmodels.LeaveViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeavesScreen(
    employeeId: String,
    viewModel: LeaveViewModel = viewModel(),
    onRequestLeaveClick: () -> Unit
) {
    val myRequests by viewModel.myRequests.collectAsState()
    val leaveBalance by viewModel.leaveBalance.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    LaunchedEffect(employeeId) {
        viewModel.loadMyRequests(employeeId)
        viewModel.loadBalance(employeeId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_leaves)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onRequestLeaveClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.request_leave))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isLoading && myRequests.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Balance Section
                    item {
                        Text(
                            text = stringResource(R.string.leave_balance),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                viewModel.getLeaveSummaries().forEach { summary ->
                                    LeaveBalanceRow(summary)
                                    if (summary != viewModel.getLeaveSummaries().last()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                    
                    // Requests Section
                    item {
                        Text(
                            text = stringResource(R.string.my_requests),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
                    
                    if (myRequests.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_leave_requests_found),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(myRequests) { request ->
                            LeaveRequestCard(request)
                        }
                    }
                }
            }
            
            // Error handling
            if (errorMessage != null) {
                // Show Snackbar or error dialog
            }
        }
    }
}

@Composable
fun LeaveBalanceRow(summary: LeaveSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon based on type
                Icon(
                    imageVector = when(summary.type) {
                        LeaveType.VACATION -> Icons.Default.DateRange // Placeholder icon
                        else -> Icons.Default.DateRange
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when(summary.type) {
                        LeaveType.VACATION -> stringResource(R.string.vacation)
                        LeaveType.SICK -> stringResource(R.string.sick_leave)
                        LeaveType.PERSONAL -> stringResource(R.string.personal_leave)
                        LeaveType.EMERGENCY -> stringResource(R.string.emergency_leave)
                        LeaveType.UNPAID -> stringResource(R.string.unpaid_leave)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = stringResource(R.string.days_remaining_format, summary.remaining, summary.total),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (summary.remaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        
        // Progress Bar
        LinearProgressIndicator(
            progress = summary.percentage().toFloat() / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (summary.remaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        
        Text(
            text = stringResource(R.string.days_used_format, summary.used),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LeaveRequestCard(request: LeaveRequest) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = request.getLeaveType().displayName(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatDate(request.startDate.toDate())} - ${formatDate(request.endDate.toDate())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                StatusBadge(status = request.getStatus())
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${request.getNumberOfDays()} days",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                
                if (request.reason.isNotEmpty()) {
                    Text(
                        text = " • ${request.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            
            if (!request.reviewNotes.isNullOrEmpty()) {
                Container(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Note: ${request.reviewNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun Container(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier) {
        content()
    }
}

@Composable
fun StatusBadge(status: LeaveStatus) {
    val (backgroundColor, textColor) = when (status) {
        LeaveStatus.PENDING -> Color(0xFFFFA726) to Color.White // Orange
        LeaveStatus.APPROVED -> Color(0xFF66BB6A) to Color.White // Green
        LeaveStatus.REJECTED -> Color(0xFFEF5350) to Color.White // Red
        LeaveStatus.CANCELLED -> Color.Gray to Color.White
    }
    
    Text(
        text = status.displayName(),
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private fun formatDate(date: java.util.Date): String {
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    return formatter.format(date)
}
