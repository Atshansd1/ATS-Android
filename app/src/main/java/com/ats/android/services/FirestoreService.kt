package com.ats.android.services

import android.util.Log
import com.ats.android.models.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FirestoreService private constructor() {
    
    private val db: FirebaseFirestore
    
    init {
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
            
        db = Firebase.firestore
        db.firestoreSettings = settings
    }
    
    companion object {
        private const val TAG = "FirestoreService"
        private const val COMPANY_ID = "it-adc"
        private const val EMPLOYEES_COLLECTION = "companies/$COMPANY_ID/employees"
        private const val ATTENDANCE_COLLECTION = "companies/$COMPANY_ID/attendance"
        private const val ACTIVE_LOCATIONS_COLLECTION = "companies/$COMPANY_ID/activeLocations"
        private const val SHIFT_CONFIGS_COLLECTION = "companies/$COMPANY_ID/shiftConfigs"
        private const val CHECKIN_LOCATION_CONFIGS_COLLECTION = "companies/$COMPANY_ID/checkInLocationConfigs"
        private const val ATTENDANCE_CENTERS_COLLECTION = "companies/$COMPANY_ID/attendanceCenters"
        private const val MOVEMENTS_COLLECTION = "companies/$COMPANY_ID/movements"
        private const val LEAVE_REQUESTS_COLLECTION = "companies/$COMPANY_ID/leaveRequests"
        private const val LEAVE_BALANCES_COLLECTION = "companies/$COMPANY_ID/leaveBalances"
        
        @Volatile
        private var instance: FirestoreService? = null
        
        fun getInstance(): FirestoreService {
            return instance ?: synchronized(this) {
                instance ?: FirestoreService().also { instance = it }
            }
        }
    }
    
    // Employee operations
    suspend fun getEmployee(uid: String): Employee {
        Log.d(TAG, "📥 Fetching employee with uid: $uid")
        val snapshot = db.collection(EMPLOYEES_COLLECTION)
            .whereEqualTo("uid", uid)
            .get()
            .await()
        
        if (snapshot.documents.isEmpty()) {
            throw Exception("Employee not found")
        }
        
        val employee = snapshot.documents[0].toObject<Employee>()
            ?: throw Exception("Failed to parse employee")
        
        Log.d(TAG, "✅ Employee fetched: ${employee.displayName}")
        return employee
    }
    
    suspend fun getAllEmployees(): List<Employee> {
        return try {
            Log.d(TAG, "📥 Fetching all employees")
            val snapshot = db.collection(EMPLOYEES_COLLECTION)
                .get()
                .await()
            
            val employees = snapshot.documents.mapNotNull { doc ->
                try {
                    val employee = doc.toObject<Employee>()
                    employee?.let {
                        Log.d(TAG, "Employee ${it.employeeId} avatarURL: ${it.avatarURL ?: "NULL"}")
                    }
                    employee
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse employee ${doc.id}: ${e.message}")
                    null
                }
            }
            // Filter out admin users
            val nonAdminEmployees = employees.filter { it.role != EmployeeRole.ADMIN }
            Log.d(TAG, "✅ Fetched ${nonAdminEmployees.size} non-admin employees from ${snapshot.size()} documents (filtered ${employees.size - nonAdminEmployees.size} admins)")
            nonAdminEmployees
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching employees: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun updateEmployee(uid: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            val snapshot = db.collection(EMPLOYEES_COLLECTION)
                .whereEqualTo("uid", uid)
                .get()
                .await()
            
            if (snapshot.documents.isEmpty()) {
                throw Exception("Employee not found")
            }
            
            val docId = snapshot.documents[0].id
            val updateData = updates.toMutableMap()
            updateData["updatedAt"] = Timestamp.now()
            
            db.collection(EMPLOYEES_COLLECTION)
                .document(docId)
                .update(updateData)
                .await()
            
            Log.d(TAG, "✅ Employee updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update employee error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update FCM token for push notifications
     */
    suspend fun updateFCMToken(employeeId: String, fcmToken: String) {
        try {
            Log.d(TAG, "🔔 Updating FCM token for employee: $employeeId")
            
            val snapshot = db.collection(EMPLOYEES_COLLECTION)
                .whereEqualTo("employeeId", employeeId)
                .get()
                .await()
            
            if (snapshot.documents.isEmpty()) {
                Log.w(TAG, "⚠️ Employee not found for FCM token update: $employeeId")
                return
            }
            
            val docId = snapshot.documents[0].id
            db.collection(EMPLOYEES_COLLECTION)
                .document(docId)
                .update(
                    mapOf(
                        "fcmToken" to fcmToken,
                        "fcmTokenUpdatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            Log.d(TAG, "✅ FCM token updated for employee: $employeeId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating FCM token: ${e.message}", e)
        }
    }
    
    /**
     * Update employee avatar URL
     * Syncs to Firebase and automatically reflects on iOS
     */
    suspend fun updateEmployeeAvatar(uid: String, avatarURL: String) {
        try {
            Log.d(TAG, "💾 Updating avatar for employee: $uid")
            
            val snapshot = db.collection(EMPLOYEES_COLLECTION)
                .whereEqualTo("uid", uid)
                .get()
                .await()
            
            if (snapshot.documents.isEmpty()) {
                throw Exception("Employee not found")
            }
            
            val docId = snapshot.documents[0].id
            db.collection(EMPLOYEES_COLLECTION)
                .document(docId)
                .update(
                    mapOf(
                        "avatarURL" to avatarURL,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            Log.d(TAG, "✅ Avatar URL updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update avatar error: ${e.message}", e)
            throw e
        }
    }
    
    // MARK: - Attendance Center Operations
    
    suspend fun getAttendanceCenter(centerId: String): Result<AttendanceCenter> {
        return try {
            val snapshot = db.collection(ATTENDANCE_CENTERS_COLLECTION)
                .document(centerId)
                .get()
                .await()
            
            val center = snapshot.toObject(AttendanceCenter::class.java)
            if (center != null) {
                // Ensure ID is set
                val centerWithId = center.copy(id = snapshot.id)
                Result.success(centerWithId)
            } else {
                Result.failure(Exception("Attendance center not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching attendance center", e)
            Result.failure(e)
        }
    }
    
    suspend fun createAttendanceCenter(center: AttendanceCenter): Result<String> {
        return try {
            val docRef = db.collection(ATTENDANCE_CENTERS_COLLECTION)
                .document()
                
            val centerWithTimestamps = center.copy(
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            docRef.set(centerWithTimestamps).await()
            Log.d(TAG, "Created attendance center: ${center.name} (ID: ${docRef.id})")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating attendance center", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateAttendanceCenter(center: AttendanceCenter): Result<Unit> {
        return try {
            val id = center.id ?: return Result.failure(Exception("Invalid document ID"))
            
            val docRef = db.collection(ATTENDANCE_CENTERS_COLLECTION)
                .document(id)
                
            val centerWithTimestamp = center.copy(
                updatedAt = Timestamp.now()
            )
            
            docRef.set(centerWithTimestamp).await() // Using set to overwrite/merge
            Log.d(TAG, "Updated attendance center: ${center.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating attendance center", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteAttendanceCenter(centerId: String): Result<Unit> {
        return try {
            db.collection(ATTENDANCE_CENTERS_COLLECTION)
                .document(centerId)
                .delete()
                .await()
                
            Log.d(TAG, "Deleted attendance center: $centerId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting attendance center", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAllAttendanceCenters(): List<AttendanceCenter> {
        return try {
            val snapshot = db.collection(ATTENDANCE_CENTERS_COLLECTION)
                .orderBy("createdAt")
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
                
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(AttendanceCenter::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all attendance centers", e)
            emptyList()
        }
    }
    
    suspend fun getActiveCenters(): List<AttendanceCenter> {
        return try {
            val snapshot = db.collection(ATTENDANCE_CENTERS_COLLECTION)
                .whereEqualTo("isActive", true)
                .get()
                .await()
                
            val centers = snapshot.documents.mapNotNull { doc ->
                doc.toObject(AttendanceCenter::class.java)?.copy(id = doc.id)
            }
            // Client-side sort if needed, as compound query might require index
            centers.sortedBy { it.createdAt }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching active attendance centers", e)
            emptyList()
        }
    }
    
    // Attendance operations
    suspend fun checkIn(
        employeeId: String,
        employeeName: String,
        location: GeoPoint,
        placeName: String?
    ): Result<Unit> {
        return try {
            // FIRST: Check if already checked-in to prevent duplicates
            val existingCheckIn = getActiveCheckIn(employeeId)
            if (existingCheckIn != null) {
                Log.w(TAG, "⚠️ Employee $employeeName ($employeeId) is already checked-in")
                return Result.failure(Exception("Already checked-in. Please check out first."))
            }
            
            val now = Timestamp.now()
            
            // Create iOS-compatible GeoPointData structure
            val checkInLocationData = hashMapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "accuracy" to null,
                "timestamp" to now
            )
            
            val attendance = hashMapOf(
                "employeeId" to employeeId,
                "checkInTime" to now,
                "checkInLocation" to checkInLocationData,
                "checkInPlaceName" to placeName,
                "status" to "checked_in",
                "syncStatus" to "synced",
                "idempotencyKey" to "${employeeId}_${now.seconds}"
            )
            
            Log.d(TAG, "📝 Creating check-in for $employeeName at ${placeName ?: "Unknown"}")
            
            db.collection(ATTENDANCE_COLLECTION)
                .add(attendance)
                .await()
            
            // Update active location
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            updateActiveLocation(employeeId, geoPoint, placeName)
            
            Log.d(TAG, "✅ Check-in successful for $employeeName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Check-in error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun checkOut(employeeId: String, location: GeoPoint, placeName: String?): Result<Unit> {
        return try {
            // Find active attendance record
            val snapshot = db.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("status", "checked_in")
                .orderBy("checkInTime", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.documents.isEmpty()) {
                throw Exception("No active check-in found")
            }
            
            val docId = snapshot.documents[0].id
            val checkInTime = snapshot.documents[0].getTimestamp("checkInTime")
            val now = Timestamp.now()
            val duration = if (checkInTime != null) {
                (now.seconds - checkInTime.seconds).toDouble()
            } else null
            
            // Create iOS-compatible GeoPointData structure for checkout
            val checkOutLocationData = hashMapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "accuracy" to null,
                "timestamp" to now
            )
            
            db.collection(ATTENDANCE_COLLECTION)
                .document(docId)
                .update(
                    mapOf(
                        "checkOutTime" to now,
                        "checkOutLocation" to checkOutLocationData,
                        "checkOutPlaceName" to placeName,
                        "totalDuration" to duration,
                        "status" to "checked_out"
                    )
                )
                .await()
            
            // Remove from active locations
            removeActiveLocation(employeeId)
            
            Log.d(TAG, "✅ Check-out successful for $employeeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Check-out error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete old attendance records before a given date
     * Admin function for cleaning up test/old data
     */
    suspend fun deleteOldAttendanceRecords(beforeDate: Date): Result<Int> {
        return try {
            Log.d(TAG, "🗑️ Deleting attendance records before: $beforeDate")
            
            val snapshot = db.collection(ATTENDANCE_COLLECTION)
                .whereLessThan("checkInTime", Timestamp(beforeDate))
                .get()
                .await()
            
            if (snapshot.documents.isEmpty()) {
                Log.d(TAG, "ℹ️ No old records found to delete")
                return Result.success(0)
            }
            
            val batch = db.batch()
            var count = 0
            
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
                count++
            }
            
            batch.commit().await()
            
            Log.d(TAG, "✅ Deleted $count old attendance records")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting old records: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAttendanceHistory(
        employeeId: String,
        startDate: Date,
        endDate: Date
    ): List<AttendanceRecord> {
        com.ats.android.utils.DebugLogger.d(TAG, "📥 Fetching attendance history for $employeeId")
        com.ats.android.utils.DebugLogger.d(TAG, "🔍 Query range: $startDate to $endDate")
        com.ats.android.utils.DebugLogger.d(TAG, "🔍 Query timestamps: ${Timestamp(startDate)} to ${Timestamp(endDate)}")
        
        // Use checkInTime for filtering (like iOS) instead of date field
        // This uses the existing employeeId + checkInTime index that already exists
        val snapshot = db.collection(ATTENDANCE_COLLECTION)
            .whereEqualTo("employeeId", employeeId)
            .whereGreaterThanOrEqualTo("checkInTime", Timestamp(startDate))
            .whereLessThanOrEqualTo("checkInTime", Timestamp(endDate))
            .orderBy("checkInTime", Query.Direction.DESCENDING)
            .get()
            .await()
        
        com.ats.android.utils.DebugLogger.d(TAG, "📦 Query returned ${snapshot.documents.size} documents")
        
        // Handle both GeoPoint and GeoPointData formats
        val records = snapshot.documents.mapNotNull { doc ->
            try {
                val record = doc.toObject<AttendanceRecord>()
                com.ats.android.utils.DebugLogger.d(TAG, "📄 Record: ${doc.id} - ${record?.checkInTime}")
                record
            } catch (e: Exception) {
                com.ats.android.utils.DebugLogger.w(TAG, "Failed to deserialize record ${doc.id}, attempting manual conversion")
                convertDocumentToAttendanceRecord(doc)
            }
        }
        
        com.ats.android.utils.DebugLogger.d(TAG, "✅ Fetched ${records.size} attendance records for $employeeId")
        return records
    }
    
    /**
     * Force checkout an employee (Admin only)
     */
    suspend fun forceCheckout(employeeId: String): Result<Unit> {
        return try {
            val record = getActiveCheckIn(employeeId) ?: return Result.failure(Exception("No active check-in found"))
            
            val checkOutTime = Timestamp.now()
            val checkInTime = record.checkInTime ?: Timestamp.now()
            val duration = checkOutTime.seconds - checkInTime.seconds
            
            val updates = mapOf(
                "checkOutTime" to checkOutTime,
                "checkOutPlaceName" to "Admin Force Checkout",
                "status" to "checked_out",
                "totalDuration" to duration.toDouble(),
                "duration" to duration
            )
            
            val recordId = record.id ?: return Result.failure(Exception("Invalid record ID"))
            
            db.collection(ATTENDANCE_COLLECTION).document(recordId).update(updates).await()
            
            // Remove from active locations
            try {
                db.collection(ACTIVE_LOCATIONS_COLLECTION).document(employeeId).delete().await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove active location: ${e.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Force checkout failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getActiveCheckIn(employeeId: String): AttendanceRecord? {
        val snapshot = db.collection(ATTENDANCE_COLLECTION)
            .whereEqualTo("employeeId", employeeId)
            .whereEqualTo("status", "checked_in")
            .orderBy("checkInTime", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        
        return if (snapshot.documents.isNotEmpty()) {
            try {
                // Try normal deserialization first
                snapshot.documents[0].toObject<AttendanceRecord>()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to deserialize attendance record, attempting manual conversion: ${e.message}")
                // Manual conversion for records with GeoPoint objects
                convertDocumentToAttendanceRecord(snapshot.documents[0])
            }
        } else null
    }
    
    /**
     * Manually convert Firestore document to AttendanceRecord
     * Handles both GeoPoint and GeoPointData formats
     */
    private fun convertDocumentToAttendanceRecord(doc: com.google.firebase.firestore.DocumentSnapshot): AttendanceRecord? {
        try {
            val data = doc.data ?: return null
            
            // Convert checkInLocation (handle both GeoPoint and Map)
            val checkInLocation = when (val loc = data["checkInLocation"]) {
                is GeoPoint -> GeoPointData(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = null,
                    timestamp = data["checkInTime"] as? Timestamp
                )
                is Map<*, *> -> GeoPointData(
                    latitude = loc["latitude"] as? Double ?: 0.0,
                    longitude = loc["longitude"] as? Double ?: 0.0,
                    accuracy = loc["accuracy"] as? Double,
                    timestamp = loc["timestamp"] as? Timestamp
                )
                else -> null
            }
            
            // Convert checkOutLocation (handle both GeoPoint and Map)
            val checkOutLocation = when (val loc = data["checkOutLocation"]) {
                is GeoPoint -> GeoPointData(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = null,
                    timestamp = data["checkOutTime"] as? Timestamp
                )
                is Map<*, *> -> GeoPointData(
                    latitude = loc["latitude"] as? Double ?: 0.0,
                    longitude = loc["longitude"] as? Double ?: 0.0,
                    accuracy = loc["accuracy"] as? Double,
                    timestamp = loc["timestamp"] as? Timestamp
                )
                else -> null
            }
            
            return AttendanceRecord(
                id = doc.id,
                employeeId = data["employeeId"] as? String ?: "",
                checkInTime = data["checkInTime"] as? Timestamp ?: Timestamp.now(),
                checkOutTime = data["checkOutTime"] as? Timestamp,
                checkInLocation = checkInLocation,
                checkOutLocation = checkOutLocation,
                checkInPlaceName = data["checkInPlaceName"] as? String,
                checkOutPlaceName = data["checkOutPlaceName"] as? String,
                totalDuration = data["totalDuration"] as? Double,
                statusString = data["status"] as? String ?: "checked_in",
                syncStatus = data["syncStatus"] as? String,
                idempotencyKey = data["idempotencyKey"] as? String,
                metadata = null, // TODO: deserialize if needed
                employeeName = data["employeeName"] as? String,
                date = data["date"] as? Timestamp,
                duration = data["duration"] as? Long
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to manually convert attendance record: ${e.message}", e)
            return null
        }
    }
    
    // Active locations
    suspend fun updateEmployeeLocation(
        employeeId: String,
        location: GeoPoint,
        placeName: String?,
        placeNameEn: String? = null,
        placeNameAr: String? = null
    ) {
        // Update active location
        updateActiveLocation(employeeId, location, placeName, placeNameEn, placeNameAr)
        
        // Removed automatic movement recording. 
        // Logic is now moved to LocationTrackingService for handling stationary/check-in area departure logic.
    }
    
    suspend fun recordMovement(
        employeeId: String,
        fromLat: Double,
        fromLng: Double,
        fromPlace: String?,
        toLat: Double,
        toLng: Double,
        toPlace: String?,
        distance: Double,
        startTime: Timestamp,
        endTime: Timestamp,
        movementType: MovementType
    ) {
        try {
            // Get employee info
            val employee = getEmployeeByEmployeeId(employeeId)
            val employeeName = employee?.displayName ?: employeeId
            
            // Get check-in info
            val activeCheckIn = getActiveCheckIn(employeeId)
            val checkInLat = activeCheckIn?.checkInLocation?.latitude ?: 0.0
            val checkInLng = activeCheckIn?.checkInLocation?.longitude ?: 0.0
            val checkInId = activeCheckIn?.id ?: ""
            
            // Calculate duration in seconds
            val durationSeconds = endTime.seconds - startTime.seconds
            
            val movement = hashMapOf<String, Any?>(
                "employeeId" to employeeId,
                "employeeName" to employeeName,
                "movementType" to movementType.value,
                "fromLatitude" to fromLat,
                "fromLongitude" to fromLng,
                "fromAddress" to fromPlace,
                "toLatitude" to toLat,
                "toLongitude" to toLng,
                "toAddress" to toPlace,
                "distance" to distance,
                "startTime" to startTime,
                "endTime" to endTime,
                "duration" to durationSeconds,
                "checkInId" to checkInId,
                "checkInLatitude" to checkInLat,
                "checkInLongitude" to checkInLng,
                "createdAt" to Timestamp.now()
            )
            
            db.collection(MOVEMENTS_COLLECTION)
                .add(movement)
                .await()
            
            Log.d(TAG, "✅ Movement recorded: $employeeName moved ${String.format("%.2f", distance)}km from $fromPlace to $toPlace (${durationSeconds}s)")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error recording movement: ${e.message}", e)
        }
    }
    
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
    
    private suspend fun updateActiveLocation(
        employeeId: String,
        location: GeoPoint,
        placeName: String?,
        placeNameEn: String? = null,
        placeNameAr: String? = null
    ) {
        try {
            // CRITICAL: Only update active location if employee is actually checked in
            // Fetch the raw document to get the true checkInTime (avoid model default values)
            val attendanceSnapshot = db.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("status", "checked_in")
                .orderBy("checkInTime", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            if (attendanceSnapshot.documents.isEmpty()) {
                Log.d(TAG, "⚠️ Employee $employeeId not checked in, skipping active location update")
                return
            }
            
            // Get the REAL checkInTime directly from the Firestore document
            // This avoids the model default of Timestamp.now() if deserialization fails
            val attendanceDoc = attendanceSnapshot.documents[0]
            val originalCheckInTime = attendanceDoc.getTimestamp("checkInTime")
            
            if (originalCheckInTime == null) {
                Log.e(TAG, "❌ CheckInTime is null for employee $employeeId, skipping update")
                return
            }
            
            val now = Timestamp.now()
            
            // Validate: checkInTime should be BEFORE now (not equal or very close)
            // If it's within 5 seconds of now, it's likely a deserialization default
            val timeDiffSeconds = now.seconds - originalCheckInTime.seconds
            if (timeDiffSeconds < 5) {
                Log.w(TAG, "⚠️ CheckInTime ($originalCheckInTime) is suspiciously close to now, using stored time")
            }
            
            Log.d(TAG, "📍 Using original check-in time: ${originalCheckInTime.toDate()}")
            
            // iOS-compatible GeoPointData structure (not native GeoPoint!)
            val locationData = hashMapOf<String, Any?>(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "accuracy" to null,
                "timestamp" to now
            )
            
            val activeLocation = hashMapOf<String, Any?>(
                "employeeId" to employeeId,
                "location" to locationData,  // Use GeoPointData structure
                "lastUpdated" to now,        // iOS expects "lastUpdated" not "timestamp"
                "checkInTime" to originalCheckInTime,  // Preserve original check-in time from document
                "isActive" to true,
                "placeName" to (placeName ?: "Unknown Location"),
                "placeNameEn" to (placeNameEn ?: placeName),
                "placeNameAr" to (placeNameAr ?: placeName),
            )
            
            Log.d(TAG, "📍 Updating active location for $employeeId at (${location.latitude}, ${location.longitude})")
            
            db.collection(ACTIVE_LOCATIONS_COLLECTION)
                .document(employeeId)
                .set(activeLocation)
                .await()
            
            Log.d(TAG, "✅ Active location updated for $employeeId (iOS-compatible format)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update active location error: ${e.message}", e)
        }
    }
    
    private suspend fun removeActiveLocation(employeeId: String) {
        try {
            // Set isActive to false instead of deleting (matching iOS behavior)
            db.collection(ACTIVE_LOCATIONS_COLLECTION)
                .document(employeeId)
                .update("isActive", false)
                .await()
            
            Log.d(TAG, "✅ Active location marked as inactive")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Remove active location error: ${e.message}", e)
        }
    }
    
    suspend fun getActiveLocations(): List<Pair<Employee, ActiveLocation>> {
        Log.d(TAG, "📥 Fetching active locations")
        try {
            val snapshot = db.collection(ACTIVE_LOCATIONS_COLLECTION)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val result = mutableListOf<Pair<Employee, ActiveLocation>>()
            
            for (document in snapshot.documents) {
                try {
                    val employeeId = document.getString("employeeId") ?: continue
                    
                    // Manually parse location to handle HashMap
                    val locationData = document.get("location")
                    val geoPoint = when (locationData) {
                        is GeoPoint -> locationData
                        is Map<*, *> -> {
                            val lat = (locationData["latitude"] as? Number)?.toDouble() ?: 0.0
                            val lng = (locationData["longitude"] as? Number)?.toDouble() ?: 0.0
                            GeoPoint(lat, lng)
                        }
                        else -> GeoPoint(0.0, 0.0)
                    }
                    
                    val activeLocation = ActiveLocation(
                        employeeId = employeeId,
                        location = GeoPointData.fromGeoPoint(geoPoint),
                        timestamp = document.getTimestamp("lastUpdated") ?: Timestamp.now(),
                        checkInTime = document.getTimestamp("checkInTime") ?: Timestamp.now(),
                        isActive = document.getBoolean("isActive") ?: true,
                        placeName = document.getString("placeName"),
                        previousPlaceName = document.getString("previousPlaceName"),
                        batteryLevel = document.getDouble("batteryLevel"),
                        speed = document.getDouble("speed"),
                        accuracy = document.getDouble("accuracy")
                    )
                    
                    val employee = getEmployeeByEmployeeId(employeeId)
                    if (employee != null) {
                        result.add(Pair(employee, activeLocation))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing active location: ${e.message}")
                }
            }
            
            Log.d(TAG, "✅ Fetched ${result.size} active locations")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching active locations: ${e.message}", e)
            return emptyList()
        }
    }
    
    private suspend fun getEmployeeByEmployeeId(employeeId: String): Employee? {
        return try {
            val snapshot = db.collection(EMPLOYEES_COLLECTION)
                .whereEqualTo("id", employeeId)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents[0].toObject<Employee>()
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting employee: ${e.message}")
            null
        }
    }
    
    /**
     * Get authoritative check-in times from Attendance collection.
     * This is required because legacy app versions may overwrite ActiveLocation checkInTime
     * with incorrect values (e.g. current timestamp).
     */
    suspend fun getLatestCheckInTimes(employeeIds: List<String>): Map<String, Date> {
        if (employeeIds.isEmpty()) return emptyMap()
        
        return try {
            val result = mutableMapOf<String, Date>()
            
            // Query in batches of 10 to avoid 'IN' query limits (limit is 30, but 10 is safe)
            employeeIds.chunked(10).forEach { batch ->
                val snapshot = db.collection(ATTENDANCE_COLLECTION)
                    .whereIn("employeeId", batch)
                    .whereEqualTo("status", "checked_in")
                    .get()
                    .await()
                    
                for (doc in snapshot.documents) {
                    val employeeId = doc.getString("employeeId")
                    val checkInTimestamp = doc.getTimestamp("checkInTime")
                    
                    if (employeeId != null && checkInTimestamp != null) {
                        // If multiple records exist (shouldn't happen for checked_in), 
                        // verify we get the latest if we query sorted, but here we just take what matches.
                        // Ideally we trust the 'checked_in' status implies the current session.
                        result[employeeId] = checkInTimestamp.toDate()
                    }
                }
            }
            Log.d(TAG, "✅ Fetched ${result.size} verified check-in times from attendance")
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching check-in times: ${e.message}", e)
            emptyMap()
        }
    }

    suspend fun getRecentAttendance(limit: Int = 10): List<AttendanceRecord> {
        return try {
            Log.d(TAG, "📥 Fetching recent attendance records (last 24 hours)")
            
            // Get timestamp for 24 hours ago
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.HOUR_OF_DAY, -24)
            val twentyFourHoursAgo = Timestamp(calendar.time)
            
            val snapshot = db.collection(ATTENDANCE_COLLECTION)
                .whereGreaterThanOrEqualTo("checkInTime", twentyFourHoursAgo)
                .orderBy("checkInTime", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            // Handle both GeoPoint and GeoPointData formats
            val records = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject<AttendanceRecord>()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to deserialize record ${doc.id}, attempting manual conversion")
                    convertDocumentToAttendanceRecord(doc)
                }
            }
            
            Log.d(TAG, "✅ Fetched ${records.size} attendance records from last 24 hours")
            records
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching attendance: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getAllAttendanceRecords(
        startDate: Date,
        endDate: Date
    ): List<AttendanceRecord> {
        com.ats.android.utils.DebugLogger.d(TAG, "📥 Fetching ALL attendance records for analytics")
        com.ats.android.utils.DebugLogger.d(TAG, "🔍 Query range: $startDate to $endDate")
        
        return try {
            val snapshot = db.collection(ATTENDANCE_COLLECTION)
                .whereGreaterThanOrEqualTo("checkInTime", Timestamp(startDate))
                .whereLessThanOrEqualTo("checkInTime", Timestamp(endDate))
                .orderBy("checkInTime", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val records = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject<AttendanceRecord>()
                } catch (e: Exception) {
                    convertDocumentToAttendanceRecord(doc)
                }
            }
            
            Log.d(TAG, "✅ Fetched ${records.size} total attendance records for analytics")
            records
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching analytics attendance: ${e.message}", e)
            emptyList()
        }
    }
    
    // Real-time active locations observer (matching iOS getActiveLocations)
    fun observeActiveLocations(onUpdate: (List<Pair<Employee, ActiveLocation>>) -> Unit) {
        Log.d(TAG, "📍 Starting real-time location observer on: $ATTENDANCE_COLLECTION (status=checked_in)")
        
        db.collection(ATTENDANCE_COLLECTION)
            .whereEqualTo("status", "checked_in")
            .addSnapshotListener { attendanceSnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Listen failed: ${error.message}", error)
                    return@addSnapshotListener
                }
                
                if (attendanceSnapshot == null || attendanceSnapshot.isEmpty) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                
                // Get all checked-in employee IDs
                val checkedInEmployeeIds = attendanceSnapshot.documents.mapNotNull { it.getString("employeeId") }
                
                if (checkedInEmployeeIds.isEmpty()) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                
                db.collection(ACTIVE_LOCATIONS_COLLECTION)
                    .whereEqualTo("isActive", true)
                    .addSnapshotListener { locationSnapshot, locationError ->
                         if (locationError != null) {
                            Log.e(TAG, "❌ Location listen failed: ${locationError.message}")
                            return@addSnapshotListener
                         }
                         
                         if (locationSnapshot == null) return@addSnapshotListener
                         
                         val activeLocations = mutableListOf<Pair<Employee, ActiveLocation>>()
                         
                         CoroutineScope(Dispatchers.IO).launch {
                            locationSnapshot.documents.forEach { doc ->
                                try {
                                    val employeeId = doc.getString("employeeId") ?: return@forEach
                                    
                                    // Only include if checked in
                                    if (!checkedInEmployeeIds.contains(employeeId)) return@forEach
                                    
                                    val geoPoint = when (val loc = doc.get("location")) {
                                        is GeoPoint -> loc
                                        is HashMap<*, *> -> {
                                            val lat = (loc["latitude"] as? Double) ?: 0.0
                                            val lng = (loc["longitude"] as? Double) ?: 0.0
                                            GeoPoint(lat, lng)
                                        }
                                        else -> return@forEach
                                    }
                                    
                                    val activeLocation = ActiveLocation(
                                        employeeId = employeeId,
                                        location = GeoPointData.fromGeoPoint(geoPoint),
                                        timestamp = doc.getTimestamp("lastUpdated") ?: Timestamp.now(),
                                        checkInTime = doc.getTimestamp("checkInTime") ?: Timestamp.now(),
                                        placeName = doc.getString("placeName")
                                    )
                                    
                                    val employee = getEmployeeByEmployeeId(employeeId)
                                    if (employee != null) {
                                        activeLocations.add(Pair(employee, activeLocation))
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing active location: ${e.message}")
                                }
                            }
                            
                            withContext(Dispatchers.Main) {
                                onUpdate(activeLocations)
                            }
                         }
                    }
            }
    }

    // Real-time listener for a single employee's check-in status
    fun listenToActiveCheckIn(employeeId: String, onUpdate: (AttendanceRecord?) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        Log.d(TAG, "👂 listening to active check-in for: $employeeId")
        return db.collection(ATTENDANCE_COLLECTION)
            .whereEqualTo("employeeId", employeeId)
            .whereEqualTo("status", "checked_in")
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Listen active check-in error: ${error.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot == null || snapshot.isEmpty) {
                    onUpdate(null)
                    return@addSnapshotListener
                }
                
                try {
                    val record = try {
                        snapshot.documents[0].toObject<AttendanceRecord>()
                    } catch (e: Exception) {
                        convertDocumentToAttendanceRecord(snapshot.documents[0])
                    }
                    onUpdate(record)
                } catch (e: Exception) {
                     Log.e(TAG, "❌ Error parsing active record: ${e.message}")
                     onUpdate(null)
                }
            }
    }
                                

    suspend fun createEmployee(employee: Employee) {
        try {
            Log.d(TAG, "➕ Creating employee: ${employee.employeeId}")
            db.collection(EMPLOYEES_COLLECTION)
                .document(employee.employeeId)
                .set(employee)
                .await()
            Log.d(TAG, "✅ Employee created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating employee: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun updateEmployeeFields(employeeId: String, updates: Map<String, Any?>) {
        try {
            Log.d(TAG, "📝 Updating employee fields: $employeeId")
            db.collection(EMPLOYEES_COLLECTION)
                .document(employeeId)
                .update(updates)
                .await()
            Log.d(TAG, "✅ Employee fields updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating employee fields: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun deleteEmployee(employeeId: String) {
        try {
            Log.d(TAG, "🗑️ Deleting employee: $employeeId")
            db.collection(EMPLOYEES_COLLECTION)
                .document(employeeId)
                .delete()
                .await()
            Log.d(TAG, "✅ Employee deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting employee: ${e.message}", e)
            throw e
        }
    }
    
    // Check-in Location Configuration
    suspend fun saveCheckInLocationConfig(config: CheckInLocationConfig): Result<Unit> {
        return try {
            Log.d(TAG, "💾 Saving check-in location config: ${config.type}")
            val docId = config.id ?: "default"
            val configMap = mapOf(
                "name" to config.name,
                "type" to config.type.name,
                "allowedLocations" to config.allowedLocations.map { location ->
                    mapOf(
                        "id" to location.id,
                        "name" to location.name,
                        "address" to location.address,
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "radius" to location.radius,
                        "placeId" to location.placeId
                    )
                },
                "applicableEmployeeIds" to config.applicableEmployeeIds,
                "isActive" to config.isActive,
                "createdAt" to (config.createdAt ?: Timestamp.now()),
                "updatedAt" to Timestamp.now()
            )
            
            db.collection(CHECKIN_LOCATION_CONFIGS_COLLECTION)
                .document(docId)
                .set(configMap)
                .await()
            
            Log.d(TAG, "✅ Check-in location config saved successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving check-in location config", e)
            Result.failure(e)
        }
    }
    
    suspend fun getCheckInLocationConfig(): Result<CheckInLocationConfig?> {
        return try {
            Log.d(TAG, "📥 Fetching check-in location config")
            val snapshot = db.collection(CHECKIN_LOCATION_CONFIGS_COLLECTION)
                .document("default")
                .get()
                .await()
            
            if (!snapshot.exists()) {
                Log.d(TAG, "ℹ️ No check-in location config found")
                return Result.success(null)
            }
            
            val data = snapshot.data ?: return Result.success(null)
            
            @Suppress("UNCHECKED_CAST")
            val allowedLocations = (data["allowedLocations"] as? List<Map<String, Any>>)?.map { locMap ->
                AllowedLocation(
                    id = locMap["id"] as? String ?: "",
                    name = locMap["name"] as? String ?: "",
                    address = locMap["address"] as? String ?: "",
                    latitude = (locMap["latitude"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (locMap["longitude"] as? Number)?.toDouble() ?: 0.0,
                    radius = (locMap["radius"] as? Number)?.toDouble() ?: 100.0,
                    placeId = locMap["placeId"] as? String
                )
            } ?: emptyList()
            
            @Suppress("UNCHECKED_CAST")
            val applicableEmployeeIds = (data["applicableEmployeeIds"] as? List<String>) ?: emptyList()
            
            val config = CheckInLocationConfig(
                id = snapshot.id,
                name = data["name"] as? String ?: "Check-In Policy",
                type = LocationRestrictionType.fromString(data["type"] as? String ?: "ANYWHERE"),
                allowedLocations = allowedLocations,
                applicableEmployeeIds = applicableEmployeeIds,
                isActive = data["isActive"] as? Boolean ?: true,
                createdAt = data["createdAt"] as? Timestamp,
                updatedAt = data["updatedAt"] as? Timestamp
            )
            
            Log.d(TAG, "✅ Check-in location config loaded: ${config.type}")
            Result.success(config)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching check-in location config", e)
            Result.failure(e)
        }
    }
    
    // Location Movement Operations (matching iOS implementation)
    suspend fun saveLocationMovement(movement: LocationMovement): Result<Unit> {
        return try {
            Log.d(TAG, "💾 Saving location movement: ${movement.getType().displayName}")
            db.collection(MOVEMENTS_COLLECTION)
                .add(movement)
                .await()
            Log.d(TAG, "✅ Location movement saved successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving location movement", e)
            Result.failure(e)
        }
    }
    
    fun observeLocationMovements(
        employeeId: String? = null,
        limit: Int = 50,
        onUpdate: (List<LocationMovement>) -> Unit
    ) {
        Log.d(TAG, "📍 Starting real-time movements observer")
        
        var query: Query = db.collection(MOVEMENTS_COLLECTION)
        
        if (employeeId != null) {
            query = query.whereEqualTo("employeeId", employeeId)
        }
        
        query = query
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        
        query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "❌ Movements listener error: ${error.message}", error)
                return@addSnapshotListener
            }
            
            if (snapshot == null || snapshot.isEmpty) {
                Log.d(TAG, "⚠️ No movements in snapshot")
                onUpdate(emptyList())
                return@addSnapshotListener
            }
            
            val movements = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject<LocationMovement>()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing movement: ${e.message}")
                    null
                }
            }
            
            Log.d(TAG, "✅ Real-time movements update: ${movements.size} movements")
            onUpdate(movements)
        }
    }
    
    fun observeTodayMovements(
        employeeId: String? = null,
        onUpdate: (List<LocationMovement>) -> Unit
    ) {
        Log.d(TAG, "📍 Starting today's movements observer")
        
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = Timestamp(calendar.time)
        
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val endOfDay = Timestamp(calendar.time)
        
        var query: Query = db.collection(MOVEMENTS_COLLECTION)
            .whereGreaterThanOrEqualTo("createdAt", startOfDay)
            .whereLessThan("createdAt", endOfDay)
        
        if (employeeId != null) {
            query = query.whereEqualTo("employeeId", employeeId)
        }
        
        query = query.orderBy("createdAt", Query.Direction.DESCENDING)
        
        query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "❌ Today movements listener error: ${error.message}", error)
                return@addSnapshotListener
            }
            
            if (snapshot == null || snapshot.isEmpty) {
                Log.d(TAG, "⚠️ No today movements in snapshot")
                onUpdate(emptyList())
                return@addSnapshotListener
            }
            
            val movements = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject<LocationMovement>()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing today movement: ${e.message}")
                    null
                }
            }
            
            Log.d(TAG, "✅ Real-time today movements update: ${movements.size} movements")
            onUpdate(movements)
        }
    }
    // MARK: - Leave Management
    
    suspend fun submitLeaveRequest(
        employeeId: String,
        employeeName: String,
        leaveType: LeaveType,
        startDate: Date,
        endDate: Date,
        reason: String
    ): Result<LeaveRequest> {
        return try {
            val now = Timestamp.now()
            val request = LeaveRequest(
                employeeId = employeeId,
                employeeName = employeeName,
                leaveType = leaveType.value,
                startDate = Timestamp(startDate),
                endDate = Timestamp(endDate),
                reason = reason,
                status = LeaveStatus.PENDING.value,
                submittedAt = now
            )
            
            val docRef = db.collection(LEAVE_REQUESTS_COLLECTION)
                .add(request)
                .await()
            
            Log.d(TAG, "✅ Leave request submitted: ${docRef.id}")
            Result.success(request.copy(id = docRef.id))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Submit leave request error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getMyLeaveRequests(employeeId: String): List<LeaveRequest> {
        return try {
            val snapshot = db.collection(LEAVE_REQUESTS_COLLECTION)
                .whereEqualTo("employeeId", employeeId)
                .orderBy("submittedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.toObjects<LeaveRequest>()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching my leave requests: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun getPendingRequests(): List<LeaveRequest> {
        return try {
            val snapshot = db.collection(LEAVE_REQUESTS_COLLECTION)
                .whereEqualTo("status", LeaveStatus.PENDING.value)
                .orderBy("submittedAt", Query.Direction.ASCENDING)
                .get()
                .await()
            
            snapshot.toObjects<LeaveRequest>()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching pending requests: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun getAllLeaveRequests(limit: Long = 100): List<LeaveRequest> {
        return try {
            val snapshot = db.collection(LEAVE_REQUESTS_COLLECTION)
                .orderBy("submittedAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            
            snapshot.toObjects<LeaveRequest>()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching all leave requests: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun approveLeaveRequest(
        requestId: String,
        reviewedBy: String,
        notes: String?
    ): Result<Unit> {
        return try {
            db.collection(LEAVE_REQUESTS_COLLECTION)
                .document(requestId)
                .update(
                    mapOf(
                        "status" to LeaveStatus.APPROVED.value,
                        "reviewedBy" to reviewedBy,
                        "reviewedAt" to Timestamp.now(),
                        "reviewNotes" to (notes ?: "")
                    )
                )
                .await()
            
            Log.d(TAG, "✅ Leave request approved: $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Approve leave request error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun rejectLeaveRequest(
        requestId: String,
        reviewedBy: String,
        notes: String?
    ): Result<Unit> {
        return try {
            db.collection(LEAVE_REQUESTS_COLLECTION)
                .document(requestId)
                .update(
                    mapOf(
                        "status" to LeaveStatus.REJECTED.value,
                        "reviewedBy" to reviewedBy,
                        "reviewedAt" to Timestamp.now(),
                        "reviewNotes" to (notes ?: "")
                    )
                )
                .await()
            
            Log.d(TAG, "✅ Leave request rejected: $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Reject leave request error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getLeaveBalance(
        employeeId: String,
        year: Int = Calendar.getInstance().get(Calendar.YEAR)
    ): LeaveBalance {
        return try {
            val snapshot = db.collection(LEAVE_BALANCES_COLLECTION)
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("year", year)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents[0].toObject<LeaveBalance>() ?: createDefaultBalance(employeeId, year)
            } else {
                createDefaultBalance(employeeId, year)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching leave balance: ${e.message}", e)
            // Return default balance on error to not block UI
            LeaveBalance(employeeId = employeeId, year = year)
        }
    }
    
    private suspend fun createDefaultBalance(employeeId: String, year: Int): LeaveBalance {
        try {
            val balance = LeaveBalance(
                employeeId = employeeId,
                year = year,
                vacationTotal = 20,
                vacationUsed = 0,
                sickTotal = 10,
                sickUsed = 0,
                personalTotal = 5,
                personalUsed = 0
            )
            
            val docRef = db.collection(LEAVE_BALANCES_COLLECTION)
                .add(balance)
                .await()
            
            Log.d(TAG, "✅ Default leave balance created for $employeeId")
            return balance.copy(id = docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating default balance: ${e.message}", e)
            return LeaveBalance(employeeId = employeeId, year = year)
        }
    }
    
    suspend fun updateLeaveBalance(
        employeeId: String,
        year: Int,
        leaveType: LeaveType,
        daysUsed: Int
    ): Result<Unit> {
        return try {
            val balance = getLeaveBalance(employeeId, year)
            val balanceId = balance.id
            
            if (balanceId == null) {
                throw Exception("Balance ID not found")
            }
            
            val updates = HashMap<String, Any>()
            
            when (leaveType) {
                LeaveType.VACATION -> updates["vacationUsed"] = balance.vacationUsed + daysUsed
                LeaveType.SICK -> updates["sickUsed"] = balance.sickUsed + daysUsed
                LeaveType.PERSONAL -> updates["personalUsed"] = balance.personalUsed + daysUsed
                else -> { /* No update for other types */ }
            }
            
            if (updates.isNotEmpty()) {
                db.collection(LEAVE_BALANCES_COLLECTION)
                    .document(balanceId)
                    .update(updates)
                    .await()
            }
            
            Log.d(TAG, "✅ Leave balance updated for $employeeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update leave balance error: ${e.message}", e)
            Result.failure(e)
        }
    }


}
