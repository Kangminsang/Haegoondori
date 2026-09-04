package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate

/**
 * 외박 사용 기록 (스펙 4.9절).
 *
 * 예정일도 차수 번호도 저장하지 않는다 — 둘 다 [UserProfile.firstOvernightDate]와
 * 전체 기록으로부터 [com.kangminsang.hagoondori.core.calc.OvernightScheduleCalculator]가 계산한다.
 */
data class OvernightRecord(
    val id: String,
    /** 외박 나간 날짜 */
    val date: LocalDate,
    val memo: String? = null,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
    }
}
