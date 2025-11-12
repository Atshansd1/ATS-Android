package com.ats.android

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.ats.android.ui.navigation.ATSNavigation
import com.ats.android.ui.theme.ATSTheme
import com.ats.android.utils.LocaleManager
import com.ats.android.utils.LanguageProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Make status bar and navigation bar transparent
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        setContent {
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
    
    override fun attachBaseContext(newBase: Context) {
        // Apply locale before attaching context
        val context = LocaleManager.createLocaleContext(newBase)
        super.attachBaseContext(context)
    }
}
