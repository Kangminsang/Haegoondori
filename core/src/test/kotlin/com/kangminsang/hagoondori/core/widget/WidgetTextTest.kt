package com.kangminsang.hagoondori.core.widget

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WidgetTextTest {

    @Test
    fun `D-day는 양수 음수 0을 구분해 쓴다`() {
        assertEquals("D-231", WidgetText.formatDDay(231))
        assertEquals("D-0", WidgetText.formatDDay(0))
        assertEquals("D+5", WidgetText.formatDDay(-5))
    }

    @Test
    fun `제목은 전역 D-day 형식이다`() {
        assertEquals("전역 D-231", WidgetText.dischargeTitle(231))
        assertEquals("전역 D+1", WidgetText.dischargeTitle(-1))
    }

    @Test
    fun `날짜는 월일을 두 자리로 쓴다`() {
        assertEquals("12-16", WidgetText.monthDay(LocalDate(2026, 12, 16)))
        assertEquals("01-05", WidgetText.monthDay(LocalDate(2027, 1, 5)))
    }

    @Test
    fun `다음 진급 줄`() {
        val promotion = WidgetPromotion("상병", LocalDate(2026, 12, 16), 45)
        assertEquals("상병 12-16 (D-45)", WidgetText.promotionLine(promotion))
    }

    @Test
    fun `출타는 시작 전이면 날짜와 D-day, 진행 중이면 진행 중 글자`() {
        val upcoming = WidgetOuting("외박", LocalDate(2026, 11, 15), 14)
        assertEquals("외박 11-15 (D-14)", WidgetText.outingLine(upcoming))

        val today = WidgetOuting("외박", LocalDate(2026, 11, 1), 0)
        assertEquals("외박 진행 중", WidgetText.outingLine(today))

        val ongoing = WidgetOuting("외박", LocalDate(2026, 10, 30), -2)
        assertEquals("외박 진행 중", WidgetText.outingLine(ongoing))
    }

    @Test
    fun `외출 줄은 날짜와 D-day만 쓰고 오늘이면 D-0이다`() {
        assertEquals("11-08 (D-7)", WidgetText.passLine(WidgetPass(LocalDate(2026, 11, 8), 7)))
        assertEquals("11-01 (D-0)", WidgetText.passLine(WidgetPass(LocalDate(2026, 11, 1), 0)))
    }

    @Test
    fun `휴가 잔여는 종류 순서대로 잇고 비어 있으면 null`() {
        val items = listOf(
            WidgetLeaveRemaining("포상", 3),
            WidgetLeaveRemaining("정기", 17),
            WidgetLeaveRemaining("위로", 2),
        )
        assertEquals("포상 3 · 정기 17 · 위로 2", WidgetText.leaveRemainingLine(items))
        assertNull(WidgetText.leaveRemainingLine(emptyList()))
    }

    @Test
    fun `진행률은 정수로 반올림하고 0에서 100 사이로 제한한다`() {
        assertEquals(62, WidgetText.progressPercent(62.4))
        assertEquals(63, WidgetText.progressPercent(62.5))
        assertEquals(0, WidgetText.progressPercent(-3.0))
        assertEquals(100, WidgetText.progressPercent(150.0))
        assertEquals(100, WidgetText.progressPercent(Double.POSITIVE_INFINITY))
        assertEquals(0, WidgetText.progressPercent(Double.NaN))
        assertEquals("62%", WidgetText.progressText(62.0))
    }
}
