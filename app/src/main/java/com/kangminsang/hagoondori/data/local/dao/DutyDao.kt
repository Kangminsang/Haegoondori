package com.kangminsang.hagoondori.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kangminsang.hagoondori.data.local.entity.DutyAssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DutyDao {
    @Query("SELECT * FROM duty_assignment ORDER BY date")
    fun observeAll(): Flow<List<DutyAssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DutyAssignmentEntity)

    /**
     * 근무표 일괄 입력(F6)에서 쓴다. `(date, type)` 유니크 제약을 이미 가진 배정은
     * 조용히 건너뛴다(REPLACE가 아니라 IGNORE) - 일괄 입력은 "새로 채워 넣는" 동작이지
     * 기존 개별 기록(메모 등 향후 확장 필드)을 덮어쓰는 동작이 아니어야 하기 때문이다.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoringConflicts(entities: List<DutyAssignmentEntity>): List<Long>

    @Delete
    suspend fun delete(entity: DutyAssignmentEntity)
}
