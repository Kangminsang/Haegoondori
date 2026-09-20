package com.kangminsang.hagoondori.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangminsang.hagoondori.core.calc.HolidayJudge
import com.kangminsang.hagoondori.core.calc.PassCalculator
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightRecord
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.PassType
import com.kangminsang.hagoondori.data.repository.CombatRestRepository
import com.kangminsang.hagoondori.data.repository.DutyRepository
import com.kangminsang.hagoondori.data.repository.EventRepository
import com.kangminsang.hagoondori.data.repository.HolidayRepository
import com.kangminsang.hagoondori.data.repository.LeaveRepository
import com.kangminsang.hagoondori.data.repository.OvernightRepository
import com.kangminsang.hagoondori.data.repository.PassRepository
import com.kangminsang.hagoondori.data.repository.ProfileRepository
import com.kangminsang.hagoondori.core.model.DutyType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import com.kangminsang.hagoondori.util.AppClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val today = AppClock.today()
    private val yearMonth = MutableStateFlow(today.year to today.monthNumber)
    private val selectedDate = MutableStateFlow<LocalDate?>(today)

    private val baseState = combine(
        yearMonth,
        selectedDate,
        combine(eventRepository.observeAll(), dutyRepository.observeAll(), holidayRepository.observeAll(), ::EventDutyHoliday),
        combine(leaveRepository.observeAllUsages(), combatRestRepository.observeUsages(), ::LeaveCombatRest),
        combine(overnightRepository.observeRecords(), passRepository.observeAll(), ::OvernightPass),
    ) { yearMonthValue, selected, eventDutyHoliday, leaveCombatRest, overnightPass ->
        val (year, month) = yearMonthValue
        build(year, month, selected, eventDutyHoliday, leaveCombatRest, overnightPass)
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        baseState,
        profileRepository.observe().map { it?.autoAddOffDuty == true },
    ) { state, autoAddOffDuty -> state.copy(autoAddOffDuty = autoAddOffDuty) }.stateIn(
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
                hasOvernight = op.overnightRecords.any { date in it.date..it.endDate },
                hasPass = op.passRecords.any { it.date == date },
                passRecords = op.passRecords.filter { it.date == date },
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

    // ---- 일반 일정 (F4) ----

    fun addEvent(title: String, startDate: LocalDate, endDate: LocalDate?, isImportant: Boolean, memo: String?) {
        viewModelScope.launch { eventRepository.addEvent(title, startDate, endDate, isImportant, memo) }
    }

    // ---- 외출 ----

    fun deletePass(record: PassRecord) {
        viewModelScope.launch { passRepository.deleteRecord(record) }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch { eventRepository.delete(event) }
    }

    // ---- 근무 일괄 입력 (F6) ----
    // 당직표가 한 달치가 한 번에 나오는 근무 환경 특성상, 여러 날짜를 골라 한 번에 저장한다(스펙 4.13절).

    /**
     * [dates]를 외출로 일괄 저장한다. 평일/휴일은 날짜별로 자동 판정하고, 이미 외출이 기록된
     * 날짜는 건너뛴다.
     *
     * @param onResult (저장된 건수, 건너뛴 건수)
     */
    fun submitPass(dates: Set<LocalDate>, onResult: (savedCount: Int, skippedCount: Int) -> Unit) {
        viewModelScope.launch {
            val holidays = holidayRepository.getAll()
            val alreadyPassed = passRepository.observeAll().first().map { it.date }.toSet()
            val targets = dates.filter { it !in alreadyPassed }
            targets.forEach { date ->
                passRepository.addRecord(date, PassCalculator.classifyType(date, holidays), null)
            }
            onResult(targets.size, dates.size - targets.size)
        }
    }

    /**
     * [dates]를 [type]으로 일괄 저장한다. 이미 있는 `(date, type)` 조합은 조용히 건너뛴다.
     * [type]이 DUTY이고 [com.kangminsang.hagoondori.core.model.UserProfile.autoAddOffDuty]가
     * 켜져 있으면, 선택한 날짜들의 **다음 날**을 OFF_DUTY로도 자동 추가한다.
     *
     * @param onResult (저장된 건수, 건너뛴 건수) - 화면이 이걸로 안내 문구를 만든다.
     */
    fun submitDuty(dates: Set<LocalDate>, type: DutyType, onResult: (savedCount: Int, skippedCount: Int) -> Unit) {
        if (dates.isEmpty()) {
            onResult(0, 0)
            return
        }
        viewModelScope.launch {
            val primarySaved = dutyRepository.bulkAssign(dates, type)
            val primarySkipped = dates.size - primarySaved

            var autoAddedOffDutyCount = 0
            if (type == DutyType.DUTY && profileRepository.get()?.autoAddOffDuty == true) {
                val nextDayDates = dates.map { it.plus(1, DateTimeUnit.DAY) }
                autoAddedOffDutyCount = dutyRepository.bulkAssign(nextDayDates, DutyType.OFF_DUTY)
            }

            // 자동 추가분의 건너뜀은 부수 효과일 뿐이라 "건너뜀" 안내는 사용자가 직접 고른 날짜 기준으로만 센다.
            onResult(primarySaved + autoAddedOffDutyCount, primarySkipped)
        }
    }
}
