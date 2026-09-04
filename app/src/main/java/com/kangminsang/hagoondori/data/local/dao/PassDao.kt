package com.kangminsang.hagoondori.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kangminsang.hagoondori.data.local.entity.PassRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PassDao {
    @Query("SELECT * FROM pass_record ORDER BY date")
    fun observeAll(): Flow<List<PassRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PassRecordEntity)

    @Delete
    suspend fun delete(entity: PassRecordEntity)
}
