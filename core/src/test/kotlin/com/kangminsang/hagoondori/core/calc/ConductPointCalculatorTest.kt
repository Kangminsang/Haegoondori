package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.ConductPoint
import com.kangminsang.hagoondori.core.model.ConductPointKind
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.OvernightForfeiture
import com.kangminsang.hagoondori.core.model.Overtime
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConductPointCalculatorTest {

    private val records = listOf(
        ConductPoint("c1", LocalDate(2026, 9, 1), ConductPointKind.MERIT, 500, "표창"),
        ConductPoint("c2", LocalDate(2026, 9, 20), ConductPointKind.DEMERIT, 25, "지각"),
        ConductPoint("c3", LocalDate(2026, 10, 3), ConductPointKind.MERIT, 150, "선행"),
    )

    @Test
    fun `summarize totals merit, demerit and net`() {
        val s = ConductPointCalculator.summarize(records)
        assertEquals(650, s.merit)
        assertEquals(25, s.demerit)
        assertEquals(625, s.net)
    }

    @Test
    fun `quarter points add up without rounding error`() {
        val quarters = (1..10).map { ConductPoint("q$it", LocalDate(2026, 9, it), ConductPointKind.MERIT, 25, "x") }
        assertEquals("2.5", ConductPointCalculator.format(ConductPointCalculator.summarize(quarters).merit))
    }

    @Test
    fun `net can be negative`() {
        val s = ConductPointCalculator.summarize(
            listOf(ConductPoint("c1", LocalDate(2026, 9, 1), ConductPointKind.DEMERIT, 400, "x")),
        )
        assertEquals(-400, s.net)
    }

    @Test
    fun `empty list summarizes to zero`() {
        val s = ConductPointCalculator.summarize(emptyList())
        assertEquals(0, s.merit)
        assertEquals(0, s.net)
    }

    @Test
    fun `summarizeMonth counts only the given month`() {
        val s = ConductPointCalculator.summarizeMonth(records, 2026, 9)
        assertEquals(500, s.merit)
        assertEquals(25, s.demerit)
    }

    @Test
    fun `parseCentiPoints reads integers and up to two decimals`() {
        mapOf("3" to 300, "0.25" to 25, ".5" to 50, "1.5" to 150, "1." to 100, " 2.05 " to 205, "999.99" to 99_999)
            .forEach { (text, expected) -> assertEquals(expected, ConductPointCalculator.parseCentiPoints(text), text) }
        listOf("", ".", "abc", "-1", "0.125", "1,5", "1000", "1.2.3")
            .forEach { assertNull(ConductPointCalculator.parseCentiPoints(it), it) }
    }

    @Test
    fun `format drops trailing zeros`() {
        assertEquals("0.25", ConductPointCalculator.format(25))
        assertEquals("1.5", ConductPointCalculator.format(150))
        assertEquals("3", ConductPointCalculator.format(300))
        assertEquals("0.05", ConductPointCalculator.format(5))
        assertEquals("0", ConductPointCalculator.format(0))
        assertEquals("2.5", ConductPointCalculator.format(-250))
    }

    @Test
    fun `validate accepts and trims`() {
        val r = ConductPointCalculator.validate(" 0.25 ", "  표창  ")
        assertNull(r.error)
        assertEquals(25, r.centiPoints)
        assertEquals("표창", r.reason)
    }

    @Test
    fun `validate rejects out of range or malformed points`() {
        for (p in listOf("", "abc", "0", "0.00", "-1", "1000", "0.001")) {
            assertNotNull(ConductPointCalculator.validate(p, "사유").error, p)
        }
        assertNull(ConductPointCalculator.validate("999.99", "사유").error)
        assertNull(ConductPointCalculator.validate("0.01", "사유").error)
    }

    @Test
    fun `validate rejects blank or too long reason`() {
        assertNotNull(ConductPointCalculator.validate("1", "   ").error)
        assertNotNull(ConductPointCalculator.validate("1", "가".repeat(51)).error)
        assertNull(ConductPointCalculator.validate("1", "가".repeat(50)).error)
    }

    @Test
    fun `overtime points are exact for weekday and holiday`() {
        assertEquals(50, ConductPointCalculator.overtimeCentiPoints(10, onHoliday = false))
        assertEquals(75, ConductPointCalculator.overtimeCentiPoints(15, onHoliday = false))
        assertEquals(100, ConductPointCalculator.overtimeCentiPoints(10, onHoliday = true))
        assertEquals(300, ConductPointCalculator.overtimeCentiPoints(30, onHoliday = true))
        assertEquals(5, ConductPointCalculator.overtimeCentiPoints(1, onHoliday = false))
    }

    @Test
    fun `parse hours accepts one decimal place`() {
        assertEquals(20, ConductPointCalculator.parseTenthHours("2"))
        assertEquals(15, ConductPointCalculator.parseTenthHours("1.5"))
        assertEquals(5, ConductPointCalculator.parseTenthHours(".5"))
        assertEquals(240, ConductPointCalculator.parseTenthHours("24"))
        assertNull(ConductPointCalculator.parseTenthHours("1.25"))
        assertNull(ConductPointCalculator.parseTenthHours("abc"))
        assertNull(ConductPointCalculator.parseTenthHours(""))
        assertNull(ConductPointCalculator.parseTenthHours("-1"))
    }

    @Test
    fun `validate overtime hours enforces 0_1 to 24`() {
        assertNull(ConductPointCalculator.validateOvertimeHours("0.1").error)
        assertEquals(1, ConductPointCalculator.validateOvertimeHours("0.1").tenthHours)
        assertNull(ConductPointCalculator.validateOvertimeHours("24").error)
        assertNotNull(ConductPointCalculator.validateOvertimeHours("0").error)
        assertNotNull(ConductPointCalculator.validateOvertimeHours("24.1").error)
        assertNotNull(ConductPointCalculator.validateOvertimeHours("1.25").error)
    }

    @Test
    fun `format hours and default reason`() {
        assertEquals("1.5", ConductPointCalculator.formatHours(15))
        assertEquals("3", ConductPointCalculator.formatHours(30))
        assertEquals("평일 초과근무 1.5시간", ConductPointCalculator.overtimeReason(15, onHoliday = false))
        assertEquals("휴일 초과근무 3시간", ConductPointCalculator.overtimeReason(30, onHoliday = true))
    }

    @Test
    fun `overtime record requires merit kind and matching points`() {
        val ok = ConductPoint("o1", LocalDate(2026, 11, 3), ConductPointKind.MERIT, 75, "야근", overtime = Overtime(15, false))
        assertEquals(75, ok.centiPoints)
        assertThrows(IllegalArgumentException::class.java) {
            ConductPoint("o2", LocalDate(2026, 11, 3), ConductPointKind.DEMERIT, 75, "x", overtime = Overtime(15, false))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ConductPoint("o3", LocalDate(2026, 11, 3), ConductPointKind.MERIT, 100, "x", overtime = Overtime(15, false))
        }
        assertThrows(IllegalArgumentException::class.java) { Overtime(0, false) }
        assertThrows(IllegalArgumentException::class.java) { Overtime(241, true) }
    }

    @Test
    fun `holiday classification uses weekends and holidays`() {
        val holidays = listOf(Holiday(LocalDate(2026, 11, 4), "임시"))
        assertFalse(ConductPointCalculator.isHolidayOvertime(LocalDate(2026, 11, 3), holidays)) // 화
        assertTrue(ConductPointCalculator.isHolidayOvertime(LocalDate(2026, 11, 7), holidays)) // 토
        assertTrue(ConductPointCalculator.isHolidayOvertime(LocalDate(2026, 11, 4), holidays)) // 공휴일
    }

    @Test
    fun `find reward leave type by id, name, then overflow behavior`() {
        val other = com.kangminsang.hagoondori.core.model.LeaveType("a", "연가", cap = null)
        val byName = com.kangminsang.hagoondori.core.model.LeaveType("b", "포상휴가", cap = 17)
        val byBehavior = com.kangminsang.hagoondori.core.model.LeaveType(
            "c", "상", cap = 17, overflowBehavior = com.kangminsang.hagoondori.core.model.OverflowBehavior.CONVERT_TO_COMBAT_REST,
        )
        val byId = com.kangminsang.hagoondori.core.model.LeaveType("leave-reward", "이름바뀜", cap = 17)
        assertEquals("leave-reward", ConductPointCalculator.findRewardLeaveType(listOf(other, byName, byBehavior, byId))?.id)
        assertEquals("b", ConductPointCalculator.findRewardLeaveType(listOf(other, byBehavior, byName))?.id)
        assertEquals("c", ConductPointCalculator.findRewardLeaveType(listOf(other, byBehavior))?.id)
        assertNull(ConductPointCalculator.findRewardLeaveType(listOf(other)))
    }

    private fun conversion(merit: Int, demerit: Int, rewardDays: Int = 0, restrictions: Int = 0) =
        ConductConversion(merit, demerit, rewardDays, restrictions)

    @Test
    fun `merit and demerit offset each other`() {
        val c = conversion(merit = 2650, demerit = 500)
        assertEquals(2150, c.balance)
        assertEquals(2, c.rewardLeaveDaysPending)
        assertEquals(850, c.meritTowardNextRewardDay)
        assertEquals(0, c.overnightRestrictionsPending)
        assertEquals(4150, c.demeritTowardNextRestriction)

        val offset = conversion(merit = 1500, demerit = 1000)
        assertEquals(0, offset.rewardLeaveDaysPending)
        assertEquals(500, offset.meritTowardNextRewardDay)
    }

    @Test
    fun `remaining demerit leads to overnight restriction`() {
        val c = conversion(merit = 500, demerit = 5000)
        assertEquals(-4500, c.balance)
        assertEquals(2, c.overnightRestrictionsPending)
        assertEquals(1500, c.demeritTowardNextRestriction)
        assertEquals(0, c.rewardLeaveDaysPending)
        assertEquals(5500, c.meritTowardNextRewardDay)
    }

    @Test
    fun `converted points are used up`() {
        val afterReward = conversion(merit = 2650, demerit = 0, rewardDays = 2)
        assertEquals(650, afterReward.balance)
        assertEquals(0, afterReward.rewardLeaveDaysPending)
        assertEquals(350, afterReward.meritTowardNextRewardDay)

        val afterRestriction = conversion(merit = 0, demerit = 4500, restrictions = 2)
        assertEquals(-500, afterRestriction.balance)
        assertEquals(0, afterRestriction.overnightRestrictionsPending)
        assertEquals(1500, afterRestriction.demeritTowardNextRestriction)
    }

    @Test
    fun `conversion counts applied records by reason`() {
        val summary = ConductPointSummary(merit = 3000, demerit = 0)
        val grants = listOf(
            LeaveGrant("g1", "leave-reward", 1, LocalDate(2026, 11, 1), ConductPointCalculator.REWARD_CONVERSION_REASON),
            LeaveGrant("g2", "leave-reward", 5, LocalDate(2026, 11, 1), "표창"),
        )
        val forfeitures = listOf(
            OvernightForfeiture("f1", 0, ConductPointCalculator.OVERNIGHT_RESTRICTION_REASON),
            OvernightForfeiture("f2", 1, "징계"),
        )
        val c = ConductPointCalculator.conversion(summary, grants, forfeitures)
        assertEquals(1, c.rewardLeaveDaysApplied)
        assertEquals(1, c.overnightRestrictionsApplied)
        assertEquals(3000 - 1000 + 2000, c.balance)
        assertEquals(4, c.rewardLeaveDaysPending)
    }
}
