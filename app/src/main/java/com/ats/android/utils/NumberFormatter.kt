package com.ats.android.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Utility object for formatting numbers with English digits
 * Ensures all numbers display in English (0-9) even when app language is Arabic
 */
object NumberFormatter {
    
    private val englishSymbols = DecimalFormatSymbols(Locale.US)
    
    /**
     * Format integer with English digits
     */
    fun formatInt(value: Int): String {
        return value.toString() // Kotlin uses Western digits by default
    }
    
    /**
     * Format long with English digits
     */
    fun formatLong(value: Long): String {
        return value.toString()
    }
    
    /**
     * Format double with 1 decimal place using English digits
     */
    fun formatDecimal1(value: Double): String {
        val format = DecimalFormat("#0.0", englishSymbols)
        return format.format(value)
    }
    
    /**
     * Format double with 2 decimal places using English digits
     */
    fun formatDecimal2(value: Double): String {
        val format = DecimalFormat("#0.00", englishSymbols)
        return format.format(value)
    }
    
    /**
     * Format distance in km or meters with English digits
     */
    fun formatDistance(distanceKm: Double): String {
        return if (distanceKm < 1.0) {
            val meters = (distanceKm * 1000).toInt()
            "$meters m"
        } else {
            "${formatDecimal2(distanceKm)} km"
        }
    }
    
    /**
     * Format duration in seconds to human-readable format with English digits
     */
    fun formatDuration(seconds: Long): String {
        val minutes = (seconds / 60).toInt()
        val hours = minutes / 60
        val mins = minutes % 60
        
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            mins > 0 -> "${mins}m"
            else -> "${seconds}s"
        }
    }
    
    /**
     * Format time ago string with English digits
     */
    fun formatTimeAgo(seconds: Long): String {
        val minutes = (seconds / 60).toInt()
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "now"
        }
    }
    
    /**
     * Convert any string with Arabic numerals to English numerals
     */
    fun toEnglishDigits(text: String): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        
        var result = text
        for (i in arabicDigits.indices) {
            result = result.replace(arabicDigits[i], englishDigits[i])
        }
        return result
    }
}
