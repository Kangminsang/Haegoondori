package com.kangminsang.hagoondori.core.payload

import kotlinx.datetime.LocalDate

/**
 * 전송 범위 `[동기화시점-7일, 동기화시점+180일]`(스펙 7.4절) 밖의 레코드를 걸러내는
 * 공용 유틸. 기간을 갖는 레코드(일정, 휴가, 전투휴무 등)는 범위와 "겹치면" 포함하고,
 * 단일 날짜 레코드(공휴일, 근무 배정)는 범위 "안에" 있으면 포함한다.
 */
object DateRangeFilter {

    /** [itemStart, itemEnd]와 [rangeStart, rangeEnd]가 하루라도 겹치는지. */
    fun overlaps(itemStart: LocalDate, itemEnd: LocalDate, rangeStart: LocalDate, rangeEnd: LocalDate): Boolean =
        itemStart <= rangeEnd && itemEnd >= rangeStart

    /** 기간을 갖는 항목 중 [rangeStart]~[rangeEnd]와 겹치는 것만 남긴다. */
    fun <T> filterOverlapping(
        items: List<T>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        start: (T) -> LocalDate,
        end: (T) -> LocalDate,
    ): List<T> = items.filter { overlaps(start(it), end(it), rangeStart, rangeEnd) }

    /** 단일 날짜 항목 중 [rangeStart]~[rangeEnd] 안에 있는 것만 남긴다. */
    fun <T> filterWithin(
        items: List<T>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        date: (T) -> LocalDate,
    ): List<T> = items.filter { date(it) in rangeStart..rangeEnd }
}
