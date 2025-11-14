package com.ats.android.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.core.content.FileProvider
import com.ats.android.models.AttendanceRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Professional PDF Report Generator using Android's native PDF API
 * - No external dependencies
 * - Professional formatting with colors and styling
 * - Summary statistics and data tables
 * - Landscape orientation for better data display
 */
object PdfReportGenerator {
    
    private const val TAG = "PdfReportGenerator"
    
    // Page dimensions (A4 Landscape: 842 x 595 points)
    private const val PAGE_WIDTH = 842
    private const val PAGE_HEIGHT = 595
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 20f
    
    // Colors
    private val COLOR_PRIMARY = Color.rgb(33, 150, 243) // Blue
    private val COLOR_SUCCESS = Color.rgb(76, 175, 80)  // Green
    private val COLOR_WARNING = Color.rgb(255, 152, 0)  // Orange
    private val COLOR_TEXT = Color.rgb(33, 33, 33)      // Dark gray
    private val COLOR_LIGHT_GRAY = Color.rgb(245, 245, 245)
    private val COLOR_BORDER = Color.rgb(224, 224, 224)
    
    /**
     * Generate professional PDF report with summary and data table
     */
    fun generatePdfReport(
        context: Context,
        records: List<AttendanceRecord>,
        isArabic: Boolean = false
    ): Boolean {
        return try {
            Log.d(TAG, "📄 Generating PDF report with ${records.size} records...")
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            val filename = "attendance_report_${dateFormat.format(Date())}.pdf"
            val file = File(context.getExternalFilesDir(null), filename)
            
            // Create PDF document
            val document = PdfDocument()
            
            // Calculate pages needed (approximately 15 records per page)
            val recordsPerPage = 15
            val totalPages = (records.size + recordsPerPage - 1) / recordsPerPage
            
            records.chunked(recordsPerPage).forEachIndexed { pageIndex, pageRecords ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                
                var yPosition = MARGIN
                
                // Draw header (only on first page)
                if (pageIndex == 0) {
                    yPosition = drawHeader(canvas, isArabic)
                    yPosition += 20f
                    yPosition = drawSummary(canvas, records, yPosition, isArabic)
                    yPosition += 30f
                } else {
                    yPosition = drawPageHeader(canvas, isArabic)
                    yPosition += 20f
                }
                
                // Draw table
                yPosition = drawTable(canvas, pageRecords, yPosition, isArabic)
                
                // Draw footer
                drawFooter(canvas, pageIndex + 1, totalPages)
                
                document.finishPage(page)
            }
            
            // Write to file
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()
            
            Log.d(TAG, "✅ PDF generated successfully: ${file.absolutePath}")
            
            // Share the file
            shareFile(context, file)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating PDF report", e)
            false
        }
    }
    
    /**
     * Draw main header with title and date
     */
    private fun drawHeader(canvas: Canvas, isArabic: Boolean): Float {
        var y = MARGIN
        
        // Title background
        val titlePaint = Paint().apply {
            color = COLOR_PRIMARY
            style = Paint.Style.FILL
        }
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 50f, titlePaint)
        
        // Title text
        val titleTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val title = if (isArabic) "تقرير الحضور والانصراف" else "Attendance Report"
        canvas.drawText(title, PAGE_WIDTH / 2f, y + 35f, titleTextPaint)
        
        y += 60f
        
