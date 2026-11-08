package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.ConductPoint
import com.kangminsang.hagoondori.core.model.ConductPointKind

/** 양호·과실 점수 합계. [net] = 양호 − 과실. */
data class ConductPointSummary(val merit: Int, val demerit: Int) {
    val net: Int get() = merit - demerit
}

/** 점수 입력 검증 결과. [error]가 null이면 통과, [points]·[reason]은 정리된 값이다. */
data class ConductPointInput(val points: Int?, val reason: String, val error: String?)

object ConductPointCalculator {
    const val MIN_POINTS = 1
    const val MAX_POINTS = 999
    const val MAX_REASON_LENGTH = 50
    const val MAX_MEMO_LENGTH = 200

    fun summarize(records: List<ConductPoint>): ConductPointSummary = ConductPointSummary(
        merit = records.filter { it.kind == ConductPointKind.MERIT }.sumOf { it.points },
        demerit = records.filter { it.kind == ConductPointKind.DEMERIT }.sumOf { it.points },
    )

    /** [year]년 [month]월에 기록된 점수만 합산한다. */
    fun summarizeMonth(records: List<ConductPoint>, year: Int, month: Int): ConductPointSummary =
        summarize(records.filter { it.date.year == year && it.date.monthNumber == month })

    /** 입력 문자열을 검증한다. 점수는 1..999, 사유는 공백 제거 후 비어 있지 않고 50자 이하. */
    fun validate(pointsText: String, reason: String, memo: String = ""): ConductPointInput {
        val trimmedReason = reason.trim()
        val points = pointsText.trim().toIntOrNull()
        val error = when {
            points == null || points !in MIN_POINTS..MAX_POINTS -> "점수는 $MIN_POINTS~${MAX_POINTS}점으로 입력해 주세요."
            trimmedReason.isEmpty() -> "사유를 입력해 주세요."
            trimmedReason.length > MAX_REASON_LENGTH -> "사유는 ${MAX_REASON_LENGTH}자 이하로 입력해 주세요."
            memo.trim().length > MAX_MEMO_LENGTH -> "메모는 ${MAX_MEMO_LENGTH}자 이하로 입력해 주세요."
            else -> null
        }
        return ConductPointInput(points, trimmedReason, error)
    }
}
