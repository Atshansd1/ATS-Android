package com.ats.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ats.android.R
import com.ats.android.models.Employee
import com.ats.android.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reports Screen matching iOS ImprovedReportsView design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IOSReportsScreen(
    currentEmployee: Employee?,
    viewModel: com.ats.android.viewmodels.ReportsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val reportData by viewModel.reportData.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Get string resources that will be used in callbacks
    val todayReportGenerated = stringResource(R.string.today_report_generated)
    val weeklyReportGenerated = stringResource(R.string.weekly_report_generated)
    val monthlyReportGenerated = stringResource(R.string.monthly_report_generated)
    val reportExported = stringResource(R.string.report_exported)
    val reportExportFailed = stringResource(R.string.report_export_failed)
    
    var showConfigDialog by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var showMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.reports),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Reports Section
                item {
                    QuickReportsSection(
                        onGenerateToday = {
                            scope.launch {
                                viewModel.generateQuickReport(days = 0)
                                val exported = viewModel.exportAndShare(context)
                                if (exported) {
                                    showMessage = todayReportGenerated
                                } else {
                                    showMessage = reportExportFailed
                                }
                            }
                        },
                        onGenerateWeek = {
                            scope.launch {
                                viewModel.generateQuickReport(days = 7)
                                val exported = viewModel.exportAndShare(context)
                                if (exported) {
                                    showMessage = weeklyReportGenerated
                                } else {
                                    showMessage = reportExportFailed
                                }
                            }
                        },
                        onGenerateMonth = {
                            scope.launch {
                                viewModel.generateQuickReport(days = 30)
                                val exported = viewModel.exportAndShare(context)
                                if (exported) {
                                    showMessage = monthlyReportGenerated
                                } else {
                                    showMessage = reportExportFailed
                                }
                            }
                        }
                    )
                }
                
                // Custom Report Section
                item {
                    val startDate by viewModel.startDate.collectAsState()
                    val endDate by viewModel.endDate.collectAsState()
                    val selectedEmployees by viewModel.selectedEmployees.collectAsState()
                    
                    CustomReportSection(
                        startDate = startDate,
                        endDate = endDate,
                        selectedEmployeeCount = selectedEmployees.size,
                        onClick = { showConfigDialog = true }
                    )
                }
                
                // Generate Buttons (Preview and Export)
                item {
                    // Preview button
                    OutlinedButton(
                        onClick = {
                            viewModel.generateReport(preview = true)
                            showPreview = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = uiState !is com.ats.android.viewmodels.ReportsUiState.Generating
                    ) {
                        Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.preview))
                    }
                }
                
                // Export Options
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // CSV Export
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.generateReport(preview = false)
                                    val exported = viewModel.exportAndShare(context)
                                    if (exported) {
                                        showMessage = reportExported
                                    } else {
                                        showMessage = reportExportFailed
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = uiState !is com.ats.android.viewmodels.ReportsUiState.Generating,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFF217346)
                            )
                        ) {
                            Icon(Icons.Default.TableChart, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV")
                        }
                        

                    }
                }
                
                // Info Section
                item {
                    InfoSection()
                }
            }
            
            // Loading overlay
            if (uiState is com.ats.android.viewmodels.ReportsUiState.Generating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(stringResource(R.string.generating_report))
                        }
                    }
                }
            }
        }
    }
    
    // Success message
    showMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            showMessage = null
        }
        
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                Text(message)
            }
        }
    }
    
    // Configure Report Dialog
    if (showConfigDialog) {
        ConfigureReportDialog(
            viewModel = viewModel,
            onDismiss = { showConfigDialog = false }
        )
    }
    
    // Preview sheet
    if (showPreview && reportData.isNotEmpty()) {
        ReportPreviewSheet(
            reportData = reportData,
            summary = viewModel.getReportSummary(),
            onDismiss = { showPreview = false },
            onExport = {
                viewModel.generateReport(preview = false)
                showPreview = false
                showMessage = reportExported
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPreviewSheet(
    reportData: List<com.ats.android.models.AttendanceRecord>,
    summary: com.ats.android.viewmodels.ReportSummary,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.report_preview),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(R.string.close))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Summary cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.records),
                    value = summary.totalRecords.toString(),
                    icon = Icons.Default.Description,
                    color = ATSColors.SupervisorBlue
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.employees),
                    value = summary.uniqueEmployees.toString(),
                    icon = Icons.Default.People,
                    color = ATSColors.EmployeeGreen
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.total_hours),
                    value = String.format(java.util.Locale.US, "%.1f", summary.totalHours),
                    icon = Icons.Default.Timer,
                    color = ATSColors.OnLeaveOrange
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.avg_day),
                    value = String.format(java.util.Locale.US, "%.1f", summary.averageHoursPerRecord),
                    icon = Icons.Default.BarChart,
                    color = ATSColors.AdminPurple
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Records list
            Text(
                text = stringResource(R.string.attendance_records),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reportData.size) { index ->
                    val record = reportData[index]
                    PreviewRecordCard(record = record)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Export button
            Button(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.export_records_csv, reportData.size))
            }
        }
    }
}

