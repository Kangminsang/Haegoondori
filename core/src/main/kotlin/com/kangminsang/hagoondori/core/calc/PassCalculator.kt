package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.PassType
import com.kangminsang.hagoondori.core.model.UserProfile
import kotlinx.datetime.LocalDate

/**
 * 정기 할당형 자원(외출) 계산 (스펙 3.5절).
 *
 * 평일 외출 월 2회 / 휴일 외출 월 1회이며, 매월 리셋되고 미사용분은 이월되지 않는다.
 * 이월이 없으므로 해당 월의 기록 개수만 세면 되고, 잔여를 저장하지 않는다.
 * 평일과 휴일은 별개 할당량이다 — 합쳐서 "월 3회"로 관리하지 않는다.
 */
object PassCalculator {

    /** [type]에 해당하는 월 할당 횟수. */
    fun allowance(profile: UserProfile, type: PassType): Int = when (type) {
        PassType.WEEKDAY -> profile.weekdayPassPerMonth
        PassType.HOLIDAY -> profile.holidayPassPerMonth
    }

    /** [year]/[month](1~12)에 [type]으로 사용된 기록 개수. */
    fun usedInMonth(records: List<PassRecord>, year: Int, month: Int, type: PassType): Int =
        records.count { it.type == type && it.date.year == year && it.date.monthNumber == month }

    /** 해당 월 잔여 횟수 = 할당량 - 사용량. 이월이 없으므로 이전 달과 무관하다. */
    fun remainingInMonth(profile: UserProfile, records: List<PassRecord>, year: Int, month: Int, type: PassType): Int =
        allowance(profile, type) - usedInMonth(records, year, month, type)

    /**
     * 기록 시점 기준으로 평일/휴일을 자동 판정한다. 입력 UI에서 기본값을 채우는 데 쓰고,
     * 이후 사용자가 수정하면 [PassRecord.type]에 그 값이 그대로 고정 저장된다
     * (나중에 공휴일 데이터가 갱신돼도 과거 기록은 바뀌지 않는다, 4.11절).
     */
    fun classifyType(date: LocalDate, holidays: List<Holiday>): PassType =
        if (HolidayJudge.isHoliday(date, holidays)) PassType.HOLIDAY else PassType.WEEKDAY
}
