package com.ats.android.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.ats.android.models.AttendanceRecord
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
    fun generateExcelReport(
        context: Context,
        records: List<AttendanceRecord>,
        isArabic: Boolean = false
    ): Boolean {
        return try {
            Log.d(TAG, "📊 Creating Excel report with ${records.size} records...")
            
            if (records.isEmpty()) {
                Log.e(TAG, "❌ No records to export")
                return false
            }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            val filename = "attendance_report_${dateFormat.format(Date())}.xlsx"
            val file = File(context.getExternalFilesDir(null), filename)
            
            Log.d(TAG, "📁 File path: ${file.absolutePath}")
            
            // Create workbook
            val workbook = XSSFWorkbook()
            
            // Create styles
            val headerStyle: CellStyle = createHeaderStyle(workbook)
            val titleStyle: CellStyle = createTitleStyle(workbook)
            val dataStyle: CellStyle = createDataStyle(workbook)
            val dateStyle: CellStyle = createDateStyle(workbook)
            val statusCompleteStyle: CellStyle = createStatusStyle(workbook, isComplete = true)
            val statusActiveStyle: CellStyle = createStatusStyle(workbook, isComplete = false)
            val summaryHeaderStyle: CellStyle = createSummaryHeaderStyle(workbook)
            
            // Create Data sheet
            createDataSheet(workbook, records, headerStyle, titleStyle, dataStyle, 
                           dateStyle, statusCompleteStyle, statusActiveStyle, isArabic)
            
            // Create Summary sheet
            createSummarySheet(workbook, records, summaryHeaderStyle, dataStyle, isArabic)
            
            // Write to file
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
            
            Log.d(TAG, "✅ Excel generated successfully: ${file.absolutePath}")
            
            // Share the file
            shareFile(context, file)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating Excel report: ${e.message}", e)
            e.printStackTrace()
            false
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
                    Log.e(TAG, "Error formatting check-in time: ${e.message}")
                }
            }
            
            // Check-In Location
            row.createCell(3).apply {
                setCellValue(record.checkInPlaceName ?: "N/A")
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
                    Log.e(TAG, "Error formatting check-out time: ${e.message}")
                }
            }
            
            // Check-Out Location
            row.createCell(5).apply {
                setCellValue(record.checkOutPlaceName ?: "--")
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
        
        // Auto-size all columns for perfect fit
        try {
            for (i in 0..7) {
                sheet.autoSizeColumn(i)
                // Add padding (20% extra width)
                val currentWidth = sheet.getColumnWidth(i)
                sheet.setColumnWidth(i, (currentWidth * 1.2).toInt())
            }
            Log.d(TAG, "✅ Columns auto-sized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Auto-sizing columns failed, using default widths: ${e.message}")
            // Fallback to fixed widths
            val defaultWidths = intArrayOf(3000, 4000, 4000, 5000, 4000, 5000, 3000, 3000)
            for (i in 0..7) {
                sheet.setColumnWidth(i, defaultWidths[i])
            }
        }
        
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
        
        // Auto-size columns
        sheet.autoSizeColumn(0)
        sheet.autoSizeColumn(1)
        sheet.setColumnWidth(0, (sheet.getColumnWidth(0) * 1.3).toInt())
        sheet.setColumnWidth(1, (sheet.getColumnWidth(1) * 1.3).toInt())
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
            Log.e(TAG, "Error calculating statistics: ${e.message}")
            Statistics("0", "0", "0", "N/A", "0")
        }
    }
    
    /**
     * Create header style (blue background, white bold text)
     */
    private fun createHeaderStyle(workbook: Workbook): CellStyle {
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
    private fun createTitleStyle(workbook: Workbook): CellStyle {
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
    private fun createDataStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        
        style.alignment = HorizontalAlignment.LEFT
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
    private fun createDateStyle(workbook: Workbook): CellStyle {
        val style = createDataStyle(workbook)
        style.alignment = HorizontalAlignment.CENTER
        return style
    }
    
    /**
     * Create status style (green for complete, orange for active)
     */
    private fun createStatusStyle(workbook: Workbook, isComplete: Boolean): CellStyle {
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
    private fun createSummaryHeaderStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        
        font.bold = true
        font.fontHeightInPoints = 11
        
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.LEFT
        style.verticalAlignment = VerticalAlignment.CENTER
        
        // Borders
        style.borderTop = BorderStyle.THIN
        style.borderBottom = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        
        return style
    }
    
    /**
     * Share the Excel file
     */
    private fun shareFile(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Log.e(TAG, "❌ File does not exist: ${file.absolutePath}")
                return
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "Share Excel Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            
            Log.d(TAG, "📤 Sharing Excel file: ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sharing file: ${e.message}", e)
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
