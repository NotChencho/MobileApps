package es.uc3m.android.mobile_app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Reviews
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Reviews
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.uc3m.android.mobile_app.screens.ExploreScreen
import es.uc3m.android.mobile_app.screens.LoginScreen
import es.uc3m.android.mobile_app.screens.MyProfileScreen
import es.uc3m.android.mobile_app.screens.RestaurantDetailsScreen
import es.uc3m.android.mobile_app.screens.ReviewsScreen
import es.uc3m.android.mobile_app.screens.SettingsScreen
import es.uc3m.android.mobile_app.screens.SignUpScreen
import es.uc3m.android.mobile_app.ui.theme.MyAppTheme
import es.uc3m.android.mobile_app.viewmodel.AuthResult
import es.uc3m.android.mobile_app.viewmodel.MyViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyAppTheme {
                // Create a single ViewModel instance to share across the app
                val viewModel: MyViewModel = viewModel()
                MyScreen(viewModel)
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

@Composable
fun MyScreen(viewModel: MyViewModel) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snack = remember { SnackbarHostState() }

    // --- Observe Authentication State ---
    val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val authResult by viewModel.authResult.collectAsState()

    // --- Define Navigation Items ---
    val allItems = remember {
        listOf(
            NavigationItem(
                title = "Explore",
                selectedIcon = Icons.Filled.Explore,
                unselectedIcon = Icons.Outlined.Explore,
                route = NavGraph.Explore.route
            ),
            NavigationItem(
                title = "Reviews",
                selectedIcon = Icons.Filled.Reviews,
                unselectedIcon = Icons.Outlined.Reviews,
                route = NavGraph.Reviews.route
            ),
            NavigationItem(
                title = "Settings",
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                route = NavGraph.Settings.createRoute("From Navigation")
            ),
            NavigationItem(
                title = "Profile",
                selectedIcon = Icons.Filled.AccountCircle,
                unselectedIcon = Icons.Filled.AccountCircle,
                route = NavGraph.Profile.route
            )
        )
    }

    // --- Determine Visible Navigation Items Based on Auth State ---
    val visibleItems = remember(isLoggedIn) {
        if (isLoggedIn) {
            allItems.filter { it.route != NavGraph.Login.route && it.route != NavGraph.SignUp.route }
        } else {
            emptyList()
        }
    }

    // --- Preload Restaurants Data When Logged In ---
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            viewModel.loadRestaurants()
        }
    }

    // --- Handle Post-Authentication Navigation ---
    LaunchedEffect(isLoggedIn, authResult) {
        if (isLoggedIn && authResult == AuthResult.Success) {
            navController.navigate(NavGraph.Explore.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
            viewModel.resetAuthResult()
        }
    }

    // --- Handle Authentication Errors ---
    LaunchedEffect(authResult) {
        if (authResult is AuthResult.Error) {
            snack.showSnackbar("Error: ${(authResult as AuthResult.Error).message}")
            viewModel.resetAuthResult()
        }
    }

    // --- Main UI Structure ---
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isLoggedIn,
        drawerContent = {
            if (isLoggedIn) {
                MyDrawerContent(
                    items = visibleItems,
                    scope = scope,
                    drawerState = drawerState,
                    navController = navController,
                    viewModel = viewModel
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                if (isLoggedIn) {
                    MyTopAppBar(scope, drawerState, snack)
                }
            },
            floatingActionButton = {
                if (isLoggedIn) {
                    MyFloatingActionButtons(scope, snack, navController)
                }
            },
            content = { innerPadding ->
                MyContent(
                    modifier = Modifier.padding(innerPadding),
                    navController = navController,
                    isLoggedIn = isLoggedIn,
                    viewModel = viewModel
                )
            },
            bottomBar = {
                if (isLoggedIn) {
                    MyNavigationBar(
                        items = visibleItems,
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            },
            snackbarHost = { SnackbarHost(snack) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopAppBar(scope: CoroutineScope, drawerState: DrawerState, snack: SnackbarHostState) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = {
                scope.launch {
                    drawerState.open()
                }
            }) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = stringResource(R.string.menu)
                )
            }
        },
        title = { Text(text = stringResource(R.string.app_name)) },
        actions = {
            IconButton(onClick = {
                scope.launch {
                    snack.showSnackbar("TODO: Search")
                }
            }) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.search)
                )
            }
            MyDropdownMenu(scope, snack)
        })
}

