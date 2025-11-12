# 🗺️ iOS Map Screen Design Specification

## Current Android Map vs iOS Map

### iOS Map Features (ModernAdminMapView.swift):

#### **1. Top Search Bar**
- **Compact State** (default):
  - `.ultraThinMaterial` background
  - 16dp rounded corners
  - "Search location" placeholder
  - Magnifying glass icon + text
  - Filter button (circle, `.ultraThinMaterial`)
  
- **Expanded State** (when tapped):
  - Full-width search field
  - Real-time search as you type
  - Cancel button (blue text)
  - Clear button (x icon)
  - Search results below with divider
  - Shows distances from search point

#### **2. Bottom Employee List**
- **Compact State** (default):
  - Small button showing employee count
  - "12 active employees" text
  - `.ultraThinMaterial` background
  
- **Expanded State** (when tapped):
  - `ModalBottomSheet` style
  - 24dp rounded top corners
  - Drag handle at top (36dp × 5dp gray bar)
  - Scrollable employee list
  - Each employee row shows:
    - Avatar (50dp circle)
    - Name + role badge
    - Distance from search point (if searching)
    - Check-in time
    - Tap to center map on employee

#### **3. Map Markers**
- **Employee Markers**:
  - Custom markers with avatars
  - Role-colored border (Purple/Blue/Green)
  - Selected marker is larger
  - Nearest employee has special highlight
  
- **Search Location Marker**:
  - Red pin for searched location
  - Shows when user searches a place

#### **4. Animations**
- Smooth expand/collapse with spring animation
- Map camera moves smoothly when selecting employee
- Markers animate when appearing

---

## Implementation Plan

### Phase 1: Basic iOS-Style Map
```kotlin
// IOSMapScreen.kt

@Composable
fun IOSMapScreen() {
    var expandedSearch by remember { mutableStateOf(false) }
    var expandedEmployeeList by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    
    Box(Modifier.fillMaxSize()) {
        // 1. Google Map (full screen)
        GoogleMap(...)
        
        // 2. Top Search Bar
        Column(Modifier.align(Alignment.TopCenter)) {
            if (expandedSearch) {
                ExpandedSearchBar(...)
            } else {
                CompactSearchBar(onClick = { expandedSearch = true })
            }
        }
        
        // 3. Bottom Employee List
        Column(Modifier.align(Alignment.BottomCenter)) {
            if (expandedEmployeeList) {
                EmployeeBottomSheet(...)
            } else {
                CompactEmployeeButton(...)
            }
        }
    }
}
```

### Phase 2: Components

#### **CompactSearchBar**
```kotlin
@Composable
fun CompactSearchBar(
    onClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            cornerRadius = 16.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, null)
                Text("Search location")
            }
        }
        
        GlassCard(
            modifier = Modifier.clickable(onClick = onFilterClick),
            cornerRadius = 50.dp // Circle
        ) {
            Icon(
                Icons.Default.FilterList,
                null,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
```

#### **ExpandedSearchBar**
```kotlin
@Composable
fun ExpandedSearchBar(
    searchText: String,
    onSearchChange: (String) -> Unit,
    onCancel: () -> Unit,
    searchResults: List<PlaceResult>
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        cornerRadius = 16.dp
    ) {
        Column {
            // Search Field
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null)
                Spacer(Modifier.width(12.dp))
                
                TextField(
                    value = searchText,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search places") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
                
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, null)
                    }
                }
                
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color.Blue)
                }
            }
            
            Divider()
            
            // Search Results
            LazyColumn {
                items(searchResults) { result ->
                    SearchResultRow(
                        place = result.name,
                        distance = result.distance,
                        onClick = { /* Move map to location */ }
                    )
                }
            }
        }
    }
}
```

#### **EmployeeBottomSheet**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeBottomSheet(
    employees: List<EmployeeLocation>,
    onDismiss: () -> Unit,
    onEmployeeClick: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedRectangleShape(
            topStart = 24.dp,
            topEnd = 24.dp
        )
    ) {
        Column {
            // Drag Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .background(
                        Color.Gray.copy(alpha = 0.5f),
                        RoundedCornerShape(3.dp)
                    )
            )
            
            Text(
                text = "${employees.size} Active Employees",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            
            Divider()
            
            LazyColumn {
                items(employees) { employee ->
                    EmployeeLocationRow(
                        employee = employee,
                        onClick = { onEmployeeClick(employee.id) }
                    )
                }
            }
        }
    }
}
```

#### **EmployeeLocationRow**
```kotlin
@Composable
fun EmployeeLocationRow(
    employee: EmployeeLocation,
    distance: String?,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            // Use existing EmployeeAvatar component!
            EmployeeAvatar(
                employee = employee.employee,
                size = 50.dp
            )
        },
        headlineContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(employee.name)
                RoleBadge(employee.role) // Use existing component!
            }
        },
        supportingContent = {
            Column {
                Text("Checked in at ${employee.checkInTime}")
                distance?.let {
                    Row {
                        Icon(
                            Icons.Default.Navigation,
                            null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(it)
                    }
                }
            }
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, null)
        }
    )
}
```

### Phase 3: ViewModel Updates

```kotlin
// MapViewModel.kt - Add these features:

