package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate

/**
 * 전투휴무 사용 기록 (스펙 4.8절). [endDate]는 포함(inclusive).
 *
 * 휴가와 달리 전투휴무는 영외로 나갈 수 없다는 점이 다르다 (3.3.1절) — 이 차이 때문에
 * [LeaveUsage]와 합치지 않고 별도 엔티티로 분리되어 있다.
 */
data class CombatRestUsage(
    val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val memo: String? = null,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
        require(endDate >= startDate) { "endDate는 startDate보다 앞설 수 없다" }
    }
}
