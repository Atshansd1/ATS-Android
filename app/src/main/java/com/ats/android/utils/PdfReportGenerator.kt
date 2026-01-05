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
    private const val MARGIN = 30f
    private const val LINE_HEIGHT = 18f
    
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
            
            // Calculate pages needed (approximately 20 records per page with compact layout)
            val recordsPerPage = 20
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
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        // Draw headers with text wrapping if needed
        for (i in headers.indices) {
            drawTextInCell(canvas, headers[i], colX[i] + 3f, y + 12f, 
                          colWidths[i] - 6f, headerTextPaint)
        }
        
        y += 25f
        
        // Data rows with improved text fitting
        val dataPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 7.5f
        }
        
        val dateTimeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.US)
        
        records.forEachIndexed { index, record ->
            // Alternating row colors
            if (index % 2 == 0) {
                val rowBgPaint = Paint().apply {
                    color = COLOR_LIGHT_GRAY
                    style = Paint.Style.FILL
                }
                canvas.drawRect(MARGIN, y - 12f, PAGE_WIDTH - MARGIN, y + 6f, rowBgPaint)
            }
            
            // Draw row border
            val borderPaint = Paint().apply {
                color = COLOR_BORDER
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }
            canvas.drawRect(MARGIN, y - 12f, PAGE_WIDTH - MARGIN, y + 6f, borderPaint)
            
            // Draw data with smart fitting (no cutoff)
            drawTextInCell(canvas, record.employeeId, colX[0] + 2f, y, colWidths[0] - 4f, dataPaint)
            drawTextInCell(canvas, record.employeeName ?: "N/A", colX[1] + 2f, y, colWidths[1] - 4f, dataPaint)
            drawTextInCell(canvas, dateTimeFormat.format(record.checkInTime.toDate()), colX[2] + 2f, y, colWidths[2] - 4f, dataPaint)
            drawTextInCell(canvas, record.checkInPlaceName ?: "N/A", colX[3] + 2f, y, colWidths[3] - 4f, dataPaint)
            
            if (record.checkOutTime != null) {
                drawTextInCell(canvas, dateTimeFormat.format(record.checkOutTime!!.toDate()), colX[4] + 2f, y, colWidths[4] - 4f, dataPaint)
            } else {
                drawTextInCell(canvas, "--", colX[4] + 2f, y, colWidths[4] - 4f, dataPaint)
            }
            
            drawTextInCell(canvas, record.checkOutPlaceName ?: "--", colX[5] + 2f, y, colWidths[5] - 4f, dataPaint)
            
            // Duration
            val duration = if (record.totalDuration != null && record.totalDuration!! > 0) {
                val hours = (record.totalDuration!! / 3600).toInt()
                val minutes = ((record.totalDuration!! % 3600) / 60).toInt()
                if (isArabic) "${hours}س ${minutes}د" else "${hours}h ${minutes}m"
            } else {
                "--"
            }
            drawTextInCell(canvas, duration, colX[6] + 2f, y, colWidths[6] - 4f, dataPaint)
            
            // Status with color
            val status = when (record.statusString) {
                "checked_out" -> if (isArabic) "منصرف" else "Done"
                else -> if (isArabic) "حاضر" else "Active"
            }
            val statusColor = if (record.statusString == "checked_out") COLOR_SUCCESS else COLOR_WARNING
            val statusPaint = Paint().apply {
                color = statusColor
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            drawTextInCell(canvas, status, colX[7] + 2f, y, colWidths[7] - 4f, statusPaint)
            
            y += 18f
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
            "Generated by Hodoor+ Android",
            PAGE_WIDTH - MARGIN,
            PAGE_HEIGHT - 20f,
            generatedPaint
        )
    }
    
    /**
     * Draw text in cell with smart truncation (no cutoff, adds ellipsis if needed)
     */
    private fun drawTextInCell(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        var displayText = text
        var textWidth = paint.measureText(displayText)
        
        // If text is too wide, truncate with ellipsis
        if (textWidth > maxWidth) {
            val ellipsis = "..."
            val ellipsisWidth = paint.measureText(ellipsis)
            
            // Binary search for best fit
            var left = 0
            var right = displayText.length
            while (left < right) {
                val mid = (left + right + 1) / 2
                val testText = displayText.substring(0, mid) + ellipsis
                if (paint.measureText(testText) <= maxWidth) {
                    left = mid
                } else {
                    right = mid - 1
                }
            }
            
            displayText = if (left > 0) {
                displayText.substring(0, left) + ellipsis
            } else {
                ellipsis
            }
        }
        
        canvas.drawText(displayText, x, y, paint)
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
