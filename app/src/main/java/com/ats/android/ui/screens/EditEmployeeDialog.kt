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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import com.ats.android.R

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
                    stringResource(R.string.edit_employee),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(R.string.close))
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
                    label = { Text(stringResource(R.string.english_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Arabic Name
                OutlinedTextField(
                    value = arabicName,
                    onValueChange = { arabicName = it },
                    label = { Text(stringResource(R.string.arabic_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Employee ID
                OutlinedTextField(
                    value = employeeId,
                    onValueChange = { },
                    label = { Text(stringResource(R.string.employee_id_label)) },
                    enabled = false, // ID cannot be changed
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Phone Number
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text(stringResource(R.string.phone_number)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Department (English)
                OutlinedTextField(
                    value = departmentEn,
                    onValueChange = { departmentEn = it },
                    label = { Text(stringResource(R.string.department_en)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Department (Arabic)
                OutlinedTextField(
                    value = departmentAr,
                    onValueChange = { departmentAr = it },
                    label = { Text(stringResource(R.string.department_ar)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Role Selection
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = when (selectedRole) {
                            EmployeeRole.ADMIN -> stringResource(R.string.admin)
                            EmployeeRole.SUPERVISOR -> stringResource(R.string.supervisor)
                            EmployeeRole.EMPLOYEE -> stringResource(R.string.employee)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.role)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.employee)) },
                            onClick = {
                                selectedRole = EmployeeRole.EMPLOYEE
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.supervisor)) },
                            onClick = {
                                selectedRole = EmployeeRole.SUPERVISOR
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.admin)) },
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.active),
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
                Text(stringResource(R.string.save_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
