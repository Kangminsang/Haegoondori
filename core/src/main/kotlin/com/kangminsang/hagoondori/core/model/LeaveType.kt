package com.kangminsang.hagoondori.core.model

/**
 * 포상휴가처럼 상한(cap)에 도달했을 때, 초과분을 어떻게 처리할지 (스펙 4.4절).
 */
enum class OverflowBehavior {
    /** 초과분을 전투휴무로 전환한다 (해군 포상휴가 규정) */
    CONVERT_TO_COMBAT_REST,

    /** 초과분 부여 자체를 거부한다 */
    REJECT,

    /** 상한이 없거나, 있어도 초과분에 대해 아무 처리도 하지 않는다 */
    NONE,
}

/**
 * 휴가 종류 정의 (스펙 4.4절). 예: 연가, 포상휴가.
 */
data class LeaveType(
    val id: String,
    val name: String,
    /** 상한 일수. 포상휴가는 17. 없으면 무제한 */
    val cap: Int?,
    val overflowBehavior: OverflowBehavior = OverflowBehavior.NONE,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
        require(name.isNotBlank()) { "name은 비어 있을 수 없다" }
        cap?.let { require(it >= 0) { "cap은 음수일 수 없다: $it" } }
    }
}
