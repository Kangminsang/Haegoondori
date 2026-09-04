package com.kangminsang.hagoondori.core.holiday

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BuiltInHolidaySeedTest {

    @Test
    fun `generates 8 fixed holidays per year`() {
        val holidays = BuiltInHolidaySeed.generate(2026, 2026)
        assertEquals(8, holidays.size)
        assertTrue(holidays.none { it.isSubstitute })
    }

    @Test
    fun `covers the full inclusive year range`() {
        val holidays = BuiltInHolidaySeed.generate(2026, 2028)
        assertEquals(8 * 3, holidays.size)
        assertTrue(holidays.any { it.date == LocalDate(2026, 1, 1) && it.name == "신정" })
        assertTrue(holidays.any { it.date == LocalDate(2027, 8, 15) && it.name == "광복절" })
        assertTrue(holidays.any { it.date == LocalDate(2028, 12, 25) && it.name == "성탄절" })
    }

    @Test
    fun `does not include lunar-calendar holidays`() {
        val holidays = BuiltInHolidaySeed.generate(2026, 2026)
        assertTrue(holidays.none { it.name.contains("설날") })
        assertTrue(holidays.none { it.name.contains("추석") })
    }

    @Test
    fun `rejects a toYear before fromYear`() {
        assertThrows(IllegalArgumentException::class.java) { BuiltInHolidaySeed.generate(2027, 2026) }
    }
}
