package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate

/** 근무 배정 종류: 당직 / 비번 / 츄라이(식사 당번). */
enum class DutyType {
    DUTY,
    OFF_DUTY,
    MESS,
}

/**
 * 근무 배정 (스펙 4.13절).
 *
 * 하루에 여러 종류가 겹칠 수 있다 — `(date, type)` 조합이 유일 키다(app 레이어의
 * Room 엔티티가 이 유니크 제약을 강제한다). 당직표는 한 달치가 한 번에 나오므로
 * 다중 선택 일괄 입력 UI가 필수다(F6).
 */
data class DutyAssignment(
    val id: String,
    val date: LocalDate,
    val type: DutyType,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
    }
}
