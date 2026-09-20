package com.kangminsang.hagoondori.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * [com.kangminsang.hagoondori.core.model.UserProfile]의 Room 매핑 (스펙 4.3절).
 * 앱 전체에서 단 1행만 존재한다 - [id]는 항상 [SINGLETON_ID]로 고정한다.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val enlistmentDate: LocalDate,
    val dischargeDate: LocalDate,
    val privateFirstClassDate: LocalDate?,
    val corporalDate: LocalDate?,
    val sergeantDate: LocalDate?,
    val firstOvernightDate: LocalDate?,
    val overnightCycleWeeks: Int,
    val weekdayPassPerMonth: Int,
    val holidayPassPerMonth: Int,
    val wakeUpTime: LocalTime,
    val dinnerTime: LocalTime,
    val autoAddOffDuty: Boolean,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
