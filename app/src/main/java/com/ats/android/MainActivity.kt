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

class MainActivity : ComponentActivity() {
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
