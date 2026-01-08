package com.ats.android.services

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.json.JSONObject
import java.io.File
import java.net.URL

class UpdateManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateManager"
        private const val GITHUB_API_URL = "https://api.github.com/repos/Atshansd1/ATS-Android/releases/latest"
        private const val APK_FILENAME = "Hodoor-Update.apk"
    }
    
    data class VersionInfo(
        val latestVersion: String,
        val downloadUrl: String,
        val isUpdateAvailable: Boolean
    )
    
    private val _downloadProgress = MutableStateFlow<DownloadProgress>(DownloadProgress.Idle)
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()
    
    private val _versionInfo = MutableStateFlow<VersionInfo?>(null)
    val versionInfo: StateFlow<VersionInfo?> = _versionInfo.asStateFlow()
    
    private var downloadId: Long = -1
    private var downloadManager: DownloadManager? = null
    private var latestDownloadUrl: String = ""
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                Log.d(TAG, "Download completed: $id")
                progressJob?.cancel()
                _downloadProgress.value = DownloadProgress.Completed
                installUpdate()
            }
        }
    }
    
    fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current version: ${e.message}")
            "1.0.0"
        }
    }
    
    suspend fun checkForUpdates(): VersionInfo? {
        return withContext(Dispatchers.IO) {
            try {
                // Get current version from BuildConfig
                val currentVersion = try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    packageInfo.versionName
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting current version: ${e.message}")
                    "1.0.0"
                }
                
                Log.d(TAG, "📱 Current app version: $currentVersion")
                Log.d(TAG, "🔍 Checking for updates from GitHub API")
                
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection()
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val response = connection.getInputStream().bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val tagName = json.getString("tag_name").removePrefix("v")
                val assets = json.getJSONArray("assets")
                
                // Look for app-release.apk (the actual filename in releases)
                var apkUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    Log.d(TAG, "  Asset $i: $name")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.getString("browser_download_url")
                        Log.d(TAG, "✅ Found APK: $name")
                        break
                    }
                }
                
                if (apkUrl.isEmpty()) {
                    Log.e(TAG, "❌ No APK file found in release assets")
                }
                
                val isNewer = compareVersions(tagName, currentVersion) > 0
                val versionInfo = VersionInfo(
                    latestVersion = tagName,
                    downloadUrl = apkUrl,
                    isUpdateAvailable = isNewer
                )
                
                _versionInfo.value = versionInfo
                Log.d(TAG, "📊 Latest: $tagName, Current: $currentVersion, Update available: $isNewer")
                versionInfo
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking for updates: ${e.message}", e)
                null
            }
        }
    }
    
    private fun compareVersions(v1: String, v2: String): Int {
        // Handle tags like "2.1.4-build58" by extracting build number
        val normalized1 = normalizeVersion(v1)
        val normalized2 = normalizeVersion(v2)
        
        val parts1 = normalized1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = normalized2.split(".").map { it.toIntOrNull() ?: 0 }
        
        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val part1 = parts1.getOrNull(i) ?: 0
            val part2 = parts2.getOrNull(i) ?: 0
            if (part1 != part2) {
                return part1.compareTo(part2)
            }
        }
        return 0
    }
    
    /**
     * Normalize version string:
     * "2.1.4-build58" -> "2.1.4.58"
     * "2.1.4.58" -> "2.1.4.58"
     * "2.1.4" -> "2.1.4.0"
     */
    private fun normalizeVersion(version: String): String {
        // Handle "-buildXX" suffix
        val buildRegex = Regex("(\\d+\\.\\d+\\.\\d+)-build(\\d+)")
        val buildMatch = buildRegex.find(version)
        if (buildMatch != null) {
            val baseVersion = buildMatch.groupValues[1]
            val buildNumber = buildMatch.groupValues[2]
            return "$baseVersion.$buildNumber"
        }
        
        // If already in format "X.X.X.X", return as is
        val parts = version.split(".")
        return if (parts.size >= 4) version else "$version.0"
    }
    
    fun downloadAndInstallUpdate(downloadUrl: String? = null) {
        try {
            val urlToUse = downloadUrl ?: latestDownloadUrl
            if (urlToUse.isEmpty()) {
                _downloadProgress.value = DownloadProgress.Error("No download URL available")
                return
            }
            
            latestDownloadUrl = urlToUse
            
            Log.d(TAG, "Starting update download from: $urlToUse")
            _downloadProgress.value = DownloadProgress.Downloading(0)
            
            // Register download complete receiver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    downloadReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    downloadReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
            
            // Delete old APK if exists
            val file = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                APK_FILENAME
            )
            if (file.exists()) {
                file.delete()
            }
            
            // Create download request
            val request = DownloadManager.Request(Uri.parse(urlToUse)).apply {
                setTitle("Hodoor+ Update")
                setDescription("Downloading latest version")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    APK_FILENAME
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            
            // Start download
            downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager?.enqueue(request) ?: -1
            
            Log.d(TAG, "Download started with ID: $downloadId")
            
            // Start monitoring progress
            startProgressMonitoring()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting download: ${e.message}", e)
            _downloadProgress.value = DownloadProgress.Error(e.message ?: "Download failed")
        }
    }
    
    private fun installUpdate() {
        try {
            Log.d(TAG, "Installing update")
            
            val file = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                APK_FILENAME
            )
            
            Log.d(TAG, "APK file path: ${file.absolutePath}")
            Log.d(TAG, "APK file exists: ${file.exists()}")
            Log.d(TAG, "APK file size: ${file.length()} bytes")
            
            if (!file.exists()) {
                Log.e(TAG, "APK file not found")
                _downloadProgress.value = DownloadProgress.Error("File not found")
                return
            }
            
            if (file.length() == 0L) {
                Log.e(TAG, "APK file is empty")
                _downloadProgress.value = DownloadProgress.Error("Download incomplete")
                return
            }
            
            // Check if we have install permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Log.w(TAG, "Install permission not granted, opening settings")
                    _downloadProgress.value = DownloadProgress.Error("Please enable 'Install unknown apps' permission")
                    
                    // Open settings to enable install permission
                    val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Log.d(TAG, "Install URI: $uri")
            
            // Use ACTION_INSTALL_PACKAGE for better compatibility
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = uri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            
            _downloadProgress.value = DownloadProgress.Installing
            context.startActivity(intent)
            Log.d(TAG, "Install intent started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error installing update: ${e.message}", e)
            _downloadProgress.value = DownloadProgress.Error(e.message ?: "Installation failed")
        }
    }
    
    // Public method to retry installation after granting permission
    fun retryInstall() {
        installUpdate()
    }
    
    private fun startProgressMonitoring() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val progress = getDownloadProgress()
                if (progress >= 100) {
                    break
                }
                delay(500) // Update every 500ms
            }
        }
    }
    
    private fun getDownloadProgress(): Int {
        if (downloadId == -1L) return 0
        
        try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager?.query(query)
            
            if (cursor?.moveToFirst() == true) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(statusIndex)
                
                when (status) {
                    DownloadManager.STATUS_FAILED -> {
                        val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                        cursor.close()
                        progressJob?.cancel()
                        _downloadProgress.value = DownloadProgress.Error("Download failed (code: $reason)")
                        return 0
                    }
                    DownloadManager.STATUS_PAUSED -> {
                        cursor.close()
                        return -1
                    }
                    DownloadManager.STATUS_PENDING -> {
                        cursor.close()
                        _downloadProgress.value = DownloadProgress.Downloading(0)
                        return 0
                    }
                    DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_SUCCESSFUL -> {
                        val bytesDownloaded = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        )
                        val bytesTotal = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        )
                        
                        cursor.close()
                        
                        if (bytesTotal > 0) {
                            val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()
                            Log.d(TAG, "Download progress: $progress% ($bytesDownloaded / $bytesTotal bytes)")
                            _downloadProgress.value = DownloadProgress.Downloading(progress)
                            return progress
                        }
                    }
                }
                cursor.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting download progress: ${e.message}", e)
        }
        
        return 0
    }
    
    fun cleanup() {
        try {
            context.unregisterReceiver(downloadReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
        progressJob?.cancel()
        scope.cancel()
    }
    
    sealed class DownloadProgress {
        object Idle : DownloadProgress()
        data class Downloading(val progress: Int) : DownloadProgress()
        object Completed : DownloadProgress()
        object Installing : DownloadProgress()
        data class Error(val message: String) : DownloadProgress()
    }
}
