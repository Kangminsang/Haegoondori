package com.kangminsang.hagoondori.core.export

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * 전송 범위 계산 (calendar.txt 전송 명세 v5, 7절).
 *
 * - 시작: 동기화 날짜가 속한 달의 1일. 장치는 오늘이 속한 달의 달력을 그리므로 그 달의
 *   지난 날짜의 칩·점선·라벨이 범위에 들어 있어야 한다.
 * - 끝: (동기화 날짜 + 180일)이 속한 달의 말일. 동기화 없이 180일이 지난 날의 달력도
 *   그 달 끝까지 완전하게 나오게 하기 위해서다.
 */
object TransmissionRange {
    const val FUTURE_DAYS = 180

    fun start(today: LocalDate): LocalDate = LocalDate(today.year, today.monthNumber, 1)

    fun end(today: LocalDate): LocalDate {
        val target = today.plus(FUTURE_DAYS, DateTimeUnit.DAY)
        val firstOfNextMonth = LocalDate(target.year, target.monthNumber, 1).plus(1, DateTimeUnit.MONTH)
        return firstOfNextMonth.minus(1, DateTimeUnit.DAY)
    }
}
