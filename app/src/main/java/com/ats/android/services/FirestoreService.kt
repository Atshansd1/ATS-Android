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

class FirestoreService private constructor() {
    
    private val db: FirebaseFirestore = Firebase.firestore
    
    companion object {
        private const val TAG = "FirestoreService"
        private const val COMPANY_ID = "it-adc"
        private const val EMPLOYEES_COLLECTION = "companies/$COMPANY_ID/employees"
        private const val ATTENDANCE_COLLECTION = "companies/$COMPANY_ID/attendance"
        private const val ACTIVE_LOCATIONS_COLLECTION = "companies/$COMPANY_ID/activeLocations"
        private const val SHIFT_CONFIGS_COLLECTION = "companies/$COMPANY_ID/shiftConfigs"
        private const val CHECKIN_LOCATION_CONFIGS_COLLECTION = "companies/$COMPANY_ID/checkInLocationConfigs"
        private const val MOVEMENTS_COLLECTION = "companies/$COMPANY_ID/movements"
        
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
    
    suspend fun getAttendanceHistory(
        employeeId: String,
        startDate: Date,
        endDate: Date
    ): List<AttendanceRecord> {
        Log.d(TAG, "📥 Fetching attendance history for $employeeId (iOS-compatible query)")
        // Use checkInTime for filtering (like iOS) instead of date field
        // This uses the existing employeeId + checkInTime index that already exists
        val snapshot = db.collection(ATTENDANCE_COLLECTION)
            .whereEqualTo("employeeId", employeeId)
            .whereGreaterThanOrEqualTo("checkInTime", Timestamp(startDate))
            .whereLessThanOrEqualTo("checkInTime", Timestamp(endDate))
            .orderBy("checkInTime", Query.Direction.DESCENDING)
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
        
        Log.d(TAG, "✅ Fetched ${records.size} attendance records")
        return records
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
    private suspend fun updateActiveLocation(
        employeeId: String,
        location: GeoPoint,
        placeName: String?
    ) {
        try {
            val now = Timestamp.now()
            
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
                "checkInTime" to now,
                "isActive" to true,
                "placeName" to (placeName ?: "Unknown Location")
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
                        location = geoPoint,
                        timestamp = document.getTimestamp("timestamp") ?: Timestamp.now(),
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
                .whereEqualTo("employeeId", employeeId)
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
    
    // Real-time active locations observer (matching iOS getActiveLocations)
    fun observeActiveLocations(onUpdate: (List<Pair<Employee, ActiveLocation>>) -> Unit) {
        Log.d(TAG, "📍 Starting real-time location observer on: $ATTENDANCE_COLLECTION (status=checked_in)")
        
        // FIX: Query attendance collection for checked-in employees, then get their locations
        // This is more accurate than relying on isActive flag which may not be synced properly
        db.collection(ATTENDANCE_COLLECTION)
            .whereEqualTo("status", "checked_in")
            .addSnapshotListener { attendanceSnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Attendance listener error: ${error.message}", error)
                    return@addSnapshotListener
                }
                
                if (attendanceSnapshot == null || attendanceSnapshot.isEmpty) {
                    Log.d(TAG, "⚠️ No checked-in employees in attendance collection")
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                
                Log.d(TAG, "📍 Found ${attendanceSnapshot.size()} checked-in employees in attendance")
                
                // Get unique employeeIds (deduplicate in case of multiple check-in records)
                val employeeIds = attendanceSnapshot.documents.mapNotNull { doc ->
                    doc.getString("employeeId")
                }.distinct().also { ids ->
                    Log.d(TAG, "📍 Checked-in employees (unique): ${ids.size} - $ids")
                    if (attendanceSnapshot.size() > ids.size) {
                        Log.w(TAG, "⚠️ Found ${attendanceSnapshot.size() - ids.size} duplicate check-in records")
                    }
                }
                
                if (employeeIds.isEmpty()) {
                    Log.d(TAG, "⚠️ No employeeIds found in attendance documents")
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                
                // Fetch employees and locations in parallel using Tasks
                val employeesFuture = db.collection(EMPLOYEES_COLLECTION).get()
                val locationFutures = employeeIds.map { id ->
                    db.collection(ACTIVE_LOCATIONS_COLLECTION).document(id).get()
                }
                
                // Wait for all employee documents
                employeesFuture.addOnSuccessListener { employeeDocs ->
                    Log.d(TAG, "📥 Fetched ${employeeDocs.size()} employee documents")
                    
                    val employees = employeeDocs.documents.mapNotNull { doc ->
                        try {
                            doc.toObject<Employee>()
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse employee ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    
                    // Wait for all location documents
                    val allLocationTasks = com.google.android.gms.tasks.Tasks.whenAllSuccess<com.google.firebase.firestore.DocumentSnapshot>(locationFutures)
                    allLocationTasks.addOnSuccessListener { locationDocs ->
                        val locations = mutableListOf<Pair<Employee, ActiveLocation>>()
                        
                        locationDocs.forEach { doc ->
                            try {
                                if (!doc.exists()) {
                                    Log.w(TAG, "⚠️ Location doc doesn't exist: ${doc.id}")
                                    return@forEach
                                }
                                
                                val employeeId = doc.id // Document ID is the employeeId
                                
                                val locationData = doc.get("location")
                                val geoPoint = when (locationData) {
                                    is com.google.firebase.firestore.GeoPoint -> locationData
                                    is Map<*, *> -> {
                                        val lat = (locationData["latitude"] as? Number)?.toDouble() ?: 0.0
                                        val lng = (locationData["longitude"] as? Number)?.toDouble() ?: 0.0
                                        com.google.firebase.firestore.GeoPoint(lat, lng)
                                    }
                                    else -> {
                                        Log.w(TAG, "Unknown location data type: ${locationData?.javaClass}")
                                        return@forEach
                                    }
                                }
                                
                                val activeLocation = ActiveLocation(
                                    employeeId = employeeId,
                                    location = geoPoint,
                                    timestamp = doc.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now(),
                                    checkInTime = doc.getTimestamp("checkInTime") ?: com.google.firebase.Timestamp.now(),
                                    isActive = true, // They're checked-in, so they're active
                                    placeName = doc.getString("placeName"),
                                    previousPlaceName = doc.getString("previousPlaceName"),
                                    batteryLevel = doc.getDouble("batteryLevel"),
                                    speed = doc.getDouble("speed"),
                                    accuracy = doc.getDouble("accuracy")
                                )
                                
                                // Find employee by employeeId (Firestore field "id" maps to property employeeId)
                                val employee = employees.find { emp ->
                                    emp.employeeId == employeeId
                                }
                                if (employee != null) {
                                    Log.d(TAG, "✅ Matched: ${employee.displayName} ($employeeId)")
                                    locations.add(Pair(employee, activeLocation))
                                } else {
                                    Log.w(TAG, "⚠️ No employee found for: $employeeId")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing location: ${e.message}", e)
                            }
                        }
                        
                        Log.d(TAG, "✅ Real-time update: ${locations.size} active locations with employee data")
                        onUpdate(locations)
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "❌ Error fetching locations: ${e.message}", e)
                        onUpdate(emptyList())
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error fetching employees: ${e.message}", e)
                    onUpdate(emptyList())
                }
            }
    }
    
    // Employee Management operations
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
            Log.d(TAG, "💾 Saving location movement: ${movement.movementType.displayName}")
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
}
