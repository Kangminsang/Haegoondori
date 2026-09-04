package com.kangminsang.hagoondori.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

/**
 * [com.kangminsang.hagoondori.core.model.Holiday]의 Room 매핑 (스펙 4.14절).
 * [date]를 기본 키로 쓴다 - 스펙 원본 모델에도 별도 id가 없고, 하루당 하나의
 * 공휴일 레코드만 의미가 있다(같은 날짜를 다시 upsert하면 최신 정보로 덮어써야
 * 하는 캐시 성격의 데이터이기도 하다, 4.14절 "반드시 로컬 캐시한다").
 */
@Entity(tableName = "holiday")
data class HolidayEntity(
    @PrimaryKey val date: LocalDate,
    val name: String,
    val isSubstitute: Boolean,
)
