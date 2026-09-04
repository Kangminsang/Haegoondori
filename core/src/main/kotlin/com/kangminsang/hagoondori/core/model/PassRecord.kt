package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate

/** 외출 종류: 평일 / 휴일 (주말·공휴일). 월별 할당량이 서로 독립적이다 (3.5.2절). */
enum class PassType {
    WEEKDAY,
    HOLIDAY,
}

/**
 * 외출 사용 기록 (스펙 4.11절).
 *
 * [type]을 기록 시점에 고정해서 저장한다 — 공휴일 데이터가 나중에 갱신되어도
 * 과거 기록의 종류가 바뀌어버리면 안 되기 때문이다. 입력 시
 * [com.kangminsang.hagoondori.core.calc.PassCalculator.classifyType]으로 자동 판정해
 * 기본값을 채우되, 사용자가 수정할 수 있게 한다.
 */
data class PassRecord(
    val id: String,
    /** 외출 날짜 */
    val date: LocalDate,
    val type: PassType,
    val memo: String? = null,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
    }
}
