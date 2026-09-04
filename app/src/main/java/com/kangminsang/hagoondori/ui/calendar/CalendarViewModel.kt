package com.kangminsang.hagoondori.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangminsang.hagoondori.core.calc.HolidayJudge
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightRecord
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.data.repository.CombatRestRepository
import com.kangminsang.hagoondori.data.repository.DutyRepository
import com.kangminsang.hagoondori.data.repository.EventRepository
import com.kangminsang.hagoondori.data.repository.HolidayRepository
import com.kangminsang.hagoondori.data.repository.LeaveRepository
import com.kangminsang.hagoondori.data.repository.OvernightRepository
import com.kangminsang.hagoondori.data.repository.PassRepository
import com.kangminsang.hagoondori.util.AppClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * 달력(F3) 상태 결합. 월 이동/날짜 선택 같은 화면 전용 상태와, 여러 Repository의
 * 원본 목록을 격자 날짜별로 매핑하는 것 외에는 새로운 계산을 하지 않는다.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val leaveRepository: LeaveRepository,
    private val combatRestRepository: CombatRestRepository,
    private val overnightRepository: OvernightRepository,
    private val passRepository: PassRepository,
    private val dutyRepository: DutyRepository,
    private val holidayRepository: HolidayRepository,
) : ViewModel() {

    private val today = AppClock.today()
    private val yearMonth = MutableStateFlow(today.year to today.monthNumber)
    private val selectedDate = MutableStateFlow<LocalDate?>(today)

    val uiState: StateFlow<CalendarUiState> = combine(
        yearMonth,
        selectedDate,
        combine(eventRepository.observeAll(), dutyRepository.observeAll(), holidayRepository.observeAll(), ::EventDutyHoliday),
        combine(leaveRepository.observeAllUsages(), combatRestRepository.observeUsages(), ::LeaveCombatRest),
        combine(overnightRepository.observeRecords(), passRepository.observeAll(), ::OvernightPass),
    ) { yearMonthValue, selected, eventDutyHoliday, leaveCombatRest, overnightPass ->
        val (year, month) = yearMonthValue
        build(year, month, selected, eventDutyHoliday, leaveCombatRest, overnightPass)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarUiState(year = today.year, month = today.monthNumber),
    )

    private data class EventDutyHoliday(val events: List<Event>, val duties: List<DutyAssignment>, val holidays: List<Holiday>)
    private data class LeaveCombatRest(val leaveUsages: List<LeaveUsage>, val combatRestUsages: List<CombatRestUsage>)
    private data class OvernightPass(val overnightRecords: List<OvernightRecord>, val passRecords: List<PassRecord>)

    private fun build(
        year: Int,
        month: Int,
        selected: LocalDate?,
        edh: EventDutyHoliday,
        lcr: LeaveCombatRest,
        op: OvernightPass,
    ): CalendarUiState {
        val gridDates = CalendarGridBuilder.buildGrid(year, month)
        val days = gridDates.map { date ->
            CalendarDayInfo(
                date = date,
                isCurrentMonth = date.year == year && date.monthNumber == month,
                isHoliday = HolidayJudge.isHoliday(date, edh.holidays),
                hasLeave = lcr.leaveUsages.any { date in it.startDate..it.endDate },
                hasCombatRest = lcr.combatRestUsages.any { date in it.startDate..it.endDate },
                hasOvernight = op.overnightRecords.any { it.date == date },
                hasPass = op.passRecords.any { it.date == date },
                events = edh.events.filter { date in it.startDate..(it.endDate ?: it.startDate) },
                dutyAssignments = edh.duties.filter { it.date == date },
            )
        }
        return CalendarUiState(year = year, month = month, days = days, selectedDate = selected)
    }

    fun previousMonth() {
        val (year, month) = yearMonth.value
        yearMonth.value = if (month == 1) (year - 1) to 12 else year to (month - 1)
    }

    fun nextMonth() {
        val (year, month) = yearMonth.value
        yearMonth.value = if (month == 12) (year + 1) to 1 else year to (month + 1)
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }
}
