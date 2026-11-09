package com.kangminsang.hagoondori.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kangminsang.hagoondori.data.local.entity.ConductPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConductPointDao {
    @Query("SELECT * FROM conduct_point ORDER BY date DESC")
    fun observeAll(): Flow<List<ConductPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConductPointEntity)

    @Delete
    suspend fun delete(entity: ConductPointEntity)
}
