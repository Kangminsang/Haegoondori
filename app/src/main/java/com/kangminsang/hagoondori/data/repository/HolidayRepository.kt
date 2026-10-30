package com.kangminsang.hagoondori.data.repository

import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.data.local.dao.HolidayDao
import com.kangminsang.hagoondori.data.mapper.toCore
import com.kangminsang.hagoondori.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 공휴일 데이터 접근(스펙 4.14절, F15). 로컬 캐시에 대한 CRUD만 다룬다 - 공공데이터포털
 * API 호출/내장 폴백 선택 로직은 [com.kangminsang.hagoondori.data.remote.holiday.HolidayRemoteRepository]가
 * 담당하고, 그 결과를 이 Repository의 [upsertAll]로 캐시에 반영한다(4.14절 "반드시 로컬 캐시").
 */
@Singleton
class HolidayRepository @Inject constructor(
    private val dao: HolidayDao,
    private val syncStateRepository: SyncStateRepository,
) {
    fun observeAll(): Flow<List<Holiday>> =
        dao.observeAll().map { list -> list.map { it.toCore() } }

    suspend fun getAll(): List<Holiday> = dao.getAll().map { it.toCore() }

    /** API 조회 결과나 내장 데이터를 캐시에 반영한다. 같은 날짜는 최신 정보로 덮어쓴다. */
    suspend fun upsertAll(holidays: List<Holiday>) {
        if (holidays.isEmpty()) return
        // 내용이 실제로 바뀔 때만 "동기화 안 된 변경"으로 센다. 자동 갱신이 같은 데이터를 다시
        // 받아올 때마다 배너가 뜨면 안 된다.
        val existing = dao.getAll().map { it.toCore() }.toSet()
        val changed = holidays.any { it !in existing }
        dao.upsertAll(holidays.map { it.toEntity() })
        if (changed) syncStateRepository.markChanged("공휴일 정보 갱신")
    }

    /** 사용자 수동 입력(확보 우선순위의 최후 수단, 4.14절). */
    suspend fun addManual(holiday: Holiday) {
        dao.upsert(holiday.toEntity())
        syncStateRepository.markChanged("공휴일 수동 추가: ${holiday.name} (${holiday.date})")
    }

    suspend fun delete(holiday: Holiday) {
        dao.delete(holiday.toEntity())
        syncStateRepository.markChanged("공휴일 삭제: ${holiday.name} (${holiday.date})")
    }
}
