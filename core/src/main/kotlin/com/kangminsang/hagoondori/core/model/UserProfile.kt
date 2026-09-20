package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * 복무 기준 정보 및 규정 설정. 앱 전체에서 단 1개만 존재한다 (스펙 4.3절).
 *
 * D-day, 복무 진행률 등은 여기서 파생되지만 저장하지 않는다 — 그때그때 계산한다.
 * (원칙 1: 원본만 저장, 파생값은 저장하지 않는다.)
 */
data class UserProfile(
    /** 입대일 */
    val enlistmentDate: LocalDate,
    /** 전역일 */
    val dischargeDate: LocalDate,
    /** 일병·상병·병장 진급일. 입력하지 않은 계급은 비어 있다 */
    val promotionDates: PromotionDates = PromotionDates(),
    /** 첫 외박일. 6주 격자의 기산점. 아직 외박을 나가지 않았다면 없을 수 있다 */
    val firstOvernightDate: LocalDate?,
    /** 외박 주기(주). 기본 6 */
    val overnightCycleWeeks: Int = 6,
    /** 월 평일 외출 허용 횟수. 기본 2 */
    val weekdayPassPerMonth: Int = 2,
    /** 월 휴일 외출 허용 횟수. 기본 1 */
    val holidayPassPerMonth: Int = 1,
    /** 총기상 시각. 장치 낮 테마 전환 기준(총기상 15분 전) */
    val wakeUpTime: LocalTime,
    /** 저녁 식사 시각. 장치 밤 테마 전환 기준 */
    val dinnerTime: LocalTime,
    /** 당직 입력 시 다음 날 비번을 자동으로 추가할지 여부 */
    val autoAddOffDuty: Boolean,
) {
    init {
        require(overnightCycleWeeks > 0) { "overnightCycleWeeks는 1 이상이어야 한다: $overnightCycleWeeks" }
        require(weekdayPassPerMonth >= 0) { "weekdayPassPerMonth는 음수일 수 없다: $weekdayPassPerMonth" }
        require(holidayPassPerMonth >= 0) { "holidayPassPerMonth는 음수일 수 없다: $holidayPassPerMonth" }
        require(dischargeDate >= enlistmentDate) { "전역일은 입대일보다 앞설 수 없다" }
    }
}
