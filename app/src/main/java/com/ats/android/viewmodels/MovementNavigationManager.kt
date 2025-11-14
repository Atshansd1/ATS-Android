package com.ats.android.viewmodels

import com.ats.android.models.LocationMovement

/**
 * Simple singleton to share movement data between screens for navigation
 * This is a lightweight alternative to passing complex objects through navigation args
 */
object MovementNavigationManager {
    private var currentMovement: LocationMovement? = null
    
    fun setMovement(movement: LocationMovement) {
        currentMovement = movement
    }
    
    fun getMovement(): LocationMovement? {
        return currentMovement
    }
    
    fun clearMovement() {
        currentMovement = null
    }
}
