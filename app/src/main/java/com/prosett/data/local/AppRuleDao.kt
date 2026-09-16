package com.prosett.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {

    @Query("SELECT * FROM app_rules")
    fun getAllRules(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName")
    suspend fun getRuleByPackage(packageName: String): AppRuleEntity?

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName")
    fun getRuleFlowByPackage(packageName: String): Flow<AppRuleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rule: AppRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<AppRuleEntity>)

    @Query("UPDATE app_rules SET isWifiBlocked = :blocked, updatedAt = :timestamp WHERE packageName = :packageName")
    suspend fun updateWifiBlocked(packageName: String, blocked: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE app_rules SET isMobileBlocked = :blocked, updatedAt = :timestamp WHERE packageName = :packageName")
    suspend fun updateMobileBlocked(packageName: String, blocked: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE app_rules SET isWifiBlocked = :blocked, isMobileBlocked = :blocked, updatedAt = :timestamp WHERE packageName = :packageName")
    suspend fun updateBothNetwork(packageName: String, blocked: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE app_rules SET isBackgroundRestricted = :restricted, updatedAt = :timestamp WHERE packageName = :packageName")
    suspend fun updateBackgroundRestricted(packageName: String, restricted: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE app_rules SET lastKilledTime = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastKilled(packageName: String, timestamp: Long)

    @Query("UPDATE app_rules SET isWifiBlocked = :blocked")
    suspend fun setAllWifiBlocked(blocked: Boolean)

    @Query("UPDATE app_rules SET isMobileBlocked = :blocked")
    suspend fun setAllMobileBlocked(blocked: Boolean)

    @Query("UPDATE app_rules SET isBackgroundRestricted = :restricted")
    suspend fun setAllBackgroundRestricted(restricted: Boolean)

    @Query("DELETE FROM app_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)
}
