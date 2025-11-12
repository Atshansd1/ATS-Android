# 🚀 Build Instructions for ATS Android

## Prerequisites ✅

Before building, ensure you have:
- ✅ Android Studio (latest version)
- ✅ JDK 17+
- ✅ Android SDK 26+
- ✅ google-services.json (downloaded from Firebase)
- ✅ Google Maps API Key

---

## Step 1: Setup local.properties

Create `local.properties` in the project root:

```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
MAPS_API_KEY=AIzaSyDqzJ9FhZ8vQQZX-yU5_xW8nY9KqQFJxYo
```

**Find your Android SDK path:**
- macOS: `/Users/YOUR_USERNAME/Library/Android/sdk`
- Windows: `C:\Users\YOUR_USERNAME\AppData\Local\Android\Sdk`
- Linux: `~/Android/Sdk`

---

## Step 2: Verify google-services.json

Make sure `google-services.json` is in:
```
ATS-Android/app/google-services.json
```

If not, download it from:
https://console.firebase.google.com/project/it-adc/settings/general

---

## Step 3: Open in Android Studio

1. Open **Android Studio**
2. Click **Open**
3. Navigate to: `/Users/mohanadsd/Desktop/Myapps/ATS-Android`
4. Click **OK**
5. Wait for Gradle sync to complete

---

## Step 4: Sync Gradle

Android Studio should automatically sync. If not:
- Click **File** → **Sync Project with Gradle Files**
- Or click the **Sync Now** button in the notification banner

---

## Step 5: Build the Project

### Option A: Using Android Studio
1. Click **Build** → **Make Project** (⌘F9 / Ctrl+F9)
2. Wait for build to complete
3. Check the **Build** panel for any errors

### Option B: Using Command Line
```bash
cd /Users/mohanadsd/Desktop/Myapps/ATS-Android

# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

---

## Step 6: Run the App

### Option A: Using Android Studio
1. Connect an Android device (USB debugging enabled)
   - Or start an Android emulator
2. Click **Run** button (▶️) or press **⌃R** (macOS) / **Shift+F10** (Windows)
3. Select your device/emulator
4. App will install and launch!

### Option B: Using Command Line
```bash
# Install on connected device
./gradlew installDebug

# Run on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Build Output Locations

After successful build, APKs are located at:

**Debug APK:**
```
app/build/outputs/apk/debug/app-debug.apk
```

**Release APK:**
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## Troubleshooting Common Issues

### Issue 1: "SDK location not found"
**Solution:** Create `local.properties` with your SDK path

### Issue 2: "google-services.json not found"
**Solution:** Download from Firebase Console and place in `app/` folder

### Issue 3: "MAPS_API_KEY not found"
**Solution:** Add `MAPS_API_KEY=YOUR_KEY` to `local.properties`

### Issue 4: Gradle sync failed
**Solution:** 
```bash
./gradlew --stop
./gradlew clean
./gradlew build
```

### Issue 5: Build takes too long
**Solution:** Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m
org.gradle.parallel=true
org.gradle.caching=true
```

### Issue 6: "Manifest merger failed"
**Solution:** Check for duplicate permissions or activities in AndroidManifest.xml

### Issue 7: Firebase initialization error
**Solution:** 
- Verify package name in `google-services.json` matches `com.ats.android`
- Clean and rebuild project

---

## Testing on Physical Device

### Enable Developer Options:
1. Go to **Settings** → **About Phone**
2. Tap **Build Number** 7 times
3. Go back to **Settings** → **Developer Options**
4. Enable **USB Debugging**

### Connect Device:
1. Connect via USB
2. Allow USB debugging on device
3. Run `adb devices` to verify connection
4. Device should appear in Android Studio

---

## Testing on Emulator

### Create Emulator:
1. Click **Device Manager** in Android Studio
2. Click **Create Device**
3. Select **Phone** → **Pixel 7 Pro** (recommended)
4. Select **API 34** (Android 14)
5. Click **Finish**
6. Click **Play** button to start

---

## Build Types

### Debug Build (Default)
- Includes debug information
- Allows debugging
- Larger APK size
- Use for development

### Release Build
- Optimized and minified
- Smaller APK size
- Requires signing for distribution
- Use for production

---

## Next Steps After Build

1. ✅ Test login with Firebase credentials
2. ✅ Test check-in/check-out
3. ✅ Test location permissions
4. ✅ Test Arabic language
5. ✅ Test all screens and navigation
6. ✅ Generate test reports
7. ✅ Test on different devices

---

## Build Configuration Summary

| Item | Value |
|------|-------|
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 34 (Android 14) |
| **Compile SDK** | 34 |
| **Build Tools** | 34.0.0 |
| **Kotlin** | 1.9.20 |
| **Gradle** | 8.2.0 |
| **Compose** | 2023.10.01 |

---

## Firebase Configuration

Make sure these are enabled in Firebase Console:
- ✅ Authentication (Email/Password)
- ✅ Firestore Database
- ✅ Storage
- ✅ Cloud Messaging (optional)

---

## Google Maps Configuration

Make sure Maps SDK for Android is enabled:
1. Go to: https://console.cloud.google.com/apis/dashboard?project=it-adc
2. Enable: **Maps SDK for Android**
3. Enable: **Places API**
4. Enable: **Geocoding API**

---

## Success! 🎉

If the build succeeds, you should see:
```
BUILD SUCCESSFUL in Xs Ys
```

You're ready to run the app!

---

## Need Help?

Check:
- **Build** panel in Android Studio for errors
- **Logcat** for runtime errors
- **ANDROID_SETUP_GUIDE.md** for detailed instructions
- Firebase Console for backend issues

---

**Happy Building! 🚀**
