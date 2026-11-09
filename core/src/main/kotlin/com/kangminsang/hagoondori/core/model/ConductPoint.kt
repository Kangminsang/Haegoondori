package com.kangminsang.hagoondori.core.model

import com.kangminsang.hagoondori.core.calc.ConductPointCalculator
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** 점수 종류: 양호(가점) / 과실(감점). */
enum class ConductPointKind {
    MERIT,
    DEMERIT,
}

/**
 * 초과근무로 받은 양호의 근거. [tenthHours]는 근무 시간을 0.1시간 단위 정수로 둔 값(1.5시간 = 15),
 * [onHoliday]가 true면 휴일 근무다. 점수는 [com.kangminsang.hagoondori.core.calc.ConductPointCalculator.overtimeCentiPoints]로 정해진다.
 * [startTime]·[endTime]은 시작·끝 시각이며 함께 있거나 함께 없다(시각 입력 이전의 옛 기록은 없음).
 * 있으면 [tenthHours]는 두 시각에서 계산한 값과 같아야 한다.
 */
data class Overtime(
    val tenthHours: Int,
    val onHoliday: Boolean,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
) {
    init {
        require(tenthHours in 1..240) { "초과근무 시간은 0.1~24시간이어야 한다: $tenthHours" }
        require((startTime == null) == (endTime == null)) { "시작·끝 시각은 함께 있어야 한다" }
        if (startTime != null && endTime != null) {
            require(tenthHours == ConductPointCalculator.overtimeTenthHours(startTime, endTime)) {
                "초과근무 시간이 시각과 맞지 않는다: $tenthHours"
            }
        }
    }
}

/**
 * 양호·과실 점수 기록. 기록과 합계만 다루며 장치로 보내지 않는다.
 * 점수는 0.25처럼 소수일 수 있어, 합산 오차가 없도록 0.01점 단위 정수([centiPoints], 0.25점 = 25)로 둔다.
 * 항상 양수이고, 가점인지 감점인지는 [kind]가 정한다.
 * [overtime]이 있으면 초과근무로 받은 양호 기록이며, 점수는 근무 시간에서 계산한 값과 같아야 한다.
 */
data class ConductPoint(
    val id: String,
    val date: LocalDate,
    val kind: ConductPointKind,
    val centiPoints: Int,
    val reason: String,
    val memo: String? = null,
    val overtime: Overtime? = null,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
        require(centiPoints > 0) { "점수는 0보다 커야 한다" }
        if (overtime != null) {
            require(kind == ConductPointKind.MERIT) { "초과근무 기록은 양호여야 한다" }
            require(centiPoints == ConductPointCalculator.overtimeCentiPoints(overtime.tenthHours, overtime.onHoliday)) {
                "초과근무 점수가 근무 시간과 맞지 않는다: $centiPoints"
            }
        }
    }
}

/** 위원회 결과의 종류: 포상휴가(양호 전환, [ConductConversionRecord.count] = 일수) / 외박제한(과실 전환, count = 횟수). */
enum class ConductConversionType {
    REWARD_LEAVE,
    OVERNIGHT_RESTRICTION,
}

/**
 * 군기지도위원회 결과 기록. 전환으로 차감되는 점수를 세는 근거일 뿐이며, 휴가 부여나 외박 차수 소멸은 만들지 않는다
 * (사용자가 휴가·외박 탭에서 직접 기록한다). 장치로 보내지 않는다.
 */
data class ConductConversionRecord(
    val id: String,
    val meetingDate: LocalDate,
    val type: ConductConversionType,
    val count: Int,
    val memo: String? = null,
) {
    init {
        require(id.isNotBlank()) { "id는 비어 있을 수 없다" }
        require(count > 0) { "일수·횟수는 0보다 커야 한다" }
    }
}
