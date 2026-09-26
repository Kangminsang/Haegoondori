package com.kangminsang.hagoondori.ui.dashboard

import com.kangminsang.hagoondori.core.calc.OutingPeriod
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.SyncChangeLogEntry
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
    /** 이미 시작한(출타를 나간) 휴가만 사용으로 반영한 잔여. */
    val remaining: Int,
    /** 아직 시작하지 않고 계획만 해 둔 휴가 일수. */
    val planned: Int = 0,
    /** 계획한 휴가까지 모두 쓴다고 가정했을 때의 잔여. */
    val remainingAfterPlanned: Int = remaining,
    /** 부여별 상태(정기휴가처럼 총량이 고정이면 비어 있다). */
    val grantStatuses: List<LeaveCalculator.GrantStatus> = emptyList(),
    /** 상한까지 남은 여유량. 상한이 없으면 null(무제한) - 3.2.3절 포상 상한 임박 경고에 쓰인다. */
    val remainingCapCapacity: Int?,
)

/** 전체 복무 기간 위의 진급 지점 하나. [progressPercent]는 입대일~전역일 안에서의 위치(0~100). */
data class PromotionMarker(val label: String, val progressPercent: Double, val passed: Boolean)

/** 다음 진급까지의 상태. [progressPercent]는 직전 진급(또는 입대)부터 이 진급까지 중 지금까지 온 정도. */
data class NextPromotion(val label: String, val dDay: Int, val progressPercent: Double)

/** ① 대시보드(스펙 5.1/5.2절) 화면 상태. */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val today: LocalDate? = null,
    val dischargeDDay: Int? = null,
    val serviceProgressPercent: Double = 0.0,
    /** 진행 막대에 표시할 계급별 진급 지점(지난 것 포함). */
    val promotionMarkers: List<PromotionMarker> = emptyList(),
    /** 가장 가까운 남은 진급. 남은 진급이 없으면 null. */
    val nextPromotion: NextPromotion? = null,
    val leaveSummaries: List<LeaveTypeSummary> = emptyList(),
    val combatRestSummary: CombatRestSummary = CombatRestSummary(convertedFromLeave = 0, directGranted = 0, totalUsed = 0),
    /** 오늘 이후(진행 중 포함) 가장 가까운 외박·휴가 기간(이어진 것은 한 기간). 없으면 null. */
    val nextOuting: OutingPeriod? = null,
    /** 오늘 이후 가장 가까운 외출. 없으면 null. */
    val nextPass: LocalDate? = null,
    val syncState: SyncState = SyncState(),
    val syncChangeLog: List<SyncChangeLogEntry> = emptyList(),
)
