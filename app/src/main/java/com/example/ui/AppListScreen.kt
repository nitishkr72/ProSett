package com.example.ui

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppDetailBottomSheet
import com.example.ui.components.AppItemCard
import com.example.ui.components.DashboardStatsCard
import com.example.ui.theme.StatusAllowed
import com.example.ui.theme.StatusBlocked
import com.example.ui.theme.StatusRestricted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
                                text = "App Controller",
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
                        onClick = { viewModel.showMessage("Tip: Tap an app to view detailed system info and settings.") },
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
                    titleContentColor = MaterialTheme.colorScheme.onBackground
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
                // 1. Dashboard Stats & Master Switches Card
                item(key = "dashboard_stats") {
                    DashboardStatsCard(
                        isVpnActive = uiState.isVpnActive,
                        totalApps = uiState.totalAppsCount,
                        wifiBlockedCount = uiState.wifiBlockedCount,
                        mobileBlockedCount = uiState.mobileBlockedCount,
                        bgRestrictedCount = uiState.backgroundRestrictedCount,
                        onToggleVpn = { willEnable ->
                            if (willEnable) {
                                val prepareIntent = VpnService.prepare(context)
                                if (prepareIntent != null) {
                                    vpnPrepareLauncher.launch(prepareIntent)
                                } else {
                                    viewModel.startFirewall()
                                }
                            } else {
                                viewModel.stopFirewall()
                            }
                        },
                        onKillAllRestricted = { viewModel.stopAllRestrictedBackgroundProcesses() },
                        onBatchWifiToggle = { blocked -> viewModel.batchSetWifiForDisplayed(blocked) },
                        onBatchMobileToggle = { blocked -> viewModel.batchSetMobileForDisplayed(blocked) }
                    )
                }

                // 2. Search Box
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

                // 3. Filter Tabs (Horizontal Scroll)
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
                                text = "Tap switches to toggle",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 4. List of Installed Apps
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
                            onToggleWifi = { viewModel.toggleWifiAccess(app) },
                            onToggleMobile = { viewModel.toggleMobileAccess(app) },
                            onToggleBackground = { viewModel.toggleBackgroundActivity(app) },
                            onKillProcess = { viewModel.killProcessNow(app) },
                            onAppClick = { viewModel.onAppSelectedForDetail(app) }
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
            onDismiss = { viewModel.onAppSelectedForDetail(null) },
            onToggleWifi = { viewModel.toggleWifiAccess(selectedApp) },
            onToggleMobile = { viewModel.toggleMobileAccess(selectedApp) },
            onToggleBoth = { viewModel.toggleBothNetworkAccess(selectedApp) },
            onToggleBackground = { viewModel.toggleBackgroundActivity(selectedApp) },
            onKillProcess = { viewModel.killProcessNow(selectedApp) },
            onOpenSettings = { viewModel.openAppSettings(selectedApp.packageName) },
            onOpenBatterySettings = { viewModel.openBatterySettings() },
            onLaunchApp = { viewModel.launchApp(selectedApp.packageName) }
        )
    }
}
