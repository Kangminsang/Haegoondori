package com.kangminsang.hagoondori.ui.calendar

import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.Event
import kotlinx.datetime.LocalDate

/** 격자 위 날짜 하나의 표시 정보. */
data class CalendarDayInfo(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isHoliday: Boolean,
    val hasLeave: Boolean,
    val hasCombatRest: Boolean,
    val hasOvernight: Boolean,
    val hasPass: Boolean,
    val events: List<Event>,
    val dutyAssignments: List<DutyAssignment>,
) {
    val hasAnyMarker: Boolean
        get() = hasLeave || hasCombatRest || hasOvernight || hasPass || events.isNotEmpty() || dutyAssignments.isNotEmpty()
}

/** ② 달력(스펙 5.1/5.2절) 화면 상태. */
data class CalendarUiState(
    val year: Int,
    val month: Int,
    val days: List<CalendarDayInfo> = emptyList(),
    val selectedDate: LocalDate? = null,
) {
    val selectedDayInfo: CalendarDayInfo?
        get() = selectedDate?.let { selected -> days.firstOrNull { it.date == selected } }
}
