package com.ats.android.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.ats.android.R

data class ShiftConfig(
    val id: String = "default",
    val name: String = "Default Shift",
    val workDays: List<WorkDay> = listOf(
        WorkDay.MONDAY,
        WorkDay.TUESDAY,
        WorkDay.WEDNESDAY,
        WorkDay.THURSDAY,
        WorkDay.FRIDAY
    ),
    val schedules: Map<String, DaySchedule> = mapOf(
        "MONDAY" to DaySchedule(startTime = "07:00", endTime = "15:00"),
        "TUESDAY" to DaySchedule(startTime = "07:00", endTime = "15:00"),
        "WEDNESDAY" to DaySchedule(startTime = "07:00", endTime = "15:00"),
        "THURSDAY" to DaySchedule(startTime = "07:00", endTime = "15:00"),
        "FRIDAY" to DaySchedule(startTime = "07:00", endTime = "11:00")
    ),
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class DaySchedule(
    val startTime: String = "07:00", // HH:mm format
    val endTime: String = "15:00",
    val isWorkDay: Boolean = true
) {
    // Helper to check if a time is within this schedule
    fun isWithinSchedule(hour: Int, minute: Int): Boolean {
        val currentMinutes = hour * 60 + minute
        val startMinutes = startTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val endMinutes = endTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        return currentMinutes in startMinutes until endMinutes
    }
    
    fun getStartHour(): Int = startTime.split(":")[0].toInt()
    fun getStartMinute(): Int = startTime.split(":")[1].toInt()
    fun getEndHour(): Int = endTime.split(":")[0].toInt()
}

enum class WorkDay(val labelResId: Int) {
    SUNDAY(R.string.day_sunday),
    MONDAY(R.string.day_monday),
    TUESDAY(R.string.day_tuesday),
    WEDNESDAY(R.string.day_wednesday),
    THURSDAY(R.string.day_thursday),
    FRIDAY(R.string.day_friday),
    SATURDAY(R.string.day_saturday);
    
    companion object {
        fun fromCalendarDay(calendarDay: Int): WorkDay {
            return when (calendarDay) {
                java.util.Calendar.SUNDAY -> SUNDAY
                java.util.Calendar.MONDAY -> MONDAY
                java.util.Calendar.TUESDAY -> TUESDAY
                java.util.Calendar.WEDNESDAY -> WEDNESDAY
                java.util.Calendar.THURSDAY -> THURSDAY
                java.util.Calendar.FRIDAY -> FRIDAY
                java.util.Calendar.SATURDAY -> SATURDAY
                else -> MONDAY
            }
        }
    }
    
    fun toCalendarDay(): Int {
        return when (this) {
            SUNDAY -> java.util.Calendar.SUNDAY
            MONDAY -> java.util.Calendar.MONDAY
            TUESDAY -> java.util.Calendar.TUESDAY
            WEDNESDAY -> java.util.Calendar.WEDNESDAY
            THURSDAY -> java.util.Calendar.THURSDAY
            FRIDAY -> java.util.Calendar.FRIDAY
            SATURDAY -> java.util.Calendar.SATURDAY
        }
    }
}
