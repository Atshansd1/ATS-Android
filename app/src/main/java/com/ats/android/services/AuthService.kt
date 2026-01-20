package com.ats.android.services

import android.util.Log
import com.ats.android.models.Employee
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class AuthService private constructor() {
    
    private val auth: FirebaseAuth = Firebase.auth
    private val firestoreService = FirestoreService.getInstance()
    
    companion object {
        private const val TAG = "AuthService"
        
        @Volatile
        private var instance: AuthService? = null
        
        fun getInstance(): AuthService {
            return instance ?: synchronized(this) {
                instance ?: AuthService().also { instance = it }
            }
        }
    }
    
    val currentUser: Employee?
        get() = _currentEmployee
    
    private var _currentEmployee: Employee? = null
    
    val isAuthenticated: Boolean
        get() = auth.currentUser != null
    
    suspend fun signIn(emailOrEmployeeId: String, password: String): Result<Employee> {
        return try {
            Log.d(TAG, "🔐 Attempting sign in for: $emailOrEmployeeId")
            
            val email = if (emailOrEmployeeId.contains("@")) {
                emailOrEmployeeId
            } else {
                "$emailOrEmployeeId@it-adc.internal"
            }
            
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("No user ID")
            
            Log.d(TAG, "✅ Firebase auth successful, loading employee data...")
            
            val employee = firestoreService.getEmployee(uid)
            _currentEmployee = employee
            
            Log.d(TAG, "✅ Loaded employee: ${employee.displayName}")
            
            Result.success(employee)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sign in error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun loadCurrentEmployee(): Employee? {
        return try {
            val uid = auth.currentUser?.uid ?: return null
            val employee = firestoreService.getEmployee(uid)
            _currentEmployee = employee
            
            // Save user role for notification logic
            com.ats.android.services.LocalNotificationManager.getInstance(com.ats.android.ATSApplication.instance)
                .setUserRole(employee.role.name)
                
            Log.d(TAG, "✅ Current employee loaded: ${employee.displayName}")
            employee
        } catch (e: Exception) {
            Log.e(TAG, "❌ Load employee error: ${e.message}", e)
            null
        }
    }
    
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No user signed in")
            val email = user.email ?: throw Exception("No email for user")
            
            // Re-authenticate first
            Log.d(TAG, "🔐 Re-authenticating before password change...")
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            
            // Update password
            Log.d(TAG, "🔐 Updating password...")
            user.updatePassword(newPassword).await()
            
            Log.d(TAG, "✅ Password updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Change password error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
        _currentEmployee = null
        Log.d(TAG, "✅ User signed out")
    }
}
