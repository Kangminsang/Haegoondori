package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightRecord
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OutingPeriodCalculatorTest {

    private val types = listOf(
        LeaveType("reward", "포상휴가", cap = 17),
        LeaveType("regular", "정기휴가", cap = null, fixedDays = 27),
    )
    private fun d(m: Int, day: Int) = LocalDate(2026, m, day)
    private val today = d(9, 19)

    private fun next(
        usages: List<LeaveUsage> = emptyList(),
        records: List<OvernightRecord> = emptyList(),
        nextOvernight: LocalDate? = null,
    ) = OutingPeriodCalculator.next(today, usages, types, records, nextOvernight)

    @Test
    fun `nothing scheduled returns null`() {
        assertNull(next())
    }

    @Test
    fun `standalone overnight is its own one day period`() {
        val p = next(nextOvernight = d(9, 26))!!
        assertEquals(d(9, 26), p.start)
        assertEquals(d(9, 26), p.end)
        assertEquals(listOf(OutingPeriod.Part("외박", 1)), p.parts)
    }

    @Test
    fun `overnight followed by reward and regular leave merges into one period`() {
        val p = next(
            usages = listOf(
                LeaveUsage("a", "reward", d(9, 23), d(9, 25)),
                LeaveUsage("b", "regular", d(9, 26), d(9, 28)),
            ),
            nextOvernight = d(9, 22),
        )!!
        assertEquals(d(9, 22), p.start)
        assertEquals(d(9, 28), p.end)
        assertEquals(7, p.totalDays)
        assertEquals(
            listOf(OutingPeriod.Part("외박", 1), OutingPeriod.Part("포상", 3), OutingPeriod.Part("연가", 3)),
            p.parts,
        )
    }

    @Test
    fun `overnight inside a leave is not double counted or named`() {
        val p = next(
            usages = listOf(LeaveUsage("a", "reward", d(9, 23), d(9, 27))),
            nextOvernight = d(9, 26),
        )!!
        assertEquals(listOf(OutingPeriod.Part("포상", 5)), p.parts)
    }

    @Test
    fun `gap of one day keeps periods separate and picks the earlier one`() {
        val p = next(
            usages = listOf(LeaveUsage("a", "reward", d(9, 30), d(10, 2))),
            nextOvernight = d(9, 26),
        )!!
        assertEquals(d(9, 26), p.start)
        assertEquals(1, p.totalDays)
    }

    @Test
    fun `finished periods are skipped and an ongoing one is returned`() {
        val p = next(
            usages = listOf(
                LeaveUsage("old", "reward", d(9, 1), d(9, 3)),
                LeaveUsage("now", "regular", d(9, 18), d(9, 21)),
            ),
        )!!
        assertEquals(d(9, 18), p.start)
        assertEquals(d(9, 21), p.end)
    }

    @Test
    fun `overnight record with end date extends the period`() {
        val p = next(
            records = listOf(OvernightRecord("o", d(9, 22), endDate = d(9, 23))),
            usages = listOf(LeaveUsage("a", "reward", d(9, 24), d(9, 25))),
        )!!
        assertEquals(d(9, 22), p.start)
        assertEquals(d(9, 25), p.end)
        assertEquals(listOf(OutingPeriod.Part("외박", 2), OutingPeriod.Part("포상", 2)), p.parts)
    }

    @Test
    fun `departure is 8 in the morning on the start date in the given zone`() {
        val seoul = kotlinx.datetime.TimeZone.of("Asia/Seoul")
        val period = OutingPeriod(d(9, 25), d(9, 28), listOf(OutingPeriod.Part("외박", 4)))
        // 2026-09-25 08:00 KST == 2026-09-24 23:00 UTC
        assertEquals(kotlinx.datetime.Instant.parse("2026-09-24T23:00:00Z"), OutingPeriodCalculator.departureAt(period, seoul))
    }
}
