package com.ats.android.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

object PermissionManager {
    
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    fun hasLocationPermission(context: Context): Boolean {
        return LOCATION_PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

@Composable
fun RequestLocationPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var permissionDeniedPermanently by remember { mutableStateOf(false) }
    
    // Check if already granted
    LaunchedEffect(Unit) {
        if (PermissionManager.hasLocationPermission(context)) {
            onPermissionGranted()
        }
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            onPermissionGranted()
        } else {
            // Check if user selected "Don't ask again"
            val activity = context as? Activity
            val shouldShowRationale = activity?.let {
                PermissionManager.LOCATION_PERMISSIONS.any { permission ->
                    activity.shouldShowRequestPermissionRationale(permission)
                }
            } ?: false
            
            if (shouldShowRationale) {
                showRationale = true
            } else {
                permissionDeniedPermanently = true
            }
            onPermissionDenied()
        }
    }
    
    // Auto-request on first launch
    LaunchedEffect(Unit) {
        if (!PermissionManager.hasLocationPermission(context)) {
            launcher.launch(PermissionManager.LOCATION_PERMISSIONS)
        }
    }
    
    // Show rationale dialog
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Location Permission Required") },
            text = { 
                Text("ATS needs location access to track your attendance. Please grant location permission to continue.") 
            },
            confirmButton = {
                Button(onClick = {
                    showRationale = false
                    launcher.launch(PermissionManager.LOCATION_PERMISSIONS)
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRationale = false
                    onPermissionDenied()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Show settings dialog if permanently denied
    if (permissionDeniedPermanently) {
        AlertDialog(
            onDismissRequest = { permissionDeniedPermanently = false },
            title = { Text("Permission Required") },
            text = { 
                Text("Location permission was denied. Please enable it in app settings to use check-in features.") 
            },
            confirmButton = {
                Button(onClick = {
                    permissionDeniedPermanently = false
                    PermissionManager.openAppSettings(context)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    permissionDeniedPermanently = false
                    onPermissionDenied()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
