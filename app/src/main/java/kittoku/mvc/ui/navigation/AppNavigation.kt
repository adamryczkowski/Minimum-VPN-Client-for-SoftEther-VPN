package kittoku.mvc.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kittoku.mvc.ui.screen.AboutScreen
import kittoku.mvc.ui.screen.HomeScreen
import kittoku.mvc.ui.screen.SettingsScreen

// Navigation routes
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    data object Home : Screen("home", "Home", Icons.Default.Home)

    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    data object About : Screen("about", "About", Icons.Default.Info)
}

val bottomNavItems =
    listOf(
        Screen.Home,
        Screen.Settings,
        Screen.About,
    )

@Composable
fun AppNavigation(
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onConnectClick = onConnectClick,
                    onDisconnectClick = onDisconnectClick,
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(Screen.About.route) {
                AboutScreen()
            }
        }
    }
}
