package com.kangminsang.hagoondori.data.repository

import com.kangminsang.hagoondori.core.model.OvernightForfeiture
import com.kangminsang.hagoondori.core.model.OvernightRecord
import com.kangminsang.hagoondori.data.local.dao.OvernightDao
import com.kangminsang.hagoondori.data.mapper.toCore
import com.kangminsang.hagoondori.data.mapper.toEntity
import com.kangminsang.hagoondori.data.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 주기형 자원(6주 외박) 데이터 접근(스펙 4.9/4.10절, F11~F13). 예정일/차수 번호는
 * 저장하지 않는다 - [com.kangminsang.hagoondori.core.calc.OvernightScheduleCalculator]가
 * 여기서 노출하는 원본 기록/소멸 목록으로부터 매번 계산한다.
 */
@Singleton
class OvernightRepository @Inject constructor(
    private val dao: OvernightDao,
    private val syncStateRepository: SyncStateRepository,
) {
    fun observeRecords(): Flow<List<OvernightRecord>> =
        dao.observeRecords().map { list -> list.map { it.toCore() } }

    suspend fun addRecord(date: LocalDate, memo: String?, endDate: LocalDate = date): OvernightRecord {
        val record = OvernightRecord(IdGenerator.newId(), date, memo, endDate)
        dao.upsertRecord(record.toEntity())
        syncStateRepository.markChanged()
        return record
    }

    /** 기존 외박 기록의 날짜·메모를 고친다(id는 그대로). 차수는 저장하지 않으므로 날짜순으로 다시 매겨진다. */
    suspend fun updateRecord(record: OvernightRecord) {
        dao.upsertRecord(record.toEntity())
        syncStateRepository.markChanged()
    }

    suspend fun deleteRecord(record: OvernightRecord) {
        dao.deleteRecord(record.toEntity())
        syncStateRepository.markChanged()
    }

    fun observeForfeitures(): Flow<List<OvernightForfeiture>> =
        dao.observeForfeitures().map { list -> list.map { it.toCore() } }

    /**
     * 차수 소멸 처리(F13). 입력 UI는 차수 번호를 직접 받지 말고 예정일 목록에서
     * 선택하게 해야 한다는 것이 스펙 4.10절의 요구사항이다 - 그 UI 제약은 화면 쪽
     * 책임이고, 여기서는 이미 정해진 [slotIndex]를 저장만 한다.
     */
    suspend fun addForfeiture(slotIndex: Int, reason: String?, recordedDate: LocalDate?): OvernightForfeiture {
        val forfeiture = OvernightForfeiture(IdGenerator.newId(), slotIndex, reason, recordedDate)
        dao.upsertForfeiture(forfeiture.toEntity())
        syncStateRepository.markChanged()
        return forfeiture
    }

    suspend fun deleteForfeiture(forfeiture: OvernightForfeiture) {
        dao.deleteForfeiture(forfeiture.toEntity())
        syncStateRepository.markChanged()
    }
}
