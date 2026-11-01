package com.kangminsang.hagoondori.core.widget

import com.kangminsang.hagoondori.core.calc.DDayCalculator
import com.kangminsang.hagoondori.core.calc.LeaveCalculator
import com.kangminsang.hagoondori.core.calc.OutingPeriodCalculator
import com.kangminsang.hagoondori.core.calc.OvernightScheduleCalculator
import com.kangminsang.hagoondori.core.calc.ServiceScheduleCalculator
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightForfeiture
import com.kangminsang.hagoondori.core.model.OvernightRecord
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.UserProfile
import kotlinx.datetime.LocalDate

/**
 * 홈 화면 위젯에 보낼 요약 값. 대시보드(F2)와 같은 계산기·같은 인자로 구하며, 화면 상태 타입에 의존하지 않는다.
 *
 * @property todayDuties 오늘 근무 종류. 중복을 뺐고 [DutyType] 선언 순서(당직 → 비번 → 츄라이)다.
 * @property leaveRemaining 휴가 종류별 잔여. 입력된 휴가 종류 순서를 그대로 유지한다.
 */
data class WidgetSummary(
    val dischargeDDay: Int,
    val serviceProgressPercent: Double,
    val todayDuties: List<DutyType>,
    val nextPromotion: WidgetPromotion?,
    val nextOuting: WidgetOuting?,
    val nextPass: WidgetPass?,
    val leaveRemaining: List<WidgetLeaveRemaining>,
)

/** 다음 진급. [dDay]는 오늘 기준 진급일까지의 일수다. */
data class WidgetPromotion(val label: String, val date: LocalDate, val dDay: Int)

/**
 * 다음 외박·휴가 기간. [label]은 대시보드와 같이 종류 라벨을 `·`로 이은 값이고, [dDay]는 시작일 기준이다.
 * 오늘 진행 중이면 0 이하가 될 수 있다.
 */
data class WidgetOuting(val label: String, val start: LocalDate, val dDay: Int)

/** 다음 외출일. */
data class WidgetPass(val date: LocalDate, val dDay: Int)

/** 휴가 종류 하나의 잔여 일수. [shortName]은 이름 끝의 "휴가"를 뗀 값이다(포상휴가 → 포상). */
data class WidgetLeaveRemaining(val shortName: String, val remaining: Int)

/**
 * 위젯 요약 계산. 시간([today])은 인자로만 받으므로 순수 JVM에서 테스트할 수 있다.
 * 계산 규칙은 `DashboardViewModel.buildUiState`와 같아야 한다.
 */
object WidgetSummaryBuilder {

    fun build(
        profile: UserProfile,
        today: LocalDate,
        leaveTypes: List<LeaveType>,
        leaveGrants: List<LeaveGrant>,
        leaveUsages: List<LeaveUsage>,
        overnightRecords: List<OvernightRecord>,
        overnightForfeitures: List<OvernightForfeiture>,
        passRecords: List<PassRecord>,
        duties: List<DutyAssignment>,
    ): WidgetSummary {
        val overnightSchedule = profile.firstOvernightDate?.let { first ->
            OvernightScheduleCalculator.buildSchedule(
                firstOvernightDate = first,
                cycleWeeks = profile.overnightCycleWeeks,
                records = overnightRecords,
                forfeitures = overnightForfeitures,
            )
        }
        val nextOvernightDate = overnightSchedule?.nextScheduledDate

        val nextPromotion = ServiceScheduleCalculator.nextPromotion(profile.promotionDates, today)
            ?.let { (rank, date) -> WidgetPromotion(rank.label, date, DDayCalculator.dDay(today, date)) }

        val nextOuting = OutingPeriodCalculator.next(
            today = today,
            leaveUsages = leaveUsages,
            leaveTypes = leaveTypes,
            overnightRecords = overnightRecords,
            nextOvernightDate = nextOvernightDate,
        )?.let { period ->
            WidgetOuting(
                label = period.parts.joinToString("·") { it.label },
                start = period.start,
                dDay = DDayCalculator.dDay(today, period.start),
            )
        }

        val nextPass = passRecords.map { it.date }.filter { it >= today }.minOrNull()
            ?.let { WidgetPass(it, DDayCalculator.dDay(today, it)) }

        val todayDuties = duties.filter { it.date == today }.map { it.type }.distinct().sorted()

        val leaveRemaining = leaveTypes.map { type ->
            WidgetLeaveRemaining(
                shortName = shortName(type.name),
                remaining = LeaveCalculator.summarize(
                    type, leaveGrants, leaveUsages, today,
                    overnightRecords = overnightRecords,
                    nextOvernightDate = nextOvernightDate,
                ).remaining,
            )
        }

        return WidgetSummary(
            dischargeDDay = DDayCalculator.dDay(today, profile.dischargeDate),
            serviceProgressPercent = DDayCalculator.serviceProgress(profile.enlistmentDate, profile.dischargeDate, today),
            todayDuties = todayDuties,
            nextPromotion = nextPromotion,
            nextOuting = nextOuting,
            nextPass = nextPass,
            leaveRemaining = leaveRemaining,
        )
    }

    /** 이름 끝의 "휴가"를 뗀다. 남는 글자가 없으면 원래 이름을 그대로 쓴다. */
    private fun shortName(name: String): String {
        val base = name.trim().removeSuffix("휴가").trim()
        return base.ifEmpty { name }
    }
}
