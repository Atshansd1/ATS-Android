package com.ats.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.ArrowBack
import com.ats.android.R
import com.ats.android.viewmodels.AuthViewModel // Assuming AuthViewModel handles password change logic or I'll add it there.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val passwordChangedSuccess = stringResource(R.string.password_changed_success)
    val passwordMinLengthError = stringResource(R.string.password_min_length_error)
    val passwordMismatchError = stringResource(R.string.password_mismatch_error)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.change_password)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text(stringResource(R.string.current_password_label)) },
                visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                        Icon(
                            imageVector = if (showCurrentPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showCurrentPassword) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text(stringResource(R.string.new_password_label)) },
                visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                        Icon(
                            imageVector = if (showNewPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showNewPassword) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(stringResource(R.string.confirm_password_label)) },
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            imageVector = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showConfirmPassword) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && newPassword != confirmPassword,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                        Text(stringResource(R.string.passwords_mismatch))
                    }
                }
            )
            
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (successMessage != null) {
                Text(
                    text = successMessage!!,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    successMessage = null
                    
                    if (newPassword.length < 6) {
                        errorMessage = passwordMinLengthError
                        isLoading = false
                        return@Button
                    }
                    
                    if (newPassword != confirmPassword) {
                        errorMessage = passwordMismatchError
                        isLoading = false
                        return@Button
                    }
                    
                    // Call ViewModel
                    authViewModel.changePassword(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        onSuccess = {
                            isLoading = false
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                            successMessage = passwordChangedSuccess
                        },
                        onError = { error ->
                            isLoading = false
                            errorMessage = error
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && currentPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword == confirmPassword
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.update_password_action))
                }
            }
        }
    }
}
