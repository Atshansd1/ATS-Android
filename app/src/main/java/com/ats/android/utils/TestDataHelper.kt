package com.ats.android.utils

import android.util.Log
import com.ats.android.models.Employee
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

object TestDataHelper {
    private const val TAG = "TestDataHelper"
    private const val COMPANY_ID = "it-adc"
    
    suspend fun addTestEmployees(db: FirebaseFirestore = FirebaseFirestore.getInstance()): Result<String> {
        return try {
            Log.d(TAG, "🧪 Adding test employees...")
            
            val employees = listOf(
                Employee(
                    uid = "",
                    employeeId = "EMP001",
                    englishName = "John Smith",
                    arabicName = "جون سميث",
                    email = "john.smith@company.com",
                    phoneNumber = "+966501234567",
                    roleString = "admin",
                    departmentEn = "IT Department",
                    departmentAr = "قسم تقنية المعلومات",
                    isActive = true,
                    avatarURL = null,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                ),
                Employee(
                    uid = "",
                    employeeId = "EMP002",
                    englishName = "Sarah Johnson",
                    arabicName = "سارة جونسون",
                    email = "sarah.j@company.com",
                    phoneNumber = "+966502345678",
                    roleString = "supervisor",
                    departmentEn = "Operations",
                    departmentAr = "العمليات",
                    isActive = true,
                    avatarURL = null,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                ),
                Employee(
                    uid = "",
                    employeeId = "EMP003",
                    englishName = "Ahmed Ali",
                    arabicName = "أحمد علي",
                    email = "ahmed.ali@company.com",
                    phoneNumber = "+966503456789",
                    roleString = "employee",
                    departmentEn = "Sales",
                    departmentAr = "المبيعات",
                    isActive = true,
                    avatarURL = null,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                ),
                Employee(
                    uid = "",
                    employeeId = "EMP004",
                    englishName = "Mohammed Hassan",
                    arabicName = "محمد حسن",
                    email = "mohammed.h@company.com",
                    phoneNumber = "+966504567890",
                    roleString = "employee",
                    departmentEn = "Marketing",
                    departmentAr = "التسويق",
                    isActive = true,
                    avatarURL = null,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
            )
            
            employees.forEach { employee ->
                db.collection("companies/$COMPANY_ID/employees")
                    .document(employee.employeeId)
                    .set(employee)
                    .await()
                Log.d(TAG, "✅ Added employee: ${employee.englishName}")
            }
            
            Log.d(TAG, "🎉 Added ${employees.size} test employees successfully!")
            Result.success("Added ${employees.size} employees")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding test employees: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun addTestLocations(db: FirebaseFirestore = FirebaseFirestore.getInstance()): Result<String> {
        return try {
            Log.d(TAG, "🧪 Adding test active locations...")
            
            val locations = listOf(
                mapOf(
                    "employeeId" to "EMP001",
                    "location" to GeoPoint(24.7136, 46.6753), // Riyadh center
                    "placeName" to "King Fahd Road, Riyadh",
                    "previousPlaceName" to null,
                    "timestamp" to Timestamp.now(),
                    "checkInTime" to Timestamp.now()
                ),
                mapOf(
                    "employeeId" to "EMP002",
                    "location" to GeoPoint(24.7243, 46.6875), // North Riyadh
                    "placeName" to "Olaya District, Riyadh",
                    "previousPlaceName" to null,
                    "timestamp" to Timestamp.now(),
                    "checkInTime" to Timestamp.now()
                ),
                mapOf(
                    "employeeId" to "EMP003",
                    "location" to GeoPoint(24.6982, 46.6842), // South Riyadh
                    "placeName" to "Al Malqa, Riyadh",
                    "previousPlaceName" to null,
                    "timestamp" to Timestamp.now(),
                    "checkInTime" to Timestamp.now()
                )
            )
            
            locations.forEach { location ->
                val empId = location["employeeId"] as String
                db.collection("companies/$COMPANY_ID/activeLocations")
                    .document(empId)
                    .set(location)
                    .await()
                Log.d(TAG, "✅ Added location for: $empId at ${location["placeName"]}")
            }
            
            Log.d(TAG, "🎉 Added ${locations.size} test locations successfully!")
            Result.success("Added ${locations.size} locations")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding test locations: ${e.message}", e)
            Result.failure(e)
        }
    }
}
