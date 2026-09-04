package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate

/**
 * 휴가 사용 기록 (스펙 4.6절).
 *
 * [endDate]는 포함(inclusive)이다. 차감 일수 필드는 따로 없다 — 평일·주말·공휴일을
 * 가리지 않고 `endDate - startDate + 1`로 계산된다 (3.2.1절). 완전히 계산 가능한
 * 파생값이므로 저장하지 않는다.
 */
data class LeaveUsage(
    val id: String,
    val leaveTypeId: String,
    val startDate: LocalDate,
    /** 종료일. 포함(inclusive) */
    val endDate: LocalDate,
    /** 표시용 이름 */
    val label: String? = null,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
        require(leaveTypeId.isNotBlank()) { "leaveTypeId는 비어 있을 수 없다" }
        require(endDate >= startDate) { "endDate는 startDate보다 앞설 수 없다" }
    }
}
