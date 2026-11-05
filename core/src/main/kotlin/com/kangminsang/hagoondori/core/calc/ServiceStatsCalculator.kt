package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightRecord
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.PassType
import com.kangminsang.hagoondori.core.model.Promotion
import com.kangminsang.hagoondori.core.model.PromotionDates
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.plus

/** 지금까지(오늘 전) / 오늘부터 나눈 횟수·일수 한 쌍. */
data class PastAndUpcoming(val past: Int, val upcoming: Int) {
    val total: Int get() = past + upcoming
}

/**
 * 복무 기간 중 한 구간(지난 구간 또는 남은 구간)의 날짜별 통계.
 *
 * @property days 구간의 일수.
 * @property weekendDays 토·일요일 수.
 * @property publicHolidays 공휴일 목록에 있는 날짜 수(같은 날 이름이 둘이어도 한 번, 주말과 겹쳐도 센다).
 * @property restDays 주말이거나 공휴일인 날 수([HolidayJudge.isHoliday] 기준, 겹치면 한 번).
 * @property outingDays 휴가·외박으로 부대 밖에 있는(있을) 날 수. 휴가와 외박이 겹친 날은 한 번만 센다.
 * @property meals 부대에서 먹는(먹은) 식사 수. 하루 3끼에서 출타일은 모두 빼고, 외출한 날은
 *   [ServiceStatsCalculator.mealsMissedByPass]만큼 뺀다.
 */
data class PeriodStats(
    val days: Int,
    val weekendDays: Int,
    val publicHolidays: Int,
    val restDays: Int,
    val outingDays: Int,
    val meals: Int,
) {
    /** 부대 안에서 보내는(보낸) 날 = 구간 일수 − 출타한 날. */
    val onBaseDays: Int get() = days - outingDays
}

/** 다음 진급까지의 통계. [onBaseDays]는 그때까지 계획된 출타일을 뺀 부대 생활 일수다. */
data class PromotionStats(val rank: Promotion, val days: Int, val onBaseDays: Int)

/**
 * 대시보드 "군생활 통계" 카드에 보여줄 값 묶음. 모두 원본 기록에서 그때그때 계산하는 파생값이라 저장하지 않는다.
 *
 * 날짜 경계는 [DDayCalculator]와 같다. 입대일을 E, 전역일을 D, 오늘을 T라 할 때
 * - **지난 구간**은 `[E, T)` - 오늘은 아직 다 지나지 않았으므로 넣지 않는다([DDayCalculator.serviceProgress]의 경과일과 같다).
 * - **남은 구간**은 `[T, D)` - 오늘을 넣고 전역일 당일은 뺀다(전역 D-day 숫자와 같다).
 *
 * 두 구간은 겹치지 않고 합치면 정확히 전체 복무 기간 `[E, D)`가 된다. 입대 전이나 전역 뒤에 보더라도
 * 각 구간은 복무 기간 안으로 잘라서 센다.
 */
data class ServiceStats(
    val totalDays: Int,
    val passed: PeriodStats,
    val remaining: PeriodStats,
    /** 날짜 기준 복무 진행률(0~100). */
    val progressPercent: Double,
    /** 남은 일수를 `n주 m일`로 나눈 값. */
    val remainingWeeks: Int,
    val remainingWeeksExtraDays: Int,
    /** 남은 기간을 달력상 `n개월 m일`로 나눈 값(오늘부터 같은 날짜로 n개월 뒤, 그 뒤 남는 일수). */
    val remainingMonths: Int,
    val remainingMonthsExtraDays: Int,
    /** 남은 밤 = 남은 일수(오늘 밤부터 전역 전날 밤까지). */
    val remainingNights: Int,
    /** 지금까지 지난 날 중 출타한 날의 비율(%). 지난 날이 없으면 0. */
    val outingRatioPercent: Double,
    /** 휴가로 보낸 일수(지난) / 계획한 휴가 일수(오늘부터). 휴가 기간이 겹치면 한 번만 센다. */
    val leaveDays: PastAndUpcoming,
    /** 외박 횟수(시작일 기준). 예정은 이미 등록한 오늘 이후 외박만 센다. */
    val overnightCount: PastAndUpcoming,
    /** 외출 횟수. */
    val passCount: PastAndUpcoming,
    /** 전투휴무 일수(겹치면 한 번). */
    val combatRestDays: PastAndUpcoming,
    /** 근무 종류별 횟수. 기록이 없는 종류도 0으로 들어 있다. */
    val dutyCounts: Map<DutyType, PastAndUpcoming>,
    /** 가장 가까운 남은 진급(당일 포함). 없으면 null. */
    val nextPromotion: PromotionStats?,
)

/**
 * 복무 기준 정보와 출타·근무 기록으로 [ServiceStats]를 만든다. 순수 함수이며, 하루 단위로 세므로 복무
 * 기간(보통 600일 안팎)만큼만 반복한다.
 */
object ServiceStatsCalculator {
    const val MEALS_PER_DAY = 3

