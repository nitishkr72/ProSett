package com.prosett.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.prosett.MainActivity
import com.prosett.R
import com.prosett.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream

class FirewallVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        const val TAG = "FirewallVpnService"
        const val ACTION_START = "com.prosett.action.START_FIREWALL"
        const val ACTION_STOP = "com.prosett.action.STOP_FIREWALL"
        const val ACTION_RELOAD = "com.prosett.action.RELOAD_RULES"

        private const val NOTIFICATION_CHANNEL_ID = "firewall_service_channel"
        private const val NOTIFICATION_ID = 1001

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _blockedAppsCount = MutableStateFlow(0)
        val blockedAppsCount: StateFlow<Int> = _blockedAppsCount.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, FirewallVpnService::class.java).apply {
                action = ACTION_START
            }
            try {
                // When called from the foreground app UI, startService starts the service reliably
                // without imposing the crash-on-timeout startForegroundService watchdog
                val comp = context.startService(intent)
                if (comp == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "startService failed, falling back to startForegroundService: ${e.message}", e)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } catch (e2: Throwable) {
                    Log.e(TAG, "Error starting FirewallVpnService", e2)
                }
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FirewallVpnService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Throwable) {
                Log.e(TAG, "Error stopping FirewallVpnService", e)
            }
        }

        fun reloadRules(context: Context) {
            if (_isRunning.value) {
                val intent = Intent(context, FirewallVpnService::class.java).apply {
                    action = ACTION_RELOAD
                }
                try {
                    context.startService(intent)
                } catch (e: Throwable) {
                    Log.e(TAG, "Error reloading rules", e)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopFirewall()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RELOAD -> {
                scope.launch {
                    reloadFirewallTunnel()
                }
                return START_STICKY
            }
            ACTION_START, null -> {
                tryStartForeground()
                _isRunning.value = true
                scope.launch {
                    reloadFirewallTunnel()
                }
                return START_STICKY
            }
        }
        return START_STICKY
    }

    private fun tryStartForeground() {
        try {
            val notification = buildNotification("Firewall Active", "Network guard enabled")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (e: Throwable) {
                    Log.w(TAG, "Could not startForeground with specialUse: ${e.message}, falling back to default", e)
                    try {
                        startForeground(NOTIFICATION_ID, notification)
                    } catch (e2: Throwable) {
                        Log.w(TAG, "Could not startForeground: ${e2.message}. Native VPN key indicator will be used.", e2)
                    }
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Could not startForeground: ${e.message}. Operating with native VPN indicator.", e)
        }
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (_isRunning.value) {
                        scope.launch { reloadFirewallTunnel() }
                    }
                }

                override fun onLost(network: Network) {
                    if (_isRunning.value) {
                        scope.launch { reloadFirewallTunnel() }
                    }
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    // Ignore the VPN network's own capabilities change
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return
                    if (_isRunning.value) {
                        scope.launch { reloadFirewallTunnel() }
                    }
                }
            }
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    private fun isCurrentNetworkWifi(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        try {
            for (network in cm.allNetworks) {
                val caps = cm.getNetworkCapabilities(network) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                ) {
                    return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    private fun isCurrentNetworkCellular(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        try {
            for (network in cm.allNetworks) {
                val caps = cm.getNetworkCapabilities(network) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                ) {
                    return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    private suspend fun reloadFirewallTunnel() {
        try {
            val db = AppDatabase.getDatabase(this)
            val rules = db.appRuleDao().getAllRules().firstOrNull() ?: emptyList()

            val isWifi = isCurrentNetworkWifi()
            val isCellular = isCurrentNetworkCellular()
            val isIndeterminate = !isWifi && !isCellular

            val blockedPackageNames = rules.filter { rule ->
                when {
                    rule.isWifiBlocked && rule.isMobileBlocked -> true
                    isWifi && rule.isWifiBlocked -> true
                    isCellular && rule.isMobileBlocked -> true
                    isIndeterminate && (rule.isWifiBlocked || rule.isMobileBlocked) -> true
                    else -> false
                }
            }.map { it.packageName }

            _blockedAppsCount.value = blockedPackageNames.size

            val netLabel = when {
                isWifi -> "Wi-Fi"
                isCellular -> "Mobile Data"
                else -> "Active Network"
            }
            updateNotification(
                "Firewall Active",
                "${blockedPackageNames.size} apps blocked on $netLabel"
            )

            rebuildTunInterface(blockedPackageNames)
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading firewall tunnel", e)
        }
    }

    private fun rebuildTunInterface(blockedPackages: List<String>) {
        try {
            if (blockedPackages.isEmpty()) {
                Log.d(TAG, "No apps configured for blocking. Releasing TUN interface.")
                serviceJob?.cancel()
                serviceJob = null
                try { vpnInterface?.close() } catch (_: Throwable) {}
                vpnInterface = null
                return
            }

            val builder = Builder()
            builder.setSession("Firewall Guard")
            try {
                builder.setMtu(1500)
            } catch (e: Throwable) {
                Log.w(TAG, "Could not set MTU", e)
            }

            // IPv4 dummy subnet & route (traps all traffic for blocked apps into sinkhole)
            try {
                builder.addAddress("10.254.1.2", 24)
            } catch (e: Throwable) {
                Log.w(TAG, "Could not add IPv4 address", e)
            }
            try {
                builder.addRoute("0.0.0.0", 0)
            } catch (e: Throwable) {
                Log.w(TAG, "Could not add IPv4 route", e)
            }

            // IPv6 dummy subnet & route
            try {
                builder.addAddress("fd00:1::2", 64)
            } catch (e: Throwable) {
                Log.w(TAG, "Could not add IPv6 address", e)
            }
            try {
                builder.addRoute("::", 0)
            } catch (e: Throwable) {
                Log.w(TAG, "Could not add IPv6 route", e)
            }

            // Sinkhole DNS queries so blocked apps cannot resolve hostnames
            try {
                builder.addDnsServer("10.254.1.2")
            } catch (e: Throwable) {
                Log.w(TAG, "Could not add IPv4 DNS server", e)
            }
            try {
                builder.addDnsServer("fd00:1::2")
            } catch (e: Throwable) {
                Log.w(TAG, "Could not add IPv6 DNS server", e)
            }
            try {
                builder.setBlocking(true)
            } catch (e: Throwable) {
                Log.w(TAG, "Could not set blocking mode", e)
            }

            var addedAppsCount = 0
            for (pkg in blockedPackages) {
                try {
                    builder.addAllowedApplication(pkg)
                    addedAppsCount++
                } catch (e: PackageManager.NameNotFoundException) {
                    // App uninstalled
                } catch (e: Throwable) {
                    Log.w(TAG, "Could not add allowed app to VPN: $pkg", e)
                }
            }

            if (addedAppsCount == 0) {
                serviceJob?.cancel()
                serviceJob = null
                try { vpnInterface?.close() } catch (_: Throwable) {}
                vpnInterface = null
                return
            }

            val newVpnInterface = try {
                builder.establish()
            } catch (e: Throwable) {
                Log.e(TAG, "Builder.establish() threw exception", e)
                null
            }

            if (newVpnInterface != null) {
                val oldInterface = vpnInterface
                val oldJob = serviceJob

                vpnInterface = newVpnInterface

                serviceJob = scope.launch(Dispatchers.IO) {
                    var inputStream: FileInputStream? = null
                    try {
                        inputStream = FileInputStream(newVpnInterface.fileDescriptor)
                        val buffer = ByteArray(32768)
                        while (isActive) {
                            val bytesRead = inputStream.read(buffer)
                            if (bytesRead <= 0) break
                            // Dropped packet completely
                        }
                    } catch (_: Throwable) {
                    } finally {
                        try { inputStream?.close() } catch (_: Throwable) {}
                    }
                }

                // Clean up previous interface only after the new one is active
                try {
                    oldJob?.cancel()
                    oldInterface?.close()
                } catch (_: Throwable) {}

                Log.d(TAG, "Firewall TUN interface active for $addedAppsCount apps")
            } else {
                Log.e(TAG, "Builder.establish() returned null")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to establish VPN interface", e)
        }
    }

    private fun stopFirewall() {
        _isRunning.value = false
        _blockedAppsCount.value = 0
        serviceJob?.cancel()
        serviceJob = null
        try {
            vpnInterface?.close()
        } catch (_: Throwable) {}
        vpnInterface = null

        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback?.let { cm?.unregisterNetworkCallback(it) }
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        stopFirewall()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopFirewall()
        stopSelf()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "App Firewall Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of network firewall"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID, buildNotification(title, content))
        } catch (e: Throwable) {
            Log.w(TAG, "Could not update notification: ${e.message}")
        }
    }
}
