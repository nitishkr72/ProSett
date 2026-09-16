package com.prosett.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalControlScreen(onOpenDrawer: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parental Controls") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ListItem(
                headlineContent = { Text("Force Location ON") },
                supportingContent = { Text("Prevents location services from being disabled on this device.") },
                leadingContent = { Icon(Icons.Default.GpsFixed, contentDescription = null) },
                trailingContent = { Switch(checked = false, onCheckedChange = { /* TODO */ }) }
            )
            Divider()
            ListItem(
                headlineContent = { Text("App Lock") },
                supportingContent = { Text("Require a password to open specific apps (e.g. Play Store).") },
                leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingContent = { Button(onClick = {}) { Text("Configure") } }
            )
            Divider()
            ListItem(
                headlineContent = { Text("Prevent Uninstallation") },
                supportingContent = { Text("Require admin password to delete ProSett.") },
                leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                trailingContent = { Switch(checked = false, onCheckedChange = { /* TODO */ }) }
            )
        }
    }
}
