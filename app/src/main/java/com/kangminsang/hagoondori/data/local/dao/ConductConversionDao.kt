package com.kangminsang.hagoondori.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kangminsang.hagoondori.data.local.entity.ConductConversionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConductConversionDao {
    @Query("SELECT * FROM conduct_conversion ORDER BY meetingDate DESC")
    fun observeAll(): Flow<List<ConductConversionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConductConversionEntity)

    @Delete
    suspend fun delete(entity: ConductConversionEntity)
}
