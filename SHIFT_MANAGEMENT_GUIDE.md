# 🕐 Shift Management Feature - Complete Guide

## Overview

Shift Management allows **Admin users** to configure work schedules for the organization.

### Default Configuration:
- **Work Days**: Monday to Friday
- **Monday-Thursday**: 7:00 AM to 3:00 PM
- **Friday**: 7:00 AM to 11:00 AM
- **Saturday-Sunday**: Day Off

---

## 📱 Android Implementation

### Features Added:

#### 1. **Shift Configuration Data Model**
- `ShiftConfig.kt`: Main configuration model
- `DaySchedule.kt`: Individual day schedule
- `WorkDay` enum: All days of the week

#### 2. **Shift Management Screen**
- Visual time pickers for each day
- Enable/disable work days with toggle switches
- Real-time duration calculation
- Save to Firebase

#### 3. **Settings Integration**
- New "Work Schedule" section (Admin only)
- "Shift Management" row with navigation
- Access: Settings → Work Schedule → Shift Management

---

## 🎯 How to Use (Android)

### Step 1: Access Shift Management

```
1. Login as Admin user
2. Go to Settings tab (bottom right)
3. Scroll to "Work Schedule" section
4. Tap "Shift Management"
```

### Step 2: Configure Work Days

For each day, you can:
- **Toggle ON/OFF**: Enable or disable as work day
- **Set Start Time**: Tap "Start Time" button
- **Set End Time**: Tap "End Time" button
- **View Duration**: Automatically calculated

### Step 3: Save Changes

```
1. Review all changes
2. Tap "Save" button (top right or bottom)
3. Wait for confirmation
4. Configuration saved to Firebase
```

---

## 📊 Screen Layout

```
┌─────────────────────────────────┐
│ ← Shift Management         💾  │ ← Top bar with Save
├─────────────────────────────────┤
│ Configure Work Schedule         │
│ Set working days and hours...   │
│                                 │
│ Work Days                       │
│ ┌─────────────────────────────┐│
│ │ Monday             [ON ]🟢 ││
│ │ Work Day                    ││
│ │ ─────────────────────────── ││
│ │ [Start Time]  [End Time]    ││
│ │   7:00 AM      3:00 PM      ││
│ │        8 hours              ││
│ └─────────────────────────────┘│
│                                 │
│ ┌─────────────────────────────┐│
│ │ Tuesday            [ON ]🟢 ││
│ │ Work Day                    ││
│ │ ─────────────────────────── ││
│ │ [Start Time]  [End Time]    ││
│ │   7:00 AM      3:00 PM      ││
│ │        8 hours              ││
│ └─────────────────────────────┘│
│                                 │
│ ... (Wednesday, Thursday)       │
│                                 │
│ ┌─────────────────────────────┐│
│ │ Friday             [ON ]🟢 ││
│ │ Work Day                    ││
│ │ ─────────────────────────────││
│ │ [Start Time]  [End Time]    ││
│ │   7:00 AM     11:00 AM      ││
│ │        4 hours              ││
│ └─────────────────────────────┘│
│                                 │
│ ┌─────────────────────────────┐│
│ │ Saturday           [OFF]⚪  ││
│ │ Day Off                     ││
│ └─────────────────────────────┘│
│                                 │
│ ┌─────────────────────────────┐│
│ │ Sunday             [OFF]⚪  ││
│ │ Day Off                     ││
│ └─────────────────────────────┘│
│                                 │
│ ┌───────────────────────────┐  │
│ │   💾 Save Changes         │  │
│ └───────────────────────────┘  │
└─────────────────────────────────┘
```

---

## ⏰ Time Picker

When you tap Start/End Time:

```
┌─────────────────────────────────┐
│ Start Time - Monday             │
│                                 │
│        Hour          :   Minute │
│          ↑                  ↑   │
│          07         :    00     │ ← Tap arrows
│          ↓                  ↓   │
│                                 │
│               [Cancel]  [OK]    │
└─────────────────────────────────┘
```

- **Hour**: 24-hour format (0-23)
- **Minute**: 15-minute intervals (00, 15, 30, 45)
- **Display**: 12-hour format with AM/PM

---

## 💾 Firebase Storage

