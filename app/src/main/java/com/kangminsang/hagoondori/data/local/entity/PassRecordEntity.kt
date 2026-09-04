package com.kangminsang.hagoondori.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kangminsang.hagoondori.core.model.PassType
import kotlinx.datetime.LocalDate

/**
 * [com.kangminsang.hagoondori.core.model.PassRecord]의 Room 매핑 (스펙 4.11절).
 * [type]은 기록 시점에 고정 저장된다 - 이후 공휴일 데이터가 바뀌어도 값이 변하지 않는다.
 */
@Entity(tableName = "pass_record")
data class PassRecordEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val type: PassType,
    val memo: String?,
)
