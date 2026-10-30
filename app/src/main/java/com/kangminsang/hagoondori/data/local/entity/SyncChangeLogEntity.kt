package com.kangminsang.hagoondori.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/** [com.kangminsang.hagoondori.core.model.SyncChangeLogEntry]의 Room 매핑 (스펙 4.15절). */
@Entity(tableName = "sync_change_log")
data class SyncChangeLogEntity(
    @PrimaryKey val id: String,
    val description: String,
    val occurredAt: Instant,
)
