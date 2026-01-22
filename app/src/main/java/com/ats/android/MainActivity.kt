package com.ats.android

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.ats.android.ui.navigation.ATSNavigation
import com.ats.android.ui.theme.ATSTheme
import com.ats.android.utils.LocaleManager
import com.ats.android.utils.LanguageProvider
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val updateManager by lazy { com.ats.android.services.UpdateManager(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch from splash theme to main theme
        setTheme(R.style.Theme_Hodoor)
        
        super.onCreate(savedInstanceState)
        
        // Log device info for debugging
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density
        Log.d("MainActivity", "Screen size: ${screenWidthDp}x${screenHeightDp} dp")
        Log.d("MainActivity", "Density: ${displayMetrics.density}")
        Log.d("MainActivity", "Font scale: ${resources.configuration.fontScale}")
        
        // Enable edge-to-edge
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Make status bar and navigation bar transparent
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Check for updates on startup (throttled to once every 6 hours)
        checkForUpdatesOnStartup()
        
        setContent {
            // Force normal font scale to prevent zoom issues
            val normalizedDensity = Density(
                density = LocalDensity.current.density,
                fontScale = 1.0f  // Force normal font scale
            )
            
            CompositionLocalProvider(LocalDensity provides normalizedDensity) {
                // Wrap entire app with LanguageProvider for RTL support
                LanguageProvider {
                    ATSTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            ATSNavigation()
                        }
                    }
                }
            }
        }
    }
    
    private fun checkForUpdatesOnStartup() {
        val prefs = getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong("last_update_check", 0)
        val sixHoursInMs = 6 * 60 * 60 * 1000L
        
        // Only check if more than 6 hours since last check
        if (System.currentTimeMillis() - lastCheck < sixHoursInMs) {
            Log.d("MainActivity", "⏭️ Skipping update check - checked recently")
            return
        }
        
        Log.d("MainActivity", "🔄 Checking for updates on startup...")
        
        kotlinx.coroutines.MainScope().launch {
            try {
                val versionInfo = updateManager.checkForUpdates()
                
                if (versionInfo != null && versionInfo.isUpdateAvailable) {
                    Log.d("MainActivity", "📦 Update available: ${versionInfo.latestVersion}")
                    
                    // Save last check time
                    prefs.edit().putLong("last_update_check", System.currentTimeMillis()).apply()
                    
                    // Show update notification
                    showUpdateNotification(versionInfo.latestVersion)
                } else {
                    Log.d("MainActivity", "✅ App is up to date")
                    prefs.edit().putLong("last_update_check", System.currentTimeMillis()).apply()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Update check failed: ${e.message}")
            }
        }
    }
    
    private fun showUpdateNotification(newVersion: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        // Ensure channel exists
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "app_updates",
                getString(R.string.update_channel_name),
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.update_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_settings", true)
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = androidx.core.app.NotificationCompat.Builder(this, "app_updates")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.update_available_title))
            .setContentText(getString(R.string.update_available_body, newVersion))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(9999, notification)
        Log.d("MainActivity", "📣 Update notification shown for version $newVersion")
    }
    
    override fun attachBaseContext(newBase: Context) {
        // Apply locale and force normal font scale
        var context = LocaleManager.createLocaleContext(newBase)
        
        // Override font scale to prevent zoom
        val configuration = Configuration(context.resources.configuration)
        configuration.fontScale = 1.0f
        context = context.createConfigurationContext(configuration)
        
        super.attachBaseContext(context)
    }
}
