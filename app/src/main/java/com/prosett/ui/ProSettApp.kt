package com.prosett.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProSettApp(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "firewall"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text("ProSett", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                Divider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Security, contentDescription = null) },
                    label = { Text("Network Firewall") },
                    selected = currentRoute == "firewall",
                    onClick = {
                        navController.navigate("firewall") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.SettingsSuggest, contentDescription = null) },
                    label = { Text("Modes") },
                    selected = currentRoute == "modes",
                    onClick = {
                        navController.navigate("modes") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.FamilyRestroom, contentDescription = null) },
                    label = { Text("Parental Controls") },
                    selected = currentRoute == "parental",
                    onClick = {
                        navController.navigate("parental") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("Metrics") },
                    selected = currentRoute == "metrics",
                    onClick = {
                        navController.navigate("metrics") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        NavHost(navController = navController, startDestination = "firewall", modifier = Modifier.fillMaxSize()) {
            composable("firewall") {
                AppListScreen(
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("modes") {
                ModesScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
            }
            composable("parental") {
                ParentalControlScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
            }
            composable("metrics") {
                MetricsScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}
