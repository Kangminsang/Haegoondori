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
    /**
     * 외박이 이어진 마지막 날(포함). 휴가와 이어 붙여 쓰는 경우 여러 날이 된다.
     * 6주 차수 매칭은 시작일 [date]만 쓰므로 이 값은 차수 계산에 영향을 주지 않는다.
     */
    val endDate: LocalDate = date,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
        require(endDate >= date) { "endDate는 date보다 앞설 수 없다" }
    }
}
