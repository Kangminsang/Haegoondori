package com.kangminsang.hagoondori.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kangminsang.hagoondori.data.local.entity.HolidayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holiday ORDER BY date")
    fun observeAll(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holiday ORDER BY date")
    suspend fun getAll(): List<HolidayEntity>

    /** 공휴일 API 응답 upsert용. 같은 날짜가 이미 있으면 최신 정보로 덮어쓴다(캐시 갱신). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<HolidayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HolidayEntity)

    @Delete
    suspend fun delete(entity: HolidayEntity)
}
