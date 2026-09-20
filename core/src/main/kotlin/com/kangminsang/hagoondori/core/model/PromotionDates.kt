package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.LocalDate

/** 병 계급 진급. 이병으로 입대해 일병 → 상병 → 병장 순으로 진급한다. */
enum class Promotion(val label: String) {
    PRIVATE_FIRST_CLASS("일병"),
    CORPORAL("상병"),
    SERGEANT("병장"),
}

/**
 * 계급별 진급일. 이미 지난 진급일도 그대로 보관한다(진행 막대에 지난 진급도 표시하기 위해). 입력하지
 * 않은 계급은 null이다 - 예를 들어 진급일을 아직 모르면 비워 둘 수 있다.
 */
data class PromotionDates(
    val privateFirstClass: LocalDate? = null,
    val corporal: LocalDate? = null,
    val sergeant: LocalDate? = null,
) {
    operator fun get(promotion: Promotion): LocalDate? = when (promotion) {
        Promotion.PRIVATE_FIRST_CLASS -> privateFirstClass
        Promotion.CORPORAL -> corporal
        Promotion.SERGEANT -> sergeant
    }

    /** 입력된 진급일만 계급순으로. */
    val entries: List<Pair<Promotion, LocalDate>>
        get() = Promotion.entries.mapNotNull { rank -> get(rank)?.let { rank to it } }
}
