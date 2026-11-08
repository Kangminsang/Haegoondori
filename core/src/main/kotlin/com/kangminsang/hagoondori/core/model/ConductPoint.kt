package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate

/** 점수 종류: 양호(가점) / 과실(감점). */
enum class ConductPointKind {
    MERIT,
    DEMERIT,
}

/**
 * 양호·과실 점수 기록. 기록과 합계만 다루며 장치로 보내지 않는다.
 * 점수는 0.25처럼 소수일 수 있어, 합산 오차가 없도록 0.01점 단위 정수([centiPoints], 0.25점 = 25)로 둔다.
 * 항상 양수이고, 가점인지 감점인지는 [kind]가 정한다.
 */
data class ConductPoint(
    val id: String,
    val date: LocalDate,
    val kind: ConductPointKind,
    val centiPoints: Int,
    val reason: String,
    val memo: String? = null,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
        require(centiPoints > 0) { "점수는 0보다 커야 한다" }
    }
}
