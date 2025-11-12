package com.ats.android.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ats.android.models.AttendanceRecord
import com.ats.android.models.Employee
import com.ats.android.services.FirestoreService
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

/**
 * ViewModel for Employee Detail Screen
 * Handles avatar upload, employee updates, and attendance history
 * Syncs all changes to Firebase (automatically reflects on iOS)
 */
class EmployeeDetailViewModel(
    private val employee: Employee
) : ViewModel() {
    
    private val firestoreService = FirestoreService.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar.asStateFlow()
    
    private val _currentAvatarUrl = MutableStateFlow(employee.avatarURL)
    val currentAvatarUrl: StateFlow<String?> = _currentAvatarUrl.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Load attendance history for last 30 days
     */
    fun loadAttendanceHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                Log.d(TAG, "📅 Loading attendance history for: ${employee.employeeId}")
                
                val endDate = Date()
                val calendar = Calendar.getInstance()
                calendar.time = endDate
                calendar.add(Calendar.DAY_OF_MONTH, -30)
                val startDate = calendar.time
                
                val records = firestoreService.getAttendanceHistory(
                    employeeId = employee.employeeId,
                    startDate = startDate,
                    endDate = endDate
                )
                
                // Sort by check-in time (most recent first)
                _attendanceRecords.value = records.sortedByDescending { it.checkInTime.toDate() }
                
                Log.d(TAG, "✅ Loaded ${records.size} attendance records")
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load attendance history: ${e.message}"
                Log.e(TAG, "❌ Error loading attendance: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Upload avatar image
     * Automatically syncs to Firebase and updates iOS in real-time
     */
    fun uploadAvatar(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _isUploadingAvatar.value = true
            _errorMessage.value = null
            
            try {
                Log.d(TAG, "📸 Starting avatar upload for: ${employee.employeeId}")
                
                // Load and resize image
                val bitmap = loadAndResizeBitmap(context, imageUri)
                if (bitmap == null) {
                    _errorMessage.value = "Failed to load image"
                    Log.e(TAG, "❌ Failed to load bitmap from URI")
                    return@launch
                }
                
                // Compress to JPEG
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val imageData = outputStream.toByteArray()
                
                Log.d(TAG, "✅ Image processed, size: ${imageData.size / 1024}KB")
                
                // Upload to Firebase Storage
                val avatarPath = "avatars/${employee.employeeId}_${System.currentTimeMillis()}.jpg"
                val storageRef = storage.reference.child(avatarPath)
                
                Log.d(TAG, "⬆️  Uploading to: $avatarPath")
                val uploadTask = storageRef.putBytes(imageData).await()
                
                // Get download URL
                val downloadUrl = storageRef.downloadUrl.await().toString()
                Log.d(TAG, "✅ Upload successful! URL: $downloadUrl")
                
                // Update employee document in Firestore
                Log.d(TAG, "💾 Updating employee document...")
                firestoreService.updateEmployeeAvatar(employee.uid, downloadUrl)
                
                // Update local state
                _currentAvatarUrl.value = downloadUrl
                
                Log.d(TAG, "🎉 Avatar saved successfully! Changes will sync to iOS automatically.")
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to upload avatar: ${e.message}"
                Log.e(TAG, "❌ Avatar upload error: ${e.message}", e)
            } finally {
                _isUploadingAvatar.value = false
            }
        }
    }
    
    /**
     * Update employee details
     * Syncs to Firebase and automatically reflects on iOS
     */
    fun updateEmployee(updatedEmployee: Employee) {
        viewModelScope.launch {
            _errorMessage.value = null
            
            try {
                Log.d(TAG, "💾 Updating employee: ${updatedEmployee.employeeId}")
                
                // Update in Firestore
                firestoreService.updateEmployee(
                    uid = updatedEmployee.uid,
                    updates = mapOf(
                        "englishName" to updatedEmployee.englishName,
                        "arabicName" to updatedEmployee.arabicName,
                        "email" to updatedEmployee.email,
                        "phoneNumber" to updatedEmployee.phoneNumber,
                        "departmentEn" to updatedEmployee.departmentEn,
                        "departmentAr" to updatedEmployee.departmentAr,
                        "role" to updatedEmployee.role.name,
                        "isActive" to updatedEmployee.isActive,
                        "updatedAt" to Timestamp.now()
                    )
                )
                
                Log.d(TAG, "✅ Employee updated successfully! Changes will sync to iOS automatically.")
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update employee: ${e.message}"
                Log.e(TAG, "❌ Update error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Load bitmap from URI and resize to max 800px width
     */
    private fun loadAndResizeBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream for URI: $uri")
                return null
            }
            
            // Decode with inJustDecodeBounds to get dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // Calculate sample size
            val maxWidth = 800
            var sampleSize = 1
            if (options.outWidth > maxWidth) {
                sampleSize = options.outWidth / maxWidth
            }
            
            // Decode actual bitmap
            val inputStream2 = context.contentResolver.openInputStream(uri)
            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize
            var bitmap = BitmapFactory.decodeStream(inputStream2, null, options)
            inputStream2?.close()
            
            // Handle orientation
            bitmap = fixOrientation(context, uri, bitmap)
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap: ${e.message}", e)
            null
        }
    }
    
    /**
     * Fix image orientation based on EXIF data
     */
    private fun fixOrientation(context: Context, uri: Uri, bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null
        
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing orientation: ${e.message}", e)
            bitmap
        }
    }
    
    companion object {
        private const val TAG = "EmployeeDetailViewModel"
    }
}

/**
 * Factory for EmployeeDetailViewModel
 */
class EmployeeDetailViewModelFactory(
    private val context: Context,
    private val employee: Employee
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmployeeDetailViewModel::class.java)) {
            return EmployeeDetailViewModel(employee) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
