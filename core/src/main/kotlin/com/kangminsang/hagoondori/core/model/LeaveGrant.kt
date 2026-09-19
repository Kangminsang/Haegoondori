package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate

/**
 * 휴가 부여 기록 (스펙 4.5절).
 *
 * [LeaveUsage](사용 기록)와 반드시 분리되어 있다 — 포상휴가 17일 상한은
 * "부여 누적" 기준으로 판정되므로, 사용량만 세면 상한 도달 여부를 알 수 없다 (3.2.2절).
 */
data class LeaveGrant(
    val id: String,
    val leaveTypeId: String,
    /** 부여 일수 */
    val days: Int,
    /** 부여받은 날짜 */
    val grantedDate: LocalDate,
    val reason: String? = null,
    /** 유효 기간 마지막 날(포함). 이 날까지 쓰지 못한 일수는 소멸한다. 없으면 기한 없음. */
    val expiryDate: LocalDate? = null,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
        require(leaveTypeId.isNotBlank()) { "leaveTypeId는 비어 있을 수 없다" }
        require(days > 0) { "days는 1 이상이어야 한다: $days" }
        expiryDate?.let { require(it >= grantedDate) { "expiryDate는 grantedDate보다 앞설 수 없다" } }
    }
}
