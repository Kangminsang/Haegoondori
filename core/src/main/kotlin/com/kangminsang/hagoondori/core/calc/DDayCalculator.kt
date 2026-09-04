package com.kangminsang.hagoondori.core.calc

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * D-day / 복무 진행률 계산 (스펙 4.3절 "계산 예시"). 이 값들은 앱 화면(대시보드, F2)
 * 표시 전용이며, 저장하지도 장치로 전송하지도 않는다 - 장치는 RTC로 오늘 날짜를
 * 알기 때문에 매일 스스로 다시 계산한다(7.1절 계산 주체 분담).
 */
object DDayCalculator {

    /** [target] − [from] 일수. target이 미래면 양수, 과거면 음수. */
    fun dDay(from: LocalDate, target: LocalDate): Int = from.daysUntil(target)

    /**
     * 복무 진행률(%) = (오늘 − 입대일) / (전역일 − 입대일) × 100.
     * 입대 전이거나 전역 이후를 보더라도 게이지가 깨지지 않도록 0~100 범위로 clamp한다.
     */
    fun serviceProgress(enlistmentDate: LocalDate, dischargeDate: LocalDate, today: LocalDate): Double =
        progressPercent(enlistmentDate, dischargeDate, today)

    /**
     * 진급 시점의 진행률(%). 장치 게이지 바 위 세로 마커선의 가로 위치를 결정한다(4.3절).
     */
    fun promotionProgress(enlistmentDate: LocalDate, dischargeDate: LocalDate, promotionDate: LocalDate): Double =
        progressPercent(enlistmentDate, dischargeDate, promotionDate)

    private fun progressPercent(enlistmentDate: LocalDate, dischargeDate: LocalDate, at: LocalDate): Double {
        val total = enlistmentDate.daysUntil(dischargeDate)
        if (total <= 0) return 0.0
        val elapsed = enlistmentDate.daysUntil(at)
        return (elapsed.toDouble() / total.toDouble() * 100.0).coerceIn(0.0, 100.0)
    }
}
