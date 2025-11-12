# 🎨 iOS to Android Design Conversion Guide

## Design Specifications from iOS App

### Colors
- **Admin**: Purple (#9C27B0)
- **Supervisor**: Blue (#2196F3)  
- **Employee**: Green (#4CAF50)
- **Check-in**: Green (#4CAF50)
- **Check-out**: Blue (#2196F3)
- **Status Change**: Orange (#FF9800)
- **Active Status**: Green dot (8dp circle)

### Corner Radii
- Cards: 12dp
- Buttons/Chips: 8dp
- Search bars: 16dp
- Bottom sheets: 24dp (top corners)
- Avatar: Circle

### Backgrounds
- iOS `.ultraThinMaterial` = Android `surface.copy(alpha = 0.95f)` with blur effect
- Use semi-transparent backgrounds

### Typography
- Title: titleLarge.bold
- Subtitle: subheadline.weight(.medium) = bodyMedium
- Caption: caption = labelSmall
- Body: body = bodyMedium

### Spacing
- Section spacing: 20dp
- Card padding: 16dp
- Small spacing: 8-12dp
- List item padding: 16dp

### Layout Structure

#### Dashboard:
```
ScrollView/LazyColumn
├── Summary Cards (2x2 grid)
│   ├── Active Now (Green icon)
│   ├── Total Employees (Blue icon)
│   ├── On Leave (Orange icon)
│   └── Today's Check-ins (Purple icon)
├── Live Activity Feed
│   └── Activity rows with dividers
└── Active Employees Section
    └── Employee cards with green dot
```

#### Map:
```
Full-screen map
├── Top search bar (expandable)
├── Filter button
└── Bottom employee list (expandable)
    └── Employee rows with distance
```

#### Employee Management:
```
List
└── Employee rows
    ├── Avatar circle (50dp)
    ├── Name + Active status dot
    ├── Employee ID
    └── Role badge
```

This guide ensures pixel-perfect iOS-to-Android conversion.
