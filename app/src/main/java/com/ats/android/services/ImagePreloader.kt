package com.ats.android.services

import android.content.Context
import android.util.Log
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Service for preloading employee avatar images into Coil cache
 * Improves initial load times by fetching images in background
 */
object ImagePreloader {
    private const val TAG = "ImagePreloader"
    
    /**
     * Preload a list of image URLs into cache with aggressive caching
     * @param context Android context
     * @param urls List of image URLs to preload
     */
    fun preloadImages(context: Context, urls: List<String>) {
        if (urls.isEmpty()) {
            Log.d(TAG, "No URLs to preload")
            return
        }
        
        Log.d(TAG, "🎯 Starting aggressive preload of ${urls.size} images...")
        val startTime = System.currentTimeMillis()
        
        CoroutineScope(Dispatchers.IO).launch {
            val results = urls.mapIndexed { index, url ->
                if (url.isNotBlank()) {
                    try {
                        val imageStartTime = System.currentTimeMillis()
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .memoryCacheKey(url)
                            .diskCacheKey(url)
                            .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .build()
                        
                        // Execute request and wait for completion
                        val result = context.imageLoader.execute(request)
                        val imageTime = System.currentTimeMillis() - imageStartTime
                        
                        if (result.drawable != null) {
                            Log.d(TAG, "✅ Preloaded ${index + 1}/${urls.size}: ${url.substringAfterLast("/")} (${imageTime}ms)")
                            true
                        } else {
                            Log.w(TAG, "⚠️  No drawable for ${index + 1}/${urls.size}: ${url.substringAfterLast("/")}")
                            false
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to preload ${url}: ${e.message}")
                        false
                    }
                } else {
                    false
                }
            }
            
            val successCount = results.count { it }
            val totalTime = System.currentTimeMillis() - startTime
            
            withContext(Dispatchers.Main) {
                Log.d(TAG, "🎉 Preloading complete! ${successCount}/${urls.size} images cached in ${totalTime}ms")
            }
        }
    }
    
    /**
     * Preload employee avatars from a list of employees
     */
    fun preloadEmployeeAvatars(context: Context, employees: List<com.ats.android.models.Employee>) {
        val avatarUrls = employees
            .mapNotNull { it.avatarURL }
            .filter { it.isNotBlank() }
        
        preloadImages(context, avatarUrls)
    }
    
    /**
     * Clear image cache
     */
    @coil.annotation.ExperimentalCoilApi
    fun clearCache(context: Context) {
        Log.d(TAG, "🗑️ Clearing image cache...")
        context.imageLoader.memoryCache?.clear()
        context.imageLoader.diskCache?.clear()
        Log.d(TAG, "✅ Cache cleared")
    }
}
