package com.kangminsang.hagoondori.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kangminsang.hagoondori.core.model.DutyType
import kotlinx.datetime.LocalDate

/**
 * [com.kangminsang.hagoondori.core.model.DutyAssignment]의 Room 매핑 (스펙 4.13절).
 * `(date, type)` 조합이 유일 키다 - 하루에 여러 종류(당직/비번/츄라이)가 겹칠 수
 * 있지만, 같은 종류를 같은 날 두 번 배정할 수는 없다.
 */
@Entity(
    tableName = "duty_assignment",
    indices = [Index(value = ["date", "type"], unique = true)],
)
data class DutyAssignmentEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val type: DutyType,
)
