package com.kangminsang.hagoondori.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

/** [com.kangminsang.hagoondori.core.model.Event]의 Room 매핑 (스펙 4.12절). */
@Entity(tableName = "event")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isImportant: Boolean,
    val memo: String?,
)
