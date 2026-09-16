package com.prosett.data.model

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val hasInternetPermission: Boolean,
    val uid: Int,
    val firstInstallTime: Long,
    val isWifiBlocked: Boolean = false,
    val isMobileBlocked: Boolean = false,
    val isBackgroundRestricted: Boolean = false,
    val lastKilledTime: Long = 0L
) {
    val isBothNetworkBlocked: Boolean
        get() = isWifiBlocked && isMobileBlocked

    val hasAnyNetworkBlock: Boolean
        get() = isWifiBlocked || isMobileBlocked
}
