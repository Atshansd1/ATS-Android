package com.ats.android.services

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Service for creating and managing security alerts
 * Alerts are shown in the admin dashboard
 */
class SecurityAlertService(private val context: Context) {
    
    companion object {
        private const val TAG = "SecurityAlertService"
        private const val COLLECTION_SECURITY_ALERTS = "security_alerts"
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auditLogService = AuditLogService(context)
    
    // Alert types
    object AlertType {
        const val MOCK_LOCATION = "MOCK_LOCATION"
        const val PERMISSION_CHANGE = "PERMISSION_CHANGE"
        const val LEFT_AREA = "LEFT_AREA"
        const val NO_UPDATE = "NO_UPDATE"
        const val DEVICE_CHANGE = "DEVICE_CHANGE"
        const val FORCE_CLOSE = "FORCE_CLOSE"
    }
    
    // Alert severity
    object Severity {
        const val HIGH = "HIGH"
        const val MEDIUM = "MEDIUM"
        const val LOW = "LOW"
    }
    
    data class SecurityAlert(
        val id: String = "",
        val employeeId: String = "",
        val employeeName: String = "",
        val alertType: String = "",
        val severity: String = "",
        val message: String = "",
        val deviceInfo: Map<String, Any> = emptyMap(),
        val location: GeoPoint? = null,
        val timestamp: Timestamp = Timestamp.now(),
        val isRead: Boolean = false,
        val resolvedBy: String? = null,
        val resolvedAt: Timestamp? = null
    )
    
    /**
     * Create a new security alert
     */
    suspend fun createAlert(
        employeeId: String,
        employeeName: String,
        alertType: String,
        message: String,
        location: GeoPoint? = null
    ) {
        try {
            val severity = when (alertType) {
                AlertType.MOCK_LOCATION, AlertType.DEVICE_CHANGE -> Severity.HIGH
                AlertType.PERMISSION_CHANGE, AlertType.LEFT_AREA, 
                AlertType.NO_UPDATE, AlertType.FORCE_CLOSE -> Severity.MEDIUM
                else -> Severity.LOW
            }
            
            val deviceInfo = auditLogService.getDeviceInfo()
            
            val alertData = hashMapOf(
                "employeeId" to employeeId,
                "employeeName" to employeeName,
                "alertType" to alertType,
                "severity" to severity,
                "message" to message,
                "deviceInfo" to mapOf(
                    "deviceId" to deviceInfo.deviceId,
                    "model" to deviceInfo.model,
                    "manufacturer" to deviceInfo.manufacturer,
                    "osVersion" to deviceInfo.osVersion,
                    "appVersion" to deviceInfo.appVersion
                ),
                "timestamp" to Timestamp(Date()),
                "isRead" to false,
                "resolvedBy" to null,
                "resolvedAt" to null
            )
            
            location?.let { alertData["location"] = it }
            
            firestore.collection(COLLECTION_SECURITY_ALERTS)
                .add(alertData)
                .await()
            
            Log.w(TAG, "🚨 Security Alert Created: $alertType for $employeeName - $message")
            
            // Also log to audit trail
            auditLogService.logAction(
                employeeId = employeeId,
                action = "SECURITY_ALERT_$alertType",
                location = location,
                metadata = mapOf("message" to message),
                isSuspicious = true,
                flagReasons = listOf(alertType)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create alert: ${e.message}", e)
        }
    }
    
    /**
     * Create alert for mock location detection
     */
    suspend fun alertMockLocation(employeeId: String, employeeName: String, location: GeoPoint?) {
        createAlert(
            employeeId = employeeId,
            employeeName = employeeName,
            alertType = AlertType.MOCK_LOCATION,
            message = "Fake GPS/Mock location detected. Employee attempted to spoof their location.",
            location = location
        )
    }
    
    /**
     * Create alert for permission change
     */
    suspend fun alertPermissionChange(employeeId: String, employeeName: String) {
        createAlert(
            employeeId = employeeId,
            employeeName = employeeName,
            alertType = AlertType.PERMISSION_CHANGE,
            message = "Location permission was changed from 'Always' during active shift."
        )
    }
    
    /**
     * Create alert for leaving work area
     */
    suspend fun alertLeftArea(employeeId: String, employeeName: String, location: GeoPoint?, centerName: String) {
        createAlert(
            employeeId = employeeId,
            employeeName = employeeName,
            alertType = AlertType.LEFT_AREA,
            message = "Employee left the designated work area: $centerName",
            location = location
        )
    }
    
    /**
     * Create alert for no location update
     */
    suspend fun alertNoUpdate(employeeId: String, employeeName: String, minutesSinceLastUpdate: Int) {
        createAlert(
            employeeId = employeeId,
            employeeName = employeeName,
            alertType = AlertType.NO_UPDATE,
            message = "No location update received for $minutesSinceLastUpdate minutes during active shift."
        )
    }
    
    /**
     * Create alert for device change
     */
    suspend fun alertDeviceChange(
        employeeId: String, 
        employeeName: String, 
        oldDeviceId: String, 
        newDeviceId: String
    ) {
        createAlert(
            employeeId = employeeId,
            employeeName = employeeName,
            alertType = AlertType.DEVICE_CHANGE,
            message = "Login attempt from different device. Previous: $oldDeviceId, New: $newDeviceId"
        )
    }
    
    /**
     * Create alert for force close / app killed
     */
    suspend fun alertForceClose(employeeId: String, employeeName: String) {
        createAlert(
            employeeId = employeeId,
            employeeName = employeeName,
            alertType = AlertType.FORCE_CLOSE,
            message = "App was force-closed during active shift. Employee auto-checked out."
        )
    }
    
    /**
     * Get all unread alerts (for admin)
     */
    fun getUnreadAlerts(): Flow<List<SecurityAlert>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_SECURITY_ALERTS)
            .whereEqualTo("isRead", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting alerts: ${error.message}")
                    return@addSnapshotListener
                }
                
                val alerts = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        SecurityAlert(
                            id = doc.id,
                            employeeId = doc.getString("employeeId") ?: "",
                            employeeName = doc.getString("employeeName") ?: "",
                            alertType = doc.getString("alertType") ?: "",
                            severity = doc.getString("severity") ?: "",
                            message = doc.getString("message") ?: "",
                            deviceInfo = doc.get("deviceInfo") as? Map<String, Any> ?: emptyMap(),
                            location = doc.getGeoPoint("location"),
                            timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                            isRead = doc.getBoolean("isRead") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(alerts)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Mark alert as read
     */
    suspend fun markAsRead(alertId: String) {
        try {
            firestore.collection(COLLECTION_SECURITY_ALERTS)
                .document(alertId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark alert as read: ${e.message}")
        }
    }
    
    /**
     * Resolve an alert
     */
    suspend fun resolveAlert(alertId: String, resolvedByUserId: String) {
        try {
            firestore.collection(COLLECTION_SECURITY_ALERTS)
                .document(alertId)
                .update(
                    mapOf(
                        "isRead" to true,
                        "resolvedBy" to resolvedByUserId,
                        "resolvedAt" to Timestamp(Date())
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve alert: ${e.message}")
        }
    }
}
