package com.ats.android.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.util.Locale
import com.ats.android.R

data class Employee(
    val uid: String = "",
    @get:com.google.firebase.firestore.PropertyName("id")
    @set:com.google.firebase.firestore.PropertyName("id")
    var employeeId: String = "",
    @get:com.google.firebase.firestore.PropertyName("nameAr")
    @set:com.google.firebase.firestore.PropertyName("nameAr")
    var arabicName: String = "",
    @get:com.google.firebase.firestore.PropertyName("nameEn")
    @set:com.google.firebase.firestore.PropertyName("nameEn")
    var englishName: String = "",
    @get:com.google.firebase.firestore.PropertyName("role")
    @set:com.google.firebase.firestore.PropertyName("role")
    var roleString: String = "employee",
    val departmentEn: String = "",
    val departmentAr: String = "",
    val phoneNumber: String? = null,
    val email: String? = null,
    val isActive: Boolean = true,
    val avatarURL: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    @get:com.google.firebase.firestore.Exclude
    val id: String?
        get() = uid
    
    @get:com.google.firebase.firestore.Exclude
    val role: EmployeeRole
        get() = EmployeeRole.fromString(roleString)
    
    val displayName: String
        get() = if (Locale.getDefault().language == "ar") arabicName else englishName
    
    val team: String
        get() = if (Locale.getDefault().language == "ar") departmentAr else departmentEn
}



enum class EmployeeRole(val value: String, val labelResId: Int) {
    ADMIN("admin", R.string.admin),
    SUPERVISOR("supervisor", R.string.supervisor),
    EMPLOYEE("employee", R.string.employee);
    
    val isAdmin: Boolean get() = this == ADMIN
    val isSupervisor: Boolean get() = this == SUPERVISOR
    val isEmployee: Boolean get() = this == EMPLOYEE
    val canViewAllLocations: Boolean get() = isAdmin || isSupervisor
    
    companion object {
        fun fromString(value: String?): EmployeeRole {
            if (value == null) return EMPLOYEE
            return when (value.lowercase()) {
                "admin" -> ADMIN
                "supervisor" -> SUPERVISOR
                "employee" -> EMPLOYEE
                else -> EMPLOYEE
            }
        }
    }
    
    override fun toString(): String = value
}
