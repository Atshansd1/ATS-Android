package com.ats.android.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ats.android.models.AttendanceRecord
import com.ats.android.services.GeocodingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Professional Excel Report Generator with auto-sizing and text wrapping
 * - Compact layout with all data visible
 * - No text cutoff - columns auto-sized
 * - Professional formatting with colors
 * - Multiple sheets: Data + Summary
 */
object ExcelReportGenerator {
    
    private const val TAG = "ExcelReportGenerator"
    
    /**
     * Generate professional Excel report with auto-sized columns
     */
    suspend fun generateExcelReport(
        context: Context,
        records: List<AttendanceRecord>,
        isArabic: Boolean = false
    ): Boolean {
        var workbook: XSSFWorkbook? = null
        var outputStream: FileOutputStream? = null
        
        return try {
            DebugLogger.d(TAG, "📊 Creating Excel report with ${records.size} records...")
            
            if (records.isEmpty()) {
                DebugLogger.e(TAG, "❌ No records to export")
                return false
            }
            
            // Verify external storage is available
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir == null) {
                DebugLogger.e(TAG, "❌ External storage not available")
                return false
            }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            val filename = "attendance_report_${dateFormat.format(Date())}.xlsx"
            val file = File(externalDir, filename)
            
            DebugLogger.d(TAG, "📁 File path: ${file.absolutePath}")
            DebugLogger.d(TAG, "📁 Parent dir exists: ${file.parentFile?.exists()}")
            DebugLogger.d(TAG, "📁 Parent dir writable: ${file.parentFile?.canWrite()}")
            
            // Create workbook
            DebugLogger.d(TAG, "Creating workbook...")
            workbook = XSSFWorkbook()
            DebugLogger.d(TAG, "✅ Workbook created (${workbook.numberOfSheets} sheets)")
            
            // Create styles
            DebugLogger.d(TAG, "Creating styles...")
            val headerStyle: CellStyle = createHeaderStyle(workbook, isArabic)
            val titleStyle: CellStyle = createTitleStyle(workbook, isArabic)
            val dataStyle: CellStyle = createDataStyle(workbook, isArabic)
            val dateStyle: CellStyle = createDateStyle(workbook, isArabic)
            val statusCompleteStyle: CellStyle = createStatusStyle(workbook, isComplete = true, isArabic)
            val statusActiveStyle: CellStyle = createStatusStyle(workbook, isComplete = false, isArabic)
            val summaryHeaderStyle: CellStyle = createSummaryHeaderStyle(workbook, isArabic)
            DebugLogger.d(TAG, "✅ Styles created")
            
            // Re-geocode place names if Arabic
            val enrichedRecords = if (isArabic) {
                DebugLogger.d(TAG, "🔄 Re-geocoding place names to Arabic...")
                withContext(Dispatchers.IO) {
                    reGeocodeToArabic(context, records)
                }
            } else {
                records
            }
            
            // Create Data sheet
            DebugLogger.d(TAG, "Creating data sheet...")
            createDataSheet(workbook, enrichedRecords, headerStyle, titleStyle, dataStyle, 
                           dateStyle, statusCompleteStyle, statusActiveStyle, isArabic)
            DebugLogger.d(TAG, "✅ Data sheet created")
            
            // Create Summary sheet
            DebugLogger.d(TAG, "Creating summary sheet...")
            createSummarySheet(workbook, records, summaryHeaderStyle, dataStyle, isArabic)
            DebugLogger.d(TAG, "✅ Summary sheet created")
            
            // Write to file
            DebugLogger.d(TAG, "Writing to file: ${file.absolutePath}")
            DebugLogger.d(TAG, "Workbook sheets: ${workbook.numberOfSheets}")
            
            outputStream = FileOutputStream(file)
            DebugLogger.d(TAG, "FileOutputStream created")
            
            workbook.write(outputStream)
            DebugLogger.d(TAG, "Workbook written to stream")
            
            outputStream.flush()
            DebugLogger.d(TAG, "Stream flushed")
            
            outputStream.close()
            outputStream = null
            DebugLogger.d(TAG, "✅ File written successfully")
            
            workbook.close()
            workbook = null
            DebugLogger.d(TAG, "✅ Workbook closed")
            
            DebugLogger.d(TAG, "✅ Excel generated successfully: ${file.absolutePath}")
            DebugLogger.d(TAG, "File size: ${file.length()} bytes")
            
            // Share the file
            DebugLogger.d(TAG, "Sharing file...")
            shareFile(context, file)
            DebugLogger.d(TAG, "✅ Share intent sent")
            
            true
        } catch (e: Exception) {
            DebugLogger.e(TAG, "❌ Error generating Excel report: ${e.message}", e)
            DebugLogger.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
            DebugLogger.e(TAG, "❌ Stack trace:")
            e.printStackTrace()
            
            // Log specific error details
            when (e) {
                is java.io.IOException -> DebugLogger.e(TAG, "❌ IO Error - possibly storage permissions or disk space")
                is SecurityException -> DebugLogger.e(TAG, "❌ Security Error - storage permissions required")
                is OutOfMemoryError -> DebugLogger.e(TAG, "❌ Out of Memory - too many records")
                is IllegalArgumentException -> DebugLogger.e(TAG, "❌ Illegal Argument - invalid data")
                is NullPointerException -> DebugLogger.e(TAG, "❌ Null Pointer - missing data")
                else -> DebugLogger.e(TAG, "❌ Unexpected error: ${e::class.java.name}")
            }
            
            false
        } finally {
            // Cleanup resources
            try {
                outputStream?.close()
                workbook?.close()
                DebugLogger.d(TAG, "Resources cleaned up")
            } catch (e: Exception) {
                DebugLogger.e(TAG, "Error cleaning up resources: ${e.message}")
            }
        }
    }
    
    /**
     * Re-geocode place names to Arabic
     */
    private suspend fun reGeocodeToArabic(
        context: Context,
        records: List<AttendanceRecord>
    ): List<AttendanceRecord> {
        val geocodingService = GeocodingService(context)
        
        return records.map { record ->
            var modified = record
            
            // Re-geocode check-in location
            if (record.checkInLocation != null) {
                try {
                    val arabicCheckInPlace = geocodingService.getPlaceName(
                        record.checkInLocation!!.latitude,
                        record.checkInLocation!!.longitude
                    )
                    if (arabicCheckInPlace != null) {
                        modified = modified.copy(checkInPlaceName = arabicCheckInPlace)
                        DebugLogger.d(TAG, "✅ Check-in place: $arabicCheckInPlace")
                    }
                } catch (e: Exception) {
                    DebugLogger.w(TAG, "Failed to re-geocode check-in: ${e.message}")
                }
            }
            
            // Re-geocode check-out location
            if (record.checkOutLocation != null) {
                try {
                    val arabicCheckOutPlace = geocodingService.getPlaceName(
                        record.checkOutLocation!!.latitude,
                        record.checkOutLocation!!.longitude
                    )
                    if (arabicCheckOutPlace != null) {
                        modified = modified.copy(checkOutPlaceName = arabicCheckOutPlace)
                        DebugLogger.d(TAG, "✅ Check-out place: $arabicCheckOutPlace")
                    }
                } catch (e: Exception) {
                    DebugLogger.w(TAG, "Failed to re-geocode check-out: ${e.message}")
                }
            }
            
            modified
        }
    }
    
    /**
     * Create data sheet with all attendance records
     */
    private fun createDataSheet(
        workbook: Workbook,
        records: List<AttendanceRecord>,
        headerStyle: CellStyle,
        titleStyle: CellStyle,
        dataStyle: CellStyle,
        dateStyle: CellStyle,
        statusCompleteStyle: CellStyle,
        statusActiveStyle: CellStyle,
        isArabic: Boolean
    ) {
        val sheet = workbook.createSheet(if (isArabic) "البيانات" else "Attendance Data")
        
        // Enable RTL for Arabic
        if (isArabic) {
            sheet.isRightToLeft = true
            DebugLogger.d(TAG, "✅ RTL enabled for Arabic data sheet")
        }
        
        var rowNum = 0
        
        // Title
        val titleRow = sheet.createRow(rowNum++)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue(if (isArabic) "تقرير الحضور والانصراف" else "Attendance Report")
        titleCell.cellStyle = titleStyle
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 7))
        
        // Date
        val dateRow = sheet.createRow(rowNum++)
        val dateCell = dateRow.createCell(0)
        val dateStr = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.US).format(Date())
        dateCell.setCellValue(if (isArabic) "تاريخ التقرير: $dateStr" else "Report Date: $dateStr")
        dateCell.cellStyle = dataStyle
        sheet.addMergedRegion(CellRangeAddress(1, 1, 0, 7))
        
        rowNum++ // Empty row
        
        // Headers
        val headerRow = sheet.createRow(rowNum++)
        val headers = if (isArabic) {
            arrayOf("رقم الموظف", "اسم الموظف", "وقت الحضور", "موقع الحضور", 
                   "وقت الانصراف", "موقع الانصراف", "المدة", "الحالة")
        } else {
            arrayOf("Employee ID", "Employee Name", "Check-In Time", "Check-In Location",
                   "Check-Out Time", "Check-Out Location", "Duration", "Status")
        }
        
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        
        // Data rows
        val dateTimeFormat = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US)
        
        records.forEach { record ->
            val row = sheet.createRow(rowNum++)
            
            // Employee ID
            row.createCell(0).apply {
                setCellValue(record.employeeId ?: "N/A")
                cellStyle = dataStyle
            }
            
            // Employee Name
            row.createCell(1).apply {
                setCellValue(record.employeeName ?: "N/A")
                cellStyle = dataStyle
            }
            
            // Check-In Time
            row.createCell(2).apply {
                try {
                    setCellValue(dateTimeFormat.format(record.checkInTime.toDate()))
                    cellStyle = dateStyle
                } catch (e: Exception) {
                    setCellValue("N/A")
                    cellStyle = dataStyle
                    DebugLogger.e(TAG, "Error formatting check-in time: ${e.message}")
                }
            }
            
            // Check-In Location
            row.createCell(3).apply {
                setCellValue(record.getLocalizedCheckInPlaceName(isArabic).ifEmpty { "N/A" })
                cellStyle = dataStyle
            }
            
            // Check-Out Time
            row.createCell(4).apply {
                try {
                    if (record.checkOutTime != null) {
                        setCellValue(dateTimeFormat.format(record.checkOutTime!!.toDate()))
                        cellStyle = dateStyle
                    } else {
                        setCellValue("--")
                        cellStyle = dataStyle
                    }
                } catch (e: Exception) {
                    setCellValue("--")
                    cellStyle = dataStyle
                    DebugLogger.e(TAG, "Error formatting check-out time: ${e.message}")
                }
            }
            
            // Check-Out Location
            row.createCell(5).apply {
                setCellValue(record.getLocalizedCheckOutPlaceName(isArabic).ifEmpty { "--" })
                cellStyle = dataStyle
            }
            
            // Duration
            row.createCell(6).apply {
                if (record.totalDuration != null && record.totalDuration!! > 0) {
                    val hours = (record.totalDuration!! / 3600).toInt()
                    val minutes = ((record.totalDuration!! % 3600) / 60).toInt()
                    val durationText = if (isArabic) {
                        "${hours}س ${minutes}د"
                    } else {
                        "${hours}h ${minutes}m"
                    }
                    setCellValue(durationText)
                } else {
                    setCellValue("--")
                }
                cellStyle = dataStyle
            }
            
            // Status
            row.createCell(7).apply {
                val status = when (record.statusString) {
                    "checked_out" -> if (isArabic) "منصرف" else "Complete"
                    else -> if (isArabic) "حاضر" else "Active"
                }
                setCellValue(status)
                cellStyle = if (record.statusString == "checked_out") 
                    statusCompleteStyle else statusActiveStyle
            }
        }
        
        // Set fixed column widths (autoSizeColumn requires AWT which is not available on Android)
        // Widths are in 1/256th of a character width
        val columnWidths = intArrayOf(
            3000,  // Employee ID
            5000,  // Employee Name
            4500,  // Check-In Time
            6000,  // Check-In Location
            4500,  // Check-Out Time
            6000,  // Check-Out Location
            3500,  // Duration
            3500   // Status
        )
        
        for (i in 0..7) {
            sheet.setColumnWidth(i, columnWidths[i])
        }
        DebugLogger.d(TAG, "✅ Columns sized with fixed widths")
        
        // Freeze header rows
        sheet.createFreezePane(0, 4)
    }
    
    /**
     * Create summary sheet with statistics
     */
    private fun createSummarySheet(
        workbook: Workbook,
        records: List<AttendanceRecord>,
        headerStyle: CellStyle,
        dataStyle: CellStyle,
        isArabic: Boolean
    ) {
        val sheet = workbook.createSheet(if (isArabic) "الملخص" else "Summary")
        
        // Enable RTL for Arabic
        if (isArabic) {
            sheet.isRightToLeft = true
            DebugLogger.d(TAG, "✅ RTL enabled for Arabic summary sheet")
        }
        
        var rowNum = 0
        
        // Title
        val titleRow = sheet.createRow(rowNum++)
        titleRow.createCell(0).apply {
            setCellValue(if (isArabic) "ملخص التقرير" else "Report Summary")
            cellStyle = headerStyle
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 1))
        
        rowNum++ // Empty row
        
        // Statistics
        val stats = calculateStatistics(records)
        
        val statsData = if (isArabic) {
            listOf(
                "إجمالي السجلات" to stats.totalRecords,
                "منصرف" to stats.completedCount,
                "حاضر" to stats.activeCount,
                "متوسط المدة" to stats.avgDuration,
                "إجمالي الموظفين" to stats.uniqueEmployees
            )
        } else {
            listOf(
                "Total Records" to stats.totalRecords,
                "Completed" to stats.completedCount,
                "Active" to stats.activeCount,
                "Average Duration" to stats.avgDuration,
                "Unique Employees" to stats.uniqueEmployees
            )
        }
        
        statsData.forEach { (label, value) ->
            val row = sheet.createRow(rowNum++)
            row.createCell(0).apply {
                setCellValue(label)
                cellStyle = headerStyle
            }
            row.createCell(1).apply {
                setCellValue(value)
                cellStyle = dataStyle
            }
        }
        
        // Set fixed column widths (autoSizeColumn not available on Android)
        sheet.setColumnWidth(0, 6000)  // Label column
        sheet.setColumnWidth(1, 4000)  // Value column
    }
    
    /**
     * Calculate summary statistics
     */
    private fun calculateStatistics(records: List<AttendanceRecord>): Statistics {
        return try {
            val total = records.size
            val completed = records.count { it.statusString == "checked_out" }
            val active = total - completed
            
            val durations = records.mapNotNull { it.totalDuration }.filter { it > 0 }
            val avgDuration = if (durations.isNotEmpty()) {
                val avgSeconds = durations.average()
                val hours = (avgSeconds / 3600).toInt()
                val minutes = ((avgSeconds % 3600) / 60).toInt()
                "${hours}h ${minutes}m"
            } else {
                "N/A"
            }
            
            val uniqueEmployees = records.mapNotNull { it.employeeId }.distinct().size
            
            Statistics(
                totalRecords = total.toString(),
                completedCount = completed.toString(),
                activeCount = active.toString(),
                avgDuration = avgDuration,
                uniqueEmployees = uniqueEmployees.toString()
            )
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error calculating statistics: ${e.message}")
            Statistics("0", "0", "0", "N/A", "0")
        }
    }
    
    /**
     * Create header style (blue background, white bold text)
     */
    private fun createHeaderStyle(workbook: Workbook, isArabic: Boolean = false): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        
        font.bold = true
        font.color = IndexedColors.WHITE.index
        font.fontHeightInPoints = 11
        
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.DARK_BLUE.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER
        style.wrapText = true
        
        // Borders
        style.borderTop = BorderStyle.THIN
        style.borderBottom = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        
        return style
    }
    
    /**
     * Create title style
     */
    private fun createTitleStyle(workbook: Workbook, isArabic: Boolean = false): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        
        font.bold = true
        font.fontHeightInPoints = 16
        font.color = IndexedColors.DARK_BLUE.index
        
        style.setFont(font)
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER
        
        return style
    }
    
    /**
     * Create data style with borders and text wrapping
     */
    private fun createDataStyle(workbook: Workbook, isArabic: Boolean = false): CellStyle {
        val style = workbook.createCellStyle()
        
        // RTL alignment for Arabic
        style.alignment = if (isArabic) HorizontalAlignment.RIGHT else HorizontalAlignment.LEFT
        style.verticalAlignment = VerticalAlignment.CENTER
        style.wrapText = true
        
        // Borders
        style.borderTop = BorderStyle.THIN
        style.borderBottom = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        style.topBorderColor = IndexedColors.GREY_25_PERCENT.index
        style.bottomBorderColor = IndexedColors.GREY_25_PERCENT.index
        style.leftBorderColor = IndexedColors.GREY_25_PERCENT.index
        style.rightBorderColor = IndexedColors.GREY_25_PERCENT.index
        
        return style
    }
    
    /**
     * Create date/time style
     */
    private fun createDateStyle(workbook: Workbook, isArabic: Boolean = false): CellStyle {
        val style = createDataStyle(workbook, isArabic)
        style.alignment = HorizontalAlignment.CENTER
        return style
    }
    
    /**
     * Create status style (green for complete, orange for active)
     */
    private fun createStatusStyle(workbook: Workbook, isComplete: Boolean, isArabic: Boolean = false): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        
        font.bold = true
        font.color = IndexedColors.WHITE.index
        
        style.setFont(font)
        style.fillForegroundColor = if (isComplete) 
            IndexedColors.GREEN.index else IndexedColors.ORANGE.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER
        
        // Borders
        style.borderTop = BorderStyle.THIN
        style.borderBottom = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        
        return style
    }
    
    /**
     * Create summary header style (yellow background)
     */
    private fun createSummaryHeaderStyle(workbook: Workbook, isArabic: Boolean = false): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        
        font.bold = true
        font.fontHeightInPoints = 11
        
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        // RTL alignment for Arabic
        style.alignment = if (isArabic) HorizontalAlignment.RIGHT else HorizontalAlignment.LEFT
        style.verticalAlignment = VerticalAlignment.CENTER
        
        // Borders
        style.borderTop = BorderStyle.THIN
        style.borderBottom = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        
        return style
    }
    
    /**
     * Share the Excel file with option to save to internal storage
     */
    private fun shareFile(context: Context, file: File) {
        try {
            if (!file.exists()) {
                DebugLogger.e(TAG, "❌ File does not exist: ${file.absolutePath}")
                return
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            // Create multiple intent options
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Create chooser with both share and view/save options
            val chooser = Intent.createChooser(shareIntent, "Excel Report - Share or Save")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(chooser)
            
            DebugLogger.d(TAG, "📤 Showing Excel file options: ${file.name}")
            DebugLogger.d(TAG, "📁 File location: ${file.absolutePath}")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "❌ Error sharing file: ${e.message}", e)
        }
    }
    
    /**
     * Statistics data class
     */
    private data class Statistics(
        val totalRecords: String,
        val completedCount: String,
        val activeCount: String,
        val avgDuration: String,
        val uniqueEmployees: String
    )
}
