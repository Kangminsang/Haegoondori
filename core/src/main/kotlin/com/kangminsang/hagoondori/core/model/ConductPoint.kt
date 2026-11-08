package com.kangminsang.hagoondori.core.model

import com.kangminsang.hagoondori.core.calc.ConductPointCalculator
import kotlinx.datetime.LocalDate

/** 점수 종류: 양호(가점) / 과실(감점). */
enum class ConductPointKind {
    MERIT,
    DEMERIT,
}

/**
 * 초과근무로 받은 양호의 근거. [tenthHours]는 근무 시간을 0.1시간 단위 정수로 둔 값(1.5시간 = 15),
 * [onHoliday]가 true면 휴일 근무다. 점수는 [com.kangminsang.hagoondori.core.calc.ConductPointCalculator.overtimeCentiPoints]로 정해진다.
 */
data class Overtime(val tenthHours: Int, val onHoliday: Boolean) {
    init {
        require(tenthHours in 1..240) { "초과근무 시간은 0.1~24시간이어야 한다: $tenthHours" }
    }
}

/**
 * 양호·과실 점수 기록. 기록과 합계만 다루며 장치로 보내지 않는다.
 * 점수는 0.25처럼 소수일 수 있어, 합산 오차가 없도록 0.01점 단위 정수([centiPoints], 0.25점 = 25)로 둔다.
 * 항상 양수이고, 가점인지 감점인지는 [kind]가 정한다.
 * [overtime]이 있으면 초과근무로 받은 양호 기록이며, 점수는 근무 시간에서 계산한 값과 같아야 한다.
 */
data class ConductPoint(
    val id: String,
    val date: LocalDate,
    val kind: ConductPointKind,
    val centiPoints: Int,
    val reason: String,
    val memo: String? = null,
    val overtime: Overtime? = null,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
        require(centiPoints > 0) { "점수는 0보다 커야 한다" }
        if (overtime != null) {
            require(kind == ConductPointKind.MERIT) { "초과근무 기록은 양호여야 한다" }
            require(centiPoints == ConductPointCalculator.overtimeCentiPoints(overtime.tenthHours, overtime.onHoliday)) {
                "초과근무 점수가 근무 시간과 맞지 않는다: $centiPoints"
            }
        }
    }
}
