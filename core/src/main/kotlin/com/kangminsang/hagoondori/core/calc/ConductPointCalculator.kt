package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.ConductPoint
import com.kangminsang.hagoondori.core.model.ConductPointKind
import kotlin.math.abs

/** 양호·과실 점수 합계(0.01점 단위). [net] = 양호 − 과실. */
data class ConductPointSummary(val merit: Int, val demerit: Int) {
    val net: Int get() = merit - demerit
}

/** 점수 입력 검증 결과. [error]가 null이면 통과, [centiPoints]·[reason]은 정리된 값이다. */
data class ConductPointInput(val centiPoints: Int?, val reason: String, val error: String?)

object ConductPointCalculator {
    /** 소수점 아래 최대 자릿수(0.01점 단위). */
    const val DECIMALS = 2
    const val MIN_CENTI_POINTS = 1
    const val MAX_CENTI_POINTS = 99_999
    const val MAX_REASON_LENGTH = 50
    const val MAX_MEMO_LENGTH = 200

    private val POINTS_PATTERN = Regex("""\d{1,3}(\.\d{0,$DECIMALS})?|\.\d{1,$DECIMALS}""")

    fun summarize(records: List<ConductPoint>): ConductPointSummary = ConductPointSummary(
        merit = records.filter { it.kind == ConductPointKind.MERIT }.sumOf { it.centiPoints },
        demerit = records.filter { it.kind == ConductPointKind.DEMERIT }.sumOf { it.centiPoints },
    )

    /** [year]년 [month]월에 기록된 점수만 합산한다. */
    fun summarizeMonth(records: List<ConductPoint>, year: Int, month: Int): ConductPointSummary =
        summarize(records.filter { it.date.year == year && it.date.monthNumber == month })

    /** "0.25", "1.5", ".5", "3" 같은 입력을 0.01점 단위 정수로 바꾼다. 형식이 맞지 않으면 null. */
    fun parseCentiPoints(text: String): Int? {
        val t = text.trim()
        if (!POINTS_PATTERN.matches(t)) return null
        val whole = t.substringBefore('.').ifEmpty { "0" }.toInt()
        val fraction = t.substringAfter('.', "").padEnd(DECIMALS, '0').toInt()
        return whole * 100 + fraction
    }

    /** 0.01점 단위 정수를 사람이 읽는 점수로 바꾼다: 25 → "0.25", 150 → "1.5", 300 → "3". 부호는 붙이지 않는다. */
    fun format(centiPoints: Int): String {
        val a = abs(centiPoints)
        val whole = a / 100
        val fraction = (a % 100).toString().padStart(DECIMALS, '0').trimEnd('0')
        return if (fraction.isEmpty()) "$whole" else "$whole.$fraction"
    }

    /** 입력 문자열을 검증한다. 점수는 0.01~999.99(소수 둘째 자리까지), 사유는 공백 제거 후 비어 있지 않고 50자 이하. */
    fun validate(pointsText: String, reason: String, memo: String = ""): ConductPointInput {
        val trimmedReason = reason.trim()
        val centiPoints = parseCentiPoints(pointsText)
        val error = when {
            centiPoints == null || centiPoints !in MIN_CENTI_POINTS..MAX_CENTI_POINTS ->
                "점수는 0.01~999.99점(소수 둘째 자리까지)으로 입력해 주세요."
            trimmedReason.isEmpty() -> "사유를 입력해 주세요."
            trimmedReason.length > MAX_REASON_LENGTH -> "사유는 ${MAX_REASON_LENGTH}자 이하로 입력해 주세요."
            memo.trim().length > MAX_MEMO_LENGTH -> "메모는 ${MAX_MEMO_LENGTH}자 이하로 입력해 주세요."
            else -> null
        }
        return ConductPointInput(centiPoints, trimmedReason, error)
    }
}
