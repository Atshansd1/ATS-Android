package com.ats.android.viewmodels

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.app.Application
import com.ats.android.utils.LocaleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = getApplication<Application>()
    
    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _theme = MutableStateFlow("system")
    val theme: StateFlow<String> = _theme.asStateFlow()
    
    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    
    // Granular State
    private val _pushEnabled = MutableStateFlow(true)
    val pushEnabled: StateFlow<Boolean> = _pushEnabled.asStateFlow()

    private val _emailEnabled = MutableStateFlow(true)
    val emailEnabled: StateFlow<Boolean> = _emailEnabled.asStateFlow()

    private val _checkInReminders = MutableStateFlow(true)
    val checkInReminders: StateFlow<Boolean> = _checkInReminders.asStateFlow()

    private val _leaveUpdates = MutableStateFlow(true)
    val leaveUpdates: StateFlow<Boolean> = _leaveUpdates.asStateFlow()

    private val _announcementAlerts = MutableStateFlow(true)
    val announcementAlerts: StateFlow<Boolean> = _announcementAlerts.asStateFlow()

    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // Read language from LocaleManager (SharedPreferences) as source of truth
                val currentLanguage = LocaleManager.getCurrentLanguage(context)
                _language.value = currentLanguage
                
                // Read other settings from DataStore
                val preferences = context.dataStore.data.first()
                _theme.value = preferences[THEME_KEY] ?: "system"
                _notificationsEnabled.value = preferences[NOTIFICATIONS_KEY]?.toBoolean() ?: true
                
                // Load granular
                _pushEnabled.value = preferences[NOTIF_PUSH_KEY]?.toBoolean() ?: true
                _emailEnabled.value = preferences[NOTIF_EMAIL_KEY]?.toBoolean() ?: true
                _checkInReminders.value = preferences[NOTIF_REMINDERS_KEY]?.toBoolean() ?: true
                _leaveUpdates.value = preferences[NOTIF_LEAVE_KEY]?.toBoolean() ?: true
                _announcementAlerts.value = preferences[NOTIF_ANNOUNCEMENTS_KEY]?.toBoolean() ?: true
                
                // Sync language to DataStore if needed
                if (preferences[LANGUAGE_KEY] != currentLanguage) {
                    context.dataStore.edit { prefs ->
                        prefs[LANGUAGE_KEY] = currentLanguage
                    }
                }
                
                Log.d(TAG, "✅ Settings loaded: language=${_language.value}, theme=${_theme.value}")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading settings: ${e.message}", e)
            }
        }
    }
    
    fun setLanguage(language: String) {
        viewModelScope.launch {
            try {
                // Save to LocaleManager (SharedPreferences) - source of truth
                LocaleManager.setLanguage(context, language)
                
                // Also save to DataStore for consistency
                context.dataStore.edit { preferences ->
                    preferences[LANGUAGE_KEY] = language
                }
                
                _language.value = language
                Log.d(TAG, "✅ Language set to: $language (synced to both stores)")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error setting language: ${e.message}", e)
            }
        }
    }
    
    fun setTheme(theme: String) {
        viewModelScope.launch {
            try {
                context.dataStore.edit { preferences ->
                    preferences[THEME_KEY] = theme
                }
                _theme.value = theme
                Log.d(TAG, "✅ Theme set to: $theme")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error setting theme: ${e.message}", e)
            }
        }
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                context.dataStore.edit { preferences ->
                    preferences[NOTIFICATIONS_KEY] = enabled.toString()
                }
                _notificationsEnabled.value = enabled
                Log.d(TAG, "✅ Notifications set to: $enabled")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error setting notifications: ${e.message}", e)
            }
        }
    }
    
    fun updateNotificationSetting(key: String, enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                val prefKey = when(key) {
                    "push" -> NOTIF_PUSH_KEY
                    "email" -> NOTIF_EMAIL_KEY
                    "reminders" -> NOTIF_REMINDERS_KEY
                    "leave" -> NOTIF_LEAVE_KEY
                    "announcements" -> NOTIF_ANNOUNCEMENTS_KEY
                    else -> return@edit
                }
                prefs[prefKey] = enabled.toString()
            }
            when(key) {
                "push" -> _pushEnabled.value = enabled
                "email" -> _emailEnabled.value = enabled
                "reminders" -> _checkInReminders.value = enabled
                "leave" -> _leaveUpdates.value = enabled
                "announcements" -> _announcementAlerts.value = enabled
            }
        }
    }
    
    companion object {
        private const val TAG = "SettingsViewModel"
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val THEME_KEY = stringPreferencesKey("theme")
        private val NOTIFICATIONS_KEY = stringPreferencesKey("notifications_enabled")
        // Granular keys
        private val NOTIF_PUSH_KEY = stringPreferencesKey("notif_push")
        private val NOTIF_EMAIL_KEY = stringPreferencesKey("notif_email")
        private val NOTIF_REMINDERS_KEY = stringPreferencesKey("notif_reminders")
        private val NOTIF_LEAVE_KEY = stringPreferencesKey("notif_leave")
        private val NOTIF_ANNOUNCEMENTS_KEY = stringPreferencesKey("notif_announcements")
    }
}
