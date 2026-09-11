package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
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
import java.io.IOException

class FirewallVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isConnectedToWifi: Boolean = true

    companion object {
        const val TAG = "FirewallVpnService"
        const val ACTION_START = "com.example.action.START_FIREWALL"
        const val ACTION_STOP = "com.example.action.STOP_FIREWALL"
        const val ACTION_RELOAD = "com.example.action.RELOAD_RULES"

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FirewallVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun reloadRules(context: Context) {
            if (_isRunning.value) {
                val intent = Intent(context, FirewallVpnService::class.java).apply {
                    action = ACTION_RELOAD
                }
                context.startService(intent)
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
                startForeground(NOTIFICATION_ID, buildNotification("Firewall Active", "Monitoring network rules"))
                _isRunning.value = true
                scope.launch {
                    reloadFirewallTunnel()
                }
                return START_STICKY
            }
        }
        return START_STICKY
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    val wasWifi = isConnectedToWifi
                    isConnectedToWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    if (wasWifi != isConnectedToWifi && _isRunning.value) {
                        scope.launch {
                            reloadFirewallTunnel()
                        }
                    }
                }
            }
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    private suspend fun reloadFirewallTunnel() {
        try {
            val db = AppDatabase.getDatabase(this)
            val rules = db.appRuleDao().getAllRules().firstOrNull() ?: emptyList()

            // Check current connectivity
            checkCurrentNetworkType()

            // Filter apps that should be blocked right now
            val blockedPackageNames = rules.filter { rule ->
                if (isConnectedToWifi) {
                    rule.isWifiBlocked
                } else {
                    rule.isMobileBlocked
                }
            }.map { it.packageName }

            _blockedAppsCount.value = blockedPackageNames.size

            // Update notification
            updateNotification(
                "Firewall Active",
                "${blockedPackageNames.size} apps blocked on ${if (isConnectedToWifi) "Wi-Fi" else "Mobile Data"}"
            )

            // Rebuild TUN interface
            rebuildTunInterface(blockedPackageNames)
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading firewall tunnel", e)
        }
    }

    private fun checkCurrentNetworkType() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        isConnectedToWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: true
    }

    private fun rebuildTunInterface(blockedPackages: List<String>) {
        try {
            // Close existing
            serviceJob?.cancel()
            vpnInterface?.close()
            vpnInterface = null

            if (blockedPackages.isEmpty()) {
                Log.d(TAG, "No blocked apps, TUN interface idle.")
                return
            }

            val builder = Builder()
            builder.setSession("Firewall Guard")
            builder.addAddress("10.254.1.2", 30)
            builder.addRoute("0.0.0.0", 0)

            var addedAppsCount = 0
            for (pkg in blockedPackages) {
                try {
                    builder.addAllowedApplication(pkg)
                    addedAppsCount++
                } catch (e: PackageManager.NameNotFoundException) {
                    // App might have been uninstalled
                } catch (e: Exception) {
                    Log.w(TAG, "Could not add allowed app to VPN: $pkg", e)
                }
            }

            if (addedAppsCount == 0) {
                return
            }

            vpnInterface = builder.establish()

            // Packet sink: discard packets from blocked apps so connections stall/fail cleanly
            vpnInterface?.let { pfd ->
                serviceJob = scope.launch {
                    val inputStream = FileInputStream(pfd.fileDescriptor)
                    val buffer = ByteArray(4096)
                    try {
                        while (isActive) {
                            val length = inputStream.read(buffer)
                            if (length <= 0) break
                            // Drop packet completely
                        }
                    } catch (e: IOException) {
                        // Interface closed
                    } finally {
                        try {
                            inputStream.close()
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
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
        } catch (_: Exception) {}
        vpnInterface = null

        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback?.let { cm?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
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
                description = "Shows current status of network firewall"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(title, content))
    }
}
