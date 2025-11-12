# 🔥 Manual Cleanup - Firebase Console

## Problem
The cleanup buttons failed due to Firebase permissions. You need to manually delete the old locations.

---

## ✅ Solution: Manual Deletion from Firebase Console

### Step 1: Open Firebase Console
```
1. Go to: https://console.firebase.google.com
2. Select project: "it-adc"
3. Click "Firestore Database" in left menu
```

### Step 2: Find Active Locations
```
1. Find collection: "activeLocations"
2. You should see 3 documents:
   - 10010 (Ahmedyaseen Abdelrhman)
   - 10013 (Mohammed Khogali)
   - 10017 (Mohanad Elhag)
```

### Step 3: Check Which is Current
```
Look at the "timestamp" field of each document:
- If timestamp is from today → KEEP IT
- If timestamp is from yesterday or older → DELETE IT
```

### Step 4: Delete Old Documents
```
1. Click on the old document
2. Click the three dots menu (⋮)
3. Click "Delete document"
4. Confirm deletion
5. Repeat for all old documents
```

### Step 5: Verify on Android
```
1. Open Android app
2. Dashboard should now show: 1 active ✅
3. Map should now show: 1 location ✅
```

---

## 🎯 Quick Identification

### To find which employee is CURRENTLY active:

**Option A: Ask the User**
- Which employee checked in most recently?
- That's the one to KEEP
- Delete the other 2

**Option B: Check Timestamps**
```
Firebase Console → activeLocations → Each document

Document 10010:
  timestamp: November 10, 5:00 PM → OLD, DELETE ❌

Document 10013:
  timestamp: November 10, 8:30 PM → OLD, DELETE ❌

Document 10017:
  timestamp: November 11, 5:42 PM → CURRENT, KEEP ✅
```

---

## 📝 Alternative: Fix Firestore Rules

If you want the cleanup buttons to work, update Firestore rules:

### Current Rule (Probably):
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /activeLocations/{document=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null; // Only write, no delete
    }
  }
}
```

### Fixed Rule:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /activeLocations/{document=**} {
      allow read: if request.auth != null;
      allow write, delete: if request.auth != null; // Now allows delete
    }
  }
}
```

---

## 🚀 Fastest Solution RIGHT NOW

### Just tell me which employee is currently active:

**Example**: "Mohanad Elhag is the current one"

Then I'll tell you exactly which 2 to delete from Firebase Console.

The 3 documents you have are:
1. **10010** - Ahmedyaseen Abdelrhman
2. **10013** - Mohammed Khogali  
3. **10017** - Mohanad Elhag

**Which ONE is currently checked in?** (The others will be deleted)

---

## 📱 After Manual Cleanup

Once you delete the old 2 from Firebase Console:

```
1. Android app will update automatically (real-time listener)
2. Dashboard shows: 1 active ✅
3. Map shows: 1 location ✅
4. Both synchronized ✅
```

No need to restart the app - it updates in real-time!

---

## 🔧 Summary

**Problem**: Firebase permissions prevent app from deleting
**Solution**: Delete manually from Firebase Console
**Steps**: 
1. Firebase Console → Firestore → activeLocations
2. Find the 2 old documents (check timestamps)
3. Delete them
4. App updates automatically

**Time**: ~2 minutes

Let me know which employee is the current one and I'll give you exact delete instructions!
