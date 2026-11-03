package com.kangminsang.hagoondori.core.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class WidgetCellLinesTest {

    @Test
    fun `줄이 상한 안에 들면 모두 보여 주고 +N은 없다`() {
        assertEquals(CellLinePlan(shown = 2, hidden = 0), WidgetCellLines.plan(total = 2, maxLines = 3))
        assertEquals("", WidgetCellLines.suffix(0))
    }

    @Test
    fun `줄 수가 넘치면 마지막으로 보여 준 줄에 못 보여 준 수를 붙인다`() {
        // 3줄 중 1줄만 들어가면 1줄 + 나머지 2줄 → "병원 +2" 모양.
        assertEquals(CellLinePlan(shown = 1, hidden = 2), WidgetCellLines.plan(total = 3, maxLines = 1))
        assertEquals(" +2", WidgetCellLines.suffix(2))
        assertEquals(CellLinePlan(shown = 3, hidden = 2), WidgetCellLines.plan(total = 5, maxLines = 3))
    }

    @Test
    fun `줄 칸이 없으면 아무것도 보여 주지 않고 전부 못 보여 준 것으로 센다`() {
        assertEquals(CellLinePlan(shown = 0, hidden = 4), WidgetCellLines.plan(total = 4, maxLines = 0))
        assertEquals(CellLinePlan(shown = 0, hidden = 0), WidgetCellLines.plan(total = 0, maxLines = 0))
    }

    @Test
    fun `높이에 들어가는 줄 수는 한 줄 미만이면 0, 상한은 3이다`() {
        assertEquals(0, WidgetCellLines.maxLines(availableDp = 12.9f, lineDp = 13f))
        assertEquals(1, WidgetCellLines.maxLines(availableDp = 13f, lineDp = 13f))
        assertEquals(2, WidgetCellLines.maxLines(availableDp = 27f, lineDp = 13f))
        assertEquals(WidgetCellLines.MAX_LINES, WidgetCellLines.maxLines(availableDp = 200f, lineDp = 13f))
    }

    @Test
    fun `음수 줄 수는 허용하지 않는다`() {
        assertThrows<IllegalArgumentException> { WidgetCellLines.plan(total = -1, maxLines = 1) }
    }
}
