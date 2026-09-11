package com.example.data.repository

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.data.local.AppRuleDao
import com.example.data.local.AppRuleEntity
import com.example.data.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AppManagerRepository(
    private val context: Context,
    private val appRuleDao: AppRuleDao
) {
    private val packageManager: PackageManager = context.packageManager
    private val activityManager: ActivityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    /**
     * Flow of installed apps merged with their configured firewall and background rules.
     */
    fun getInstalledAppsFlow(): Flow<List<InstalledApp>> {
        val baseAppsFlow = flow {
            emit(queryInstalledPackages())
        }

        return combine(baseAppsFlow, appRuleDao.getAllRules()) { rawApps, rules ->
            val ruleMap = rules.associateBy { it.packageName }
            rawApps.map { base ->
                val rule = ruleMap[base.packageName]
                if (rule != null) {
                    base.copy(
                        isWifiBlocked = rule.isWifiBlocked,
                        isMobileBlocked = rule.isMobileBlocked,
                        isBackgroundRestricted = rule.isBackgroundRestricted,
                        lastKilledTime = rule.lastKilledTime
                    )
                } else {
                    base
                }
            }.sortedWith(
                compareBy<InstalledApp> { it.isSystemApp }
                    .thenBy { it.appName.lowercase() }
            )
        }.flowOn(Dispatchers.IO)
    }

    suspend fun reloadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val raw = queryInstalledPackages()
        val rules = appRuleDao.getAllRules()
        // Ensure new installed packages are in database or have defaults
        raw
    }

    private fun queryInstalledPackages(): List<InstalledApp> {
        val flags = PackageManager.GET_PERMISSIONS
        val packages: List<PackageInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                packageManager.getInstalledPackages(flags)
            }
        } catch (e: Exception) {
            emptyList()
        }

        val myPackage = context.packageName

        return packages.mapNotNull { pkgInfo ->
            val appInfo = pkgInfo.applicationInfo ?: return@mapNotNull null
            // We can show all apps, including system apps, but user can filter them
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val appName = try {
                appInfo.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                pkgInfo.packageName
            }
            val hasInternet = pkgInfo.requestedPermissions?.contains(android.Manifest.permission.INTERNET) == true

            @Suppress("DEPRECATION")
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                pkgInfo.versionCode.toLong()
            }

            InstalledApp(
                packageName = pkgInfo.packageName,
                appName = if (appName.isBlank()) pkgInfo.packageName else appName,
                versionName = pkgInfo.versionName ?: "1.0",
                versionCode = versionCode,
                isSystemApp = isSystem,
                hasInternetPermission = hasInternet,
                uid = appInfo.uid,
                firstInstallTime = pkgInfo.firstInstallTime
            )
        }
    }

    suspend fun setWifiBlocked(packageName: String, appName: String, blocked: Boolean) =
        withContext(Dispatchers.IO) {
            val existing = appRuleDao.getRuleByPackage(packageName)
            if (existing != null) {
                appRuleDao.updateWifiBlocked(packageName, blocked)
            } else {
                appRuleDao.insertOrUpdate(
                    AppRuleEntity(
                        packageName = packageName,
                        appName = appName,
                        isWifiBlocked = blocked
                    )
                )
            }
        }

    suspend fun setMobileBlocked(packageName: String, appName: String, blocked: Boolean) =
        withContext(Dispatchers.IO) {
            val existing = appRuleDao.getRuleByPackage(packageName)
            if (existing != null) {
                appRuleDao.updateMobileBlocked(packageName, blocked)
            } else {
                appRuleDao.insertOrUpdate(
                    AppRuleEntity(
                        packageName = packageName,
                        appName = appName,
                        isMobileBlocked = blocked
                    )
                )
            }
        }

    suspend fun setBothNetworkBlocked(packageName: String, appName: String, blocked: Boolean) =
        withContext(Dispatchers.IO) {
            val existing = appRuleDao.getRuleByPackage(packageName)
            if (existing != null) {
                appRuleDao.updateBothNetwork(packageName, blocked)
            } else {
                appRuleDao.insertOrUpdate(
                    AppRuleEntity(
                        packageName = packageName,
                        appName = appName,
                        isWifiBlocked = blocked,
                        isMobileBlocked = blocked
                    )
                )
            }
        }

    suspend fun setBackgroundRestricted(packageName: String, appName: String, restricted: Boolean) =
        withContext(Dispatchers.IO) {
            val existing = appRuleDao.getRuleByPackage(packageName)
            if (existing != null) {
                appRuleDao.updateBackgroundRestricted(packageName, restricted)
            } else {
                appRuleDao.insertOrUpdate(
                    AppRuleEntity(
                        packageName = packageName,
                        appName = appName,
                        isBackgroundRestricted = restricted
                    )
                )
            }
            if (restricted) {
                // Instantly terminate background processes for this restricted app
                killAppBackgroundProcess(packageName)
            }
        }

    suspend fun killAppBackgroundProcess(packageName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                activityManager.killBackgroundProcesses(packageName)
                val now = System.currentTimeMillis()
                appRuleDao.updateLastKilled(packageName, now)
                true
            } catch (e: Exception) {
                false
            }
        }

    suspend fun killAllRestrictedApps(apps: List<InstalledApp>): Int =
        withContext(Dispatchers.IO) {
            var killedCount = 0
            val now = System.currentTimeMillis()
            apps.filter { it.isBackgroundRestricted }.forEach { app ->
                try {
                    activityManager.killBackgroundProcesses(app.packageName)
                    appRuleDao.updateLastKilled(app.packageName, now)
                    killedCount++
                } catch (e: Exception) {
                    // Ignore failures for protected system processes
                }
            }
            killedCount
        }

    suspend fun setBatchWifiBlocked(packages: List<InstalledApp>, blocked: Boolean) =
        withContext(Dispatchers.IO) {
            packages.forEach { app ->
                setWifiBlocked(app.packageName, app.appName, blocked)
            }
        }

    suspend fun setBatchMobileBlocked(packages: List<InstalledApp>, blocked: Boolean) =
        withContext(Dispatchers.IO) {
            packages.forEach { app ->
                setMobileBlocked(app.packageName, app.appName, blocked)
            }
        }

    suspend fun setBatchBackgroundRestricted(packages: List<InstalledApp>, restricted: Boolean) =
        withContext(Dispatchers.IO) {
            packages.forEach { app ->
                setBackgroundRestricted(app.packageName, app.appName, restricted)
            }
        }

    fun openAppInfoSettings(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic settings
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {}
        }
    }

    fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {}
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
