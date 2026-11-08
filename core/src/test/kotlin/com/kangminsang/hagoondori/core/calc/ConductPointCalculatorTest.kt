package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.ConductPoint
import com.kangminsang.hagoondori.core.model.ConductPointKind
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ConductPointCalculatorTest {

    private val records = listOf(
        ConductPoint("c1", LocalDate(2026, 9, 1), ConductPointKind.MERIT, 5, "표창"),
        ConductPoint("c2", LocalDate(2026, 9, 20), ConductPointKind.DEMERIT, 2, "지각"),
        ConductPoint("c3", LocalDate(2026, 10, 3), ConductPointKind.MERIT, 3, "선행"),
    )

    @Test
    fun `summarize totals merit, demerit and net`() {
        val s = ConductPointCalculator.summarize(records)
        assertEquals(8, s.merit)
        assertEquals(2, s.demerit)
        assertEquals(6, s.net)
    }

    @Test
    fun `net can be negative`() {
        val s = ConductPointCalculator.summarize(
            listOf(ConductPoint("c1", LocalDate(2026, 9, 1), ConductPointKind.DEMERIT, 4, "x")),
        )
        assertEquals(-4, s.net)
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
        assertEquals(5, s.merit)
        assertEquals(2, s.demerit)
    }

    @Test
    fun `validate accepts and trims`() {
        val r = ConductPointCalculator.validate(" 5 ", "  표창  ")
        assertNull(r.error)
        assertEquals(5, r.points)
        assertEquals("표창", r.reason)
    }

    @Test
    fun `validate rejects out of range or non numeric points`() {
        for (p in listOf("", "abc", "0", "-1", "1000")) {
            assertNotNull(ConductPointCalculator.validate(p, "사유").error, p)
        }
        assertNull(ConductPointCalculator.validate("999", "사유").error)
        assertNull(ConductPointCalculator.validate("1", "사유").error)
    }

    @Test
    fun `validate rejects blank or too long reason`() {
        assertNotNull(ConductPointCalculator.validate("1", "   ").error)
        assertNotNull(ConductPointCalculator.validate("1", "가".repeat(51)).error)
        assertNull(ConductPointCalculator.validate("1", "가".repeat(50)).error)
    }
}
