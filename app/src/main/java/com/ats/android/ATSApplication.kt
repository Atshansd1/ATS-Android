package com.ats.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.ats.android.utils.LocaleManager
import com.google.firebase.FirebaseApp

class ATSApplication : Application(), ImageLoaderFactory {
    
    companion object {
        const val TAG = "ATSApplication"
        const val LOCATION_NOTIFICATION_CHANNEL_ID = "location_tracking"
        const val GENERAL_NOTIFICATION_CHANNEL_ID = "general"
    }
    
    override fun attachBaseContext(base: Context) {
        // Apply locale at application level - critical for language switching
        val language = base.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("app_language", "en") ?: "en"
        
        Log.d(TAG, "📱 Applying language at app level: $language")
        
        val localeContext = LocaleManager.createLocaleContext(base)
        super.attachBaseContext(localeContext)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        val currentLanguage = LocaleManager.getCurrentLanguage(this)
        Log.d(TAG, "🔥 Hodoor+ Application is initializing... Language: $currentLanguage")
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d(TAG, "✅ Firebase initialized")
        
        // Create notification channels
        createNotificationChannels()
        
        Log.d(TAG, "🎉 Hodoor+ Application initialization complete!")
    }
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30) // 30% of available memory for instant loading
                    .strongReferencesEnabled(true) // Keep strong references
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100 MB disk cache (was 2%)
                    .build()
            }
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false) // Ignore cache headers, cache everything
            .crossfade(300) // Smooth 300ms crossfade
            .logger(DebugLogger())
            .build()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // Location tracking channel
            val locationChannel = NotificationChannel(
                LOCATION_NOTIFICATION_CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when location is being tracked"
                setShowBadge(false)
            }
            
            // General notifications channel
            val generalChannel = NotificationChannel(
                GENERAL_NOTIFICATION_CHANNEL_ID,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }
            
            notificationManager.createNotificationChannel(locationChannel)
            notificationManager.createNotificationChannel(generalChannel)
            
            Log.d(TAG, "✅ Notification channels created")
        }
    }
}