    /** 외출로 거르는 부대 식사: 평일 외출은 저녁 1끼, 휴일 외출은 점심·저녁 2끼. */
    fun mealsMissedByPass(type: PassType): Int = when (type) {
        PassType.WEEKDAY -> 1
        PassType.HOLIDAY -> 2
    }

    fun calculate(
        enlistmentDate: LocalDate,
        dischargeDate: LocalDate,
        today: LocalDate,
        promotionDates: PromotionDates = PromotionDates(),
        holidays: List<Holiday> = emptyList(),
        leaveUsages: List<LeaveUsage> = emptyList(),
        overnightRecords: List<OvernightRecord> = emptyList(),
        nextOvernightDate: LocalDate? = null,
        passRecords: List<PassRecord> = emptyList(),
        dutyAssignments: List<DutyAssignment> = emptyList(),
        combatRestUsages: List<CombatRestUsage> = emptyList(),
    ): ServiceStats {
        val totalDays = enlistmentDate.daysUntil(dischargeDate).coerceAtLeast(0)
        // 오늘을 복무 기간 안으로 잘라 두 구간의 경계로 쓴다.
        val split = today.coerceIn(enlistmentDate, maxOf(enlistmentDate, dischargeDate))

        val holidayDates = holidays.map { it.date }.toSet()
        val leaveDates = leaveUsages.flatMapTo(mutableSetOf()) { daysOf(it.startDate, it.endDate) }
        val outingDates = buildSet {
            addAll(leaveDates)
            overnightRecords.forEach { addAll(daysOf(it.date, it.endDate)) }
            // 아직 등록하지 않은 다음 외박 예정일도 대시보드 "다음 출타"와 같이 출타 예정으로 본다. 이미 지난 예정일은 뺀다.
            nextOvernightDate?.takeIf { it >= today }?.let(::add)
        }

        // 같은 날 외출 기록이 둘이면 더 많이 거르는 쪽 하나만 센다.
        val passMealsMissed = passRecords.groupBy { it.date }
            .mapValues { (_, records) -> records.maxOf { mealsMissedByPass(it.type) } }

        fun period(from: LocalDate, until: LocalDate): PeriodStats {
            var days = 0
            var weekend = 0
            var publicHoliday = 0
            var rest = 0
            var outing = 0
            var meals = 0
            var date = from
            while (date < until) {
                days++
                val isWeekend = HolidayJudge.isWeekend(date)
                val isPublicHoliday = date in holidayDates
                if (isWeekend) weekend++
                if (isPublicHoliday) publicHoliday++
                if (isWeekend || isPublicHoliday) rest++
                if (date in outingDates) {
                    outing++
                } else {
                    meals += MEALS_PER_DAY - (passMealsMissed[date] ?: 0)
                }
                date = date.plus(1, DateTimeUnit.DAY)
            }
            return PeriodStats(days, weekend, publicHoliday, rest, outing, meals)
        }

        val passed = period(enlistmentDate, split)
        val remaining = period(split, dischargeDate)

        val remainingMonths = if (remaining.days > 0) split.monthsUntil(dischargeDate) else 0
        val remainingMonthsExtraDays =
            if (remaining.days > 0) split.plus(remainingMonths, DateTimeUnit.MONTH).daysUntil(dischargeDate) else 0

        fun splitDates(dates: Collection<LocalDate>) =
            PastAndUpcoming(past = dates.count { it < today }, upcoming = dates.count { it >= today })

        val nextPromotion = ServiceScheduleCalculator.nextPromotion(promotionDates, today)?.let { (rank, date) ->
            val outingBefore = outingDates.count { it >= today && it < date }
            val days = today.daysUntil(date)
            PromotionStats(rank, days, days - outingBefore)
        }

        return ServiceStats(
            totalDays = totalDays,
            passed = passed,
            remaining = remaining,
            progressPercent = DDayCalculator.serviceProgress(enlistmentDate, dischargeDate, today),
            remainingWeeks = remaining.days / 7,
            remainingWeeksExtraDays = remaining.days % 7,
            remainingMonths = remainingMonths,
            remainingMonthsExtraDays = remainingMonthsExtraDays,
            remainingNights = remaining.days,
            outingRatioPercent = if (passed.days == 0) 0.0 else passed.outingDays * 100.0 / passed.days,
            leaveDays = splitDates(leaveDates),
            overnightCount = splitDates(overnightRecords.map { it.date }),
            passCount = splitDates(passRecords.map { it.date }),
            combatRestDays = splitDates(combatRestUsages.flatMapTo(mutableSetOf()) { daysOf(it.startDate, it.endDate) }),
            dutyCounts = DutyType.entries.associateWith { type ->
                splitDates(dutyAssignments.filter { it.type == type }.map { it.date })
            },
            nextPromotion = nextPromotion,
        )
    }

    /** [start]부터 [endInclusive]까지(포함) 하루씩. */
    private fun daysOf(start: LocalDate, endInclusive: LocalDate): List<LocalDate> = buildList {
        var date = start
        while (date <= endInclusive) {
            add(date)
            date = date.plus(1, DateTimeUnit.DAY)
        }
    }
}
