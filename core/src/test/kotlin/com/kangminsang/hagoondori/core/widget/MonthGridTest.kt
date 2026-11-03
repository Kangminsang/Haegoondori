package com.kangminsang.hagoondori.core.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MonthGridTest {

    @Test
    fun `윤년 2월은 29일이고 1일 앞 빈칸을 요일로 센다`() {
        // 2028년 2월 1일은 화요일이라 앞 빈칸 2개(일·월).
        assertEquals(29, WidgetMonthGrid.daysInMonth(2028, 2))
        assertEquals(2, WidgetMonthGrid.leadingBlanks(2028, 2))
        assertEquals(5, WidgetMonthGrid.weekCount(2028, 2))
    }

    @Test
    fun `1일이 일요일인 평년 2월은 28일이 정확히 4주다`() {
        // 2026년 2월 1일은 일요일.
        assertEquals(0, WidgetMonthGrid.leadingBlanks(2026, 2))
        assertEquals(28, WidgetMonthGrid.daysInMonth(2026, 2))
        assertEquals(4, WidgetMonthGrid.weekCount(2026, 2))
        assertEquals(3, WidgetMonthGrid.rowOf(2026, 2, 28))
        assertEquals(6, WidgetMonthGrid.columnOf(2026, 2, 28))
    }

    @Test
    fun `6주가 필요한 달이 있다`() {
        // 2026년 8월 1일은 토요일: 앞 빈칸 6개 + 31일 = 37칸 = 6주.
        assertEquals(6, WidgetMonthGrid.leadingBlanks(2026, 8))
        assertEquals(6, WidgetMonthGrid.weekCount(2026, 8))
        assertEquals(5, WidgetMonthGrid.rowOf(2026, 8, 31))
    }

    @Test
    fun `2026년 11월은 1일이 일요일이라 5주이고 날짜 위치가 맞다`() {
        assertEquals(5, WidgetMonthGrid.weekCount(2026, 11))
        assertEquals(0, WidgetMonthGrid.rowOf(2026, 11, 1))
        assertEquals(0, WidgetMonthGrid.columnOf(2026, 11, 1))
        assertEquals(1, WidgetMonthGrid.rowOf(2026, 11, 8))
        assertEquals(0, WidgetMonthGrid.columnOf(2026, 11, 8))
        assertEquals(4, WidgetMonthGrid.rowOf(2026, 11, 30))
        assertEquals(1, WidgetMonthGrid.columnOf(2026, 11, 30))
    }

    @Test
    fun `12월은 연도가 넘어가도 31일이다`() {
        assertEquals(31, WidgetMonthGrid.daysInMonth(2026, 12))
    }
}
