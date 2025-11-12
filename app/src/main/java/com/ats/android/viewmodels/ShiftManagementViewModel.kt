package com.ats.android.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ats.android.models.DaySchedule
import com.ats.android.models.ShiftConfig
import com.ats.android.models.WorkDay
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShiftManagementViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    
    private val _shiftConfig = MutableStateFlow<ShiftConfig?>(null)
    val shiftConfig: StateFlow<ShiftConfig?> = _shiftConfig.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadShiftConfig() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val doc = db.collection("companies")
                    .document("it-adc")
                    .collection("settings")
                    .document("shift_config")
                    .get()
                    .await()
                
                if (doc.exists()) {
                    // Parse the document
                    val id = doc.getString("id") ?: "default"
                    val name = doc.getString("name") ?: "Default Shift"
                    val isActive = doc.getBoolean("isActive") ?: true
                    val createdAt = doc.getTimestamp("createdAt")
                    val updatedAt = doc.getTimestamp("updatedAt")
                    
                    // Parse work days
                    val workDaysList = doc.get("workDays") as? List<*>
                    val workDays = workDaysList?.mapNotNull { 
                        try {
                            WorkDay.valueOf(it.toString())
                        } catch (e: Exception) {
                            null
                        }
                    } ?: getDefaultWorkDays()
                    
                    // Parse schedules
                    val schedulesMap = doc.get("schedules") as? Map<*, *>
                    val schedules = schedulesMap?.mapNotNull { (key, value) ->
                        try {
                            val dayName = key.toString()
                            val scheduleMap = value as? Map<*, *>
                            val startTime = scheduleMap?.get("startTime")?.toString() ?: "07:00"
                            val endTime = scheduleMap?.get("endTime")?.toString() ?: "15:00"
                            val isWorkDay = scheduleMap?.get("isWorkDay") as? Boolean ?: true
                            dayName to DaySchedule(startTime, endTime, isWorkDay)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing schedule: ${e.message}")
                            null
                        }
                    }?.toMap() ?: getDefaultSchedules()
                    
                    _shiftConfig.value = ShiftConfig(
                        id = id,
                        name = name,
                        workDays = workDays,
                        schedules = schedules,
                        isActive = isActive,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                    
                    Log.d(TAG, "✅ Loaded shift config: ${workDays.size} work days")
                } else {
                    // Create default config
                    _shiftConfig.value = ShiftConfig()
                    Log.d(TAG, "📝 Created default shift config")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading shift config: ${e.message}", e)
                _errorMessage.value = "Failed to load shift configuration: ${e.message}"
                // Set default config on error
                _shiftConfig.value = ShiftConfig()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun saveShiftConfig() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val config = _shiftConfig.value ?: return@launch
                
                val data = hashMapOf<String, Any>(
                    "id" to config.id,
                    "name" to config.name,
                    "workDays" to config.workDays.map { it.name },
                    "schedules" to config.schedules.mapValues { (_, schedule) ->
                        hashMapOf(
                            "startTime" to schedule.startTime,
                            "endTime" to schedule.endTime,
                            "isWorkDay" to schedule.isWorkDay
                        )
                    },
                    "isActive" to config.isActive,
                    "updatedAt" to Timestamp.now()
                )
                
                // Add createdAt only if it doesn't exist
                if (config.createdAt == null) {
                    data["createdAt"] = Timestamp.now()
                }
                
                db.collection("companies")
                    .document("it-adc")
                    .collection("settings")
                    .document("shift_config")
                    .set(data)
                    .await()
                
                Log.d(TAG, "✅ Shift config saved successfully")
                _errorMessage.value = null
                
                // Reload to get the updated timestamp
                loadShiftConfig()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving shift config: ${e.message}", e)
                _errorMessage.value = "Failed to save: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun toggleWorkDay(day: WorkDay, enabled: Boolean) {
        val current = _shiftConfig.value ?: return
        val updatedWorkDays = if (enabled) {
            (current.workDays + day).distinct().sortedBy { it.ordinal }
        } else {
            current.workDays.filter { it != day }
        }
        
        _shiftConfig.value = current.copy(workDays = updatedWorkDays)
    }
    
    fun updateStartTime(day: WorkDay, time: String) {
        val current = _shiftConfig.value ?: return
        val currentSchedule = current.schedules[day.name] ?: DaySchedule()
        val updatedSchedule = currentSchedule.copy(startTime = time)
        
        _shiftConfig.value = current.copy(
            schedules = current.schedules + (day.name to updatedSchedule)
        )
    }
    
    fun updateEndTime(day: WorkDay, time: String) {
        val current = _shiftConfig.value ?: return
        val currentSchedule = current.schedules[day.name] ?: DaySchedule()
        val updatedSchedule = currentSchedule.copy(endTime = time)
        
        _shiftConfig.value = current.copy(
            schedules = current.schedules + (day.name to updatedSchedule)
        )
    }
    
    private fun getDefaultWorkDays(): List<WorkDay> {
        return listOf(
            WorkDay.MONDAY,
            WorkDay.TUESDAY,
            WorkDay.WEDNESDAY,
            WorkDay.THURSDAY,
            WorkDay.FRIDAY
        )
    }
    
    private fun getDefaultSchedules(): Map<String, DaySchedule> {
        return mapOf(
            "MONDAY" to DaySchedule(startTime = "07:00", endTime = "15:00"),
            "TUESDAY" to DaySchedule(startTime = "07:00", endTime = "15:00"),
            "WEDNESDAY" to DaySchedule(startTime = "07:00", endTime = "15:00"),
            "THURSDAY" to DaySchedule(startTime = "07:00", endTime = "15:00"),
            "FRIDAY" to DaySchedule(startTime = "07:00", endTime = "11:00"),
            "SATURDAY" to DaySchedule(startTime = "07:00", endTime = "15:00", isWorkDay = false),
            "SUNDAY" to DaySchedule(startTime = "07:00", endTime = "15:00", isWorkDay = false)
        )
    }
    
    companion object {
        private const val TAG = "ShiftManagementVM"
    }
}
