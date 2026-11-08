package com.kangminsang.hagoondori.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kangminsang.hagoondori.core.model.ConductConversionType
import kotlinx.datetime.LocalDate

/** [com.kangminsang.hagoondori.core.model.ConductConversionRecord]의 Room 매핑. 장치로 보내지 않는 기록이다. */
@Entity(tableName = "conduct_conversion")
data class ConductConversionEntity(
    @PrimaryKey val id: String,
    val meetingDate: LocalDate,
    val type: ConductConversionType,
    val count: Int,
    val memo: String?,
)
