package com.kangminsang.hagoondori.ui.calendar

import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.calc.PlanKind
import com.kangminsang.hagoondori.core.calc.PlanPart
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
    /** 이 날이 시작일인 휴가 종류 이름(끝의 '휴가'를 뗌). 홈 화면 위젯의 칸 글자에 쓴다. */
    val startingLeaveNames: List<String> = emptyList(),
    /** 이 날 외박을 나가기 시작했는지(외박 기록의 시작일). 홈 화면 위젯의 칸 글자에 쓴다. */
    val overnightStarts: Boolean = false,
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
    /** 선택한 날을 포함하는 출타(맞닿은 휴가·외박 기록 묶음). 없으면 null. */
    val selectedTrip: TripSummary? = null,
    /** 휴가 계획 모드에서 고른 기간과 배분 패널. 기간을 고르지 않았으면 null. */
    val plan: LeavePlanState? = null,
) {
    val selectedDayInfo: CalendarDayInfo?
        get() = selectedDate?.let { selected -> days.firstOrNull { it.date == selected } }
}

/** 앱 밖(홈 화면 위젯 등)에서 달력을 열라는 요청. [date]가 null이면 달력 탭만 연다. */
data class CalendarOpenRequest(val date: LocalDate?)

/** 보기 모드 상세에 보이는 출타 요약. [partsText]는 "외박1 포상3 연가3"처럼 줄인 종류별 일수다. */
data class TripSummary(val start: LocalDate, val end: LocalDate, val totalDays: Int, val partsText: String)

/** 휴가 계획 패널의 한 행. [remaining]은 이 기간의 기존 기록을 뺀 계획 후 잔여(외박은 null: 잔여를 보이지 않는다). */
data class PlanRow(val kind: PlanKind, val name: String, val remaining: Int?)

/**
 * 휴가 계획 모드 상태. [end]가 null이면 시작일만 고른 상태다.
 * [parts]는 [rows]와 같은 순서의 행별 일수(처음엔 고른 기간의 기존 기록으로 채운다).
 * [dayLabels]는 계획대로라면 각 날짜에 들어갈 종류의 한 글자(달력 칸 표시용).
 * [editing]은 보기 모드의 [수정]으로 기존 출타를 고치는 중인지다.
 */
data class LeavePlanState(
    val start: LocalDate,
    val end: LocalDate?,
    val rows: List<PlanRow>,
    val parts: List<PlanPart>,
    val dayLabels: Map<LocalDate, String>,
    val editing: Boolean,
)
