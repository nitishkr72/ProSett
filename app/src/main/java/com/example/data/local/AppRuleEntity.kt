package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isWifiBlocked: Boolean = false,
    val isMobileBlocked: Boolean = false,
    val isBackgroundRestricted: Boolean = false,
    val lastKilledTime: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
