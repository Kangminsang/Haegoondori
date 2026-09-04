package com.kangminsang.hagoondori.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kangminsang.hagoondori.data.local.entity.SyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE id = ${SyncStateEntity.SINGLETON_ID}")
    fun observe(): Flow<SyncStateEntity?>

    @Query("SELECT * FROM sync_state WHERE id = ${SyncStateEntity.SINGLETON_ID}")
    suspend fun get(): SyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncStateEntity)
}
