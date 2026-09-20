package com.kangminsang.hagoondori.ui.dashboard

import com.kangminsang.hagoondori.core.calc.OvernightSchedule
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.SyncState
import com.kangminsang.hagoondori.core.model.UserProfile
import com.kangminsang.hagoondori.core.calc.CombatRestSummary
import com.kangminsang.hagoondori.core.calc.LeaveCalculator
import kotlinx.datetime.LocalDate

/** 휴가 종류 하나의 부여/사용/잔여 요약 (대시보드·출타관리 화면 공용). */
data class LeaveTypeSummary(
    val type: LeaveType,
    val granted: Int,
    val used: Int,
    /** 유효 기간이 지나 소멸한 일수. */
    val expired: Int = 0,
    val remaining: Int,
    /** 부여별 상태(정기휴가처럼 총량이 고정이면 비어 있다). */
    val grantStatuses: List<LeaveCalculator.GrantStatus> = emptyList(),
    /** 상한까지 남은 여유량. 상한이 없으면 null(무제한) - 3.2.3절 포상 상한 임박 경고에 쓰인다. */
    val remainingCapCapacity: Int?,
)

/** ① 대시보드(스펙 5.1/5.2절) 화면 상태. */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val today: LocalDate? = null,
    val dischargeDDay: Int? = null,
    val serviceProgressPercent: Double = 0.0,
    val promotionDDay: Int? = null,
    val promotionProgressPercent: Double? = null,
    val leaveSummaries: List<LeaveTypeSummary> = emptyList(),
    val combatRestSummary: CombatRestSummary = CombatRestSummary(convertedFromLeave = 0, directGranted = 0, totalUsed = 0),
    val overnightSchedule: OvernightSchedule? = null,
    val weekdayPassRemaining: Int = 0,
    val holidayPassRemaining: Int = 0,
    val syncState: SyncState = SyncState(),
)
