package com.kangminsang.hagoondori.ui.calendar

import com.kangminsang.hagoondori.core.calc.HolidayJudge
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightRecord
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.widget.WidgetText

/** 달력 한 달을 만드는 원본 목록. 앱 달력 화면과 홈 화면 달력 위젯이 같은 입력을 쓴다. */
data class CalendarSource(
    val events: List<Event>,
    val duties: List<DutyAssignment>,
    val holidays: List<Holiday>,
    val leaveUsages: List<LeaveUsage>,
    val leaveTypes: List<LeaveType>,
    val combatRestUsages: List<CombatRestUsage>,
    val overnightRecords: List<OvernightRecord>,
    val passRecords: List<PassRecord>,
)

/**
 * 격자(앞뒤 달 칸 포함)의 날짜별 표시 정보를 만든다. 날짜별 계산 규칙은 여기가 유일한 출처다 -
 * 앱 달력 화면([CalendarViewModel])과 홈 화면 위젯이 함께 쓰므로 한쪽만 바꾸면 안 된다.
 */
object CalendarDayBuilder {

    fun build(year: Int, month: Int, source: CalendarSource): List<CalendarDayInfo> {
        val typeNames = source.leaveTypes.associate { it.id to it.name }
        return CalendarGridBuilder.buildGrid(year, month).map { date ->
            CalendarDayInfo(
                date = date,
                isCurrentMonth = date.year == year && date.monthNumber == month,
                isHoliday = HolidayJudge.isHoliday(date, source.holidays),
                leaveNames = source.leaveUsages
                    .filter { date in it.startDate..it.endDate }
                    .map { usage ->
                        val name = typeNames[usage.leaveTypeId] ?: "휴가"
                        usage.label?.takeIf { it.isNotBlank() }?.let { "$name ($it)" } ?: name
                    }
                    .distinct(),
                hasCombatRest = source.combatRestUsages.any { date in it.startDate..it.endDate },
                hasOvernight = source.overnightRecords.any { date in it.date..it.endDate },
                hasPass = source.passRecords.any { it.date == date },
                passRecords = source.passRecords.filter { it.date == date },
                events = source.events.filter { date in it.startDate..(it.endDate ?: it.startDate) },
                dutyAssignments = source.duties.filter { it.date == date },
                startingLeaveNames = source.leaveUsages
                    .filter { it.startDate == date }
                    .map { WidgetText.shortLeaveName(typeNames[it.leaveTypeId] ?: "휴가") }
                    .distinct(),
                overnightStarts = source.overnightRecords.any { it.date == date },
            )
        }
    }
}
