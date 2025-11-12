package com.ats.android.utils

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.hours

object CleanupHelper {
    private const val TAG = "CleanupHelper"
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Remove old active locations (older than specified hours)
     * This cleans up locations from check-ins that never checked out
     */
    suspend fun cleanupOldActiveLocations(olderThanHours: Int = 24): Result<String> {
        return try {
            val cutoffTime = Timestamp(
                Timestamp.now().seconds - (olderThanHours * 3600),
                0
            )
            
            Log.d(TAG, "🧹 Starting cleanup of locations older than $olderThanHours hours...")
            
            val snapshot = db.collection("activeLocations")
                .get()
                .await()
            
            var deletedCount = 0
            var keptCount = 0
            
            for (document in snapshot.documents) {
                val timestamp = document.getTimestamp("timestamp") ?: continue
                
                if (timestamp.seconds < cutoffTime.seconds) {
                    // Old location - delete it
                    document.reference.delete().await()
                    deletedCount++
                    Log.d(TAG, "   🗑️ Deleted old location: ${document.id}")
                } else {
                    keptCount++
                }
            }
            
            val message = "Cleanup complete: Deleted $deletedCount old locations, kept $keptCount active"
            Log.d(TAG, "✅ $message")
            Result.success(message)
        } catch (e: Exception) {
            val error = "Cleanup failed: ${e.message}"
            Log.e(TAG, "❌ $error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Remove ALL active locations (nuclear option for testing)
     */
    suspend fun clearAllActiveLocations(): Result<String> {
        return try {
            Log.d(TAG, "🧹 Clearing all active locations...")
            
            val snapshot = db.collection("activeLocations")
                .get()
                .await()
            
            val count = snapshot.documents.size
            
            for (document in snapshot.documents) {
                document.reference.delete().await()
                Log.d(TAG, "   🗑️ Deleted: ${document.id}")
            }
            
            val message = "Cleared $count active locations"
            Log.d(TAG, "✅ $message")
            Result.success(message)
        } catch (e: Exception) {
            val error = "Clear failed: ${e.message}"
            Log.e(TAG, "❌ $error", e)
            Result.failure(e)
        }
    }
}
