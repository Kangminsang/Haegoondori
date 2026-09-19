package com.kangminsang.hagoondori.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

/**
 * [com.kangminsang.hagoondori.core.model.OvernightRecord]의 Room 매핑 (스펙 4.9절).
 * 예정일도 차수 번호도 저장하지 않는다 - 둘 다 매번 계산한다.
 */
@Entity(tableName = "overnight_record")
data class OvernightRecordEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val memo: String?,
    val endDate: LocalDate,
)

/**
 * [com.kangminsang.hagoondori.core.model.OvernightForfeiture]의 Room 매핑 (스펙 4.10절).
 * [slotIndex]는 `UserProfile.firstOvernightDate` 기준 상대 번호임에 주의.
 */
@Entity(tableName = "overnight_forfeiture")
data class OvernightForfeitureEntity(
    @PrimaryKey val id: String,
    val slotIndex: Int,
    val reason: String?,
    val recordedDate: LocalDate?,
)
