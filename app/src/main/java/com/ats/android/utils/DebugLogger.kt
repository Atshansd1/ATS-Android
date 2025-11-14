package com.ats.android.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * In-app debug logger that stores logs in memory for viewing
 */
object DebugLogger {
    
    private const val MAX_LOGS = 500
    private val logs = ConcurrentLinkedQueue<LogEntry>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    
    data class LogEntry(
        val timestamp: String,
        val level: String,
        val tag: String,
        val message: String
    )
    
    fun d(tag: String, message: String) {
        Log.d(tag, message)
        addLog("D", tag, message)
    }
    
    fun i(tag: String, message: String) {
        Log.i(tag, message)
        addLog("I", tag, message)
    }
    
    fun w(tag: String, message: String) {
        Log.w(tag, message)
        addLog("W", tag, message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.message}\n${throwable.stackTraceToString().take(500)}"
        } else {
            message
        }
        addLog("E", tag, fullMessage)
    }
    
    private fun addLog(level: String, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        logs.offer(LogEntry(timestamp, level, tag, message))
        
        // Remove old logs if exceeding max
        while (logs.size > MAX_LOGS) {
            logs.poll()
        }
    }
    
    fun getLogs(): List<LogEntry> {
        return logs.toList()
    }
    
    fun getLogsAsString(): String {
        return buildString {
            logs.forEach { entry ->
                appendLine("${entry.timestamp} ${entry.level}/${entry.tag}: ${entry.message}")
            }
        }
    }
    
    fun getFilteredLogs(filter: String): List<LogEntry> {
        return logs.filter { 
            it.tag.contains(filter, ignoreCase = true) || 
            it.message.contains(filter, ignoreCase = true)
        }
    }
    
    fun clear() {
        logs.clear()
        Log.d("DebugLogger", "Logs cleared")
    }
    
    fun getLogCount(): Int = logs.size
}
