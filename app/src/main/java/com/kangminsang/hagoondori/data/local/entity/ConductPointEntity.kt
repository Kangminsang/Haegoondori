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
    /** 초과근무 기록이면 근무 시간(0.1시간 단위)과 휴일 여부. 둘은 함께 null이거나 함께 값이 있다. */
    val overtimeTenthHours: Int? = null,
    val overtimeOnHoliday: Boolean? = null,
)
