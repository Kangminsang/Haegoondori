package com.kangminsang.hagoondori.data.repository

import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.PassType
import com.kangminsang.hagoondori.data.local.dao.PassDao
import com.kangminsang.hagoondori.data.mapper.toCore
import com.kangminsang.hagoondori.data.mapper.toEntity
import com.kangminsang.hagoondori.data.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 정기 할당형 자원(외출) 데이터 접근(스펙 4.11절, F14). */
@Singleton
class PassRepository @Inject constructor(
    private val dao: PassDao,
    private val syncStateRepository: SyncStateRepository,
) {
    fun observeAll(): Flow<List<PassRecord>> =
        dao.observeAll().map { list -> list.map { it.toCore() } }

    /**
     * [type]은 호출 시점에 고정 저장된다(4.11절) - 입력 UI가
     * [com.kangminsang.hagoondori.core.calc.PassCalculator.classifyType]으로 자동
     * 판정한 기본값을 그대로 넘기거나, 사용자가 수정한 값을 넘긴다.
     */
    suspend fun addRecord(date: LocalDate, type: PassType, memo: String?): PassRecord {
        val record = PassRecord(IdGenerator.newId(), date, type, memo)
        dao.upsert(record.toEntity())
        syncStateRepository.markChanged("외출 등록: $date")
        return record
    }

    suspend fun deleteRecord(record: PassRecord) {
        dao.delete(record.toEntity())
        syncStateRepository.markChanged("외출 삭제: ${record.date}")
    }
}
