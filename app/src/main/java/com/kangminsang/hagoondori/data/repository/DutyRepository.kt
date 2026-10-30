package com.kangminsang.hagoondori.data.repository

import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.data.local.dao.DutyDao
import com.kangminsang.hagoondori.data.mapper.toCore
import com.kangminsang.hagoondori.data.mapper.toEntity
import com.kangminsang.hagoondori.data.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 근무 배정 데이터 접근(스펙 4.13절, F5/F6). */
@Singleton
class DutyRepository @Inject constructor(
    private val dao: DutyDao,
    private val syncStateRepository: SyncStateRepository,
) {
    fun observeAll(): Flow<List<DutyAssignment>> =
        dao.observeAll().map { list -> list.map { it.toCore() } }

    suspend fun assign(date: LocalDate, type: DutyType): DutyAssignment {
        val assignment = DutyAssignment(IdGenerator.newId(), date, type)
        dao.upsert(assignment.toEntity())
        syncStateRepository.markChanged("근무 등록: $date")
        return assignment
    }

    /**
     * 근무표 일괄 입력(F6, 4.13절). 이미 존재하는 `(date, type)` 조합은 조용히
     * 건너뛴다(DutyDao.insertAllIgnoringConflicts, OnConflictStrategy.IGNORE).
     *
     * @return 실제로 새로 삽입된 건수. `dates.size - 반환값`이 건너뛴(이미 있던) 개수다 -
     *   호출부(ViewModel)가 이 값으로 "N건 저장, M건은 이미 있어 건너뜀" 안내를 만들 수 있다.
     */
    suspend fun bulkAssign(dates: Collection<LocalDate>, type: DutyType): Int {
        if (dates.isEmpty()) return 0
        val entities = dates.map { DutyAssignment(IdGenerator.newId(), it, type).toEntity() }
        val insertedRowIds = dao.insertAllIgnoringConflicts(entities)
        // OnConflictStrategy.IGNORE로 충돌해 삽입되지 않은 행은 rowId로 -1을 반환한다(Room 규약).
        val insertedCount = insertedRowIds.count { it != -1L }
        if (insertedCount > 0) syncStateRepository.markChanged("근무 일괄 등록: ${insertedCount}건")
        return insertedCount
    }

    suspend fun delete(assignment: DutyAssignment) {
        dao.delete(assignment.toEntity())
        syncStateRepository.markChanged("근무 취소: ${assignment.date}")
    }
}
