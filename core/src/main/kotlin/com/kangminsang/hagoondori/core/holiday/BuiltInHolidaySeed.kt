package com.kangminsang.hagoondori.core.holiday

import com.kangminsang.hagoondori.core.model.Holiday
import kotlinx.datetime.LocalDate

/**
 * 공휴일 확보 우선순위의 두 번째 단계: 공공데이터포털 API 키가 없거나 호출에
 * 실패했을 때 쓰는 내장 데이터(오프라인 대비, 스펙 4.14절).
 *
 * **의도적으로 날짜가 고정된(양력) 공휴일만 담는다.** 설날·추석·부처님오신날처럼
 * 음력 기준으로 매년 날짜가 바뀌는 공휴일은 정확한 음양력 변환표 없이 하드코딩하면
 * 틀릴 위험이 크므로 포함하지 않는다 - 그 공휴일들은 (권장) API 키를 등록하거나,
 * 설정 화면에서 수동으로 추가해야 한다(확보 우선순위의 세 번째 단계, 4.14절).
 */
object BuiltInHolidaySeed {

    private data class FixedHoliday(val month: Int, val day: Int, val name: String)

    private val FIXED_HOLIDAYS = listOf(
        FixedHoliday(1, 1, "신정"),
        FixedHoliday(3, 1, "삼일절"),
        FixedHoliday(5, 5, "어린이날"),
        FixedHoliday(6, 6, "현충일"),
        FixedHoliday(8, 15, "광복절"),
        FixedHoliday(10, 3, "개천절"),
        FixedHoliday(10, 9, "한글날"),
        FixedHoliday(12, 25, "성탄절"),
    )

    /** [fromYear]부터 [toYear]까지(포함) 고정 양력 공휴일을 생성한다. */
    fun generate(fromYear: Int, toYear: Int): List<Holiday> {
        require(toYear >= fromYear) { "toYear는 fromYear 이상이어야 한다: fromYear=$fromYear, toYear=$toYear" }
        return (fromYear..toYear).flatMap { year ->
            FIXED_HOLIDAYS.map { fixed -> Holiday(LocalDate(year, fixed.month, fixed.day), fixed.name, isSubstitute = false) }
        }
    }
}
