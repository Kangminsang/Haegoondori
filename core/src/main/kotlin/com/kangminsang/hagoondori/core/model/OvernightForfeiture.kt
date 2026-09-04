package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate

/**
 * 징계(외박제한)에 의한 외박 차수 소멸 기록 (스펙 4.10절).
 *
 * [slotIndex]는 [UserProfile.firstOvernightDate] 기준 0부터 시작하는 상대 차수 번호다.
 * **주의**: 첫 외박일을 수정하면 모든 [slotIndex]의 의미가 바뀐다 — 앱은 설정에서
 * 첫 외박일 변경 시 반드시 경고해야 한다. 입력 UI도 차수 번호를 직접 입력받지 말고
 * 예정일 목록에서 선택하게 해야 한다(스펙 4.10절 주의사항 그대로).
 */
data class OvernightForfeiture(
    val id: String,
    /** 소멸된 차수 번호 (0부터) */
    val slotIndex: Int,
    val reason: String? = null,
    /** 징계일 */
    val recordedDate: LocalDate? = null,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
        require(slotIndex >= 0) { "slotIndex는 0 이상이어야 한다: $slotIndex" }
    }
}
