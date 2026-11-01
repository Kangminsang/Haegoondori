package com.kangminsang.hagoondori.data.repository

import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OverflowBehavior
import com.kangminsang.hagoondori.data.local.dao.LeaveDao
import com.kangminsang.hagoondori.data.mapper.toCore
import com.kangminsang.hagoondori.data.mapper.toEntity
import com.kangminsang.hagoondori.data.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 소모형 자원(휴가) 데이터 접근(스펙 4.4~4.6절, F7/F8). 계산(상한/잔여 등)은 하지
 * 않는다 - 그건 [com.kangminsang.hagoondori.core.calc.LeaveCalculator]의 몫이며,
 * 이 Repository가 노출하는 원본 Flow를 ViewModel이 그 계산기에 넘긴다.
 */
@Singleton
class LeaveRepository @Inject constructor(
    private val dao: LeaveDao,
    private val syncStateRepository: SyncStateRepository,
) {
    fun observeTypes(): Flow<List<LeaveType>> =
        dao.observeTypes().map { list -> list.map { it.toCore() } }

    /**
     * 휴가 종류를 새로 만든다. 표시 순서는 맨 뒤(최대 순번 + 1)에 놓는다. 입력은 호출하기 전에
     * [com.kangminsang.hagoondori.core.calc.LeaveTypeValidator]로 검증한다. 이름은 앞뒤 공백을 뗀다.
     */
    suspend fun createType(
        name: String,
        cap: Int?,
        overflowBehavior: OverflowBehavior,
        fixedDays: Int?,
    ): LeaveType {
        val type = LeaveType(IdGenerator.newId(), name.trim(), cap, overflowBehavior, fixedDays)
        dao.upsertType(type.toEntity(sortOrder = dao.maxTypeSortOrder() + 1))
        syncStateRepository.markChanged("휴가 종류 추가: ${type.name}")
        return type
    }

    /**
     * 기존 종류의 이름·상한·초과 처리·고정 일수를 고친다(id는 그대로). 표시 순서는 건드리지 않는다
     * ([reorderTypes]의 몫). 입력은 호출하기 전에 검증한다.
     */
    suspend fun updateType(type: LeaveType) {
        dao.updateType(type.id, type.name.trim(), type.cap, type.overflowBehavior, type.fixedDays)
        syncStateRepository.markChanged("휴가 종류 수정: ${type.name}")
    }

    /** 휴가 종류 표시 순서를 [orderedTypeIds] 순서대로 다시 매긴다. 장치 페이로드와 무관해 동기화 대기 건수는 올리지 않는다. */
    suspend fun reorderTypes(orderedTypeIds: List<String>) {
        orderedTypeIds.forEachIndexed { index, id -> dao.updateTypeSortOrder(id, index) }
    }

    suspend fun deleteType(type: LeaveType) {
        dao.deleteType(type.toEntity())
        syncStateRepository.markChanged("휴가 종류 삭제: ${type.name}")
    }

    fun observeAllGrants(): Flow<List<LeaveGrant>> =
        dao.observeAllGrants().map { list -> list.map { it.toCore() } }

    fun observeGrants(leaveTypeId: String): Flow<List<LeaveGrant>> =
        dao.observeGrants(leaveTypeId).map { list -> list.map { it.toCore() } }

    suspend fun addGrant(
        leaveTypeId: String,
        days: Int,
        grantedDate: LocalDate,
        reason: String?,
        expiryDate: LocalDate? = null,
    ): LeaveGrant {
        val grant = LeaveGrant(IdGenerator.newId(), leaveTypeId, days, grantedDate, reason, expiryDate)
        dao.upsertGrant(grant.toEntity())
        syncStateRepository.markChanged("휴가 부여: ${days}일")
        return grant
    }

    suspend fun deleteGrant(grant: LeaveGrant) {
        dao.deleteGrant(grant.toEntity())
        syncStateRepository.markChanged("휴가 부여 삭제: ${grant.days}일")
    }

    fun observeAllUsages(): Flow<List<LeaveUsage>> =
        dao.observeAllUsages().map { list -> list.map { it.toCore() } }

    fun observeUsages(leaveTypeId: String): Flow<List<LeaveUsage>> =
        dao.observeUsages(leaveTypeId).map { list -> list.map { it.toCore() } }

    suspend fun addUsage(leaveTypeId: String, startDate: LocalDate, endDate: LocalDate, label: String?): LeaveUsage {
        val usage = LeaveUsage(IdGenerator.newId(), leaveTypeId, startDate, endDate, label)
        dao.upsertUsage(usage.toEntity())
        syncStateRepository.markChanged("휴가 사용 등록: $startDate~$endDate")
        return usage
    }

    /** 기존 사용 기록의 날짜·이름을 고친다(id는 그대로). */
    suspend fun updateUsage(usage: LeaveUsage) {
        dao.upsertUsage(usage.toEntity())
        syncStateRepository.markChanged("휴가 사용 수정: ${usage.startDate}~${usage.endDate}")
    }

    suspend fun deleteUsage(usage: LeaveUsage) {
        dao.deleteUsage(usage.toEntity())
        syncStateRepository.markChanged("휴가 사용 삭제: ${usage.startDate}~${usage.endDate}")
    }
}
