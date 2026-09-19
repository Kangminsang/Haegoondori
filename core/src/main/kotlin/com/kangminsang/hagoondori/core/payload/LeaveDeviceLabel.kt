package com.kangminsang.hagoondori.core.payload

/**
 * 장치 달력의 휴가 기간(`L` 레코드) 라벨. 장치가 어떤 휴가인지 표시하므로 휴가 종류 이름을
 * 두 글자로 줄여 보낸다: 포상휴가 → 포상, 위로휴가 → 위로. 정기휴가는 군에서 부르는 이름인
 * "연가"로 보낸다.
 */
object LeaveDeviceLabel {
    private val OVERRIDES = mapOf("정기휴가" to "연가")

    fun forTypeName(typeName: String): String = OVERRIDES[typeName] ?: typeName.trim().take(2)
}
