// Quick debug script to add to DashboardViewModel or run separately

// Add this temporary debug code to check attendance status
viewModelScope.launch {
    try {
        val snapshot = firestoreService.db.collection("companies/it-adc/attendance")
            .whereEqualTo("status", "checked_in")
            .get()
            .await()
        
        Log.d("DEBUG_ATTENDANCE", "=== CHECKED-IN EMPLOYEES ===")
        Log.d("DEBUG_ATTENDANCE", "Total checked-in: ${snapshot.size()}")
        
        snapshot.documents.forEach { doc ->
            val employeeId = doc.getString("employeeId")
            val employeeName = doc.getString("employeeName")
            val checkInTime = doc.getTimestamp("checkInTime")
            val status = doc.getString("status")
            
            Log.d("DEBUG_ATTENDANCE", "Employee: $employeeName ($employeeId)")
            Log.d("DEBUG_ATTENDANCE", "  Status: $status")
            Log.d("DEBUG_ATTENDANCE", "  Check-in: $checkInTime")
            
            // Check if there's an active location
            val activeLocDoc = firestoreService.db.collection("companies/it-adc/activeLocations")
                .document(employeeId ?: "")
                .get()
                .await()
            
            val hasActiveLoc = activeLocDoc.exists()
            val isActive = activeLocDoc.getBoolean("isActive")
            
            Log.d("DEBUG_ATTENDANCE", "  Has activeLocation doc: $hasActiveLoc")
            Log.d("DEBUG_ATTENDANCE", "  isActive in activeLocation: $isActive")
        }
    } catch (e: Exception) {
        Log.e("DEBUG_ATTENDANCE", "Error: ${e.message}", e)
    }
}
