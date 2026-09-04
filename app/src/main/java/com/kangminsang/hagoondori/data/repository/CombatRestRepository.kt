package com.kangminsang.hagoondori.data.repository

import com.kangminsang.hagoondori.core.model.CombatRestGrant
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.data.local.dao.CombatRestDao
import com.kangminsang.hagoondori.data.mapper.toCore
import com.kangminsang.hagoondori.data.mapper.toEntity
import com.kangminsang.hagoondori.data.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 혼합 소모형 자원(전투휴무) 데이터 접근(스펙 4.7/4.8절, F10). 여기서는 "직접 부여분"만
 * 다룬다 - "포상 초과 자동 전환분"은 저장되지 않으므로 [LeaveRepository]가 노출하는
 * 포상휴가 부여 Flow와 함께 [com.kangminsang.hagoondori.core.calc.CombatRestCalculator]에
 * 넘겨야 완전한 잔여를 계산할 수 있다 (그 결합은 ViewModel의 몫).
 */
@Singleton
class CombatRestRepository @Inject constructor(
    private val dao: CombatRestDao,
    private val syncStateRepository: SyncStateRepository,
) {
    fun observeGrants(): Flow<List<CombatRestGrant>> =
        dao.observeGrants().map { list -> list.map { it.toCore() } }

    suspend fun addGrant(days: Int, grantedDate: LocalDate, reason: String?): CombatRestGrant {
        val grant = CombatRestGrant(IdGenerator.newId(), days, grantedDate, reason)
        dao.upsertGrant(grant.toEntity())
        syncStateRepository.markChanged()
        return grant
    }

    suspend fun deleteGrant(grant: CombatRestGrant) {
        dao.deleteGrant(grant.toEntity())
        syncStateRepository.markChanged()
    }

    fun observeUsages(): Flow<List<CombatRestUsage>> =
        dao.observeUsages().map { list -> list.map { it.toCore() } }

    suspend fun addUsage(startDate: LocalDate, endDate: LocalDate, memo: String?): CombatRestUsage {
        val usage = CombatRestUsage(IdGenerator.newId(), startDate, endDate, memo)
        dao.upsertUsage(usage.toEntity())
        syncStateRepository.markChanged()
        return usage
    }

    suspend fun deleteUsage(usage: CombatRestUsage) {
        dao.deleteUsage(usage.toEntity())
        syncStateRepository.markChanged()
    }
}