@Composable
fun SummaryStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PreviewRecordCard(record: com.ats.android.models.AttendanceRecord) {
    // Always use English format for dates and numbers
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = record.employeeName ?: record.employeeId,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dateFormat.format((record.date ?: record.checkInTime).toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.check_in),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = timeFormat.format(record.checkInTime.toDate()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                record.checkOutTime?.let { checkOut ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.check_out),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = timeFormat.format(checkOut.toDate()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                if (record.totalDuration != null || record.duration != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.1fh", record.durationHours),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ATSColors.SupervisorBlue
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quick Reports Section (Today, Week, Month)
 */
@Composable
fun QuickReportsSection(
    onGenerateToday: () -> Unit,
    onGenerateWeek: () -> Unit,
    onGenerateMonth: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.quick_reports),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickReportButton(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.today),
                icon = Icons.Default.CalendarToday,
                onClick = onGenerateToday
            )
            
            QuickReportButton(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.this_week),
                icon = Icons.Default.CalendarMonth,
                onClick = onGenerateWeek
            )
            
            QuickReportButton(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.this_month),
                icon = Icons.Default.Event,
                onClick = onGenerateMonth
            )
        }
    }
}

@Composable
fun QuickReportButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Custom Report Section
 */
@Composable
fun CustomReportSection(
    startDate: Date,
    endDate: Date,
    selectedEmployeeCount: Int,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.custom_report),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                // Employee Selection
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.employees),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    supportingContent = {
                        Text(
                            text = if (selectedEmployeeCount == 0) stringResource(R.string.all_employees) else stringResource(R.string.employees_selected, selectedEmployeeCount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, null)
                    }
                )
                
                Divider()
                
                // Date Range
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.date_range),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    supportingContent = {
                        // Always use English format for dates
                        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                        Text(
                            text = "${formatter.format(startDate)} - ${formatter.format(endDate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, null)
                    }
                )
            }
        }
    }
}

/**
 * Generate Report Button
 */
@Composable
fun GenerateReportButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(CornerRadius.medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, "Generate Report")
            Text(
                text = "Generate & Export Report",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Configure Report Dialog (matching iOS CreateDetailedReportView)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureReportDialog(
    viewModel: com.ats.android.viewmodels.ReportsViewModel,
    onDismiss: () -> Unit
) {
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val selectedEmployees by viewModel.selectedEmployees.collectAsState()
    val allEmployees by viewModel.allEmployees.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectAll by remember(selectedEmployees, allEmployees) { 
        mutableStateOf(selectedEmployees.size == allEmployees.size && allEmployees.isNotEmpty())
    }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Text(
                text = stringResource(R.string.configure_report),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Date Range Section
            Text(
                text = stringResource(R.string.date_range),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Start Date
                    DatePickerField(
                        label = stringResource(R.string.start_date),
                        date = startDate,
                        onDateSelected = { viewModel.setStartDate(it) },
                        context = context
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // End Date
                    DatePickerField(
                        label = stringResource(R.string.end_date),
                        date = endDate,
                        onDateSelected = { viewModel.setEndDate(it) },
                        context = context
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Employee Selection Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.employee_selection),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.employees_of, selectedEmployees.size, allEmployees.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Select All Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.select_all),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = selectAll,
                        onCheckedChange = { checked ->
                            selectAll = checked
                            if (checked) {
                                viewModel.selectAllEmployees()
                            } else {
                                viewModel.clearEmployeeSelection()
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_employees)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Employee List
            val filteredEmployees = remember(allEmployees, searchQuery) {
                if (searchQuery.isEmpty()) {
                    allEmployees
                } else {
                    allEmployees.filter { employee ->
                        employee.englishName.contains(searchQuery, ignoreCase = true) ||
                        employee.arabicName.contains(searchQuery, ignoreCase = true) ||
                        employee.employeeId.contains(searchQuery, ignoreCase = true)
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredEmployees.size) { index ->
                    val employee = filteredEmployees[index]
                    EmployeeCheckItem(
                        employee = employee,
                        isSelected = selectedEmployees.contains(employee.uid),
                        onToggle = { viewModel.toggleEmployeeSelection(employee.uid) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Done Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.done))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DatePickerField(
    label: String,
    date: Date,
    onDateSelected: (Date) -> Unit,
    context: android.content.Context
) {
    // Always use English format for dates
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    
    OutlinedButton(
        onClick = {
            val calendar = Calendar.getInstance()
            calendar.time = date
            
            val datePickerDialog = android.app.DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val newCalendar = Calendar.getInstance()
                    newCalendar.set(year, month, dayOfMonth)
                    onDateSelected(newCalendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatter.format(date),
                    fontWeight = FontWeight.Medium
                )
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun EmployeeCheckItem(
    employee: com.ats.android.models.Employee,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Circle,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = employee.employeeId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = employee.team,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Info Section
 */
@Composable
fun InfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.about_reports),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = stringResource(R.string.reports_info_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Simulate report generation (In production, this would call a ViewModel/Service)
 */
private suspend fun generateQuickReport(days: Int) {
    // Simulate API call
    kotlinx.coroutines.delay(1500)
    // In production: Call ReportService to generate CSV
}

private suspend fun generateCustomReport(
    startDate: Date,
    endDate: Date,
    selectedEmployees: Set<String>
) {
    // Simulate API call
    kotlinx.coroutines.delay(2000)
    // In production: Call ReportService with parameters
}
