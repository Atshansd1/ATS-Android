package com.ats.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ats.android.models.Employee
import com.ats.android.ui.components.*
import com.ats.android.ui.theme.*
import com.ats.android.viewmodels.EmployeeManagementViewModel
import com.ats.android.viewmodels.EmployeeManagementUiState

/**
 * Employee Management Screen matching iOS design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IOSEmployeeManagementScreen(
    viewModel: EmployeeManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val employees by viewModel.employees.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadEmployees()
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Employees",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Employee")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar (iOS style)
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { },
                active = false,
                onActiveChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                placeholder = { Text("Search employees") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                }
            ) {}
            
            when (uiState) {
                is EmployeeManagementUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is EmployeeManagementUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                (uiState as EmployeeManagementUiState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.loadEmployees() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is EmployeeManagementUiState.Success -> {
                    val filteredEmployees = employees.filter { employee ->
                        searchQuery.isEmpty() || 
                        employee.displayName.contains(searchQuery, ignoreCase = true) ||
                        employee.employeeId.contains(searchQuery, ignoreCase = true) ||
                        employee.email?.contains(searchQuery, ignoreCase = true) == true
                    }
                    
                    if (filteredEmployees.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Spacing.md)
                            ) {
                                Icon(
                                    Icons.Default.People,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    if (searchQuery.isEmpty()) "No Employees" else "No Results",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    if (searchQuery.isEmpty()) "Add employees to get started" 
                                    else "Try a different search",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = Spacing.sm)
                        ) {
                            items(filteredEmployees, key = { it.uid }) { employee ->
                                EmployeeListRow(
                                    employee = employee,
                                    onClick = { selectedEmployee = employee }
                                )
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Show comprehensive employee detail screen
    selectedEmployee?.let { employee ->
        EmployeeDetailScreen(
            employee = employee,
            onDismiss = { selectedEmployee = null },
            onUpdate = { viewModel.loadEmployees() }
        )
    }
}

/**
 * Employee List Row matching iOS design exactly
 */
@Composable
fun EmployeeListRow(
    employee: Employee,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            // Use the iOS-style EmployeeAvatar component
            EmployeeAvatar(
                employee = employee,
                size = AvatarSize.large // 50dp - iOS standard
            )
        },
        headlineContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = employee.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // 8dp active status dot (iOS style)
                if (employee.isActive) {
                    ActiveStatusDot(isActive = true, size = 8.dp)
                }
            }
        },
        supportingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = employee.employeeId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Use the iOS-style RoleBadge component
                RoleBadge(role = employee.role)
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/**
 * Employee Detail Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailSheet(
    employee: Employee,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            // Header with large avatar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                EmployeeAvatar(
                    employee = employee,
                    size = AvatarSize.xlarge // 56dp for detail view
                )
                
                Text(
                    text = employee.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoleBadge(role = employee.role)
                    
                    if (employee.isActive) {
                        TextBadge(
                            text = "Active",
                            color = ATSColors.ActiveDot
                        )
                    } else {
                        TextBadge(
                            text = "Inactive",
                            color = ATSColors.InactiveDot
                        )
                    }
                }
            }
            
            Divider()
            
            // Employee Details
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                DetailRow(
                    icon = Icons.Default.Badge,
                    label = "Employee ID",
                    value = employee.employeeId
                )
                
                employee.email?.let { email ->
                    DetailRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = email
                    )
                }
                
                DetailRow(
                    icon = Icons.Default.Group,
                    label = "Team",
                    value = employee.team
                )
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            tint = MaterialTheme.colorScheme.primary
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
