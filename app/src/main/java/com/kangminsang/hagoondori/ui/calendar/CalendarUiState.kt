package com.kangminsang.hagoondori.ui.calendar

import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.core.model.PassRecord
import kotlinx.datetime.LocalDate

/** 격자 위 날짜 하나의 표시 정보. */
data class CalendarDayInfo(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isHoliday: Boolean,
    /** 이 날 쓰는 휴가 이름들(예: 포상휴가). 사용 기록에 이름을 붙였다면 함께 표시한다. */
    val leaveNames: List<String>,
    val hasCombatRest: Boolean,
    val hasOvernight: Boolean,
    val hasPass: Boolean,
    /** 이 날짜의 외출 기록(삭제용). */
    val passRecords: List<PassRecord>,
    val events: List<Event>,
    val dutyAssignments: List<DutyAssignment>,
) {
    val hasLeave: Boolean get() = leaveNames.isNotEmpty()

    val hasAnyMarker: Boolean
        get() = hasLeave || hasCombatRest || hasOvernight || hasPass || events.isNotEmpty() || dutyAssignments.isNotEmpty()
}

/** ② 달력(스펙 5.1/5.2절) 화면 상태. */
data class CalendarUiState(
    val year: Int,
    val month: Int,
    val days: List<CalendarDayInfo> = emptyList(),
    val selectedDate: LocalDate? = null,
    /** 당직 저장 시 다음 날 비번을 자동 추가할지(근무 입력 모드 안내 문구용). */
    val autoAddOffDuty: Boolean = false,
) {
    val selectedDayInfo: CalendarDayInfo?
        get() = selectedDate?.let { selected -> days.firstOrNull { it.date == selected } }
}
