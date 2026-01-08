package com.ats.android.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Service for logging all employee actions with full audit trail
 * Logs device info, network info, location, and timestamps
 */
class AuditLogService(private val context: Context) {
    
    companion object {
        private const val TAG = "AuditLogService"
        private const val COLLECTION_AUDIT_LOGS = "audit_logs"
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    
    // Action types
    object Actions {
        const val CHECK_IN = "CHECK_IN"
        const val CHECK_OUT = "CHECK_OUT"
        const val LOCATION_UPDATE = "LOCATION_UPDATE"
        const val PERMISSION_CHANGE = "PERMISSION_CHANGE"
        const val APP_OPEN = "APP_OPEN"
        const val APP_BACKGROUND = "APP_BACKGROUND"
        const val MOCK_LOCATION_DETECTED = "MOCK_LOCATION_DETECTED"
        const val DEVICE_REGISTERED = "DEVICE_REGISTERED"
        const val DEVICE_CHANGE_ATTEMPT = "DEVICE_CHANGE_ATTEMPT"
        const val LEFT_WORK_AREA = "LEFT_WORK_AREA"
        const val RETURNED_WORK_AREA = "RETURNED_WORK_AREA"
        const val AUTO_CHECKOUT = "AUTO_CHECKOUT"
    }
    
    data class DeviceInfo(
        val deviceId: String,
        val model: String,
        val manufacturer: String,
        val osVersion: String,
        val appVersion: String
    )
    
    data class NetworkInfo(
        val type: String,
        val wifiSsid: String?,
        val isConnected: Boolean
    )
    
    /**
     * Log an action with full audit trail
     */
    suspend fun logAction(
        employeeId: String,
        action: String,
        location: GeoPoint? = null,
        metadata: Map<String, Any>? = null,
        isSuspicious: Boolean = false,
        flagReasons: List<String> = emptyList()
    ) {
        try {
            val deviceInfo = getDeviceInfo()
            val networkInfo = getNetworkInfo()
            
            val logData = hashMapOf(
                "employeeId" to employeeId,
                "action" to action,
                "deviceInfo" to mapOf(
                    "deviceId" to deviceInfo.deviceId,
                    "model" to deviceInfo.model,
                    "manufacturer" to deviceInfo.manufacturer,
                    "osVersion" to deviceInfo.osVersion,
                    "appVersion" to deviceInfo.appVersion
                ),
                "networkInfo" to mapOf(
                    "type" to networkInfo.type,
                    "wifiSsid" to (networkInfo.wifiSsid ?: ""),
                    "isConnected" to networkInfo.isConnected
                ),
                "timestamp" to Timestamp(Date()),
                "isSuspicious" to isSuspicious,
                "flagReasons" to flagReasons
            )
            
            // Add optional fields
            location?.let { logData["location"] = it }
            metadata?.let { logData["metadata"] = it }
            
            firestore.collection(COLLECTION_AUDIT_LOGS)
                .add(logData)
                .await()
            
            Log.d(TAG, "📝 Logged action: $action for $employeeId (suspicious: $isSuspicious)")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to log action: ${e.message}", e)
        }
    }
    
    /**
     * Get current device information
     */
    fun getDeviceInfo(): DeviceInfo {
        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        
        val appVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
        
        return DeviceInfo(
            deviceId = deviceId,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = appVersion
        )
    }
    
    /**
     * Get current network information
     */
    @Suppress("DEPRECATION")
    fun getNetworkInfo(): NetworkInfo {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        var networkType = "UNKNOWN"
        var isConnected = false
        var wifiSsid: String? = null
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (capabilities != null) {
                isConnected = true
                networkType = when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                    else -> "OTHER"
                }
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            isConnected = networkInfo?.isConnected == true
            networkType = networkInfo?.typeName ?: "UNKNOWN"
        }
        
        // Get WiFi SSID if connected to WiFi
        if (networkType == "WIFI") {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                wifiSsid = wifiInfo.ssid?.replace("\"", "") ?: "unknown"
            } catch (e: Exception) {
                wifiSsid = "permission_denied"
            }
        }
        
        return NetworkInfo(
            type = networkType,
            wifiSsid = wifiSsid,
            isConnected = isConnected
        )
    }
    
    /**
     * Get the unique device ID
     */
    fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }
}
