package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.Holiday
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * 휴일 판정 (스펙 4.16절).
 *
 * 토·일요일은 저장하지 않는다 — [isHoliday]가 `주말이거나 공휴일 목록에 포함`으로
 * 매번 판정한다. **달력 표시**와 **외출 종류 판정**(3.5절) 두 곳에서 공통 사용되지만,
 * **휴가 차감에는 사용되지 않는다** — 휴가는 평일/주말/공휴일을 가리지 않고 균일하게
 * 차감되기 때문이다(3.2.1절, [LeaveCalculator] 참고).
 */
object HolidayJudge {
    fun isHoliday(date: LocalDate, holidays: List<Holiday>): Boolean =
        isWeekend(date) || holidays.any { it.date == date }

    fun isWeekend(date: LocalDate): Boolean =
        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
}
