package com.prosett.ui

import android.app.Application
import android.net.VpnService
import android.util.Log
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prosett.data.local.AppDatabase
import com.prosett.data.model.InstalledApp
import com.prosett.data.repository.AppManagerRepository
import com.prosett.service.FirewallVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ThemeMode {
    SYSTEM, LIGHT, DARK_NEUTRAL, DARK_TINTED
}

enum class AppFilterTab(val label: String) {
    ALL("All"),
    USER("User Apps"),
    SYSTEM("System"),
    WIFI_BLOCKED("Wi-Fi Blocked"),
    MOBILE_BLOCKED("Cellular Blocked"),
    BACKGROUND_RESTRICTED("BG Restricted")
}

data class AppUiState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val searchQuery: String = "",
    val selectedFilter: AppFilterTab = AppFilterTab.USER,
    val allApps: List<InstalledApp> = emptyList(),
    val displayedApps: List<InstalledApp> = emptyList(),
    val totalAppsCount: Int = 0,
    val userAppsCount: Int = 0,
    val systemAppsCount: Int = 0,
    val wifiBlockedCount: Int = 0,
    val mobileBlockedCount: Int = 0,
    val backgroundRestrictedCount: Int = 0,
    val isVpnActive: Boolean = false,
    val vpnActiveBlockedCount: Int = 0,
    val selectedAppForDetail: InstalledApp? = null,
    val recentlyStoppedPackages: Set<String> = emptySet(),
    val snackbarMessage: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppManagerRepository
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(AppFilterTab.USER)
    private val _selectedAppForDetail = MutableStateFlow<InstalledApp?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    val isVpnActive: StateFlow<Boolean> = FirewallVpnService.isRunning
    val vpnBlockedCount: StateFlow<Int> = FirewallVpnService.blockedAppsCount

    val requestVpnEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AppManagerRepository(application, db.appRuleDao())
    }

    private val prefs = application.getSharedPreferences("prosett_theme_prefs", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(prefs.getString("selected_theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
    )
    private val _recentlyStoppedPackages = MutableStateFlow<Set<String>>(emptySet())

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("selected_theme_mode", mode.name).apply()
    }

    private data class FilterState(
        val query: String,
        val filter: AppFilterTab,
        val detailApp: InstalledApp?,
        val themeMode: ThemeMode,
        val recentlyStoppedPackages: Set<String>
    )

    private val _filterState = combine(
        _searchQuery,
        _selectedFilter,
        _selectedAppForDetail,
        _themeMode,
        _recentlyStoppedPackages
    ) { query, filter, detailApp, themeMode, stopped ->
        FilterState(query, filter, detailApp, themeMode, stopped)
    }

    val uiState: StateFlow<AppUiState> = combine(
        repository.getInstalledAppsFlow(),
        _filterState,
        _snackbarMessage,
        isVpnActive,
        vpnBlockedCount
    ) { apps, filterState, message, vpnActive, vpnBlocked ->
        val query = filterState.query
        val filter = filterState.filter
        val detailApp = filterState.detailApp
        val themeMode = filterState.themeMode
        val recentlyStopped = filterState.recentlyStoppedPackages

        val totalCount = apps.size
        val userCount = apps.count { !it.isSystemApp }
        val systemCount = apps.count { it.isSystemApp }
        val wifiBlocked = apps.count { it.isWifiBlocked }
        val mobileBlocked = apps.count { it.isMobileBlocked }
        val bgRestricted = apps.count { it.isBackgroundRestricted }

        val filtered = apps.filter { app ->
            // Filter category
            val matchesTab = when (filter) {
                AppFilterTab.ALL -> true
                AppFilterTab.USER -> !app.isSystemApp
                AppFilterTab.SYSTEM -> app.isSystemApp
                AppFilterTab.WIFI_BLOCKED -> app.isWifiBlocked
                AppFilterTab.MOBILE_BLOCKED -> app.isMobileBlocked
                AppFilterTab.BACKGROUND_RESTRICTED -> app.isBackgroundRestricted
            }

            // Search query
            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)

            matchesTab && matchesQuery
        }

        // Keep selectedAppForDetail updated with latest status
        val updatedDetail = detailApp?.let { cur ->
            apps.firstOrNull { it.packageName == cur.packageName } ?: cur
        }

        AppUiState(
            isLoading = false,
            themeMode = themeMode,
            searchQuery = query,
            selectedFilter = filter,
            allApps = apps,
            displayedApps = filtered,
            totalAppsCount = totalCount,
            userAppsCount = userCount,
            systemAppsCount = systemCount,
            wifiBlockedCount = wifiBlocked,
            mobileBlockedCount = mobileBlocked,
            backgroundRestrictedCount = bgRestricted,
            isVpnActive = vpnActive,
            vpnActiveBlockedCount = vpnBlocked,
            selectedAppForDetail = updatedDetail,
            recentlyStoppedPackages = recentlyStopped,
            snackbarMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppUiState()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelected(filter: AppFilterTab) {
        _selectedFilter.value = filter
    }

    fun onAppSelectedForDetail(app: InstalledApp?) {
        _selectedAppForDetail.value = app
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun toggleWifiAccess(app: InstalledApp) {
        viewModelScope.launch {
            val newBlocked = !app.isWifiBlocked
            repository.setWifiBlocked(app.packageName, app.appName, newBlocked)
            if (newBlocked) {
                // Drop existing lingering sockets
                repository.killAppBackgroundProcess(app.packageName)
                if (!isVpnActive.value) {
                    requestVpnEvent.tryEmit(Unit)
                }
            }
            FirewallVpnService.reloadRules(getApplication())
            val status = if (newBlocked) "blocked on Wi-Fi" else "allowed on Wi-Fi"
            _snackbarMessage.value = "${app.appName} $status"
        }
    }

    fun toggleMobileAccess(app: InstalledApp) {
        viewModelScope.launch {
            val newBlocked = !app.isMobileBlocked
            repository.setMobileBlocked(app.packageName, app.appName, newBlocked)
            if (newBlocked) {
                // Drop existing lingering sockets
                repository.killAppBackgroundProcess(app.packageName)
                if (!isVpnActive.value) {
                    requestVpnEvent.tryEmit(Unit)
                }
            }
            FirewallVpnService.reloadRules(getApplication())
            val status = if (newBlocked) "blocked on Cellular" else "allowed on Cellular"
            _snackbarMessage.value = "${app.appName} $status"
        }
    }

    fun toggleBothNetworkAccess(app: InstalledApp) {
        viewModelScope.launch {
            val willBlock = !app.isBothNetworkBlocked
            repository.setBothNetworkBlocked(app.packageName, app.appName, willBlock)
            if (willBlock) {
                // Drop existing lingering sockets
                repository.killAppBackgroundProcess(app.packageName)
                if (!isVpnActive.value) {
                    requestVpnEvent.tryEmit(Unit)
                }
            }
            FirewallVpnService.reloadRules(getApplication())
            val status = if (willBlock) "cut off from all networks" else "restored network access"
            _snackbarMessage.value = "${app.appName} $status"
        }
    }

    fun toggleBackgroundActivity(app: InstalledApp) {
        viewModelScope.launch {
            val newRestricted = !app.isBackgroundRestricted
            repository.setBackgroundRestricted(app.packageName, app.appName, newRestricted)
            if (newRestricted) {
                repository.killAppBackgroundProcess(app.packageName)
            }
            val status = if (newRestricted) "Background stopped & restricted" else "Background execution allowed"
            _snackbarMessage.value = "${app.appName}: $status"
        }
    }

    fun killProcessNow(app: InstalledApp) {
        viewModelScope.launch {
            val killed = repository.killAppBackgroundProcess(app.packageName)
            if (killed) {
                _recentlyStoppedPackages.update { it + app.packageName }
            }
            _snackbarMessage.value = if (killed) {
                "Background process stopped for ${app.appName}"
            } else {
                "Could not stop ${app.appName}"
            }
        }
    }

    fun refreshProcessState(app: InstalledApp) {
        // When user clicks the checkmark, we "refresh" by assuming it might be running again,
        // letting them stop it once more if they want.
        _recentlyStoppedPackages.update { it - app.packageName }
    }

    fun stopAllRestrictedBackgroundProcesses() {
        viewModelScope.launch {
            val currentApps = uiState.value.allApps
            val killed = repository.killAllRestrictedApps(currentApps)
            _snackbarMessage.value = if (killed > 0) {
                "Stopped background activity for $killed restricted apps"
            } else {
                "No restricted background processes currently active"
            }
        }
    }

    fun batchSetWifiForDisplayed(blocked: Boolean) {
        viewModelScope.launch {
            val targetApps = uiState.value.displayedApps
            repository.setBatchWifiBlocked(targetApps, blocked)
            FirewallVpnService.reloadRules(getApplication())
            val action = if (blocked) "Blocked Wi-Fi" else "Allowed Wi-Fi"
            _snackbarMessage.value = "$action for ${targetApps.size} apps"
        }
    }

    fun batchSetMobileForDisplayed(blocked: Boolean) {
        viewModelScope.launch {
            val targetApps = uiState.value.displayedApps
            repository.setBatchMobileBlocked(targetApps, blocked)
            FirewallVpnService.reloadRules(getApplication())
            val action = if (blocked) "Blocked Cellular" else "Allowed Cellular"
            _snackbarMessage.value = "$action for ${targetApps.size} apps"
        }
    }

    fun batchSetBackgroundForDisplayed(restricted: Boolean) {
        viewModelScope.launch {
            val targetApps = uiState.value.displayedApps
            repository.setBatchBackgroundRestricted(targetApps, restricted)
            val action = if (restricted) "Restricted background" else "Allowed background"
            _snackbarMessage.value = "$action for ${targetApps.size} apps"
        }
    }

    fun openAppSettings(packageName: String) {
        repository.openAppInfoSettings(packageName)
    }

    fun openBatterySettings() {
        repository.openBatteryOptimizationSettings()
    }

    fun launchApp(packageName: String) {
        val launched = repository.launchApp(packageName)
        if (!launched) {
            _snackbarMessage.value = "Cannot launch this application directly"
        }
    }

    fun startFirewall() {
        try {
            FirewallVpnService.start(getApplication())
            _snackbarMessage.value = "Firewall protection started"
        } catch (e: Throwable) {
            Log.e("AppViewModel", "Failed to start firewall", e)
            _snackbarMessage.value = "Unable to start firewall: ${e.message}"
        }
    }

    fun stopFirewall() {
        try {
            FirewallVpnService.stop(getApplication())
            _snackbarMessage.value = "Firewall protection stopped"
        } catch (e: Throwable) {
            Log.e("AppViewModel", "Failed to stop firewall", e)
            _snackbarMessage.value = "Unable to stop firewall: ${e.message}"
        }
    }
}
