# 🗺️ Map Not Loading - Debug Guide

## Quick Fix Steps

### Step 1: Check if Test Data Exists

```
1. Open app
2. Go to Settings tab
3. Scroll to "Test Data (Development)"
4. Tap "Add Test Employees" → Wait for success
5. Tap "Add Test Locations" → Wait for success
6. Go back to Map tab
```

**CRITICAL**: You MUST add test locations! Without them, the map will be empty.

### Step 2: Check Map Status

After adding test data, on the Map screen you should see:
- Top right corner shows: "3 locations" (or number)
- Refresh button (circular arrow icon)
- Map with 3 markers in Riyadh area

### Step 3: Use Refresh Button

If map shows "0 locations":
1. Tap the refresh button (top right)
2. Watch the count update
3. Markers should appear

### Step 4: Check Logs

```bash
# Clear logs
adb -s emulator-5554 logcat -c

# Open app and go to Map tab

# Check logs
adb -s emulator-5554 logcat -s IOSMapScreen:D MapViewModel:D FirestoreService:D

# Look for:
# 🗺️ UI State: Success, Locations: 3
# 📍 Received 3 active locations from Firestore
# ✅ Real-time update: 3 active locations with employee data
```

## Common Issues

### Issue 1: "0 locations" shown

**Cause**: No active locations in database

**Fix**:
```
Settings → Test Data:
1. Add Test Employees (if not done)
2. Add Test Locations ← MUST DO THIS!
3. Tap refresh button on map
```

### Issue 2: Map shows but nothing happens

**Cause**: Firestore listener not triggering

**Fix**:
1. Tap refresh button (top right of map)
2. Check internet connection
3. Verify Firebase project is correct
4. Check logs for errors

### Issue 3: Map stuck on "Loading locations..."

**Cause**: Firestore query hanging

**Fix**:
1. Wait 10 seconds
2. Tap refresh button
3. Check Firestore console for activeLocations collection
4. Verify Firestore rules allow reads

### Issue 4: Crash when opening Map

**Cause**: Missing Google Maps API key or permission

**Fix**:
1. Check AndroidManifest.xml has Maps API key
2. Check location permissions granted
3. Check logs for specific error

## What I Added

### 1. Debug Counter (Top Right)

Shows exactly how many locations are loaded:
- "0 locations" = No data, add test data!
- "3 locations" = Working correctly
- Updates in real-time

### 2. Refresh Button (Top Right)

Manual refresh if auto-update doesn't work:
- Tap to force reload from Firestore
- Useful if data was just added
- Shows loading state

### 3. Better Logging

Added log statements to track:
- UI state changes
- Location count updates
- When map is rendered

## Verification Steps

### ✅ Map Loaded Successfully:

- [ ] Map appears immediately (Riyadh area)
- [ ] Top right shows location count
- [ ] Refresh button visible
- [ ] If 0 locations: Add test data
- [ ] After test data: See 3 markers
- [ ] Markers are in Riyadh area
- [ ] Tap marker shows employee info

### ✅ Data Flow Working:

- [ ] Settings → Add Test Locations → Success
- [ ] Map → See count increase to 3
- [ ] Markers appear on map
- [ ] Bottom button shows "3 active employees"
- [ ] Tap button → Sheet opens with 3 employees

## Firebase Console Check

If still having issues, check Firebase Console:

```
1. Go to https://console.firebase.google.com
2. Select "it-adc" project
3. Go to Firestore Database
4. Check "activeLocations" collection
5. Should have 3 documents (if test data added)
6. Each document should have:
   - employeeId: string
   - location: GeoPoint (lat, lng)
   - timestamp: timestamp
   - checkInTime: timestamp
   - placeName: string
```

## Test Data Structure

When you tap "Add Test Locations", it creates:

```
activeLocations/location1:
{
  employeeId: "EMP001",
  location: GeoPoint(24.7136, 46.6753),
  placeName: "Main Office, Riyadh",
  timestamp: <now>,
  checkInTime: <now>
}

activeLocations/location2:
{
  employeeId: "EMP002",
  location: GeoPoint(24.7189, 46.6781),
  placeName: "Branch Office, Riyadh",
  timestamp: <now>,
  checkInTime: <now>
}

activeLocations/location3:
{
  employeeId: "EMP003",
  location: GeoPoint(24.7098, 46.6722),
  placeName: "Remote Site, Riyadh",
  timestamp: <now>,
  checkInTime: <now>
}
```

## Quick Diagnosis

### If you see "0 locations":
→ **Add test data in Settings!**

### If you see "3 locations" but no markers:
→ **Check logs for errors**
→ **Tap refresh button**

### If nothing changes:
→ **Check internet connection**
→ **Verify Firebase project**
→ **Check Firestore rules**

## Success Pattern

```
1. Open app
2. Go to Settings
3. Add Test Employees → ✅ Success
4. Add Test Locations → ✅ Success
5. Go to Map
6. See "3 locations" in top right
7. See 3 markers on map
8. Tap marker → See employee info
9. Tap bottom button → See employee list
```

## Still Not Working?

If you've tried all the above:

1. **Capture full logs**:
```bash
adb logcat -d > map_logs.txt
```

2. **Check Firestore rules**:
```javascript
// Must allow read access
allow read: if request.auth != null;
```

3. **Verify API key**:
```xml
<!-- In AndroidManifest.xml -->
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_ACTUAL_KEY"/>
```

4. **Check network**:
```bash
# In emulator
Settings → Network & Internet → WiFi
Should be connected
```

---

**Most Common Solution**: Just add test locations in Settings! The map works fine, it just needs data.
