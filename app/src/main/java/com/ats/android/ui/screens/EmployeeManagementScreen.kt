package com.ats.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ats.android.R
import com.ats.android.models.Employee
import com.ats.android.models.EmployeeRole
import com.ats.android.viewmodels.EmployeeManagementViewModel
import com.ats.android.viewmodels.EmployeeManagementUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeManagementScreen(
    viewModel: EmployeeManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredEmployees by viewModel.filteredEmployees.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Employee?>(null) }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stringResource(R.string.employee_management))
                        Text(
                            text = stringResource(R.string.employees_count, filteredEmployees.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadEmployees() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, "Add Employee")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                        Text(
                            text = (uiState as EmployeeManagementUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is EmployeeManagementUiState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchEmployees(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text(stringResource(R.string.search_employees)) },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchEmployees("") }) {
                                        Icon(Icons.Default.Clear, stringResource(R.string.clear))
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                textDirection = TextDirection.Content
                            )
                        )
                        
                        // Employee list
                        if (filteredEmployees.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isEmpty()) stringResource(R.string.no_employees_found) else stringResource(R.string.no_matches),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredEmployees) { employee ->
                                    EmployeeListItem(
                                        employee = employee,
                                        onEdit = { selectedEmployee = employee },
                                        onDelete = { showDeleteDialog = employee },
                                        onToggleStatus = { 
                                            scope.launch {
                                                viewModel.toggleEmployeeStatus(
                                                    employee.employeeId,
                                                    !employee.isActive
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Add/Edit dialog
    if (showAddDialog || selectedEmployee != null) {
        EmployeeFormDialog(
            employee = selectedEmployee,
            onDismiss = {
                showAddDialog = false
                selectedEmployee = null
            },
            onSave = { nameEn, nameAr, email, phone, role, avatarUri ->
                scope.launch {
                    val result = if (selectedEmployee == null) {
                        viewModel.addEmployee(nameEn, nameAr, email, phone, role, avatarUri)
                    } else {
                        viewModel.updateEmployee(
                            selectedEmployee!!.employeeId,
                            nameEn,
                            nameAr,
                            email,
                            phone,
                            role,
                            avatarUri,
                            selectedEmployee!!.avatarURL
                        )
                    }
                    
                    if (result.isSuccess) {
                        showAddDialog = false
                        selectedEmployee = null
                    }
                }
            }
        )
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { employee ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_employee_title)) },
            text = { Text(stringResource(R.string.delete_employee_confirmation_fmt, employee.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteEmployee(employee.employeeId)
                            showDeleteDialog = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListItem(
    employee: Employee,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        onClick = onEdit
    ) {
        ListItem(
            headlineContent = { 
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(employee.displayName)
                    if (!employee.isActive) {
                        Text(
                            text = stringResource(R.string.inactive),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            supportingContent = { 
                Column {
                    Text(employee.email ?: stringResource(R.string.no_email))
                    Text(
                        text = "${employee.role} • ${employee.employeeId}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            leadingContent = {
                if (employee.avatarURL != null) {
                    AsyncImage(
                        model = employee.avatarURL,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                }
            },
            trailingContent = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.more_options))
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(if (employee.isActive) R.string.deactivate else R.string.activate)) },
                            onClick = {
                                showMenu = false
                                onToggleStatus()
                            },
                            leadingIcon = { 
                                Icon(
                                    if (employee.isActive) Icons.Default.Close else Icons.Default.Check,
                                    null
                                ) 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeFormDialog(
    employee: Employee?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, EmployeeRole, Uri?) -> Unit
) {
    var nameEn by remember { mutableStateOf(employee?.englishName ?: "") }
    var nameAr by remember { mutableStateOf(employee?.arabicName ?: "") }
    var email by remember { mutableStateOf(employee?.email ?: "") }
    var phone by remember { mutableStateOf(employee?.phoneNumber ?: "") }
    var role by remember { mutableStateOf(employee?.role ?: EmployeeRole.EMPLOYEE) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var showRoleMenu by remember { mutableStateOf(false) }
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        avatarUri = uri
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (employee == null) R.string.add_employee_title else R.string.edit_employee_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (avatarUri != null || employee?.avatarURL != null) {
                            AsyncImage(
                                model = avatarUri ?: employee?.avatarURL,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                        
                        TextButton(onClick = { imagePicker.launch("image/*") }) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.change_photo))
                        }
                    }
                }
                
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(stringResource(R.string.english_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(stringResource(R.string.arabic_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.phone_number_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Role selector
                ExposedDropdownMenuBox(
                    expanded = showRoleMenu,
                    onExpandedChange = { showRoleMenu = it }
                ) {
                    OutlinedTextField(
                        value = role.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.role_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRoleMenu) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showRoleMenu,
                        onDismissRequest = { showRoleMenu = false }
                    ) {
                        EmployeeRole.entries.forEach { roleOption ->
                            DropdownMenuItem(
                                text = { Text(roleOption.toString()) },
                                onClick = {
                                    role = roleOption
                                    showRoleMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nameEn.isNotBlank() && email.isNotBlank()) {
                        onSave(nameEn, nameAr, email, phone, role, avatarUri)
                    }
                },
                enabled = nameEn.isNotBlank() && email.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
