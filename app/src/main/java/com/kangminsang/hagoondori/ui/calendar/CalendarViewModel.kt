package com.kangminsang.hagoondori.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangminsang.hagoondori.core.calc.PassCalculator
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.LeaveType
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
import com.kangminsang.hagoondori.ui.duty.dutyTypeLabel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        combine(leaveRepository.observeAllUsages(), leaveRepository.observeTypes(), combatRestRepository.observeUsages(), ::LeaveCombatRest),
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
    private data class LeaveCombatRest(
        val leaveUsages: List<LeaveUsage>,
        val leaveTypes: List<LeaveType>,
        val combatRestUsages: List<CombatRestUsage>,
    )
    private data class OvernightPass(val overnightRecords: List<OvernightRecord>, val passRecords: List<PassRecord>)

    private fun build(
        year: Int,
        month: Int,
        selected: LocalDate?,
        edh: EventDutyHoliday,
        lcr: LeaveCombatRest,
        op: OvernightPass,
    ): CalendarUiState {
        val days = CalendarDayBuilder.build(
            year,
            month,
            CalendarSource(
                events = edh.events,
                duties = edh.duties,
                holidays = edh.holidays,
                leaveUsages = lcr.leaveUsages,
                leaveTypes = lcr.leaveTypes,
                combatRestUsages = lcr.combatRestUsages,
                overnightRecords = op.overnightRecords,
                passRecords = op.passRecords,
            ),
        )
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

    /** 홈 화면 위젯에서 날짜를 골랐을 때: 그 날짜의 달을 보이고 그 날을 선택한다. */
    fun showDate(date: LocalDate) {
        yearMonth.value = date.year to date.monthNumber
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

    fun updateEvent(event: Event) {
        viewModelScope.launch { eventRepository.update(event) }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch { eventRepository.delete(event) }
    }

    fun deleteDuty(assignment: DutyAssignment) {
        viewModelScope.launch { dutyRepository.delete(assignment) }
    }

    // ---- 근무·외출 탭 입력 (F6) ----
    // 날짜를 한 번 누르면 고른 종류가 바로 저장되고, 이미 있는 날을 다시 누르면 지워진다(토글).
    // 빠르게 연속으로 눌러도 같은 날짜가 두 번 들어가거나 엇갈리지 않도록 모든 토글을 하나의 Mutex로 직렬화한다.
    // 저장 결과는 onResult로 한 줄 안내 문구를 돌려준다(화면의 "방금" 표시에 쓴다).

    private val toggleMutex = Mutex()

    /**
     * [date]의 [type] 배정을 토글한다. 이미 있으면 지우고, 없으면 추가한다.
     * [type]이 DUTY이고 [com.kangminsang.hagoondori.core.model.UserProfile.autoAddOffDuty]가
     * 켜져 있으면 **다음 날**의 비번(OFF_DUTY)도 함께 넣거나 함께 지운다(4.16절).
     *
     * @param onResult 방금 한 동작을 설명하는 안내 문구 (예: "11-17 당직 추가 · 11-18 비번 자동 추가")
     */
    fun toggleDuty(date: LocalDate, type: DutyType, onResult: (String) -> Unit) {
        viewModelScope.launch {
            toggleMutex.withLock {
                val autoAddOff = type == DutyType.DUTY && profileRepository.get()?.autoAddOffDuty == true
                val nextDay = date.plus(1, DateTimeUnit.DAY)
                val current = dutyRepository.observeAll().first()
                val label = dutyTypeLabel(type)
                val existing = current.firstOrNull { it.date == date && it.type == type }

                if (existing != null) {
                    dutyRepository.delete(existing)
                    var message = "${date.shortLabel()} $label 삭제"
                    if (autoAddOff) {
                        val nextOff = current.firstOrNull { it.date == nextDay && it.type == DutyType.OFF_DUTY }
                        if (nextOff != null) {
                            dutyRepository.delete(nextOff)
                            message += " · ${nextDay.shortLabel()} 비번 함께 삭제"
                        }
                    }
                    onResult(message)
                } else {
                    dutyRepository.assign(date, type)
                    var message = "${date.shortLabel()} $label 추가"
                    if (autoAddOff && current.none { it.date == nextDay && it.type == DutyType.OFF_DUTY }) {
                        dutyRepository.assign(nextDay, DutyType.OFF_DUTY)
                        message += " · ${nextDay.shortLabel()} 비번 자동 추가"
                    }
                    onResult(message)
                }
            }
        }
    }

    /**
     * [date]의 외출 기록을 토글한다. 기록이 있으면 그 날짜의 것을 모두 지우고, 없으면 평일/휴일을
     * 자동 판정해 추가한다.
     *
     * @param onResult 방금 한 동작을 설명하는 안내 문구 (예: "11-08 외출 추가 (평일)")
     */
    fun togglePass(date: LocalDate, onResult: (String) -> Unit) {
        viewModelScope.launch {
            toggleMutex.withLock {
                val existing = passRepository.observeAll().first().filter { it.date == date }
                if (existing.isNotEmpty()) {
                    existing.forEach { passRepository.deleteRecord(it) }
                    onResult("${date.shortLabel()} 외출 삭제")
                } else {
                    val type = PassCalculator.classifyType(date, holidayRepository.getAll())
                    passRepository.addRecord(date, type, null)
                    val kind = if (type == PassType.WEEKDAY) "평일" else "휴일"
                    onResult("${date.shortLabel()} 외출 추가 ($kind)")
                }
            }
        }
    }
}

/** 안내 문구용 날짜 표기: MM-dd (월·일 모두 두 자리). */
private fun LocalDate.shortLabel(): String = "%02d-%02d".format(monthNumber, dayOfMonth)
