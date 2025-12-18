package com.ats.android.ui.leaves

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.models.*
import com.ats.android.viewmodels.LeaveViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveApprovalScreen(
    reviewerId: String,
    viewModel: LeaveViewModel = viewModel()
) {
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    // Dialog states
    var showRejectDialog by remember { mutableStateOf(false) }
    var selectedRequest by remember { mutableStateOf<LeaveRequest?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.loadPendingRequests()
    }
    
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave Approvals") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isLoading && pendingRequests.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (pendingRequests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(bottom = 16.dp),
                            tint = Color.Green
                        )
                        Text(
                            text = "All caught up!",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "No pending leave requests",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pendingRequests) { request ->
                        LeaveApprovalCard(
                            request = request,
                            onApprove = { viewModel.approveRequest(request, reviewerId) },
                            onReject = {
                                selectedRequest = request
                                showRejectDialog = true
                            }
                        )
                    }
                }
            }
        }
        
        if (showRejectDialog && selectedRequest != null) {
            AlertDialog(
                onDismissRequest = { 
                    showRejectDialog = false 
                    rejectReason = ""
                },
                title = { Text("Reject Request") },
                text = {
                    Column {
                        Text("Are you sure you want to reject ${selectedRequest?.employeeName}'s request?")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = rejectReason,
                            onValueChange = { rejectReason = it },
                            label = { Text("Reason (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.rejectRequest(selectedRequest!!, reviewerId, rejectReason)
                            showRejectDialog = false
                            rejectReason = ""
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reject")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showRejectDialog = false 
                            rejectReason = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun LeaveApprovalCard(
    request: LeaveRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Name and Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = request.employeeName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = request.employeeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = request.getLeaveType().displayName(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Text(
                    text = "${request.getNumberOfDays()}d",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            
            // Details
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dates",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatDate(request.startDate.toDate())} - ${formatDate(request.endDate.toDate())}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (request.reason.isNotEmpty()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reason",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = request.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2
                        )
                    }
                }
            }
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reject")
                }
                
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A)) // Green
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Approve")
                }
            }
        }
    }
}

private fun formatDate(date: java.util.Date): String {
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    return formatter.format(date)
}
