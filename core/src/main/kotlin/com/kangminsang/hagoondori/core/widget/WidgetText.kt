package com.kangminsang.hagoondori.core.widget

import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

/**
 * 홈 화면 위젯에 찍히는 글자와 숫자 규칙. 안드로이드 없이 순수 함수로 두어 core 테스트로 검증한다.
 * 날짜 표기(MM-dd)와 D-day 표기는 대시보드 카드와 같은 모양이어야 한다.
 */
object WidgetText {

    /** D-day 표기. 0 이상이면 `D-n`(0은 `D-0`), 음수면 `D+|n|`. */
    fun formatDDay(dDay: Int): String = if (dDay >= 0) "D-$dDay" else "D+${-dDay}"

    /** 위젯 제목. 예: `전역 D-231`. */
    fun dischargeTitle(dDay: Int): String = "전역 ${formatDDay(dDay)}"

    /** 월-일을 두 자리로 쓴다. 예: `12-16`, `01-05`. */
    fun monthDay(date: LocalDate): String = "${pad2(date.monthNumber)}-${pad2(date.dayOfMonth)}"

    /** 다음 진급. 예: `상병 12-16 (D-45)`. */
    fun promotionLine(promotion: WidgetPromotion): String =
        "${promotion.label} ${monthDay(promotion.date)} (${formatDDay(promotion.dDay)})"

    /**
     * 다음 출타. 시작일이 아직 안 왔으면(dDay > 0) `외박 11-15 (D-14)`, 시작일이 오늘이거나
     * 이미 지났으면 `외박 진행 중`이다.
     */
    fun outingLine(outing: WidgetOuting): String =
        if (outing.dDay > 0) {
            "${outing.label} ${monthDay(outing.start)} (${formatDDay(outing.dDay)})"
        } else {
            "${outing.label} 진행 중"
        }

    /** 다음 외출일. 예: `11-08 (D-7)`. */
    fun passLine(pass: WidgetPass): String = "${monthDay(pass.date)} (${formatDDay(pass.dDay)})"

    /**
     * 휴가 잔여 한 줄. 예: `포상 3 · 정기 17 · 위로 2`. 표시할 종류가 없으면 null이라 줄을 숨긴다.
     */
    fun leaveRemainingLine(items: List<WidgetLeaveRemaining>): String? =
        items.takeIf { it.isNotEmpty() }?.joinToString(" · ") { "${it.shortName} ${it.remaining}" }

    /** 진행률을 0~100 정수로 바꾼다. NaN은 0으로 본다. */
    fun progressPercent(percent: Double): Int =
        if (percent.isNaN()) 0 else percent.coerceIn(0.0, 100.0).roundToInt()

    /** 진행률 글자. 예: `62%`. */
    fun progressText(percent: Double): String = "${progressPercent(percent)}%"

    private fun pad2(value: Int): String = value.toString().padStart(2, '0')
}
