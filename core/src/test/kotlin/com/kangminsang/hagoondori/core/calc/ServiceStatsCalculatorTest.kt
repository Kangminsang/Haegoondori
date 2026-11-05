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
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ServiceStatsCalculatorTest {

    // 2026-01-05(월) 입대, 2026-01-26(월) 전역: 21일. 오늘 2026-01-12(월).
    private val enlistment = LocalDate(2026, 1, 5)
    private val discharge = LocalDate(2026, 1, 26)
    private val today = LocalDate(2026, 1, 12)

    private fun d(day: Int) = LocalDate(2026, 1, day)

    private fun full() = ServiceStatsCalculator.calculate(
        enlistmentDate = enlistment,
        dischargeDate = discharge,
        today = today,
        promotionDates = PromotionDates(privateFirstClass = d(6), corporal = d(21)),
        holidays = listOf(
            Holiday(d(7), "수요일 공휴일"),
            Holiday(d(14), "수요일 공휴일"),
            Holiday(d(14), "같은 날 다른 이름"),
            Holiday(d(18), "일요일 공휴일"),
            Holiday(LocalDate(2026, 2, 1), "복무 기간 밖"),
        ),
        leaveUsages = listOf(
            LeaveUsage("l1", "annual", d(8), d(10)),
            LeaveUsage("l2", "annual", d(12), d(13)), // 오늘 시작해 진행 중
        ),
        overnightRecords = listOf(
            OvernightRecord("o1", d(10), endDate = d(11)), // 휴가와 10일이 겹친다
            OvernightRecord("o2", d(20)),
        ),
        nextOvernightDate = d(22),
        passRecords = listOf(
            PassRecord("p1", d(6), PassType.WEEKDAY),
            PassRecord("p2", d(12), PassType.WEEKDAY),
            PassRecord("p3", LocalDate(2026, 1, 30), PassType.HOLIDAY),
        ),
        dutyAssignments = listOf(
            DutyAssignment("d1", d(7), DutyType.DUTY),
            DutyAssignment("d2", d(15), DutyType.DUTY),
            DutyAssignment("d3", d(8), DutyType.OFF_DUTY),
        ),
        combatRestUsages = listOf(
            CombatRestUsage("c1", d(6), d(7)),
            CombatRestUsage("c2", d(7), d(8)),
        ),
    )

    @Test
    fun `passed range excludes today and remaining range includes today but not discharge day`() {
        val stats = full()
        assertEquals(21, stats.totalDays)
        assertEquals(7, stats.passed.days)
        assertEquals(14, stats.remaining.days)
        // 남은 일수는 전역 D-day와 같고, 지난 일수는 진행률의 경과일과 같다.
        assertEquals(DDayCalculator.dDay(today, discharge), stats.remaining.days)
        assertEquals(stats.totalDays, stats.passed.days + stats.remaining.days)
        assertEquals(7.0 / 21.0 * 100.0, stats.progressPercent, 1e-9)
    }

    @Test
    fun `weekends public holidays and rest days are counted per range`() {
        val stats = full()
        assertEquals(2, stats.passed.weekendDays) // 1/10, 1/11
        assertEquals(1, stats.passed.publicHolidays) // 1/7
        assertEquals(3, stats.passed.restDays)
        assertEquals(4, stats.remaining.weekendDays) // 1/17, 18, 24, 25
        assertEquals(2, stats.remaining.publicHolidays) // 1/14(이름 둘이어도 한 번), 1/18
        assertEquals(5, stats.remaining.restDays) // 일요일 공휴일 1/18은 한 번만
    }

    @Test
    fun `overlapping leave and overnight are counted once as outing days`() {
        val stats = full()
        assertEquals(4, stats.passed.outingDays) // 1/8, 9, 10, 11
        assertEquals(3, stats.passed.onBaseDays)
        assertEquals(4.0 / 7.0 * 100.0, stats.outingRatioPercent, 1e-9)
        assertEquals(4, stats.remaining.outingDays) // 1/12, 13, 20, 22(예정 외박)
        assertEquals(10, stats.remaining.onBaseDays)
    }

    @Test
    fun `record counts split at today with today counted as upcoming`() {
        val stats = full()
        assertEquals(PastAndUpcoming(3, 2), stats.leaveDays)
        assertEquals(PastAndUpcoming(1, 1), stats.overnightCount)
        assertEquals(PastAndUpcoming(1, 2), stats.passCount)
        assertEquals(PastAndUpcoming(3, 0), stats.combatRestDays) // 1/7 겹침은 한 번
        assertEquals(PastAndUpcoming(1, 1), stats.dutyCounts[DutyType.DUTY])
        assertEquals(PastAndUpcoming(1, 0), stats.dutyCounts[DutyType.OFF_DUTY])
        assertEquals(PastAndUpcoming(0, 0), stats.dutyCounts[DutyType.MESS])
        assertEquals(3, stats.passCount.total)
    }

    @Test
    fun `derived remaining figures`() {
        val stats = full()
        assertEquals(2, stats.remainingWeeks)
        assertEquals(0, stats.remainingWeeksExtraDays)
        assertEquals(0, stats.remainingMonths)
        assertEquals(14, stats.remainingMonthsExtraDays)
        assertEquals(42, stats.remainingMeals)
        assertEquals(14, stats.remainingNights)
    }

    @Test
    fun `next promotion subtracts planned outing days before it`() {
        val promotion = full().nextPromotion!!
        assertEquals(Promotion.CORPORAL, promotion.rank)
        assertEquals(9, promotion.days)
        assertEquals(6, promotion.onBaseDays) // 1/12, 13, 20 출타
    }

    @Test
    fun `promotion on today counts as next with zero days`() {
        val stats = ServiceStatsCalculator.calculate(
            enlistment, discharge, today, promotionDates = PromotionDates(privateFirstClass = today),
        )
        assertEquals(PromotionStats(Promotion.PRIVATE_FIRST_CLASS, 0, 0), stats.nextPromotion)
    }

    @Test
    fun `no remaining promotion yields null`() {
        assertNull(ServiceStatsCalculator.calculate(enlistment, discharge, today).nextPromotion)
    }

    @Test
    fun `remaining months and weeks over a long span`() {
        val stats = ServiceStatsCalculator.calculate(
            enlistmentDate = LocalDate(2025, 6, 1),
            dischargeDate = LocalDate(2027, 1, 31),
            today = LocalDate(2026, 11, 5),
        )
        assertEquals(87, stats.remaining.days)
        assertEquals(12, stats.remainingWeeks)
        assertEquals(3, stats.remainingWeeksExtraDays)
        assertEquals(2, stats.remainingMonths) // 1/5까지
        assertEquals(26, stats.remainingMonthsExtraDays)
    }

    @Test
    fun `before enlistment nothing has passed and the whole service remains`() {
        val stats = ServiceStatsCalculator.calculate(enlistment, discharge, LocalDate(2025, 12, 20))
        assertEquals(0, stats.passed.days)
        assertEquals(21, stats.remaining.days)
        assertEquals(0.0, stats.outingRatioPercent)
        assertEquals(0.0, stats.progressPercent)
    }

    @Test
    fun `on enlistment day nothing has passed yet`() {
        val stats = ServiceStatsCalculator.calculate(enlistment, discharge, enlistment)
        assertEquals(0, stats.passed.days)
        assertEquals(21, stats.remaining.days)
    }

    @Test
    fun `on and after discharge day nothing remains`() {
        for (t in listOf(discharge, LocalDate(2026, 3, 1))) {
            val stats = ServiceStatsCalculator.calculate(enlistment, discharge, t)
            assertEquals(21, stats.passed.days)
            assertEquals(0, stats.remaining.days)
            assertEquals(0, stats.remainingMonths)
            assertEquals(0, stats.remainingMonthsExtraDays)
            assertEquals(0, stats.remainingMeals)
            assertEquals(100.0, stats.progressPercent)
        }
    }

    @Test
    fun `overdue unrecorded overnight date is not counted as planned outing`() {
        val stats = ServiceStatsCalculator.calculate(enlistment, discharge, today, nextOvernightDate = d(9))
        assertEquals(0, stats.passed.outingDays)
        assertEquals(0, stats.remaining.outingDays)
    }

    @Test
    fun `outing records outside the service period are ignored in period stats`() {
        val stats = ServiceStatsCalculator.calculate(
            enlistment, discharge, today,
            leaveUsages = listOf(LeaveUsage("l", "annual", d(24), LocalDate(2026, 1, 28))),
        )
        assertEquals(2, stats.remaining.outingDays) // 1/24, 25 - 전역일 이후는 남은 구간 밖
    }
}
