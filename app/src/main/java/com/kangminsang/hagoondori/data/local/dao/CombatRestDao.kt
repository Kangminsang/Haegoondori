package com.kangminsang.hagoondori.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kangminsang.hagoondori.data.local.entity.CombatRestGrantEntity
import com.kangminsang.hagoondori.data.local.entity.CombatRestUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CombatRestDao {
    @Query("SELECT * FROM combat_rest_grant ORDER BY grantedDate")
    fun observeGrants(): Flow<List<CombatRestGrantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGrant(entity: CombatRestGrantEntity)

    @Delete
    suspend fun deleteGrant(entity: CombatRestGrantEntity)

    @Query("SELECT * FROM combat_rest_usage ORDER BY startDate")
    fun observeUsages(): Flow<List<CombatRestUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsage(entity: CombatRestUsageEntity)

    @Delete
    suspend fun deleteUsage(entity: CombatRestUsageEntity)
}
