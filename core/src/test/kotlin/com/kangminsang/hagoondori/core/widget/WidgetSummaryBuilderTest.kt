package com.kangminsang.hagoondori.core.widget

import com.kangminsang.hagoondori.core.calc.DDayCalculator
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightRecord
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.PassType
import com.kangminsang.hagoondori.core.model.PromotionDates
import com.kangminsang.hagoondori.core.model.UserProfile
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WidgetSummaryBuilderTest {

    private val today = LocalDate(2026, 9, 19)
    private fun d(m: Int, day: Int) = LocalDate(2026, m, day)

    private val reward = LeaveType("reward", "포상휴가", cap = 17)
    private val regular = LeaveType("regular", "정기휴가", cap = null, fixedDays = 27)

    private fun profile(
        promotionDates: PromotionDates = PromotionDates(),
        firstOvernightDate: LocalDate? = null,
    ) = UserProfile(
        enlistmentDate = d(3, 1),
        dischargeDate = LocalDate(2027, 10, 31),
        promotionDates = promotionDates,
        firstOvernightDate = firstOvernightDate,
        wakeUpTime = LocalTime(6, 0),
        dinnerTime = LocalTime(17, 30),
        autoAddOffDuty = true,
    )

    private fun build(
        profile: UserProfile = profile(),
        leaveTypes: List<LeaveType> = listOf(reward, regular),
        leaveGrants: List<LeaveGrant> = emptyList(),
        leaveUsages: List<LeaveUsage> = emptyList(),
        overnightRecords: List<OvernightRecord> = emptyList(),
        passRecords: List<PassRecord> = emptyList(),
        duties: List<DutyAssignment> = emptyList(),
    ) = WidgetSummaryBuilder.build(
        profile = profile,
        today = today,
        leaveTypes = leaveTypes,
        leaveGrants = leaveGrants,
        leaveUsages = leaveUsages,
        overnightRecords = overnightRecords,
        overnightForfeitures = emptyList(),
        passRecords = passRecords,
        duties = duties,
    )

    @Test
    fun `discharge D-day and service progress match the dashboard calculators`() {
        val p = profile()
        val summary = build(p)
        assertEquals(407, summary.dischargeDDay)
        assertEquals(DDayCalculator.serviceProgress(p.enlistmentDate, p.dischargeDate, today), summary.serviceProgressPercent)
    }

    @Test
    fun `only today's duties are included, overlapping types are de-duplicated and sorted DUTY then OFF_DUTY then MESS`() {
        val duties = listOf(
            DutyAssignment("a", today, DutyType.MESS),
            DutyAssignment("b", today, DutyType.DUTY),
            DutyAssignment("c", today, DutyType.OFF_DUTY),
            DutyAssignment("d", today, DutyType.DUTY),
        )
        assertEquals(listOf(DutyType.DUTY, DutyType.OFF_DUTY, DutyType.MESS), build(duties = duties).todayDuties)
    }

    @Test
    fun `duties on other dates are excluded`() {
        val duties = listOf(
            DutyAssignment("a", d(9, 18), DutyType.DUTY),
            DutyAssignment("b", d(9, 20), DutyType.OFF_DUTY),
        )
        assertEquals(emptyList<DutyType>(), build(duties = duties).todayDuties)
    }

    @Test
    fun `next promotion carries its label, date and D-day`() {
        val promotions = PromotionDates(
            privateFirstClass = d(6, 1),
            corporal = d(10, 1),
            sergeant = LocalDate(2027, 4, 1),
        )
        val next = build(profile(promotionDates = promotions)).nextPromotion
        assertEquals(WidgetPromotion("상병", d(10, 1), 12), next)
    }

    @Test
    fun `nextPromotion is null when every promotion date has passed`() {
        val promotions = PromotionDates(
            privateFirstClass = d(6, 1),
            corporal = d(8, 1),
            sergeant = d(9, 1),
        )
        assertNull(build(profile(promotionDates = promotions)).nextPromotion)
    }

    @Test
    fun `ongoing outing has a zero or negative D-day based on its start date`() {
        val usages = listOf(LeaveUsage("u", "reward", d(9, 18), d(9, 20)))
        val leaveGrants = listOf(LeaveGrant("g", "reward", 5, d(1, 1)))
        val outing = build(leaveGrants = leaveGrants, leaveUsages = usages).nextOuting
        assertEquals(WidgetOuting("포상", d(9, 18), -1), outing)
    }

    @Test
    fun `past outings are skipped and the nearest future outing is chosen`() {
        val usages = listOf(
            LeaveUsage("past", "reward", d(9, 10), d(9, 12)),
            LeaveUsage("near", "reward", d(9, 22), d(9, 23)),
        )
        val overnightProfile = profile(firstOvernightDate = d(9, 26))
        val outing = build(profile = overnightProfile, leaveUsages = usages).nextOuting
        assertEquals(WidgetOuting("포상", d(9, 22), 3), outing)
    }

    @Test
    fun `scheduled overnight is used as the next outing when it is the nearest`() {
        val overnightProfile = profile(firstOvernightDate = d(9, 26))
        assertEquals(WidgetOuting("외박", d(9, 26), 7), build(profile = overnightProfile).nextOuting)
    }

    @Test
    fun `without a first overnight date no scheduled overnight is added and leave outings still work`() {
        val usages = listOf(LeaveUsage("u", "reward", d(9, 22), d(9, 23)))
        val summary = build(profile = profile(firstOvernightDate = null), leaveUsages = usages)
        assertEquals(WidgetOuting("포상", d(9, 22), 3), summary.nextOuting)
    }

    @Test
    fun `without a first overnight date and without any leave or record the next outing is null`() {
        assertNull(build(profile = profile(firstOvernightDate = null)).nextOuting)
    }

    @Test
    fun `next pass is the earliest date on or after today`() {
        val passes = listOf(
            PassRecord("p1", d(9, 10), PassType.WEEKDAY),
            PassRecord("p2", d(9, 25), PassType.HOLIDAY),
            PassRecord("p3", d(9, 20), PassType.WEEKDAY),
        )
        assertEquals(WidgetPass(d(9, 20), 1), build(passRecords = passes).nextPass)
    }

    @Test
    fun `leave short names drop the trailing 휴가 and keep the original when nothing remains`() {
        val types = listOf(
            reward,
            regular,
            LeaveType("comfort", "위로휴가", cap = null),
            LeaveType("bare", "휴가", cap = null),
        )
        val grants = listOf(LeaveGrant("g1", "reward", 5, d(1, 1)))
        val usages = listOf(LeaveUsage("u1", "reward", d(9, 1), d(9, 3)))
        val remaining = build(leaveTypes = types, leaveGrants = grants, leaveUsages = usages).leaveRemaining
        assertEquals(
            listOf(
                WidgetLeaveRemaining("포상", 2),
                WidgetLeaveRemaining("정기", 27),
                WidgetLeaveRemaining("위로", 0),
                WidgetLeaveRemaining("휴가", 0),
            ),
            remaining,
        )
    }

    @Test
    fun `leave remaining keeps the order of the input leave types`() {
        val grants = listOf(LeaveGrant("g1", "reward", 5, d(1, 1)))
        val remaining = build(leaveTypes = listOf(regular, reward), leaveGrants = grants).leaveRemaining
        assertEquals(listOf(WidgetLeaveRemaining("정기", 27), WidgetLeaveRemaining("포상", 5)), remaining)
    }
}
