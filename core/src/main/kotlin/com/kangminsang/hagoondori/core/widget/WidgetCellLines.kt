package com.kangminsang.hagoondori.core.widget

import kotlin.math.floor
import kotlin.math.min

/**
 * 칸 하나에 보여 줄 글자 줄의 계획. [shown]은 앞에서부터 보여 줄 줄 수,
 * [hidden]은 못 보여 준 줄 수다. 보여 준 줄 중 마지막 줄 끝에 `+hidden`을 붙인다.
 */
data class CellLinePlan(val shown: Int, val hidden: Int)

/** 홈 화면 달력 칸의 글자 줄 선택. 칸 높이에서 들어갈 줄 수를 구하고, 넘치면 `+N` 계획을 만든다. */
object WidgetCellLines {

    /** 칸 하나에 쓰는 줄의 상한. 칸이 아무리 커도 이 줄 수까지만 보여 준다. */
    const val MAX_LINES = 3

    /**
     * [availableDp] 높이에 [lineDp] 높이 줄이 몇 개 들어가는지 구한다. 한 줄도 안 들어가면 0,
     * 상한은 [MAX_LINES].
     */
    fun maxLines(availableDp: Float, lineDp: Float): Int {
        if (lineDp <= 0f || availableDp < lineDp) return 0
        return min(MAX_LINES, floor(availableDp / lineDp).toInt())
    }

    /**
     * [total]줄을 [maxLines]줄 칸에 담는 계획. 넘치면 앞의 [maxLines]줄을 보여 주고,
     * 마지막 줄에 못 보여 준 수([total] - [maxLines])를 붙인다. 줄 칸이 0이면 아무것도 보여 주지 않는다.
     */
    fun plan(total: Int, maxLines: Int): CellLinePlan {
        require(total >= 0 && maxLines >= 0) { "줄 수는 0 이상이어야 한다" }
        if (maxLines == 0) return CellLinePlan(shown = 0, hidden = total)
        if (total <= maxLines) return CellLinePlan(shown = total, hidden = 0)
        return CellLinePlan(shown = maxLines, hidden = total - maxLines)
    }

    /** 마지막 줄에 붙일 접미사. 못 보여 준 줄이 없으면 빈 문자열. 예: `" +2"`. */
    fun suffix(hidden: Int): String = if (hidden > 0) " +$hidden" else ""
}
