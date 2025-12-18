package com.ats.android.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.util.Calendar
import java.util.Date

// MARK: - Leave Type

enum class LeaveType(val value: String) {
    VACATION("vacation"),
    SICK("sick"),
    PERSONAL("personal"),
    EMERGENCY("emergency"),
    UNPAID("unpaid");

    fun displayName(): String {
        return when (this) {
            VACATION -> "Vacation"
            SICK -> "Sick Leave"
            PERSONAL -> "Personal Leave"
            EMERGENCY -> "Emergency Leave"
            UNPAID -> "Unpaid Leave"
        }
    }

    fun icon(): String {
        return when (this) {
            VACATION -> "wb_sunny"
            SICK -> "local_hospital"
            PERSONAL -> "person"
            EMERGENCY -> "warning"
            UNPAID -> "money_off"
        }
    }

    companion object {
        fun fromString(value: String): LeaveType {
            return entries.find { it.value == value } ?: PERSONAL
        }
    }
}

// MARK: - Leave Status

enum class LeaveStatus(val value: String) {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    CANCELLED("cancelled");

    fun displayName(): String {
        return when (this) {
            PENDING -> "Pending"
            APPROVED -> "Approved"
            REJECTED -> "Rejected"
            CANCELLED -> "Cancelled"
        }
    }

    fun color(): String {
        return when (this) {
            PENDING -> "orange"
            APPROVED -> "green"
            REJECTED -> "red"
            CANCELLED -> "gray"
        }
    }

    companion object {
        fun fromString(value: String): LeaveStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}

// MARK: - Leave Request

data class LeaveRequest(
    @DocumentId
    val id: String? = null,
    val employeeId: String = "",
    val employeeName: String = "",
    val leaveType: String = LeaveType.PERSONAL.value,
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp = Timestamp.now(),
    val reason: String = "",
    val status: String = LeaveStatus.PENDING.value,
    val submittedAt: Timestamp = Timestamp.now(),
    val reviewedBy: String? = null,
    val reviewedAt: Timestamp? = null,
    val reviewNotes: String? = null
) {
    fun getNumberOfDays(): Int {
        val start = startDate.toDate()
        val end = endDate.toDate()
        val diffInMillis = end.time - start.time
        val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        return maxOf(diffInDays + 1, 1)
    }

    fun isPending(): Boolean = status == LeaveStatus.PENDING.value
    fun isApproved(): Boolean = status == LeaveStatus.APPROVED.value
    fun isRejected(): Boolean = status == LeaveStatus.REJECTED.value

    fun getLeaveType(): LeaveType = LeaveType.fromString(leaveType)
    fun getStatus(): LeaveStatus = LeaveStatus.fromString(status)
}

// MARK: - Leave Balance

data class LeaveBalance(
    @DocumentId
    val id: String? = null,
    val employeeId: String = "",
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val vacationTotal: Int = 0,
    val vacationUsed: Int = 0,
    val sickTotal: Int = 0,
    val sickUsed: Int = 0,
    val personalTotal: Int = 0,
    val personalUsed: Int = 0
) {
    fun vacationRemaining(): Int = maxOf(vacationTotal - vacationUsed, 0)
    fun sickRemaining(): Int = maxOf(sickTotal - sickUsed, 0)
    fun personalRemaining(): Int = maxOf(personalTotal - personalUsed, 0)

    fun remaining(type: LeaveType): Int {
        return when (type) {
            LeaveType.VACATION -> vacationRemaining()
            LeaveType.SICK -> sickRemaining()
            LeaveType.PERSONAL -> personalRemaining()
            LeaveType.EMERGENCY, LeaveType.UNPAID -> 999 // No limit
        }
    }

    fun total(type: LeaveType): Int {
        return when (type) {
            LeaveType.VACATION -> vacationTotal
            LeaveType.SICK -> sickTotal
            LeaveType.PERSONAL -> personalTotal
            LeaveType.EMERGENCY, LeaveType.UNPAID -> 999
        }
    }

    fun used(type: LeaveType): Int {
        return when (type) {
            LeaveType.VACATION -> vacationUsed
            LeaveType.SICK -> sickUsed
            LeaveType.PERSONAL -> personalUsed
            LeaveType.EMERGENCY, LeaveType.UNPAID -> 0
        }
    }
}

// MARK: - Leave Summary

data class LeaveSummary(
    val type: LeaveType,
    val total: Int,
    val used: Int,
    val remaining: Int
) {
    fun percentage(): Double {
        return if (total > 0) (used.toDouble() / total.toDouble()) * 100.0 else 0.0
    }
}
