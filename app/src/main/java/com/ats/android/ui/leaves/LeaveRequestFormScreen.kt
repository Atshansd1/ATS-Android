package com.ats.android.ui.leaves

import android.app.DatePickerDialog
import android.content.Context
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.models.LeaveType
import com.ats.android.viewmodels.LeaveViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestFormScreen(
    employeeId: String,
    employeeName: String,
    viewModel: LeaveViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(LeaveType.VACATION) }
    var startDate by remember { mutableStateOf(Date()) }
    var endDate by remember { mutableStateOf(Date()) }
    var reason by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            onDismiss()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Leave") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.submitRequest(
                                employeeId, employeeName, selectedType, startDate, endDate, reason
                            )
                        },
                        enabled = !isLoading && reason.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("Submit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Leave Type Dropdown
            Box {
                OutlinedTextField(
                    value = selectedType.displayName(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Leave Type") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                )
                
                // Invisible box to catch clicks
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expanded = true }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LeaveType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName()) },
                            onClick = {
                                selectedType = type
                                expanded = false
                            },
                        )
                    }
                }
            }
            
            // Date Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Start Date
                OutlinedTextField(
                    value = formatDate(startDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start Date") },
                    trailingIcon = { Icon(Icons.Default.DateRange, null) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker(context, startDate) { startDate = it } }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp) // Match height of TextField
                        .clickable { showDatePicker(context, startDate) { startDate = it } }
                )
                
                // End Date
                OutlinedTextField(
                    value = formatDate(endDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("End Date") },
                    trailingIcon = { Icon(Icons.Default.DateRange, null) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker(context, endDate) { endDate = it } }
                )
            }
            
            // Duration Display
            val days = calculateDays(startDate, endDate)
            Text(
                text = "Total Duration: $days ${if (days == 1) "day" else "days"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            // Reason Input
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun showDatePicker(context: Context, initialDate: Date, onDateSelected: (Date) -> Unit) {
    val calendar = Calendar.getInstance()
    calendar.time = initialDate
    
    DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, day)
            onDateSelected(selectedCalendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}

private fun calculateDays(start: Date, end: Date): Int {
    val diff = end.time - start.time
    val days = (diff / (1000 * 60 * 60 * 24)).toInt()
    return maxOf(days + 1, 1)
}
