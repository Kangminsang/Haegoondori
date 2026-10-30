package com.kangminsang.hagoondori.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kangminsang.hagoondori.data.local.entity.SyncChangeLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncChangeLogDao {
    @Query("SELECT * FROM sync_change_log ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<SyncChangeLogEntity>>

    @Insert
    suspend fun insert(entity: SyncChangeLogEntity)

    @Query("DELETE FROM sync_change_log")
    suspend fun clearAll()
}
