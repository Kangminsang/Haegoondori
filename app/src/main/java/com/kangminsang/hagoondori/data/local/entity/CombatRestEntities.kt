package com.kangminsang.hagoondori.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

/**
 * [com.kangminsang.hagoondori.core.model.CombatRestGrant]의 Room 매핑 (스펙 4.7절).
 * 포상휴가 상한 초과 자동 전환분은 여기 저장하지 않는다 - 직접 부여분만 저장된다.
 */
@Entity(tableName = "combat_rest_grant")
data class CombatRestGrantEntity(
    @PrimaryKey val id: String,
    val days: Int,
    val grantedDate: LocalDate,
    val reason: String?,
)

/** [com.kangminsang.hagoondori.core.model.CombatRestUsage]의 Room 매핑 (스펙 4.8절). */
@Entity(tableName = "combat_rest_usage")
data class CombatRestUsageEntity(
    @PrimaryKey val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val memo: String?,
)
