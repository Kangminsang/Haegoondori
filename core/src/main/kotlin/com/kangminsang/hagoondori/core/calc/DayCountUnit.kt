package com.kangminsang.hagoondori.core.calc

/**
 * 대시보드의 "남은 일수" 숫자를 누를 때마다 바뀌는 표시 단위. 일 → 시간 → 분 → 일 순으로 돈다.
 * 값 자체는 바뀌지 않고 보여주는 방식만 바뀌므로 저장하지 않는다(화면 상태로만 둔다).
 */
enum class DayCountUnit {
    DAY,
    HOUR,
    MINUTE,
    ;

    /** 한 번 눌렀을 때 넘어갈 다음 단위. */
    fun next(): DayCountUnit = entries[(ordinal + 1) % entries.size]
}

/**
 * [DayCountUnit]에 맞춰 일수·D-day를 글자로 만든다. 큰 수는 `5,376시간`처럼 세 자리마다 쉼표를 넣는다.
 *
 * 두 가지 값을 구분한다.
 * - **양(量)**: 휴가 잔여, 출타 기간처럼 "며칠어치"인 값. 하루를 그대로 24시간·1440분으로 바꾼다.
 * - **D-day**: 전역일·진급일처럼 특정 날짜까지 남은 값. 시간·분으로 볼 때는 일수×24가 아니라 그 날짜
 *   0시까지 실제로 남은 시간([DDayCalculator.remainingMillis]와 같은 기준)을 쓴다. 그래야 오늘 이미 지난
 *   시간만큼 줄어든, 정확한 값이 나온다.
 */
object DayCountFormatter {
    const val HOURS_PER_DAY = 24L
    const val MINUTES_PER_DAY = 24L * 60L
    private const val MILLIS_PER_HOUR = 3_600_000L
    private const val MILLIS_PER_MINUTE = 60_000L

    /** 일수 [days]를 단위에 맞춰 바꾼 값(소수점 없음). */
    fun convertDays(days: Long, unit: DayCountUnit): Long = when (unit) {
        DayCountUnit.DAY -> days
        DayCountUnit.HOUR -> days * HOURS_PER_DAY
        DayCountUnit.MINUTE -> days * MINUTES_PER_DAY
    }

    /** 휴가 잔여 같은 일수를 `12일` / `288시간` / `17,280분`으로. */
    fun formatAmount(days: Int, unit: DayCountUnit): String =
        groupThousands(convertDays(days.toLong(), unit)) + suffix(unit)

    /**
     * D-day 표기. [DayCountUnit.DAY]면 `D-224`·`D+3`, 아니면 [millisUntilTarget](대상 날짜 0시 − 지금, 지났으면
     * 음수)을 내림한 `5,375시간`·`322,553분`이다. 대상 날짜가 이미 지났으면 `72시간 지남`처럼 쓴다.
     * D-day가 0(오늘이 그날)이면 0시가 막 지났더라도 `0시간`으로 보여준다 - 날짜로는 아직 "그날"이기 때문이다.
     */
    fun formatDDay(dDay: Int, millisUntilTarget: Long, unit: DayCountUnit): String {
        if (unit == DayCountUnit.DAY) return if (dDay >= 0) "D-$dDay" else "D+${-dDay}"
        if (dDay == 0) return "0${suffix(unit)}"
        val perUnit = if (unit == DayCountUnit.HOUR) MILLIS_PER_HOUR else MILLIS_PER_MINUTE
        val magnitude = kotlin.math.abs(millisUntilTarget) / perUnit
        val text = groupThousands(magnitude) + suffix(unit)
        return if (dDay > 0) text else "$text 지남"
    }

    /** `5376` → `5,376`. 음수도 부호를 유지한다. 로케일에 따라 구분자가 바뀌지 않도록 직접 넣는다. */
    fun groupThousands(value: Long): String {
        val digits = kotlin.math.abs(value).toString()
        val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
        return if (value < 0) "-$grouped" else grouped
    }

    private fun suffix(unit: DayCountUnit): String = when (unit) {
        DayCountUnit.DAY -> "일"
        DayCountUnit.HOUR -> "시간"
        DayCountUnit.MINUTE -> "분"
    }
}
