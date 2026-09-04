package com.kangminsang.hagoondori.data.repository

import com.kangminsang.hagoondori.core.model.SyncState
import com.kangminsang.hagoondori.data.local.dao.SyncStateDao
import com.kangminsang.hagoondori.data.mapper.toCore
import com.kangminsang.hagoondori.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 동기화 상태(스펙 4.15절, F18). 데이터를 바꾸는 모든 Repository가 [markChanged]를
 * 호출해 [SyncState.pendingChangeCount]를 늘린다 - "앱에서 고쳤는데 동기화를 잊는"
 * 실패 시나리오(5.4절)를 막기 위한 핵심 장치다.
 */
@Singleton
class SyncStateRepository @Inject constructor(
    private val dao: SyncStateDao,
) {
    fun observe(): Flow<SyncState> = dao.observe().map { it?.toCore() ?: SyncState() }

    suspend fun get(): SyncState = dao.get()?.toCore() ?: SyncState()

    /** 데이터 변경이 있을 때마다 호출한다. 미반영 변경 건수를 1 늘린다. */
    suspend fun markChanged() {
        val current = get()
        dao.upsert(current.copy(pendingChangeCount = current.pendingChangeCount + 1).toEntity())
    }

    /** 동기화 성공 시 호출한다. 미반영 변경 건수를 0으로 초기화하고 마지막 동기화 시각을 기록한다. */
    suspend fun markSynced(at: Instant) {
        dao.upsert(SyncState(lastSyncedAt = at, pendingChangeCount = 0).toEntity())
    }
}
