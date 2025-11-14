package com.ats.android.ui.navigation

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ats.android.models.EmployeeRole
import com.ats.android.ui.screens.*
import com.ats.android.ui.screens.IOSDashboardScreen
import com.ats.android.ui.screens.ExpressiveDashboardScreen
import com.ats.android.ui.screens.ShiftManagementScreen
import com.ats.android.ui.screens.EnhancedMapScreen
import com.ats.android.ui.theme.ComponentShapes
import com.ats.android.viewmodels.AuthViewModel
import com.ats.android.viewmodels.AuthUiState
import com.ats.android.viewmodels.MovementNavigationManager

sealed class Screen(
    val route: String,
    val titleResId: Int,  // Changed from String to resource ID
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val showInNav: Boolean = true,
    val roles: List<EmployeeRole> = listOf(EmployeeRole.EMPLOYEE, EmployeeRole.SUPERVISOR, EmployeeRole.ADMIN)
) {
    object Login : Screen("login", com.ats.android.R.string.login, Icons.Default.Login, showInNav = false)
    object Dashboard : Screen("dashboard", com.ats.android.R.string.dashboard, Icons.Default.Dashboard, roles = listOf(EmployeeRole.ADMIN, EmployeeRole.SUPERVISOR))
    object Map : Screen("map", com.ats.android.R.string.map, Icons.Default.LocationOn, roles = listOf(EmployeeRole.ADMIN, EmployeeRole.SUPERVISOR))
    object CheckIn : Screen("checkin", com.ats.android.R.string.check_in, Icons.Default.CheckCircle, roles = listOf(EmployeeRole.EMPLOYEE, EmployeeRole.SUPERVISOR))
    object History : Screen("history", com.ats.android.R.string.history, Icons.Default.CalendarToday, roles = listOf(EmployeeRole.EMPLOYEE, EmployeeRole.SUPERVISOR))
    object Reports : Screen("reports", com.ats.android.R.string.reports, Icons.Default.BarChart, roles = listOf(EmployeeRole.ADMIN, EmployeeRole.SUPERVISOR))
    object EmployeeManagement : Screen("employees", com.ats.android.R.string.employees, Icons.Default.Groups, roles = listOf(EmployeeRole.ADMIN))
    object Movements : Screen("movements", com.ats.android.R.string.movements, Icons.Default.DirectionsWalk, showInNav = false, roles = listOf(EmployeeRole.ADMIN, EmployeeRole.SUPERVISOR))
    object Settings : Screen("settings", com.ats.android.R.string.settings, Icons.Default.Settings)
}

@Composable
fun ATSNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()
    val currentEmployee by authViewModel.currentEmployee.collectAsState()
    var permissionGranted by remember { mutableStateOf(false) }
    
    // Request location permission on app start
    if (authState is AuthUiState.Authenticated && !permissionGranted) {
        com.ats.android.utils.RequestLocationPermission(
            onPermissionGranted = { permissionGranted = true },
            onPermissionDenied = { /* User can still use app, just can't check in */ }
        )
    }
    
    when (authState) {
        is AuthUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }
        }
        is AuthUiState.Unauthenticated, is AuthUiState.Error -> {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(getStartDestination(currentEmployee?.role)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        is AuthUiState.Authenticated -> {
            MainScaffold(
                navController = navController,
                currentEmployee = currentEmployee,
                onSignOut = {
                    // Just sign out - the auth state change will handle navigation automatically
                    authViewModel.signOut()
                }
            )
        }
    }
}

