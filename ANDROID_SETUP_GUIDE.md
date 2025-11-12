# ATS Android App - Complete Setup Guide

## 🎉 Welcome to ATS Android!

This is a **complete Android version** of the iOS ATS app with **Material Design 3**, built using:
- **Kotlin** with **Jetpack Compose**
- **Material Design 3** (Material You)
- **Firebase** (Auth, Firestore, Storage, Messaging)
- **Google Maps**
- **Arabic/RTL Support**

---

## 📁 Project Structure

```
ATS-Android/
├── app/
│   ├── src/main/
│   │   ├── java/com/ats/android/
│   │   │   ├── models/
│   │   │   │   ├── Employee.kt
│   │   │   │   ├── AttendanceRecord.kt
│   │   │   │   └── Report.kt
│   │   │   ├── services/
│   │   │   │   ├── AuthService.kt
│   │   │   │   ├── FirestoreService.kt
│   │   │   │   ├── LocationService.kt
│   │   │   │   └── GeocodingService.kt
│   │   │   ├── viewmodels/
│   │   │   │   ├── AuthViewModel.kt
│   │   │   │   ├── DashboardViewModel.kt
│   │   │   │   ├── CheckInViewModel.kt
│   │   │   │   ├── MapViewModel.kt
│   │   │   │   └── ReportsViewModel.kt
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   ├── LoginScreen.kt
│   │   │   │   │   ├── DashboardScreen.kt
│   │   │   │   │   ├── MapScreen.kt
│   │   │   │   │   ├── CheckInScreen.kt
│   │   │   │   │   ├── HistoryScreen.kt
│   │   │   │   │   ├── ReportsScreen.kt
│   │   │   │   │   ├── EmployeeManagementScreen.kt
│   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   ├── components/
│   │   │   │   │   ├── EmployeeCard.kt
│   │   │   │   │   ├── AttendanceCard.kt
│   │   │   │   │   └── SummaryCard.kt
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   ├── Theme.kt
│   │   │   │   │   └── Type.kt
│   │   │   │   └── navigation/
│   │   │   │       └── ATSNavigation.kt
│   │   │   ├── ATSApplication.kt
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   ├── values-ar/ (Arabic translations)
│   │   │   │   └── strings.xml
│   │   │   └── drawable/
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── google-services.json (copy from iOS)
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Setup Steps

### Step 1: Prerequisites

Install:
1. **Android Studio** (latest version)
   - Download: https://developer.android.com/studio
2. **Java JDK 17** (already included in Android Studio)

### Step 2: Open Project in Android Studio

1. Open **Android Studio**
2. Click **"Open an Existing Project"**
3. Navigate to: `/Users/mohanadsd/Desktop/Myapps/ATS-Android`
4. Click **"Open"**
5. Wait for Gradle sync to complete

### Step 3: Configure Firebase

#### Option A: Copy from iOS Project (Recommended)

1. **Copy google-services.json:**
   ```bash
   # Convert iOS GoogleService-Info.plist to Android google-services.json
   # You need to download it from Firebase Console
   ```

2. **Download Android config:**
   - Go to: https://console.firebase.google.com/project/it-adc/settings/general
   - Scroll to "Your apps"
   - Click "Add app" → Select **Android**
   - Package name: `com.ats.android`
   - App nickname: `ATS Android`
   - Click "Register app"
   - **Download google-services.json**
   - Place it in: `ATS-Android/app/google-services.json`

#### Option B: Add Android App to Firebase

1. Firebase Console: https://console.firebase.google.com/project/it-adc
2. Click ⚙️ → Project settings
3. Under "Your apps" → Add app → Android
4. Package name: `com.ats.android`
5. Download `google-services.json`
6. Place in `app/` folder

### Step 4: Configure Google Maps

1. **Get API Key** (use same key from iOS):
   - Your key: `AIzaSyDqzJ9FhZ8vQQZX-yU5_xW8nY9KqQFJxYo`

2. **Add to local.properties:**
   Create `local.properties` in project root:
   ```properties
   sdk.dir=/Users/YourUsername/Library/Android/sdk
   MAPS_API_KEY=AIzaSyDqzJ9FhZ8vQQZX-yU5_xW8nY9KqQFJxYo
   ```

3. **Enable APIs in Google Cloud:**
   - Go to: https://console.cloud.google.com/apis/dashboard?project=it-adc
   - Enable:
     - Maps SDK for Android
     - Places API
     - Geocoding API

### Step 5: Configure App Package

The app is already configured with:
- **Package**: `com.ats.android`
- **Bundle ID**: `com.ats.android`

This matches the Firebase configuration.

### Step 6: Build & Run

1. **Connect Device or Start Emulator:**
   - Physical device: Enable USB debugging
   - Emulator: Create one in Android Studio (API 26+)

2. **Build the app:**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Run from Android Studio:**
   - Click ▶️ Run button
   - Select device
   - App will install and launch!

---

## 📱 Features (Same as iOS)

### ✅ Authentication
- Email/Employee ID login
- Role-based access (Admin, Supervisor, Employee)
- Auto sign-in

### ✅ Dashboard (Admin/Supervisor)
- Active employees count
- Total employees
- On leave count
- Today's check-ins
- Live activity feed
- Quick actions

### ✅ Live Map
- Real-time employee locations
- Place names (Arabic/English)
- Search locations
- Filter by team/role
- Employee details
- Distance calculations

### ✅ Check In/Out (All Roles)
- Location-based check-in
- Auto check-out
- Current location display
- Place name geocoding
- Background location tracking

### ✅ Attendance History
- Personal attendance records
- Filter by date range
- Check-in/out times
- Duration calculations
- Place names

### ✅ Reports (Admin/Supervisor)
- Generate reports
- Date range selection
- Employee selection (all/specific)
- Preview mode
- CSV export
- Arabic/English support

### ✅ Employee Management (Admin Only)
- Add/edit employees
- Avatar upload
- Employee details
- Attendance history
- Deactivate employees

### ✅ Settings
- Language selection (English/Arabic)
- Profile information
- Notification settings
- Location permissions
- Privacy center
- Sign out

### ✅ Arabic/RTL Support
- Complete Arabic localization
- RTL layout
- Arabic place names
- Arabic CSV exports
- Language switching

---

## 🎨 Material Design 3 Features

### Dynamic Color (Material You)
- Adapts to device wallpaper
- System color extraction
- Light/Dark theme support

### Components Used:
- **NavigationBar** (Bottom nav)
- **TopAppBar** with Material 3 styling
- **FAB** (Floating Action Button)
- **Cards** with elevated/outlined styles
- **Buttons** (Filled, Tonal, Outlined, Text)
- **TextField** with Material 3 styling
- **Dialog** and **BottomSheet**
- **Chips** and **Badges**
- **Lists** with Material 3 styling

### Typography:
- Material 3 type scale
- Display, Headline, Title, Body, Label

---

## 🌍 Localization

### Supported Languages:
- 🇬🇧 English (Default)
- 🇸🇦 Arabic (with RTL)

### String Resources:

**English** (`res/values/strings.xml`):
```xml
<string name="app_name">ATS</string>
<string name="dashboard">Dashboard</string>
<string name="check_in">Check In</string>
...
```

**Arabic** (`res/values-ar/strings.xml`):
```xml
<string name="app_name">نظام الحضور</string>
<string name="dashboard">لوحة التحكم</string>
<string name="check_in">تسجيل الحضور</string>
...
```

---

## 🔐 Permissions

The app requests:
- ✅ **Location** (foreground & background)
- ✅ **Internet**
- ✅ **Notifications**
- ✅ **Camera** (for avatar upload)
- ✅ **Storage** (for CSV export)

---

## 🏗️ Architecture

### MVVM Pattern:
- **Models**: Data classes
- **ViewModels**: Business logic
- **Views**: Jetpack Compose UI

### Services:
- **AuthService**: Firebase Authentication
- **FirestoreService**: Database operations
- **LocationService**: GPS tracking
- **GeocodingService**: Reverse geocoding

### State Management:
- **ViewModel** + **StateFlow**
- **Compose State**
- **Remember** and **rememberSaveable**

---

## 🧪 Testing

### Run Tests:
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

---

## 📦 Build APK

### Debug Build:
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build:
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Install APK:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🐛 Troubleshooting

### Issue: "Could not find google-services.json"
**Solution**: Download from Firebase Console and place in `app/` folder

### Issue: "Maps not showing"
**Solution**: Check `local.properties` has correct MAPS_API_KEY

### Issue: "Build failed - SDK location not found"
**Solution**: Create `local.properties` with SDK path

### Issue: "Firebase Auth not working"
**Solution**: Ensure package name matches in Firebase Console

### Issue: "Arabic text not showing"
**Solution**: Ensure font supports Arabic characters

### Issue: "Location not updating"
**Solution**: Grant location permissions in Settings

---

## 📚 Dependencies

### Core:
- Kotlin 1.9.20
- Compose BOM 2023.10.01
- Material 3 1.1.2

### Firebase:
- firebase-auth-ktx
- firebase-firestore-ktx
- firebase-storage-ktx
- firebase-messaging-ktx

### Google:
- play-services-maps 18.2.0
- play-services-location 21.0.1

### Libraries:
- Coil (image loading)
- Navigation Compose
- Accompanist (permissions, system UI)

---

## 🔄 Sync with iOS

Both apps share:
- ✅ Same Firebase project
- ✅ Same Firestore database
- ✅ Same Storage bucket
- ✅ Same Authentication
- ✅ Same data models
- ✅ Same features

Changes in one app reflect in the other!

---

## 📝 Next Steps

1. ✅ Open project in Android Studio
2. ✅ Download google-services.json
3. ✅ Configure local.properties
4. ✅ Run the app!
5. ✅ Test all features
6. ✅ Switch to Arabic
7. ✅ Generate reports
8. ✅ Upload employee avatar
9. ✅ Track attendance

---

## 🎉 You're All Set!

Your Android app is ready with:
- ✅ Material Design 3
- ✅ Same features as iOS
- ✅ Arabic/RTL support
- ✅ Firebase integration
- ✅ Google Maps
- ✅ Real-time sync

**Start Android Studio and run the app!** 🚀

---

## 📞 Support

If you encounter any issues:
1. Check this guide
2. Check Android Studio logs
3. Check Firebase Console
4. Check Google Cloud Console

Happy coding! 🎊
