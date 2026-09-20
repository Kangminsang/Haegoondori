package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.Promotion
import com.kangminsang.hagoondori.core.model.PromotionDates
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ServiceScheduleCalculatorTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate(y, m, day)

    @Test
    fun `discharge is enlistment plus 20 months minus one day`() {
        val schedule = ServiceScheduleCalculator.calculate(d(2026, 2, 9))
        assertEquals(d(2027, 10, 8), schedule.dischargeDate)
    }

    @Test
    fun `promotions fall on the first of the month 3, 9 and 15 months after the enlistment month`() {
        val p = ServiceScheduleCalculator.calculate(d(2026, 2, 9)).promotionDates
        assertEquals(d(2026, 5, 1), p.privateFirstClass)
        assertEquals(d(2026, 11, 1), p.corporal)
        assertEquals(d(2027, 5, 1), p.sergeant)
    }

    @Test
    fun `promotions cross the year boundary`() {
        val p = ServiceScheduleCalculator.calculate(d(2025, 11, 20)).promotionDates
        assertEquals(d(2026, 2, 1), p.privateFirstClass)
        assertEquals(d(2026, 8, 1), p.corporal)
        assertEquals(d(2027, 2, 1), p.sergeant)
    }

    @Test
    fun `nextPromotion picks the nearest upcoming one and includes today`() {
        val p = ServiceScheduleCalculator.calculate(d(2026, 2, 9)).promotionDates
        assertEquals(Promotion.CORPORAL to d(2026, 11, 1), ServiceScheduleCalculator.nextPromotion(p, d(2026, 9, 20)))
        assertEquals(Promotion.CORPORAL to d(2026, 11, 1), ServiceScheduleCalculator.nextPromotion(p, d(2026, 11, 1)))
        assertEquals(Promotion.SERGEANT to d(2027, 5, 1), ServiceScheduleCalculator.nextPromotion(p, d(2026, 11, 2)))
        assertNull(ServiceScheduleCalculator.nextPromotion(p, d(2027, 5, 2)))
    }

    @Test
    fun `nextPromotion ignores ranks without a date`() {
        val p = PromotionDates(sergeant = d(2027, 4, 1))
        assertEquals(Promotion.SERGEANT to d(2027, 4, 1), ServiceScheduleCalculator.nextPromotion(p, d(2026, 1, 1)))
        assertNull(ServiceScheduleCalculator.nextPromotion(PromotionDates(), d(2026, 1, 1)))
    }

    @Test
    fun `progress to next promotion runs from the previous promotion, or enlistment for the first`() {
        val enlistment = d(2026, 2, 9)
        val p = ServiceScheduleCalculator.calculate(enlistment).promotionDates
        // 일병(5/1) 진급 전: 입대일 → 5/1 구간
        assertEquals(0.0, ServiceScheduleCalculator.progressToNextPromotion(enlistment, p, d(2026, 5, 1), enlistment), 1e-9)
        // 상병(11/1)을 향해: 직전 진급인 일병(5/1)이 구간 시작
        assertEquals(0.0, ServiceScheduleCalculator.progressToNextPromotion(enlistment, p, d(2026, 11, 1), d(2026, 5, 1)), 1e-9)
        assertEquals(100.0, ServiceScheduleCalculator.progressToNextPromotion(enlistment, p, d(2026, 11, 1), d(2026, 11, 1)), 1e-9)
    }
}
