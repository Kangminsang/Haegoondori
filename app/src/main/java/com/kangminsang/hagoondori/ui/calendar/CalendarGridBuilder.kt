package com.kangminsang.hagoondori.ui.calendar

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * 월간 달력 화면(F3)이 그릴 날짜 격자를 계산한다. 일요일을 한 주의 시작으로 삼고,
 * 앞뒤로 이전/다음 달 날짜를 채워 항상 7의 배수(온전한 주 단위) 길이가 되게 한다.
 *
 * 이 격자 계산은 앱 자신의 달력 화면 전용이다 - 장치의 달력 격자 배치는 장치가
 * RTC로 스스로 계산한다(7.1절)는 것과는 별개다.
 */
object CalendarGridBuilder {
    fun buildGrid(year: Int, month: Int): List<LocalDate> {
        val firstOfMonth = LocalDate(year, month, 1)
        val nextMonthFirst = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
        val lastOfMonth = nextMonthFirst.minus(1, DateTimeUnit.DAY)

        val leadingCount = sundayFirstColumn(firstOfMonth)
        val trailingCount = 6 - sundayFirstColumn(lastOfMonth)

        val start = firstOfMonth.minus(leadingCount, DateTimeUnit.DAY)
        val end = lastOfMonth.plus(trailingCount, DateTimeUnit.DAY)

        val result = mutableListOf<LocalDate>()
        var cursor = start
        while (cursor <= end) {
            result += cursor
            cursor = cursor.plus(1, DateTimeUnit.DAY)
        }
        return result
    }

    /** 일요일=0, 월요일=1, ..., 토요일=6. */
    private fun sundayFirstColumn(date: LocalDate): Int = date.dayOfWeek.value % 7
}
