package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.ConductConversionRecord
import com.kangminsang.hagoondori.core.model.ConductConversionType
import com.kangminsang.hagoondori.core.model.ConductPoint
import com.kangminsang.hagoondori.core.model.ConductPointKind
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.Overtime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
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
    fun `conversion sums record counts by type`() {
        val summary = ConductPointSummary(merit = 3000, demerit = 0)
        val d = LocalDate(2026, 11, 5)
        val records = listOf(
            ConductConversionRecord("r1", d, ConductConversionType.REWARD_LEAVE, 1),
            ConductConversionRecord("r2", d, ConductConversionType.REWARD_LEAVE, 1),
            ConductConversionRecord("r3", d, ConductConversionType.OVERNIGHT_RESTRICTION, 1),
        )
        val c = ConductPointCalculator.conversion(summary, records)
        assertEquals(2, c.rewardLeaveDaysApplied)
        assertEquals(1, c.overnightRestrictionsApplied)
        assertEquals(3000 - 2000 + 2000, c.balance)
        assertEquals(3, c.rewardLeaveDaysPending)
    }

    @Test
    fun `conversion record requires positive count`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConductConversionRecord("r", LocalDate(2026, 11, 5), ConductConversionType.REWARD_LEAVE, 0)
        }
    }

    @Test
    fun `recentReasons returns distinct trimmed reasons, latest first, excluding auto overtime reasons and other kind`() {
        val d = ConductPointKind.DEMERIT
        val m = ConductPointKind.MERIT
        val list = listOf(
            ConductPoint("a", LocalDate(2026, 9, 1), m, 100, "표창"),
            ConductPoint("b", LocalDate(2026, 9, 5), m, 100, "선행"),
            ConductPoint("c", LocalDate(2026, 9, 5), m, 100, " 표창 "),
            ConductPoint("d", LocalDate(2026, 9, 9), d, 100, "지각"),
            ConductPoint("e", LocalDate(2026, 9, 10), m, 50, "평일 초과근무 1시간", overtime = Overtime(10, false)),
            ConductPoint("f", LocalDate(2026, 8, 1), m, 100, "봉사"),
            ConductPoint("g", LocalDate(2026, 8, 20), m, 200, "당직 지원", overtime = Overtime(20, true)),
        )
        assertEquals(listOf("표창", "선행", "당직 지원", "봉사"), ConductPointCalculator.recentReasons(list, m))
        assertEquals(listOf("지각"), ConductPointCalculator.recentReasons(list, d))
    }

    @Test
    fun `recentReasons honors the limit and handles empty`() {
        val list = (1..12).map { ConductPoint("r$it", LocalDate(2026, 9, it), ConductPointKind.DEMERIT, 100, "사유$it") }
        val r = ConductPointCalculator.recentReasons(list, ConductPointKind.DEMERIT)
        assertEquals(8, r.size)
        assertEquals("사유12", r.first())
        assertEquals(2, ConductPointCalculator.recentReasons(list, ConductPointKind.DEMERIT, limit = 2).size)
        assertTrue(ConductPointCalculator.recentReasons(emptyList(), ConductPointKind.MERIT).isEmpty())
    }

    @Test
    fun `validateConversionCount accepts 1 to pending only`() {
        assertEquals(3, ConductPointCalculator.validateConversionCount(" 3 ", 3, "일").count)
        assertEquals(1, ConductPointCalculator.validateConversionCount("1", 3, "일").count)
        for (bad in listOf("", "0", "4", "-1", "1.5", "abc")) {
            val r = ConductPointCalculator.validateConversionCount(bad, 3, "회")
            assertNull(r.count, bad)
            assertEquals("1~3회로 입력해 주세요.", r.error, bad)
        }
    }

    @Test
    fun `overtime hours from start and end times`() {
        fun h(a: LocalTime, b: LocalTime) = ConductPointCalculator.overtimeTenthHours(a, b)
        assertEquals(25, h(LocalTime(18, 0), LocalTime(20, 30)))
        assertEquals(30, h(LocalTime(22, 0), LocalTime(1, 0)))
        assertEquals(13, h(LocalTime(18, 0), LocalTime(19, 20)))
        assertEquals(0, h(LocalTime(18, 0), LocalTime(18, 5)))
        assertEquals(0, h(LocalTime(18, 0), LocalTime(18, 0)))
    }

    @Test
    fun `overtime times validation`() {
        val ok = ConductPointCalculator.validateOvertimeTimes(LocalTime(22, 0), LocalTime(1, 0))
        assertEquals(30, ok.tenthHours)
        assertTrue(ok.crossesMidnight)
        assertFalse(ConductPointCalculator.validateOvertimeTimes(LocalTime(18, 0), LocalTime(20, 30)).crossesMidnight)
        assertEquals("시작과 끝 시각이 같아요.", ConductPointCalculator.validateOvertimeTimes(LocalTime(18, 0), LocalTime(18, 0)).error)
        assertEquals("6분 이상이어야 해요.", ConductPointCalculator.validateOvertimeTimes(LocalTime(18, 0), LocalTime(18, 5)).error)
        assertNotNull(ConductPointCalculator.validateOvertimeTimes(null, LocalTime(18, 5)).error)
    }

    @Test
    fun `overtime points and reason from times`() {
        val start = LocalTime(18, 0)
        val end = LocalTime(20, 30)
        val tenth = ConductPointCalculator.overtimeTenthHours(start, end)
        assertEquals(125, ConductPointCalculator.overtimeCentiPoints(tenth, false))
        assertEquals(250, ConductPointCalculator.overtimeCentiPoints(tenth, true))
        assertEquals("평일 초과근무 18:00~20:30", ConductPointCalculator.overtimeReason(false, start, end))
        val rec = ConductPoint("t1", LocalDate(2026, 11, 3), ConductPointKind.MERIT, 125, "x", overtime = Overtime(25, false, start, end))
        assertEquals(end, rec.overtime!!.endTime)
        assertThrows(IllegalArgumentException::class.java) { Overtime(24, false, start, end) }
        assertThrows(IllegalArgumentException::class.java) { Overtime(25, false, start, null) }
    }

    @Test
    fun `isAutoOvertimeReason detects only the auto-filled overtime reason`() {
        val m = ConductPointKind.MERIT
        val start = LocalTime(18, 0)
        val end = LocalTime(20, 30)
        val timed = Overtime(25, false, start, end)
        assertTrue(ConductPointCalculator.isAutoOvertimeReason(ConductPoint("a", LocalDate(2026, 9, 1), m, 125, "평일 초과근무 18:00~20:30", overtime = timed)))
        assertTrue(ConductPointCalculator.isAutoOvertimeReason(ConductPoint("b", LocalDate(2026, 9, 1), m, 50, "평일 초과근무 1시간", overtime = Overtime(10, false))))
        assertFalse(ConductPointCalculator.isAutoOvertimeReason(ConductPoint("c", LocalDate(2026, 9, 1), m, 125, "당직 지원", overtime = timed)))
        assertFalse(ConductPointCalculator.isAutoOvertimeReason(ConductPoint("d", LocalDate(2026, 9, 1), m, 100, "평일 초과근무 1시간")))
    }
}
