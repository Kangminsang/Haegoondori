package com.kangminsang.hagoondori.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kangminsang.hagoondori.data.local.entity.OvernightForfeitureEntity
import com.kangminsang.hagoondori.data.local.entity.OvernightRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OvernightDao {
    @Query("SELECT * FROM overnight_record ORDER BY date")
    fun observeRecords(): Flow<List<OvernightRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(entity: OvernightRecordEntity)

    @Delete
    suspend fun deleteRecord(entity: OvernightRecordEntity)

    @Query("SELECT * FROM overnight_forfeiture ORDER BY slotIndex")
    fun observeForfeitures(): Flow<List<OvernightForfeitureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertForfeiture(entity: OvernightForfeitureEntity)

    @Delete
    suspend fun deleteForfeiture(entity: OvernightForfeitureEntity)
}
