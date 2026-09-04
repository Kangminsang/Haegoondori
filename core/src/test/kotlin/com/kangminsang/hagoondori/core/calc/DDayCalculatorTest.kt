package com.kangminsang.hagoondori.core.calc

import kotlinx.datetime.LocalDate
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
}
