package com.ats.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ats.android.models.Employee
import com.ats.android.models.EmployeeRole
import com.ats.android.ui.theme.Spacing

/**
 * Edit Employee Dialog matching iOS EditEmployeeView
 * Allows editing all employee fields and syncs to Firebase
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEmployeeDialog(
    employee: Employee,
    onDismiss: () -> Unit,
    onSave: (Employee) -> Unit
) {
    var englishName by remember { mutableStateOf(employee.englishName ?: employee.displayName) }
    var arabicName by remember { mutableStateOf(employee.arabicName ?: "") }
    var employeeId by remember { mutableStateOf(employee.employeeId) }
    var email by remember { mutableStateOf(employee.email ?: "") }
    var phoneNumber by remember { mutableStateOf(employee.phoneNumber ?: "") }
    var departmentEn by remember { mutableStateOf(employee.departmentEn ?: employee.team) }
    var departmentAr by remember { mutableStateOf(employee.departmentAr ?: "") }
    var selectedRole by remember { mutableStateOf(employee.role) }
    var isActive by remember { mutableStateOf(employee.isActive) }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Edit Employee",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // English Name
                OutlinedTextField(
                    value = englishName,
                    onValueChange = { englishName = it },
                    label = { Text("English Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Arabic Name
                OutlinedTextField(
                    value = arabicName,
                    onValueChange = { arabicName = it },
                    label = { Text("Arabic Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Employee ID
                OutlinedTextField(
                    value = employeeId,
                    onValueChange = { employeeId = it },
                    label = { Text("Employee ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false // Don't allow changing ID
                )
                
                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Phone Number
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Department (English)
                OutlinedTextField(
                    value = departmentEn,
                    onValueChange = { departmentEn = it },
                    label = { Text("Department (English)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Department (Arabic)
                OutlinedTextField(
                    value = departmentAr,
                    onValueChange = { departmentAr = it },
                    label = { Text("Department (Arabic)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Role Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = when (selectedRole) {
                            EmployeeRole.ADMIN -> "Admin"
                            EmployeeRole.SUPERVISOR -> "Supervisor"
                            EmployeeRole.EMPLOYEE -> "Employee"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Employee") },
                            onClick = {
                                selectedRole = EmployeeRole.EMPLOYEE
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Supervisor") },
                            onClick = {
                                selectedRole = EmployeeRole.SUPERVISOR
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Admin") },
                            onClick = {
                                selectedRole = EmployeeRole.ADMIN
                                expanded = false
                            }
                        )
                    }
                }
                
                // Active Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Active Status",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedEmployee = employee.copy(
                        englishName = englishName,
                        arabicName = arabicName,
                        email = email.ifBlank { null },
                        phoneNumber = phoneNumber.ifBlank { null },
                        departmentEn = departmentEn,
                        departmentAr = departmentAr,
                        roleString = selectedRole.value,
                        isActive = isActive
                    )
                    onSave(updatedEmployee)
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
