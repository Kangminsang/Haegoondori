package com.kangminsang.hagoondori.core.calc

/**
 * 대시보드의 "보낸 시간 / 남은 시간"처럼 초 단위로 흐르는 시간을 어떤 단위로 크게 보여줄지.
 * 누를 때마다 일 → 시간 → 분 → 초 → 일 순으로 돈다. 보여주는 방식만 바뀌므로 저장하지 않는다(화면 상태).
 */
enum class LiveDurationUnit {
    DAY,
    HOUR,
    MINUTE,
    SECOND,
    ;

    /** 한 번 눌렀을 때 넘어갈 다음 단위. */
    fun next(): LiveDurationUnit = entries[(ordinal + 1) % entries.size]
}

/**
 * 밀리초를 [LiveDurationUnit]에 맞춰 글자로 만든다. 어느 단위든 초까지 보여줘서 1초마다 바뀌고,
 * 가장 큰 자리는 세 자리마다 쉼표를 넣는다.
 *
 * - 일: `224일 22:53:26`
 * - 시간: `5,398시간 53:26`
 * - 분: `323,933분 26초`
 * - 초: `19,436,006초`
 *
 * 둘이 나란히 놓여도 한 줄에 들어가도록 시간·분 아래 자리는 짧게 쓴다. 음수(이미 지난 시각)는 0으로 본다.
 */
object LiveDurationFormatter {

    fun format(millis: Long, unit: LiveDurationUnit): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val seconds = totalSeconds % 60
        val minutes = totalSeconds % 3_600 / 60
        val hours = totalSeconds % 86_400 / 3_600
        return when (unit) {
            LiveDurationUnit.DAY -> "${groupThousands(totalSeconds / 86_400)}일 ${two(hours)}:${two(minutes)}:${two(seconds)}"
            LiveDurationUnit.HOUR -> "${groupThousands(totalSeconds / 3_600)}시간 ${two(minutes)}:${two(seconds)}"
            LiveDurationUnit.MINUTE -> "${groupThousands(totalSeconds / 60)}분 ${two(seconds)}초"
            LiveDurationUnit.SECOND -> "${groupThousands(totalSeconds)}초"
        }
    }

    /** `5376` → `5,376`. 음수도 부호를 유지한다. 로케일에 따라 구분자가 바뀌지 않도록 직접 넣는다. */
    fun groupThousands(value: Long): String {
        val digits = kotlin.math.abs(value).toString()
        val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
        return if (value < 0) "-$grouped" else grouped
    }

    private fun two(value: Long): String = value.toString().padStart(2, '0')
}
