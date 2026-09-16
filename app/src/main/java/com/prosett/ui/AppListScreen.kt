package com.prosett.ui

import android.app.Activity
import android.net.VpnService
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prosett.ui.components.AppDetailBottomSheet
import com.prosett.ui.components.AppItemCard
import com.prosett.ui.components.DashboardStatsCard
import com.prosett.data.model.InstalledApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    val onToggleWifi: (InstalledApp) -> Unit = remember { { viewModel.toggleWifiAccess(it) } }
    val onToggleMobile: (InstalledApp) -> Unit = remember { { viewModel.toggleMobileAccess(it) } }
    val onToggleBackground: (InstalledApp) -> Unit = remember { { viewModel.toggleBackgroundActivity(it) } }
    val onKillProcess: (InstalledApp) -> Unit = remember { { viewModel.killProcessNow(it) } }
    val onRefreshProcess: (InstalledApp) -> Unit = remember { { viewModel.refreshProcessState(it) } }
    val onAppClick: (InstalledApp) -> Unit = remember { { viewModel.onAppSelectedForDetail(it) } }

    // VPN Preparation launcher
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startFirewall()
        } else {
            viewModel.showMessage("VPN permission required to block network traffic.")
        }
    }

    val launchVpnActivation = {
        try {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnPrepareLauncher.launch(prepareIntent)
            } else {
                viewModel.startFirewall()
            }
        } catch (e: Throwable) {
            Log.e("AppListScreen", "Failed to activate VPN: ${e.message}", e)
            viewModel.showMessage("Unable to activate firewall: ${e.message}")
        }
    }

    // Automatically trigger VPN permission flow when user toggles an app block while VPN is off
    LaunchedEffect(Unit) {
        viewModel.requestVpnEvent.collect {
            if (!uiState.isVpnActive) {
                launchVpnActivation()
            }
        }
    }

    // Snackbar notifications
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_list_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ProSett",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Network & Background Guard",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showMessage("Tip: Tap buttons to toggle Wi-Fi, Data, or Background. Tap header for details.") },
                        modifier = Modifier.testTag("info_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "App Information"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("apps_lazy_column"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Prominent Firewall State Banner
                if (!uiState.isVpnActive) {
                    item(key = "firewall_inactive_alert") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    launchVpnActivation()
                                }
                                .testTag("firewall_inactive_banner"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Firewall is Inactive",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Network blocking requires Master Firewall to be ON.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        launchVpnActivation()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("activate_firewall_banner_button")
                                ) {
                                    Text("Turn On", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 2. Dashboard Stats & Master Switches Card
                item(key = "dashboard_stats") {
                    DashboardStatsCard(
                        isVpnActive = uiState.isVpnActive,
                        totalApps = uiState.totalAppsCount,
                        wifiBlockedCount = uiState.wifiBlockedCount,
                        mobileBlockedCount = uiState.mobileBlockedCount,
                        bgRestrictedCount = uiState.backgroundRestrictedCount,
                        onToggleVpn = { willEnable ->
                            if (willEnable) {
                                launchVpnActivation()
                            } else {
                                viewModel.stopFirewall()
                            }
                        },
                        onKillAllRestricted = { viewModel.stopAllRestrictedBackgroundProcesses() },
                        onBatchWifiToggle = { blocked -> viewModel.batchSetWifiForDisplayed(blocked) },
                        onBatchMobileToggle = { blocked -> viewModel.batchSetMobileForDisplayed(blocked) }
                    )
                }

                // 3. Search Box
                item(key = "search_box") {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_apps_input"),
                        placeholder = { Text("Search by name or package...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // 4. Filter Tabs (Horizontal Scroll)
                item(key = "filter_tabs") {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("filter_tabs_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(AppFilterTab.values()) { tab ->
                            val isSelected = uiState.selectedFilter == tab
                            val count = when (tab) {
                                AppFilterTab.ALL -> uiState.totalAppsCount
                                AppFilterTab.USER -> uiState.userAppsCount
                                AppFilterTab.SYSTEM -> uiState.systemAppsCount
                                AppFilterTab.WIFI_BLOCKED -> uiState.wifiBlockedCount
                                AppFilterTab.MOBILE_BLOCKED -> uiState.mobileBlockedCount
                                AppFilterTab.BACKGROUND_RESTRICTED -> uiState.backgroundRestrictedCount
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onFilterSelected(tab) },
                                label = {
                                    Text(
                                        text = "${tab.label} ($count)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.testTag("filter_tab_${tab.name}")
                            )
                        }
                    }
                }

                // Section Title with item count
                item(key = "section_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${uiState.selectedFilter.label} (${uiState.displayedApps.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.displayedApps.isNotEmpty()) {
                            Text(
                                text = "Tap chips to toggle access",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 5. List of Installed Apps
                if (uiState.displayedApps.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp)
                                .testTag("empty_apps_view"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No applications found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.searchQuery.isNotEmpty())
                                        "No apps matched '${uiState.searchQuery}'"
                                    else
                                        "No apps under this category",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = uiState.displayedApps,
                        key = { it.packageName }
                    ) { app ->
                        AppItemCard(
                            app = app,
                            isRecentlyStopped = uiState.recentlyStoppedPackages.contains(app.packageName),
                            onToggleWifi = onToggleWifi,
                            onToggleMobile = onToggleMobile,
                            onToggleBackground = onToggleBackground,
                            onKillProcess = onKillProcess,
                            onRefreshProcess = onRefreshProcess,
                            onAppClick = onAppClick
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Modal Bottom Sheet for detailed app control
    uiState.selectedAppForDetail?.let { selectedApp ->
        AppDetailBottomSheet(
            app = selectedApp,
            isRecentlyStopped = uiState.recentlyStoppedPackages.contains(selectedApp.packageName),
            onDismiss = { viewModel.onAppSelectedForDetail(null) },
            onToggleWifi = { viewModel.toggleWifiAccess(selectedApp) },
            onToggleMobile = { viewModel.toggleMobileAccess(selectedApp) },
            onToggleBoth = { viewModel.toggleBothNetworkAccess(selectedApp) },
            onToggleBackground = { viewModel.toggleBackgroundActivity(selectedApp) },
            onKillProcess = { viewModel.killProcessNow(selectedApp) },
            onRefreshProcess = { viewModel.refreshProcessState(selectedApp) },
            onOpenSettings = { viewModel.openAppSettings(selectedApp.packageName) },
            onOpenBatterySettings = { viewModel.openBatterySettings() },
            onLaunchApp = { viewModel.launchApp(selectedApp.packageName) }
        )
    }
}
