package com.kangminsang.hagoondori.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * [com.kangminsang.hagoondori.core.model.SyncState]의 Room 매핑 (스펙 4.15절).
 * 앱 전체에서 단 1행만 존재한다.
 *
 * [syncedPayload]는 마지막으로 장치에 성공적으로 보낸 페이로드 전문이다. "동기화하지 않은 변경"은
 * 이 값과 지금 데이터로 만든 페이로드의 순 차이로 계산한다(PendingChangesTracker). [pendingChangeCount]는
 * 예전 방식(변경 발생마다 1씩 증가)의 잔재로, 새 방식에서는 더 이상 올리지 않는다.
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastSyncedAt: Instant?,
    val pendingChangeCount: Int,
    val syncedPayload: String? = null,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
