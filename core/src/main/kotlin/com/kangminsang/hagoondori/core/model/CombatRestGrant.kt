package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate

/**
 * 전투휴무 직접 부여 기록 (스펙 4.7절).
 *
 * 포상휴가 상한 초과에 따른 자동 전환분은 여기 기록하지 않는다 — 그 값은
 * [LeaveGrant]로부터 매번 계산된다 (3.3.2절). 여기 저장되는 것은 그와 무관하게
 * 지휘관이 직접 부여한 전투휴무뿐이다.
 */
data class CombatRestGrant(
    val id: String,
    val days: Int,
    val grantedDate: LocalDate,
    val reason: String? = null,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
        require(days > 0) { "days는 1 이상이어야 한다: $days" }
    }
}
