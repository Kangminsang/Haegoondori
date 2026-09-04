package com.kangminsang.hagoondori.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kangminsang.hagoondori.core.model.OverflowBehavior
import kotlinx.datetime.LocalDate

/** [com.kangminsang.hagoondori.core.model.LeaveType]의 Room 매핑 (스펙 4.4절). */
@Entity(tableName = "leave_type")
data class LeaveTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cap: Int?,
    val overflowBehavior: OverflowBehavior,
)

/**
 * [com.kangminsang.hagoondori.core.model.LeaveGrant]의 Room 매핑 (스펙 4.5절).
 * [LeaveUsage](사용)와 반드시 분리된 별도 테이블이다 - 포상 상한은 부여 누적 기준이다.
 */
@Entity(tableName = "leave_grant")
data class LeaveGrantEntity(
    @PrimaryKey val id: String,
    val leaveTypeId: String,
    val days: Int,
    val grantedDate: LocalDate,
    val reason: String?,
)

/** [com.kangminsang.hagoondori.core.model.LeaveUsage]의 Room 매핑 (스펙 4.6절). */
@Entity(tableName = "leave_usage")
data class LeaveUsageEntity(
    @PrimaryKey val id: String,
    val leaveTypeId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val label: String?,
)
