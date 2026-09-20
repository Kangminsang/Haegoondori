package com.kangminsang.hagoondori.core.calc

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DDayCalculatorTest {

    @Test
    fun `dDay is positive for a future date and negative for a past date`() {
        val today = LocalDate(2026, 9, 4)
        assertEquals(10, DDayCalculator.dDay(today, LocalDate(2026, 9, 14)))
        assertEquals(-5, DDayCalculator.dDay(today, LocalDate(2026, 8, 30)))
        assertEquals(0, DDayCalculator.dDay(today, today))
    }

    @Test
    fun `serviceProgress is 0 at enlistment and 100 at discharge`() {
        val enlistment = LocalDate(2025, 1, 1)
        val discharge = LocalDate(2027, 1, 1) // 정확히 730일(2025는 평년, 2026도 평년)
        assertEquals(0.0, DDayCalculator.serviceProgress(enlistment, discharge, enlistment))
        assertEquals(100.0, DDayCalculator.serviceProgress(enlistment, discharge, discharge))
    }

    @Test
    fun `serviceProgress at the halfway point is 50`() {
        val enlistment = LocalDate(2025, 1, 1)
        val discharge = LocalDate(2027, 1, 1)
        val halfway = LocalDate(2026, 1, 1) // enlistment + 365일, total 730일
        assertEquals(50.0, DDayCalculator.serviceProgress(enlistment, discharge, halfway))
    }

    @Test
    fun `serviceProgress clamps to the 0 to 100 range`() {
        val enlistment = LocalDate(2025, 1, 1)
        val discharge = LocalDate(2027, 1, 1)
        assertEquals(0.0, DDayCalculator.serviceProgress(enlistment, discharge, LocalDate(2024, 1, 1))) // 입대 전
        assertEquals(100.0, DDayCalculator.serviceProgress(enlistment, discharge, LocalDate(2028, 1, 1))) // 전역 후
    }

    @Test
    fun `promotionProgress marks where the promotion date falls within the service period`() {
        val enlistment = LocalDate(2025, 1, 1)
        val discharge = LocalDate(2027, 1, 1)
        val promotion = LocalDate(2026, 1, 1) // 정확히 중간
        assertEquals(50.0, DDayCalculator.promotionProgress(enlistment, discharge, promotion))
    }

    private val seoul = TimeZone.of("Asia/Seoul")

    @Test
    fun `liveServiceProgress equals serviceProgress at midnight`() {
        val enlistment = LocalDate(2025, 1, 1)
        val discharge = LocalDate(2027, 1, 1)
        val day = LocalDate(2026, 3, 15)
        val live = DDayCalculator.liveServiceProgress(enlistment, discharge, day.atStartOfDayIn(seoul), seoul)
        assertEquals(DDayCalculator.serviceProgress(enlistment, discharge, day), live, 1e-9)
    }

    @Test
    fun `liveServiceProgress keeps rising within a day`() {
        val enlistment = LocalDate(2025, 1, 1)
        val discharge = LocalDate(2027, 1, 1)
        val midnight = LocalDate(2026, 3, 15).atStartOfDayIn(seoul)
        val noon = Instant.fromEpochMilliseconds(midnight.toEpochMilliseconds() + 12 * 3600 * 1000L)
        val atMidnight = DDayCalculator.liveServiceProgress(enlistment, discharge, midnight, seoul)
        val atNoon = DDayCalculator.liveServiceProgress(enlistment, discharge, noon, seoul)
        // 하루는 전체 730일의 1/730 = 0.137%, 반나절은 그 절반
        assertEquals(100.0 / 730 / 2, atNoon - atMidnight, 1e-9)
    }

    @Test
    fun `liveServiceProgress clamps before enlistment and after discharge`() {
        val enlistment = LocalDate(2025, 1, 1)
        val discharge = LocalDate(2027, 1, 1)
        assertEquals(0.0, DDayCalculator.liveServiceProgress(enlistment, discharge, LocalDate(2024, 6, 1).atStartOfDayIn(seoul), seoul))
        assertEquals(100.0, DDayCalculator.liveServiceProgress(enlistment, discharge, LocalDate(2028, 1, 1).atStartOfDayIn(seoul), seoul))
    }

    @Test
    fun `remainingMillis counts down to discharge midnight and stops at zero`() {
        val discharge = LocalDate(2027, 1, 1)
        val dayBefore = LocalDate(2026, 12, 31).atStartOfDayIn(seoul)
        assertEquals(24 * 3600 * 1000L, DDayCalculator.remainingMillis(discharge, dayBefore, seoul))
        assertEquals(0L, DDayCalculator.remainingMillis(discharge, LocalDate(2027, 2, 1).atStartOfDayIn(seoul), seoul))
    }

    @Test
    fun `elapsedMillis counts up from enlistment midnight and is zero before enlistment`() {
        val enlistment = LocalDate(2026, 1, 1)
        val dayAfter = LocalDate(2026, 1, 2).atStartOfDayIn(seoul)
        assertEquals(24 * 3600 * 1000L, DDayCalculator.elapsedMillis(enlistment, dayAfter, seoul))
        assertEquals(0L, DDayCalculator.elapsedMillis(enlistment, LocalDate(2025, 12, 1).atStartOfDayIn(seoul), seoul))
    }
}
