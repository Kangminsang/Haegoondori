package com.kangminsang.hagoondori.data.repository

import com.kangminsang.hagoondori.core.model.SyncChangeLogEntry
import com.kangminsang.hagoondori.core.model.SyncState
import com.kangminsang.hagoondori.data.local.dao.SyncChangeLogDao
import com.kangminsang.hagoondori.data.local.dao.SyncStateDao
import com.kangminsang.hagoondori.data.local.entity.SyncChangeLogEntity
import com.kangminsang.hagoondori.data.mapper.toCore
import com.kangminsang.hagoondori.data.mapper.toEntity
import com.kangminsang.hagoondori.data.util.IdGenerator
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 동기화 상태(스펙 4.15절, F18). 데이터를 바꾸는 모든 Repository가 [markChanged]를
 * 호출해 [SyncState.pendingChangeCount]를 늘린다 - "앱에서 고쳤는데 동기화를 잊는"
 * 실패 시나리오(5.4절)를 막기 위한 핵심 장치다.
 *
 * 건수만으로는 "무엇이" 바뀌었는지 알 수 없으므로, 각 변경을 사람이 읽을 수 있는
 * 한 줄 설명으로 [changeLogDao]에도 함께 남긴다 - 동기화 전 "변경사항 보기"가 이 목록을 보여준다.
 */
@Singleton
class SyncStateRepository @Inject constructor(
    private val dao: SyncStateDao,
    private val changeLogDao: SyncChangeLogDao,
) {
    fun observe(): Flow<SyncState> = dao.observe().map { it?.toCore() ?: SyncState() }

    suspend fun get(): SyncState = dao.get()?.toCore() ?: SyncState()

    /** 마지막 동기화 이후의 변경 내역을 최신순으로 노출한다. */
    fun observeChangeLog(): Flow<List<SyncChangeLogEntry>> =
        changeLogDao.observeAll().map { list -> list.map { it.toCore() } }

    /** 데이터 변경이 있을 때마다 호출한다. 미반영 변경 건수를 1 늘리고, [description]을 변경 내역에 남긴다. */
    suspend fun markChanged(description: String) {
        val current = get()
        dao.upsert(current.copy(pendingChangeCount = current.pendingChangeCount + 1).toEntity())
        changeLogDao.insert(SyncChangeLogEntity(IdGenerator.newId(), description, AppClock.now()))
    }

    /** 동기화 성공 시 호출한다. 미반영 변경 건수를 0으로 초기화하고, 변경 내역도 비운다. */
    suspend fun markSynced(at: Instant) {
        dao.upsert(SyncState(lastSyncedAt = at, pendingChangeCount = 0).toEntity())
        changeLogDao.clearAll()
    }
}
