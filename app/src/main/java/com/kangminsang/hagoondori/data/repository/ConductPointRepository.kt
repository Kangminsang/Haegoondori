package com.kangminsang.hagoondori.data.repository

import com.kangminsang.hagoondori.core.model.ConductPoint
import com.kangminsang.hagoondori.core.model.ConductPointKind
import com.kangminsang.hagoondori.data.local.dao.ConductPointDao
import com.kangminsang.hagoondori.data.mapper.toCore
import com.kangminsang.hagoondori.data.mapper.toEntity
import com.kangminsang.hagoondori.data.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 양호·과실 점수 데이터 접근. 장치 페이로드에 들어가지 않으므로 동기화 변경 기록을 남기지 않는다. */
@Singleton
class ConductPointRepository @Inject constructor(
    private val dao: ConductPointDao,
) {
    fun observeAll(): Flow<List<ConductPoint>> =
        dao.observeAll().map { list -> list.map { it.toCore() } }

    suspend fun add(date: LocalDate, kind: ConductPointKind, points: Int, reason: String, memo: String?): ConductPoint {
        val record = ConductPoint(IdGenerator.newId(), date, kind, points, reason, memo)
        dao.upsert(record.toEntity())
        return record
    }

    suspend fun update(record: ConductPoint) {
        dao.upsert(record.toEntity())
    }

    suspend fun delete(record: ConductPoint) {
        dao.delete(record.toEntity())
    }
}
