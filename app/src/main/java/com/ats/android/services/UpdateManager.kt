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
import java.io.File

class UpdateManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateManager"
        private const val GITHUB_LATEST_RELEASE_URL = "https://github.com/Atshansd1/ATS-Android/releases/latest"
        private const val DOWNLOAD_URL = "https://github.com/Atshansd1/ATS-Android/releases/download/v1.3.0/ATS-Android-v1.3.0-signed.apk"
        private const val APK_FILENAME = "ATS-Update.apk"
    }
    
    private val _downloadProgress = MutableStateFlow<DownloadProgress>(DownloadProgress.Idle)
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()
    
    private var downloadId: Long = -1
    private var downloadManager: DownloadManager? = null
    
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                Log.d(TAG, "Download completed: $id")
                _downloadProgress.value = DownloadProgress.Completed
                installUpdate()
            }
        }
    }
    
    fun checkForUpdates() {
        Log.d(TAG, "Opening GitHub releases page")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_LATEST_RELEASE_URL))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    fun downloadAndInstallUpdate() {
        try {
            Log.d(TAG, "Starting update download")
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
            val request = DownloadManager.Request(Uri.parse(DOWNLOAD_URL)).apply {
                setTitle("ATS Update")
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
            
            if (!file.exists()) {
                Log.e(TAG, "APK file not found")
                _downloadProgress.value = DownloadProgress.Error("File not found")
                return
            }
            
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(intent)
            Log.d(TAG, "Install intent started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error installing update: ${e.message}", e)
            _downloadProgress.value = DownloadProgress.Error(e.message ?: "Installation failed")
        }
    }
    
    fun getDownloadProgress(): Int {
        if (downloadId == -1L) return 0
        
        try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager?.query(query)
            
            if (cursor?.moveToFirst() == true) {
                val bytesDownloaded = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                )
                val bytesTotal = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                )
                
                cursor.close()
                
                if (bytesTotal > 0) {
                    val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()
                    _downloadProgress.value = DownloadProgress.Downloading(progress)
                    return progress
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting download progress: ${e.message}")
        }
        
        return 0
    }
    
    fun cleanup() {
        try {
            context.unregisterReceiver(downloadReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }
    
    sealed class DownloadProgress {
        object Idle : DownloadProgress()
        data class Downloading(val progress: Int) : DownloadProgress()
        object Completed : DownloadProgress()
        data class Error(val message: String) : DownloadProgress()
    }
}
