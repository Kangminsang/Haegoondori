package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.Holiday
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HolidayJudgeTest {

    @Test
    fun `weekday that is not a holiday is not a holiday`() {
        // 2026-09-04는 금요일
        assertFalse(HolidayJudge.isHoliday(LocalDate(2026, 9, 4), emptyList()))
    }

    @Test
    fun `saturday and sunday are holidays even without a holiday list entry`() {
        assertTrue(HolidayJudge.isHoliday(LocalDate(2026, 9, 5), emptyList())) // 토
        assertTrue(HolidayJudge.isHoliday(LocalDate(2026, 9, 6), emptyList())) // 일
    }

    @Test
    fun `a weekday listed as a holiday is a holiday`() {
        val holidays = listOf(Holiday(LocalDate(2026, 9, 25), "추석", isSubstitute = false))
        assertTrue(HolidayJudge.isHoliday(LocalDate(2026, 9, 25), holidays))
    }

    @Test
    fun `substitute holiday is judged the same as a regular holiday`() {
        val holidays = listOf(Holiday(LocalDate(2026, 10, 5), "대체공휴일", isSubstitute = true))
        assertTrue(HolidayJudge.isHoliday(LocalDate(2026, 10, 5), holidays))
    }

    @Test
    fun `leave deduction must not depend on holiday judgement`() {
        // 3.2.1절: 휴가 차감에는 isHoliday가 쓰이지 않는다. 이 테스트는 그 계약을
        // 문서화한다 - 주말이 섞인 기간이든 아니든 LeaveCalculator는 동일하게
        // start~end inclusive 일수만 본다 (LeaveCalculatorTest에서 실제 검증).
        val weekendDay = LocalDate(2026, 9, 5)
        assertTrue(HolidayJudge.isWeekend(weekendDay))
    }
}
