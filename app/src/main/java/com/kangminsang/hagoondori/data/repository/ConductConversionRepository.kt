package com.kangminsang.hagoondori.data.repository

import com.kangminsang.hagoondori.core.model.ConductConversionRecord
import com.kangminsang.hagoondori.core.model.ConductConversionType
import com.kangminsang.hagoondori.data.local.dao.ConductConversionDao
import com.kangminsang.hagoondori.data.mapper.toCore
import com.kangminsang.hagoondori.data.mapper.toEntity
import com.kangminsang.hagoondori.data.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 군기지도위원회 결과 기록 데이터 접근. 장치 페이로드에 들어가지 않으므로 동기화 변경 기록을 남기지 않는다. */
@Singleton
class ConductConversionRepository @Inject constructor(
    private val dao: ConductConversionDao,
) {
    fun observeAll(): Flow<List<ConductConversionRecord>> =
        dao.observeAll().map { list -> list.map { it.toCore() } }

    suspend fun add(meetingDate: LocalDate, type: ConductConversionType, count: Int, memo: String?): ConductConversionRecord {
        val record = ConductConversionRecord(IdGenerator.newId(), meetingDate, type, count, memo)
        dao.upsert(record.toEntity())
        return record
    }

    suspend fun delete(record: ConductConversionRecord) {
        dao.delete(record.toEntity())
    }
}
