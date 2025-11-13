package com.ats.android.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ats.android.models.AttendanceRecord
import com.ats.android.models.Employee
import com.ats.android.services.FirestoreService
import com.ats.android.utils.LocaleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReportsViewModel : ViewModel() {
    
    private val firestoreService = FirestoreService.getInstance()
    
    private val _uiState = MutableStateFlow<ReportsUiState>(ReportsUiState.Idle)
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()
    
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()
    val allEmployees: StateFlow<List<Employee>> = _employees.asStateFlow() // Alias for UI
    
    private val _selectedEmployees = MutableStateFlow<Set<String>>(emptySet())
    val selectedEmployees: StateFlow<Set<String>> = _selectedEmployees.asStateFlow()
    
    private val _startDate = MutableStateFlow(getStartOfMonth())
    val startDate: StateFlow<Date> = _startDate.asStateFlow()
    
    private val _endDate = MutableStateFlow(Date())
    val endDate: StateFlow<Date> = _endDate.asStateFlow()
    
    private val _reportData = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val reportData: StateFlow<List<AttendanceRecord>> = _reportData.asStateFlow()
    
    init {
        loadEmployees()
    }
    
    private fun loadEmployees() {
        viewModelScope.launch {
            try {
                val allEmployees = firestoreService.getAllEmployees()
                _employees.value = allEmployees
                Log.d(TAG, "✅ Loaded ${allEmployees.size} employees for reports")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading employees: ${e.message}", e)
            }
        }
    }
    
    fun toggleEmployeeSelection(employeeId: String) {
        val current = _selectedEmployees.value.toMutableSet()
        if (current.contains(employeeId)) {
            current.remove(employeeId)
        } else {
            current.add(employeeId)
        }
        _selectedEmployees.value = current
    }
    
    fun selectAllEmployees() {
        _selectedEmployees.value = _employees.value.map { it.employeeId }.toSet()
    }
    
    fun clearSelection() {
        _selectedEmployees.value = emptySet()
    }
    
    fun clearEmployeeSelection() {
        _selectedEmployees.value = emptySet()
    }
    
    fun setDateRange(start: Date, end: Date) {
        _startDate.value = start
        _endDate.value = end
    }
    
    fun setStartDate(date: Date) {
        _startDate.value = date
    }
    
    fun setEndDate(date: Date) {
        _endDate.value = date
    }
    
    fun generateReport(preview: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.value = ReportsUiState.Generating
                Log.d(TAG, "📊 Generating report (preview=$preview)...")
                
                val employeeIds = if (_selectedEmployees.value.isEmpty()) {
                    _employees.value.map { it.employeeId }
                } else {
                    _selectedEmployees.value.toList()
                }
                
                val allRecords = mutableListOf<AttendanceRecord>()
                
                for (employeeId in employeeIds) {
                    try {
                        val records = firestoreService.getAttendanceHistory(
                            employeeId = employeeId,
                            startDate = _startDate.value,
                            endDate = _endDate.value
                        )
                        allRecords.addAll(records)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading records for $employeeId: ${e.message}")
                    }
                }
                
                _reportData.value = allRecords.sortedByDescending { it.checkInTime }
                
                if (preview) {
                    _uiState.value = ReportsUiState.Preview(allRecords.size)
                } else {
                    _uiState.value = ReportsUiState.Success(allRecords.size)
                }
                
                Log.d(TAG, "✅ Report generated with ${allRecords.size} records")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error generating report: ${e.message}", e)
                _uiState.value = ReportsUiState.Error(e.message ?: "Failed to generate report")
            }
        }
    }
    
    // Quick report generation (matching iOS)
    suspend fun generateQuickReport(days: Int) {
        val calendar = Calendar.getInstance()
        val end = calendar.time
        
        // For "Today" report (days = 0), set start to beginning of today
        val start = if (days == 0) {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        } else {
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -days)
            }.time
        }
        
        android.util.Log.d(TAG, "Quick report: days=$days, start=$start, end=$end")
        
        _startDate.value = start
        _endDate.value = end
        _selectedEmployees.value = emptySet() // All employees
        
        // Wait for report generation to complete
        withContext(Dispatchers.IO) {
            generateReport(preview = false)
            // Wait for the report to be generated
            while (_uiState.value is ReportsUiState.Generating) {
                kotlinx.coroutines.delay(100)
            }
        }
    }
    
    // Get summary statistics (matching iOS)
    fun getReportSummary(): ReportSummary {
        val records = _reportData.value
        val uniqueEmployees = records.map { it.employeeId }.distinct().size
        val totalHours = records.sumOf { it.durationHours }
        val avgHoursPerRecord = if (records.isNotEmpty()) totalHours / records.size else 0.0
        
        return ReportSummary(
            totalRecords = records.size,
            uniqueEmployees = uniqueEmployees,
            totalHours = totalHours,
            averageHoursPerRecord = avgHoursPerRecord
        )
    }
    
    fun exportAndShare(context: Context): Boolean {
        return try {
            Log.d(TAG, "📄 Exporting and sharing CSV...")
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val filename = "attendance_report_${dateFormat.format(Date())}.csv"
            val file = File(context.getExternalFilesDir(null), filename)
            
            val csvContent = buildString {
                // Header - use Arabic if app language is Arabic
                val isArabic = LocaleManager.getCurrentLanguage(context) == "ar"
                val headers = if (isArabic) {
                    "رقم الموظف,اسم الموظف,وقت تسجيل الحضور,وقت تسجيل الانصراف,موقع الحضور,موقع الانصراف,المدة (ساعات),التاريخ,الحالة"
                } else {
                    "Employee ID,Employee Name,Check-In Time,Check-Out Time,Check-In Location,Check-Out Location,Duration (hours),Date,Status"
                }
                appendLine(headers)
                
                // Data rows
                val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                
                for (record in _reportData.value) {
                    val checkInTime = dateTimeFormat.format(record.checkInTime.toDate())
                    val checkOutTime = record.checkOutTime?.let { dateTimeFormat.format(it.toDate()) } ?: ""
                    val checkInPlace = record.checkInPlaceName ?: ""
                    val checkOutPlace = record.checkOutPlaceName ?: ""
                    val duration = if (record.totalDuration != null || record.duration != null) {
                        String.format("%.2f", record.durationHours)
                    } else ""
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format((record.date ?: record.checkInTime).toDate())
                    val status = record.statusString.replace("_", " ")
                    
                    appendLine("${record.employeeId},\"${record.employeeName}\",$checkInTime,$checkOutTime,\"$checkInPlace\",\"$checkOutPlace\",$duration,$date,$status")
                }
            }
            
            file.writeText(csvContent)
            Log.d(TAG, "✅ CSV exported to: ${file.absolutePath}")
            
            // Share the file
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Export Report"))
            Log.d(TAG, "✅ Share dialog opened")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error exporting/sharing CSV: ${e.message}", e)
            false
        }
    }
    
    private fun getStartOfMonth(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    companion object {
        private const val TAG = "ReportsViewModel"
    }
}

sealed class ReportsUiState {
    object Idle : ReportsUiState()
    object Generating : ReportsUiState()
    data class Success(val recordCount: Int) : ReportsUiState()
    data class Preview(val recordCount: Int) : ReportsUiState()
    data class Error(val message: String) : ReportsUiState()
}

data class ReportSummary(
    val totalRecords: Int,
    val uniqueEmployees: Int,
    val totalHours: Double,
    val averageHoursPerRecord: Double
)
