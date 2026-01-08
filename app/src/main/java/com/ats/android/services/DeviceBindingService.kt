package com.ats.android.services

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Service for managing device binding - one device per employee policy
 * Prevents employees from sharing logins or using multiple devices
 */
class DeviceBindingService(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceBindingService"
        private const val COLLECTION_DEVICE_BINDINGS = "device_bindings"
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auditLogService = AuditLogService(context)
    private val securityAlertService = SecurityAlertService(context)
    
    data class DeviceBinding(
        val primaryDeviceId: String = "",
        val deviceModel: String = "",
        val osVersion: String = "",
        val appVersion: String = "",
        val registeredAt: Timestamp = Timestamp.now(),
        val lastSeenAt: Timestamp = Timestamp.now(),
        val isLocked: Boolean = false
    )
    
    sealed class DeviceCheckResult {
        object Allowed : DeviceCheckResult()
        object NewDevice : DeviceCheckResult()
        data class DifferentDevice(val registeredDeviceId: String) : DeviceCheckResult()
        data class Blocked(val reason: String) : DeviceCheckResult()
    }
    
    /**
     * Get the current device ID
     */
    fun getCurrentDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }
    
    /**
     * Check if current device is allowed for this employee
     */
    suspend fun checkDevice(employeeId: String, employeeName: String): DeviceCheckResult {
        try {
            val currentDeviceId = getCurrentDeviceId()
            val deviceInfo = auditLogService.getDeviceInfo()
            
            val doc = firestore.collection(COLLECTION_DEVICE_BINDINGS)
                .document(employeeId)
                .get()
                .await()
            
            if (!doc.exists()) {
                // No device registered - first time login
                Log.d(TAG, "📱 No device registered for $employeeId - registering new device")
                return DeviceCheckResult.NewDevice
            }
            
            val binding = doc.toObject(DeviceBinding::class.java) ?: return DeviceCheckResult.NewDevice
            
            if (binding.primaryDeviceId == currentDeviceId) {
                // Same device - update last seen
                updateLastSeen(employeeId)
                Log.d(TAG, "✅ Device verified for $employeeId")
                return DeviceCheckResult.Allowed
            }
            
            // Different device detected
            Log.w(TAG, "⚠️ Different device detected for $employeeId")
            
            // Create security alert
            securityAlertService.alertDeviceChange(
                employeeId = employeeId,
                employeeName = employeeName,
                oldDeviceId = binding.primaryDeviceId,
                newDeviceId = currentDeviceId
            )
            
            // Log the attempt
            auditLogService.logAction(
                employeeId = employeeId,
                action = AuditLogService.Actions.DEVICE_CHANGE_ATTEMPT,
                metadata = mapOf(
                    "registeredDeviceId" to binding.primaryDeviceId,
                    "attemptedDeviceId" to currentDeviceId,
                    "isLocked" to binding.isLocked
                ),
                isSuspicious = true,
                flagReasons = listOf("DEVICE_MISMATCH")
            )
            
            if (binding.isLocked) {
                // Admin has locked this account to specific device
                return DeviceCheckResult.Blocked(
                    "This account is locked to a different device. Please contact your administrator."
                )
            }
            
            return DeviceCheckResult.DifferentDevice(binding.primaryDeviceId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device: ${e.message}", e)
            // On error, allow but log
            return DeviceCheckResult.Allowed
        }
    }
    
    /**
     * Register current device for an employee
     */
    suspend fun registerDevice(employeeId: String): Boolean {
        try {
            val deviceInfo = auditLogService.getDeviceInfo()
            
            val binding = hashMapOf(
                "primaryDeviceId" to deviceInfo.deviceId,
                "deviceModel" to "${deviceInfo.manufacturer} ${deviceInfo.model}",
                "osVersion" to deviceInfo.osVersion,
                "appVersion" to deviceInfo.appVersion,
                "registeredAt" to Timestamp(Date()),
                "lastSeenAt" to Timestamp(Date()),
                "isLocked" to false
            )
            
            firestore.collection(COLLECTION_DEVICE_BINDINGS)
                .document(employeeId)
                .set(binding)
                .await()
            
            // Log the registration
            auditLogService.logAction(
                employeeId = employeeId,
                action = AuditLogService.Actions.DEVICE_REGISTERED,
                metadata = mapOf(
                    "deviceId" to deviceInfo.deviceId,
                    "deviceModel" to "${deviceInfo.manufacturer} ${deviceInfo.model}"
                )
            )
            
            Log.d(TAG, "✅ Device registered for $employeeId: ${deviceInfo.deviceId}")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Update last seen timestamp
     */
    private suspend fun updateLastSeen(employeeId: String) {
        try {
            val deviceInfo = auditLogService.getDeviceInfo()
            
            firestore.collection(COLLECTION_DEVICE_BINDINGS)
                .document(employeeId)
                .update(
                    mapOf(
                        "lastSeenAt" to Timestamp(Date()),
                        "appVersion" to deviceInfo.appVersion
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update last seen: ${e.message}")
        }
    }
    
    /**
     * Update device binding to new device (for admin use)
     */
    suspend fun updateDeviceBinding(employeeId: String, newDeviceId: String): Boolean {
        try {
            firestore.collection(COLLECTION_DEVICE_BINDINGS)
                .document(employeeId)
                .update(
                    mapOf(
                        "primaryDeviceId" to newDeviceId,
                        "lastSeenAt" to Timestamp(Date())
                    )
                )
                .await()
            
            Log.d(TAG, "✅ Device binding updated for $employeeId")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update device binding: ${e.message}")
            return false
        }
    }
    
    /**
     * Lock account to current device (admin function)
     */
    suspend fun lockToDevice(employeeId: String, lock: Boolean): Boolean {
        try {
            firestore.collection(COLLECTION_DEVICE_BINDINGS)
                .document(employeeId)
                .update("isLocked", lock)
                .await()
            
            Log.d(TAG, "🔒 Device lock ${if (lock) "enabled" else "disabled"} for $employeeId")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update lock status: ${e.message}")
            return false
        }
    }
}