private fun navigate(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveNavigationBar(
    items: List<Screen>,
    currentDestination: androidx.navigation.NavDestination?,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // Always use compact mode for 5+ items, or if screen is small
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isCompact = items.size >= 5 || screenWidthDp < 400
    
    android.util.Log.d("NavigationBar", "Items: ${items.size}, Screen: ${screenWidthDp}dp, Compact: $isCompact")
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = ComponentShapes.NavigationBar,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ),
        shape = ComponentShapes.NavigationBar,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 64.dp else 80.dp)  // Reduced height for compact
                    .padding(
                        horizontal = if (isCompact) 2.dp else 8.dp,
                        vertical = if (isCompact) 4.dp else 8.dp
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    
                    ExpressiveNavItem(
                        screen = screen,
                        selected = selected,
                        compact = isCompact,
                        onClick = {
                            if (!selected) {
                                navigate(navController, screen.route)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressiveNavItem(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    // Minimal animation for better performance
    val iconSize = if (compact) {
        if (selected) 22.dp else 20.dp
    } else {
        if (selected) 26.dp else 24.dp
    }
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .clip(ComponentShapes.NavigationItem),
        color = Color.Transparent,
        shape = ComponentShapes.NavigationItem
    ) {
        Column(
            modifier = Modifier
                .width(if (compact) 64.dp else 80.dp)  // Fixed width for consistency
                .padding(
                    horizontal = if (compact) 2.dp else 6.dp,
                    vertical = if (compact) 2.dp else 4.dp  // Reduced vertical padding
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top  // Changed from Center to Top
        ) {
            // Icon container - moved up
            Box(
                modifier = Modifier
                    .size(if (compact) 36.dp else 44.dp)  // Slightly smaller
                    .clip(ComponentShapes.NavigationItem)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = stringResource(screen.titleResId),
                    modifier = Modifier.size(iconSize),
                    tint = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(1.dp))  // Reduced space
            
            // Label - more space for text
            Text(
                text = stringResource(screen.titleResId),  // Use stringResource for translation
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compact) 10.sp else 11.sp,  // Slightly larger
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = if (compact) 11.sp else 12.sp
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    navController: NavHostController,
    currentEmployee: com.ats.android.models.Employee?,
    onSignOut: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // DEBUG: Log current employee and role
    LaunchedEffect(currentEmployee) {
        android.util.Log.d("MainScaffold", "Current Employee: ${currentEmployee?.displayName}")
        android.util.Log.d("MainScaffold", "Role: ${currentEmployee?.role}")
    }
    
    // Match iOS navigation order exactly
    val items = buildList {
        currentEmployee?.role?.let { role ->
            android.util.Log.d("MainScaffold", "Building nav items for role: $role")
            when (role) {
                EmployeeRole.ADMIN -> {
                    // Admin: Dashboard, Map, Employees, Reports, Settings
                    add(Screen.Dashboard)
                    add(Screen.Map)
                    add(Screen.EmployeeManagement)
                    add(Screen.Reports)
                    add(Screen.Settings)
                    android.util.Log.d("MainScaffold", "Added 5 tabs for ADMIN")
                }
                EmployeeRole.SUPERVISOR -> {
                    // Supervisor: Dashboard, Map, Check-In, Reports, Settings
                    add(Screen.Dashboard)
                    add(Screen.Map)
                    add(Screen.CheckIn)
                    add(Screen.Reports)
                    add(Screen.Settings)
                    android.util.Log.d("MainScaffold", "Added 5 tabs for SUPERVISOR (Dashboard, Map, CheckIn, Reports, Settings)")
                }
                EmployeeRole.EMPLOYEE -> {
                    // Employee: Check-In, Settings
                    add(Screen.CheckIn)
                    add(Screen.Settings)
                    android.util.Log.d("MainScaffold", "Added 2 tabs for EMPLOYEE")
                }
            }
        }
        android.util.Log.d("MainScaffold", "Total items in nav: ${this.size}")
        this.forEachIndexed { index, screen ->
            android.util.Log.d("MainScaffold", "  Tab $index: ${screen.route}")
        }
    }
    
    // Check if current route is Map to hide top bar only
    val isMapScreen = currentDestination?.route == Screen.Map.route
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!isMapScreen) {
                    Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                } else {
                    Modifier
                }
            )
    ) {
        // Main content
        NavHost(
            navController = navController,
            startDestination = getStartDestination(currentEmployee?.role),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 96.dp)  // Navigation bar height
        ) {
            composable(Screen.Dashboard.route) {
                ExpressiveDashboardScreen(
                    employee = currentEmployee,
                    onNavigateToMovements = {
                        navController.navigate(Screen.Movements.route)
                    }
                )
            }
            composable(Screen.Map.route) {
                EnhancedMapScreen()
            }
            composable(Screen.Movements.route) {
                MovementsListScreen(
                    onViewRoute = { movement ->
                        MovementNavigationManager.setMovement(movement)
                        navController.navigate("movement_route_map")
                    }
                )
            }
            composable(Screen.CheckIn.route) {
                CheckInScreen(currentEmployee)
            }
            composable(Screen.History.route) {
                HistoryScreen(currentEmployee)
            }
            composable(Screen.Reports.route) {
                IOSReportsScreen(currentEmployee = currentEmployee)
            }
            composable(Screen.EmployeeManagement.route) {
                IOSEmployeeManagementScreen()
            }
            composable(Screen.Settings.route) {
                IOSSettingsScreen(
                    currentEmployee = currentEmployee,
                    onNavigateToAttendanceManagement = {
                        navController.navigate("attendance_management")
                    },
                    onNavigateToLanguageSettings = {
                        navController.navigate("language_settings")
                    },
                    onSignOut = onSignOut
                )
            }
            
            // Attendance Management Screen (Admin only)
            composable("attendance_management") {
                AttendanceManagementScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // Language Settings Screen
            composable("language_settings") {
                LanguageSettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // Movement Route Map Screen
            composable("movement_route_map") {
                val movement = MovementNavigationManager.getMovement()
                if (movement != null) {
                    EmployeeRouteMapScreen(
                        movement = movement,
                        onBack = {
                            MovementNavigationManager.clearMovement()
                            navController.popBackStack()
                        }
                    )
                } else {
                    // If movement is null, go back
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }
        }
        
        // Expressive Navigation Bar (always show)
        ExpressiveNavigationBar(
            items = items,
            currentDestination = currentDestination,
            navController = navController,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private fun getStartDestination(role: EmployeeRole?): String {
    return when (role) {
        EmployeeRole.ADMIN, EmployeeRole.SUPERVISOR -> Screen.Dashboard.route
        else -> Screen.CheckIn.route
    }
}
