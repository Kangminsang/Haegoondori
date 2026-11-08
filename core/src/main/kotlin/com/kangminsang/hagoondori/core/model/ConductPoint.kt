package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate

/** 점수 종류: 양호(가점) / 과실(감점). */
enum class ConductPointKind {
    MERIT,
    DEMERIT,
}

/**
 * 양호·과실 점수 기록. 기록과 합계만 다루며 장치로 보내지 않는다.
 * [points]는 항상 양수이고, 가점인지 감점인지는 [kind]가 정한다.
 */
data class ConductPoint(
    val id: String,
    val date: LocalDate,
    val kind: ConductPointKind,
    val points: Int,
    val reason: String,
    val memo: String? = null,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
        require(points > 0) { "점수는 1 이상이어야 한다" }
    }
}
