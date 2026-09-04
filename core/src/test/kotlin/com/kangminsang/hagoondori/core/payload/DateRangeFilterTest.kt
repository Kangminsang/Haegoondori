package com.kangminsang.hagoondori.core.payload

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DateRangeFilterTest {

    private val rangeStart = LocalDate(2026, 8, 28)
    private val rangeEnd = LocalDate(2027, 3, 3)

    @Test
    fun `item fully inside range overlaps`() {
        assertTrue(DateRangeFilter.overlaps(LocalDate(2026, 9, 1), LocalDate(2026, 9, 5), rangeStart, rangeEnd))
    }

    @Test
    fun `item starting before range but ending inside still overlaps`() {
        assertTrue(DateRangeFilter.overlaps(LocalDate(2026, 8, 20), LocalDate(2026, 8, 29), rangeStart, rangeEnd))
    }

    @Test
    fun `item entirely before range does not overlap`() {
        assertFalse(DateRangeFilter.overlaps(LocalDate(2026, 1, 1), LocalDate(2026, 8, 27), rangeStart, rangeEnd))
    }

    @Test
    fun `item entirely after range does not overlap`() {
        assertFalse(DateRangeFilter.overlaps(LocalDate(2027, 3, 4), LocalDate(2027, 4, 1), rangeStart, rangeEnd))
    }

    @Test
    fun `item touching range boundary exactly overlaps`() {
        assertTrue(DateRangeFilter.overlaps(rangeEnd, rangeEnd, rangeStart, rangeEnd))
        assertTrue(DateRangeFilter.overlaps(rangeStart, rangeStart, rangeStart, rangeEnd))
    }

    @Test
    fun `filterWithin keeps only dates inside the inclusive range`() {
        val dates = listOf(
            LocalDate(2026, 8, 27), // 범위 밖 (하루 전)
            LocalDate(2026, 8, 28), // 경계값, 포함
            LocalDate(2026, 12, 25),
            LocalDate(2027, 3, 3), // 경계값, 포함
            LocalDate(2027, 3, 4), // 범위 밖 (하루 후)
        )
        val kept = DateRangeFilter.filterWithin(dates, rangeStart, rangeEnd) { it }
        assertTrue(LocalDate(2026, 8, 27) !in kept)
        assertTrue(LocalDate(2026, 8, 28) in kept)
        assertTrue(LocalDate(2027, 3, 3) in kept)
        assertTrue(LocalDate(2027, 3, 4) !in kept)
    }
}
