package com.kangminsang.hagoondori.data.repository

import com.kangminsang.hagoondori.core.model.SyncChangeLogEntry
import com.kangminsang.hagoondori.core.model.SyncState
import com.kangminsang.hagoondori.data.local.dao.SyncChangeLogDao
import com.kangminsang.hagoondori.data.local.dao.SyncStateDao
import com.kangminsang.hagoondori.data.local.entity.SyncStateEntity
import com.kangminsang.hagoondori.data.mapper.toCore
import com.kangminsang.hagoondori.widget.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 동기화 상태(스펙 4.15절, F18).
 *
 * "동기화하지 않은 변경"은 건수를 세지 않고, 마지막으로 장치에 보낸 페이로드([SyncStateEntity.syncedPayload])와
 * 지금 데이터로 만든 페이로드의 순 차이로 계산한다(PendingChangesTracker). 그래서 데이터를 바꾸는 Repository는
 * [markChanged]로 "데이터가 바뀌었으니 다시 계산하라"는 신호([changes])만 보낸다. 이 클래스는 신호와 기준 페이로드를
 * 저장하는 곳이며, 차이 계산은 하지 않는다.
 *
 * 예전 방식이 남긴 변경 내역([observeChangeLog])은 "업데이트 전 변경"으로만 보여준다. 동기화하면 비운다.
 */
@Singleton
class SyncStateRepository @Inject constructor(
    private val dao: SyncStateDao,
    private val changeLogDao: SyncChangeLogDao,
    private val widgetRefresher: WidgetRefresher,
) {
    private val changeTick = MutableStateFlow(0L)

    /** 데이터가 바뀔 때마다 값이 오른다. 값 자체의 의미는 없고, 미반영 변경을 다시 계산하라는 신호다. */
    val changes: StateFlow<Long> = changeTick.asStateFlow()

    fun observe(): Flow<SyncState> = dao.observe().map { it?.toCore() ?: SyncState() }

    /**
     * 동기화 기준 행 전체를 관찰한다. 마지막 동기화 페이로드([SyncStateEntity.syncedPayload])까지 필요한 곳에서 쓴다.
     * 행이 아직 없으면 null.
     */
    fun observeEntity(): Flow<SyncStateEntity?> = dao.observe()

    suspend fun get(): SyncState = dao.get()?.toCore() ?: SyncState()

    /** 마지막으로 장치에 성공적으로 보낸 페이로드. 한 번도 보내지 않았거나 예전 버전에서 올라온 직후면 null. */
    suspend fun syncedPayload(): String? = dao.get()?.syncedPayload

    /** 예전 방식이 남긴 변경 내역을 최신순으로 노출한다. 새 방식에서는 "업데이트 전 변경"으로만 쓴다. */
    fun observeChangeLog(): Flow<List<SyncChangeLogEntry>> =
        changeLogDao.observeAll().map { list -> list.map { it.toCore() } }

    /**
     * 데이터 변경이 있을 때마다 호출한다(데이터 저장 직후여야 한다 - 다시 계산할 때 새 값을 읽는다).
     * 건수는 세지 않는다. [description]은 호환을 위해 받지만 저장하지 않는다(무엇이 바뀌었는지는 페이로드 차이로 안다).
     * 홈 화면 위젯도 같은 데이터를 보여 주므로 끝에서 다시 그리도록 요청한다.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun markChanged(description: String) {
        changeTick.update { it + 1 }
        widgetRefresher.refresh()
    }

    /**
     * 동기화 성공 시 호출한다. 마지막 동기화 시각과 [payload](방금 장치에 보낸 전문)를 기준으로 삼고,
     * 미반영 건수와 예전 변경 내역을 비운다.
     */
    suspend fun markSynced(at: Instant, payload: String) {
        dao.upsert(SyncStateEntity(lastSyncedAt = at, pendingChangeCount = 0, syncedPayload = payload))
        changeLogDao.clearAll()
    }

    /**
     * 기준 페이로드만 채운다(예전 버전에서 올라온 직후 한 번). 시각과 건수는 그대로 둔다.
     * 행이 없으면 만든다.
     */
    suspend fun setBaseline(payload: String) {
        val current = dao.get() ?: SyncStateEntity(lastSyncedAt = null, pendingChangeCount = 0)
        dao.upsert(current.copy(syncedPayload = payload))
    }
}