        // Date and info
        val infoPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 12f
        }
        val dateStr = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.US).format(Date())
        val dateText = if (isArabic) "تاريخ التقرير: $dateStr" else "Report Date: $dateStr"
        canvas.drawText(dateText, MARGIN, y, infoPaint)
        
        y += 30f
        return y
    }
    
    /**
     * Draw page header for subsequent pages
     */
    private fun drawPageHeader(canvas: Canvas, isArabic: Boolean): Float {
        val headerPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val title = if (isArabic) "تقرير الحضور والانصراف" else "Attendance Report"
        canvas.drawText(title, MARGIN, MARGIN + 20f, headerPaint)
        
        return MARGIN + 40f
    }
    
    /**
     * Draw summary statistics box
     */
    private fun drawSummary(canvas: Canvas, records: List<AttendanceRecord>, y: Float, isArabic: Boolean): Float {
        var yPos = y
        
        // Calculate statistics
        val totalRecords = records.size
        val checkedOut = records.count { it.statusString == "checked_out" }
        val checkedIn = totalRecords - checkedOut
        
        // Summary box background
        val boxPaint = Paint().apply {
            color = COLOR_LIGHT_GRAY
            style = Paint.Style.FILL
        }
        canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 60f, boxPaint)
        
        // Border
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 60f, borderPaint)
        
        yPos += 20f
        
        // Summary title
        val titlePaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val summaryTitle = if (isArabic) "ملخص التقرير" else "Summary"
        canvas.drawText(summaryTitle, MARGIN + 10f, yPos, titlePaint)
        
        yPos += 25f
        
        // Statistics
        val statPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 12f
        }
        
        val col1X = MARGIN + 20f
        val col2X = MARGIN + 200f
        val col3X = MARGIN + 400f
        
        if (isArabic) {
            canvas.drawText("إجمالي السجلات: $totalRecords", col1X, yPos, statPaint)
            canvas.drawText("منصرف: $checkedOut", col2X, yPos, statPaint)
            canvas.drawText("حاضر: $checkedIn", col3X, yPos, statPaint)
        } else {
            canvas.drawText("Total Records: $totalRecords", col1X, yPos, statPaint)
            canvas.drawText("Completed: $checkedOut", col2X, yPos, statPaint)
            canvas.drawText("Active: $checkedIn", col3X, yPos, statPaint)
        }
        
        return yPos + 30f
    }
    
    /**
     * Draw data table with headers and rows
     */
    private fun drawTable(canvas: Canvas, records: List<AttendanceRecord>, startY: Float, isArabic: Boolean): Float {
        var y = startY
        
        // Column widths
        val colWidths = floatArrayOf(80f, 120f, 100f, 100f, 120f, 120f, 100f)
        val colX = FloatArray(colWidths.size + 1)
        colX[0] = MARGIN
        for (i in 1..colWidths.size) {
            colX[i] = colX[i - 1] + colWidths[i - 1]
        }
        
        // Headers
        val headers = if (isArabic) {
            arrayOf("رقم الموظف", "اسم الموظف", "وقت الحضور", "موقع الحضور", "وقت الانصراف", "موقع الانصراف", "الحالة")
        } else {
            arrayOf("ID", "Employee", "Check-In", "Check-In Location", "Check-Out", "Check-Out Location", "Status")
        }
        
        // Draw header background
        val headerBgPaint = Paint().apply {
            color = COLOR_PRIMARY
            style = Paint.Style.FILL
        }
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 25f, headerBgPaint)
        
        // Draw header text
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        for (i in headers.indices) {
            canvas.drawText(headers[i], colX[i] + 5f, y + 17f, headerTextPaint)
        }
        
        y += 25f
        
        // Data rows
        val dataPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 9f
        }
        
        val dateTimeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.US)
        val dateOnlyFormat = SimpleDateFormat("MM/dd", Locale.US)
        
        records.forEachIndexed { index, record ->
            // Alternating row colors
            if (index % 2 == 0) {
                val rowBgPaint = Paint().apply {
                    color = COLOR_LIGHT_GRAY
                    style = Paint.Style.FILL
                }
                canvas.drawRect(MARGIN, y - 15f, PAGE_WIDTH - MARGIN, y + 10f, rowBgPaint)
            }
            
            // Draw row border
            val borderPaint = Paint().apply {
                color = COLOR_BORDER
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }
            canvas.drawRect(MARGIN, y - 15f, PAGE_WIDTH - MARGIN, y + 10f, borderPaint)
            
            // Draw data
            canvas.drawText(truncate(record.employeeId, 10), colX[0] + 5f, y, dataPaint)
            canvas.drawText(truncate(record.employeeName ?: "N/A", 15), colX[1] + 5f, y, dataPaint)
            canvas.drawText(dateTimeFormat.format(record.checkInTime.toDate()), colX[2] + 5f, y, dataPaint)
            canvas.drawText(truncate(record.checkInPlaceName ?: "N/A", 15), colX[3] + 5f, y, dataPaint)
            
            if (record.checkOutTime != null) {
                canvas.drawText(dateTimeFormat.format(record.checkOutTime!!.toDate()), colX[4] + 5f, y, dataPaint)
            } else {
                canvas.drawText("--", colX[4] + 5f, y, dataPaint)
            }
            
            canvas.drawText(truncate(record.checkOutPlaceName ?: "--", 15), colX[5] + 5f, y, dataPaint)
            
            // Status with color
            val status = when (record.statusString) {
                "checked_out" -> if (isArabic) "منصرف" else "Complete"
                else -> if (isArabic) "حاضر" else "Active"
            }
            val statusColor = if (record.statusString == "checked_out") COLOR_SUCCESS else COLOR_WARNING
            val statusPaint = Paint().apply {
                color = statusColor
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(status, colX[6] + 5f, y, statusPaint)
            
            y += 25f
        }
        
        return y
    }
    
    /**
     * Draw footer with page number
     */
    private fun drawFooter(canvas: Canvas, pageNum: Int, totalPages: Int) {
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Page $pageNum of $totalPages",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 20f,
            footerPaint
        )
        
        // Generated by text
        val generatedPaint = Paint().apply {
            color = Color.LTGRAY
            textSize = 8f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(
            "Generated by ATS Android",
            PAGE_WIDTH - MARGIN,
            PAGE_HEIGHT - 20f,
            generatedPaint
        )
    }
    
    /**
     * Truncate text to fit column width
     */
    private fun truncate(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.substring(0, maxLength - 2) + ".."
        } else {
            text
        }
    }
    
    /**
     * Share the PDF file
     */
    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(intent, "Share PDF Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
        
        Log.d(TAG, "📤 Sharing PDF file: ${file.name}")
    }
}
