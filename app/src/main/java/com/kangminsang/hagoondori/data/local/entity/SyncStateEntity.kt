package com.kangminsang.hagoondori.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * [com.kangminsang.hagoondori.core.model.SyncState]의 Room 매핑 (스펙 4.15절).
 * 앱 전체에서 단 1행만 존재한다.
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastSyncedAt: Instant?,
    val pendingChangeCount: Int,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
