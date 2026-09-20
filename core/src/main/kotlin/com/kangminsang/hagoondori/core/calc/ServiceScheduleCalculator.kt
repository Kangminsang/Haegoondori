package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.Promotion
import com.kangminsang.hagoondori.core.model.PromotionDates
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * 입대일로부터 진급일·전역일을 계산하고, 진급 진행 상황을 판단한다.
 *
 * 규칙은 해군 병 복무 기준이다. 전역일은 입대일 + 복무 개월 수 - 1일이고, 진급일은 입대한 달을 기준으로
 * 일병 = 3개월 뒤, 상병 = 9개월 뒤, 병장 = 15개월 뒤의 **1일**이다(입대한 달이 이병 1개월째이므로
 * 이병 3개월, 일병·상병 각 6개월).
 * 실제 진급일은 개인 사정으로 달라질 수 있으므로 계산값은 기본값일 뿐이며 사용자가 고칠 수 있다.
 */
object ServiceScheduleCalculator {
    /** 해군 병 복무 기간(개월). */
    const val NAVY_SERVICE_MONTHS = 20

    private val MONTHS_UNTIL_PROMOTION = mapOf(
        Promotion.PRIVATE_FIRST_CLASS to 3,
        Promotion.CORPORAL to 9,
        Promotion.SERGEANT to 15,
    )

    data class Schedule(val dischargeDate: LocalDate, val promotionDates: PromotionDates)

    fun calculate(enlistmentDate: LocalDate, serviceMonths: Int = NAVY_SERVICE_MONTHS): Schedule {
        val firstOfEnlistmentMonth = LocalDate(enlistmentDate.year, enlistmentDate.monthNumber, 1)
        fun promotionAt(rank: Promotion) =
            firstOfEnlistmentMonth.plus(MONTHS_UNTIL_PROMOTION.getValue(rank), DateTimeUnit.MONTH)
        return Schedule(
            dischargeDate = enlistmentDate.plus(serviceMonths, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY),
            promotionDates = PromotionDates(
                privateFirstClass = promotionAt(Promotion.PRIVATE_FIRST_CLASS),
                corporal = promotionAt(Promotion.CORPORAL),
                sergeant = promotionAt(Promotion.SERGEANT),
            ),
        )
    }

    /** [today] 이후(당일 포함) 가장 가까운 진급. 남은 진급이 없으면 null. */
    fun nextPromotion(promotionDates: PromotionDates, today: LocalDate): Pair<Promotion, LocalDate>? =
        promotionDates.entries.filter { it.second >= today }.minByOrNull { it.second }

    /**
     * 다음 진급까지의 진행률(0~100). 직전 진급일(없으면 [enlistmentDate])부터 [nextDate]까지 중 [today]가
     * 어디쯤인지를 나타낸다. 새 계급이 된 뒤 얼마나 왔는지를 보여주는 값이다.
     */
    fun progressToNextPromotion(
        enlistmentDate: LocalDate,
        promotionDates: PromotionDates,
        nextDate: LocalDate,
        today: LocalDate,
    ): Double {
        val segmentStart = promotionDates.entries.map { it.second }.filter { it < nextDate }.maxOrNull() ?: enlistmentDate
        return DDayCalculator.serviceProgress(segmentStart, nextDate, today)
    }
}
