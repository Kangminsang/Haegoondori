package com.kangminsang.hagoondori.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kangminsang.hagoondori.core.model.ConductPointKind
import kotlinx.datetime.LocalDate

/** [com.kangminsang.hagoondori.core.model.ConductPoint]의 Room 매핑. 장치로 보내지 않는 기록이다. */
@Entity(tableName = "conduct_point")
data class ConductPointEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val kind: ConductPointKind,
    val centiPoints: Int,
    val reason: String,
    val memo: String?,
)