@Composable
fun MyDropdownMenu(scope: CoroutineScope, snack: SnackbarHostState) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.dropdown_1)) },
                onClick = {
                    scope.launch {
                        snack.showSnackbar("TODO: Option 1")
                    }
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.dropdown_2)) },
                onClick = {
                    scope.launch {
                        snack.showSnackbar("TODO: Option 2")
                    }
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun MyFloatingActionButtons(scope: CoroutineScope, snack: SnackbarHostState, navController: NavHostController) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    if (currentRoute == NavGraph.Reviews.route) {
        FloatingActionButton(onClick = {
            scope.launch {
                snack.showSnackbar("TODO: Add")
            }
        }) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
        }
    }
}

// --- Updated MyContent ---
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MyContent(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    isLoggedIn: Boolean,
    viewModel: MyViewModel
) {
    val startDestination = if (isLoggedIn) NavGraph.Explore.route else NavGraph.Login.route

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
    ) {
        // --- Authentication Routes ---
        composable(NavGraph.Login.route) {
            LoginScreen(navController = navController, viewModel = viewModel)
        }
        composable(NavGraph.SignUp.route) {
            SignUpScreen(viewModel = viewModel, navController = navController)
        }

        // --- Main App Routes ---
        composable(NavGraph.Explore.route) {
            ExploreScreen(navController = navController, viewModel = viewModel)
        }
        composable(NavGraph.Reviews.route) {
            ReviewsScreen(navController = navController, viewModel = viewModel)
        }
        composable(
            NavGraph.Settings.route,
            arguments = listOf(navArgument("source") { type = NavType.StringType })
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source")
            SettingsScreen(navController = navController, source)
        }
        composable(NavGraph.Profile.route) {
            MyProfileScreen(navController = navController, viewModel = viewModel)
        }

        // Updated to handle restaurant detail navigation with optional ID
        composable(
            NavGraph.RestaurantDetails.route,
            arguments = listOf(
                navArgument("restaurantId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val restaurantId = backStackEntry.arguments?.getString("restaurantId")
            RestaurantDetailsScreen(
                navController = navController,
                viewModel = viewModel,
                restaurantId = restaurantId
            )
        }
    }
}

@Composable
fun MyDrawerContent(
    items: List<NavigationItem>,
    scope: CoroutineScope,
    drawerState: DrawerState,
    navController: NavHostController,
    viewModel: MyViewModel
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    ModalDrawerSheet {
        Spacer(modifier = Modifier.height(24.dp))
        items.forEachIndexed { index, item ->
            val isSelected = item.route == currentRoute && item.route != "logout_action"

            NavigationDrawerItem(
                label = { Text(text = item.title) },
                selected = isSelected,
                onClick = {
                    scope.launch { drawerState.close() }

                    if (item.route == "logout_action") {
                        viewModel.logout()
                    } else {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        // Add logout item separately
        NavigationDrawerItem(
            label = { Text("Logout") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                viewModel.logout()
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.AccountCircle, // Use appropriate logout icon
                    contentDescription = "Logout"
                )
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

@Composable
fun MyNavigationBar(
    items: List<NavigationItem>,
    navController: NavHostController,
    viewModel: MyViewModel
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    NavigationBar {
        items.forEachIndexed { index, item ->
            val isSelected = item.route == currentRoute && item.route != "logout_action"

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (item.route == "logout_action") {
                        viewModel.logout()
                    } else {
                        // --- Handle Regular Navigation ---
                        // Always navigate to the selected destination
                        navController.navigate(item.route) {
                            // Pop up to the destination route itself, inclusively.
                            // This clears any screens that were pushed on top of this destination within the tab.
                            popUpTo(item.route) {
                                inclusive = true
                            }
                            // Avoid multiple copies of the same destination when reselecting the same item
                            launchSingleTop = true
                            // Restore state when navigating back to previously visited destinations
                            restoreState = true
                        }
                    }
                },
                label = {
                    Text(text = item.title)
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMyScreen() {
    MyAppTheme {
        MyScreen(viewModel())
    }
}