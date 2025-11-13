package com.ats.android.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.*

/**
 * LocaleManager - Handles app language and RTL support
 * Similar to iOS LanguageManager
 */
object LocaleManager {
    private const val PREF_LANGUAGE = "app_language"
    private const val ENGLISH = "en"
    private const val ARABIC = "ar"
    
    /**
     * Get device system language
     */
    fun getDeviceLanguage(): String {
        val deviceLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.content.res.Resources.getSystem().configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            android.content.res.Resources.getSystem().configuration.locale
        }
        
        val languageCode = deviceLocale.language
        android.util.Log.d("LocaleManager", "📱 Device language: $languageCode")
        
        // Only support English and Arabic
        return if (languageCode == "ar") ARABIC else ENGLISH
    }
    
    /**
     * Get current language from SharedPreferences, defaults to device language
     */
    fun getCurrentLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedLanguage = prefs.getString(PREF_LANGUAGE, null)
        
        // If no saved language, use device language
        if (savedLanguage == null) {
            val deviceLang = getDeviceLanguage()
            android.util.Log.d("LocaleManager", "🌍 No saved language, using device language: $deviceLang")
            // Save it for next time
            setLanguage(context, deviceLang)
            return deviceLang
        }
        
        return savedLanguage
    }
    
    /**
     * Set language and persist to SharedPreferences
     */
    fun setLanguage(context: Context, languageCode: String) {
        android.util.Log.d("LocaleManager", "💾 Saving language: $languageCode")
        
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PREF_LANGUAGE, languageCode)
            .commit() // Use commit() instead of apply() to ensure immediate write
        
        // Verify it was saved
        val saved = prefs.getString(PREF_LANGUAGE, "NOT_FOUND")
        android.util.Log.d("LocaleManager", "✅ Language saved and verified: $saved")
    }
    
    /**
     * Check if current language is RTL (Arabic)
     */
    fun isRTL(context: Context): Boolean {
        return getCurrentLanguage(context) == ARABIC
    }
    
    /**
     * Get layout direction based on current language
     */
    fun getLayoutDirection(context: Context): LayoutDirection {
        return if (isRTL(context)) LayoutDirection.Rtl else LayoutDirection.Ltr
    }
    
    /**
     * Create context with selected locale
     */
    fun createLocaleContext(context: Context): Context {
        val languageCode = getCurrentLanguage(context)
        return updateContextLocale(context, languageCode)
    }
    
    /**
     * Update context with new locale
     */
    private fun updateContextLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        android.util.Log.d("LocaleManager", "🌍 Setting locale to: $languageCode (${locale.displayLanguage})")
        
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        
        android.util.Log.d("LocaleManager", "📐 Layout direction: ${configuration.layoutDirection}")
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * Get localized string
     */
    fun getString(context: Context, resId: Int): String {
        val localeContext = createLocaleContext(context)
        return localeContext.getString(resId)
    }
    
    /**
     * Get localized string with format args
     */
    fun getString(context: Context, resId: Int, vararg formatArgs: Any): String {
        val localeContext = createLocaleContext(context)
        return localeContext.getString(resId, *formatArgs)
    }
    
    /**
     * Available languages
     */
    enum class Language(val code: String, val displayName: String, val nativeName: String) {
        ENGLISH("en", "English", "English"),
        ARABIC("ar", "Arabic", "العربية");
        
        companion object {
            fun fromCode(code: String): Language {
                return values().find { it.code == code } ?: ENGLISH
            }
        }
    }
}

/**
 * CompositionLocal for current language
 */
val LocalLanguage = staticCompositionLocalOf { "en" }

/**
 * Composable wrapper to provide language and RTL support
 */
@Composable
fun LanguageProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val language = LocaleManager.getCurrentLanguage(context)
    val layoutDirection = LocaleManager.getLayoutDirection(context)
    
    CompositionLocalProvider(
        LocalLanguage provides language,
        LocalLayoutDirection provides layoutDirection,
        content = content
    )
}

/**
 * Extension to get localized string in Composable
 */
@Composable
fun stringResource(resId: Int): String {
    val context = LocalContext.current
    return LocaleManager.getString(context, resId)
}

@Composable
fun stringResource(resId: Int, vararg formatArgs: Any): String {
    val context = LocalContext.current
    return LocaleManager.getString(context, resId, *formatArgs)
}