class MapViewModel : ViewModel() {
    // Existing...
    
    // Add search functionality
    private val _searchResults = MutableStateFlow<List<PlaceResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()
    
    private val _searchedLocation = MutableStateFlow<LatLng?>(null)
    val searchedLocation = _searchedLocation.asStateFlow()
    
    fun searchPlace(query: String) {
        viewModelScope.launch {
            // Use Google Places API
            val results = placesService.searchPlaces(query)
            _searchResults.value = results
        }
    }
    
    fun calculateDistance(from: LatLng, to: LatLng): String {
        val results = FloatArray(1)
        Location.distanceBetween(
            from.latitude, from.longitude,
            to.latitude, to.longitude,
            results
        )
        
        val distanceKm = results[0] / 1000
        return if (distanceKm < 1) {
            "${(distanceKm * 1000).toInt()}m"
        } else {
            "%.1fkm".format(distanceKm)
        }
    }
    
    fun findNearestEmployee(location: LatLng): EmployeeLocation? {
        return employeeLocations.value.minByOrNull { employee ->
            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                employee.location.latitude, employee.location.longitude,
                results
            )
            results[0]
        }
    }
}
```

---

## Visual Comparison

### iOS Map:
```
┌─────────────────────────────────┐
│ [🔍 Search location]  [≡]      │ ← Glass effect, 16dp rounded
├─────────────────────────────────┤
│                                 │
│         Google Map              │
│    with employee markers        │
│                                 │
├─────────────────────────────────┤
│ ╌╌╌                            │ ← Drag handle
│ 12 Active Employees             │
│ ┌─────────────────────────────┐│
│ │ [Avatar] John Smith         ││
│ │ Admin • 2.3km               ││
│ └─────────────────────────────┘│
│ ...more employees...            │
└─────────────────────────────────┘
```

### Current Android:
```
┌─────────────────────────────────┐
│ Live Map           [↻]          │ ← Standard Material top bar
├─────────────────────────────────┤
│                                 │
│         Google Map              │
│    with basic markers           │
│                                 │
│                                 │
│                                 │
│                                 │
└─────────────────────────────────┘
```

---

## Required Dependencies

```kotlin
// build.gradle.kts

dependencies {
    // Google Places SDK (for search)
    implementation("com.google.android.libraries.places:places:3.3.0")
    
    // Already have:
    // - Google Maps Compose
    // - Material3
    // - Coil (for avatars)
}
```

---

## Key Differences from Current Android

| Feature | iOS | Current Android | Action Needed |
|---------|-----|-----------------|---------------|
| **Search Bar** | Glass effect, expandable | Top bar only | ✅ Add glass compact/expanded |
| **Employee List** | Bottom sheet, expandable | Not implemented | ✅ Add ModalBottomSheet |
| **Distance Calc** | Shows km from search | Not implemented | ✅ Add distance calculation |
| **Animations** | Spring animations | Basic | ✅ Add smooth animations |
| **Markers** | Custom avatars | Basic pins | ✅ Use EmployeeAvatar in markers |
| **Drag Handle** | 36×5dp gray bar | N/A | ✅ Add to bottom sheet |

---

## Implementation Time Estimate

| Task | Time | Status |
|------|------|--------|
| Compact search bar | 30 min | ⏳ Pending |
| Expanded search | 1 hour | ⏳ Pending |
| Bottom sheet | 1 hour | ⏳ Pending |
| Distance calculations | 30 min | ⏳ Pending |
| Custom markers | 1 hour | ⏳ Pending |
| Animations | 30 min | ⏳ Pending |
| Testing | 30 min | ⏳ Pending |
| **Total** | **~5 hours** | **0% Complete** |

---

## Next Steps

1. **Install Google Places SDK** (5 min)
2. **Create IOSMapScreen.kt** (30 min)
3. **Implement CompactSearchBar** (30 min)
4. **Implement ExpandedSearchBar** (1 hour)
5. **Implement EmployeeBottomSheet** (1 hour)
6. **Add distance calculations** (30 min)
7. **Custom avatar markers** (1 hour)
8. **Add animations** (30 min)
9. **Test and refine** (30 min)

---

## Code Structure

```
ui/screens/
├── IOSMapScreen.kt (main map screen)
└── map/
    ├── CompactSearchBar.kt
    ├── ExpandedSearchBar.kt
    ├── EmployeeBottomSheet.kt
    ├── EmployeeLocationRow.kt
    └── CustomMarker.kt

viewmodels/
└── MapViewModel.kt (enhanced with search & distance)

services/
└── PlacesService.kt (Google Places API wrapper)
```

---

## Benefits of Matching iOS Design

1. ✅ **Consistent UX** - Same flow on both platforms
2. ✅ **Better Search** - Expandable search is more intuitive
3. ✅ **Distance Context** - Users see how far employees are
4. ✅ **Easy Access** - Bottom sheet for quick employee list
5. ✅ **Visual Polish** - Glass effect looks modern
6. ✅ **Reuses Components** - EmployeeAvatar, RoleBadge already done!

---

Ready to implement when you want to continue! 🚀
