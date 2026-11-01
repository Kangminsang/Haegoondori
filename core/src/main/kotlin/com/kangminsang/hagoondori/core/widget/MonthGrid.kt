package com.kangminsang.hagoondori.core.widget

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * 홈 화면 달력 위젯의 월 격자 계산. 일요일이 첫 열이고(앱 달력 [com.kangminsang.hagoondori.ui.calendar.CalendarGridBuilder]와 같은 규칙),
 * 그 달의 날짜가 격자의 몇 행 몇 열에 놓이는지만 본다. 격자 밖(앞뒤 달)은 위젯이 비워 둔다.
 */
object WidgetMonthGrid {

    const val COLUMNS = 7

    /** 1일 앞에 놓이는 빈 칸 수(일요일=0, 토요일=6). */
    fun leadingBlanks(year: Int, month: Int): Int = LocalDate(year, month, 1).dayOfWeek.value % COLUMNS

    fun daysInMonth(year: Int, month: Int): Int {
        val firstOfMonth = LocalDate(year, month, 1)
        val nextMonthFirst = firstOfMonth.plus(1, DateTimeUnit.MONTH)
        return nextMonthFirst.minus(1, DateTimeUnit.DAY).dayOfMonth
    }

    /** 필요한 주 수(행 수). 4주(2월 1일이 일요일인 평년)부터 6주까지 나온다. */
    fun weekCount(year: Int, month: Int): Int =
        (leadingBlanks(year, month) + daysInMonth(year, month) + COLUMNS - 1) / COLUMNS

    /** [day]일이 놓이는 행(0부터). */
    fun rowOf(year: Int, month: Int, day: Int): Int = (leadingBlanks(year, month) + day - 1) / COLUMNS

    /** [day]일이 놓이는 열(0 = 일요일). */
    fun columnOf(year: Int, month: Int, day: Int): Int = (leadingBlanks(year, month) + day - 1) % COLUMNS
}