Configuration is saved to:
```
companies/it-adc/settings/shift_config
{
  id: "default",
  name: "Default Shift",
  workDays: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  schedules: {
    MONDAY: { startTime: "07:00", endTime: "15:00", isWorkDay: true },
    TUESDAY: { startTime: "07:00", endTime: "15:00", isWorkDay: true },
    WEDNESDAY: { startTime: "07:00", endTime: "15:00", isWorkDay: true },
    THURSDAY: { startTime: "07:00", endTime: "15:00", isWorkDay: true },
    FRIDAY: { startTime: "07:00", endTime: "11:00", isWorkDay: true },
    SATURDAY: { startTime: "07:00", endTime: "15:00", isWorkDay: false },
    SUNDAY: { startTime: "07:00", endTime: "15:00", isWorkDay: false }
  },
  isActive: true,
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

---

## 🔐 Access Control

**Admin Only**: Only users with `role: ADMIN` can access Shift Management

**Employee/Supervisor**: The "Work Schedule" section doesn't appear in Settings

---

##  iOS Implementation (TODO)

Similar implementation needed for iOS:

### Files to Create:
1. **Models/ShiftConfig.swift**
   ```swift
   struct ShiftConfig: Codable {
       let id: String
       let name: String
       let workDays: [WorkDay]
       let schedules: [String: DaySchedule]
       let isActive: Bool
       let createdAt: Date?
       let updatedAt: Date?
   }
   
   struct DaySchedule: Codable {
       let startTime: String
       let endTime: String
       let isWorkDay: Bool
   }
   
   enum WorkDay: String, Codable, CaseIterable {
       case sunday, monday, tuesday, wednesday, thursday, friday, saturday
   }
   ```

2. **ViewModels/ShiftManagementViewModel.swift**
   ```swift
   @MainActor
   class ShiftManagementViewModel: ObservableObject {
       @Published var shiftConfig: ShiftConfig?
       @Published var isLoading = false
       @Published var errorMessage: String?
       
       func loadShiftConfig() async
       func saveShiftConfig() async
       func toggleWorkDay(_ day: WorkDay, enabled: Bool)
       func updateStartTime(_ day: WorkDay, time: String)
       func updateEndTime(_ day: WorkDay, time: String)
   }
   ```

3. **Views/ShiftManagementView.swift**
   ```swift
   struct ShiftManagementView: View {
       @StateObject private var viewModel = ShiftManagementViewModel()
       
       var body: some View {
           List {
               ForEach(WorkDay.allCases, id: \.self) { day in
                   DayScheduleRow(day: day, ...)
               }
           }
           .navigationTitle("Shift Management")
           .toolbar {
               Button("Save") {
                   Task { await viewModel.saveShiftConfig() }
               }
           }
       }
   }
   ```

4. **Update AdminSettingsView.swift**
   ```swift
   Section(header: Text("Work Schedule")) {
       NavigationLink {
           ShiftManagementView()
       } label: {
           HStack {
               Image(systemName: "clock")
               Text("Shift Management")
               Spacer()
               Text("Configure")
                   .foregroundColor(.secondary)
           }
       }
   }
   .onlyFor(role: .admin) // Show only to admins
   ```

---

## 🎯 Use Cases

### 1. **Standard Workweek**
```
Monday-Friday: 7:00 AM - 3:00 PM
```

### 2. **Half-Day Friday**
```
Monday-Thursday: 7:00 AM - 3:00 PM
Friday: 7:00 AM - 11:00 AM (4 hours) ← Default
```

### 3. **Custom Schedule**
```
Monday: 8:00 AM - 4:00 PM
Tuesday: 7:30 AM - 3:30 PM
Wednesday: OFF
Thursday: 8:00 AM - 4:00 PM
Friday: 8:00 AM - 12:00 PM
```

### 4. **6-Day Workweek**
```
Sunday-Friday: 7:00 AM - 3:00 PM
Saturday: OFF
```

---

## ✅ Validation

The app validates:
- Start time must be before end time
- Minimum shift duration: 1 hour
- Maximum shift duration: 12 hours
- Time format: HH:mm (24-hour)

---

## 📱 Testing Steps

### Test 1: View Current Configuration
```
1. Login as Admin
2. Settings → Work Schedule → Shift Management
3. Verify default: Mon-Thu (7AM-3PM), Fri (7AM-11AM)
```

### Test 2: Change Friday Hours
```
1. Tap Friday card
2. Tap "End Time"
3. Change to 12:00 PM
4. Tap "Save"
5. Verify: Friday now shows "5 hours"
```

### Test 3: Disable Saturday
```
1. Find Saturday card
2. Toggle switch to OFF
3. Verify: Shows "Day Off"
4. Verify: Time buttons disabled
5. Tap "Save"
```

### Test 4: Firebase Sync
```
1. Make changes on Android
2. Save configuration
3. Open Firebase Console
4. Verify: companies/it-adc/settings/shift_config updated
5. Verify: updatedAt timestamp is recent
```

---

## 🔧 Future Enhancements

### Phase 2 Features:
1. **Multiple Shifts**: Morning/Evening/Night shifts
2. **Employee-Specific Schedules**: Override for individuals
3. **Break Times**: Configure lunch/prayer breaks
4. **Flexible Schedules**: Rotating shifts, on-call schedules
5. **Shift Templates**: Save and load common configurations
6. **Holiday Calendar**: Mark public holidays
7. **Overtime Rules**: Define overtime calculation rules
8. **Shift Notifications**: Alert employees of schedule changes

---

## 📊 Benefits

✅ **Centralized**: Single source of truth for work schedules  
✅ **Flexible**: Easy to adjust for holidays/special events  
✅ **Automated**: No need to communicate schedule changes manually  
✅ **Consistent**: Both iOS and Android use same configuration  
✅ **Real-time**: Changes sync instantly across all devices  
✅ **Auditable**: Track when and who changed schedules  

---

## 🚀 Status

**Android**: ✅ **IMPLEMENTED**
- Shift configuration model ✅
- Management screen UI ✅
- Firebase integration ✅
- Settings navigation ✅
- Admin-only access ✅

**iOS**: ⏳ **PENDING**
- Follow implementation guide above
- Reuse Firebase structure
- Match Android UI/UX

---

## 📝 Summary

Shift Management is now available for **Android Admin users**:

1. **Access**: Settings → Work Schedule → Shift Management
2. **Configure**: Set work days and hours for each day
3. **Save**: Stores to Firebase for org-wide use
4. **Default**: Mon-Thu (7AM-3PM), Fri (7AM-11AM)

**Next**: Implement same feature on iOS for complete cross-platform support.
