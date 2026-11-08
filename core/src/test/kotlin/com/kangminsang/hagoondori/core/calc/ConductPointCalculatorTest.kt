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
}
