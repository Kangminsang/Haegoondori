package com.kangminsang.hagoondori.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangminsang.hagoondori.core.calc.LeaveCalculator
import com.kangminsang.hagoondori.core.calc.LeavePlanner
import com.kangminsang.hagoondori.core.calc.OutingPeriodCalculator
import com.kangminsang.hagoondori.core.calc.OvernightScheduleCalculator
import com.kangminsang.hagoondori.core.calc.PassCalculator
import com.kangminsang.hagoondori.core.calc.PlanKind
import com.kangminsang.hagoondori.core.calc.PlanPart
import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.OvernightForfeiture
import com.kangminsang.hagoondori.core.model.UserProfile
import com.kangminsang.hagoondori.core.payload.LeaveDeviceLabel
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

    /**
     * 휴가 계획 모드에서 고른 기간과 배분. 시작일만 고른 동안은 [end]가 null이다.
     * [parts]는 패널 행 순서(외박 → 휴가 종류)의 일수이고, null이면 아직 기존 기록으로 채우지 않은 것이다.
     * [originalStart]~[originalEnd]는 기존 기록으로 패널을 채운 기간(수정 대상)이다. 저장할 때 이 기간과 새 기간을
     * 합친 범위를 정리해, 줄여서 빠진 날의 기록도 지운다.
     */
    private data class PlanDraft(
        val start: LocalDate,
        val end: LocalDate?,
        val parts: List<PlanPart>? = null,
        val originalStart: LocalDate? = null,
        val originalEnd: LocalDate? = null,
        /** 보기 모드의 [수정]으로 들어왔는지(패널의 [이 출타 삭제] 표시용). */
        val editing: Boolean = false,
    )

    private val planDraft = MutableStateFlow<PlanDraft?>(null)

    private val sources = combine(
        combine(eventRepository.observeAll(), dutyRepository.observeAll(), holidayRepository.observeAll(), ::EventDutyHoliday),
        combine(
            leaveRepository.observeAllUsages(),
            leaveRepository.observeTypes(),
            leaveRepository.observeAllGrants(),
            combatRestRepository.observeUsages(),
            ::LeaveCombatRest,
        ),
        combine(overnightRepository.observeRecords(), overnightRepository.observeForfeitures(), passRepository.observeAll(), ::OvernightPass),
        profileRepository.observe(),
        ::Sources,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<CalendarUiState> = combine(
        yearMonth,
        selectedDate,
        planDraft,
        sources,
    ) { yearMonthValue, selected, draft, src ->
        val (year, month) = yearMonthValue
        if (src == null) CalendarUiState(year = year, month = month, selectedDate = selected)
        else build(year, month, selected, draft, src)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarUiState(year = today.year, month = today.monthNumber),
    )

    private data class EventDutyHoliday(val events: List<Event>, val duties: List<DutyAssignment>, val holidays: List<Holiday>)
    private data class LeaveCombatRest(
        val leaveUsages: List<LeaveUsage>,
        val leaveTypes: List<LeaveType>,
        val leaveGrants: List<LeaveGrant>,
        val combatRestUsages: List<CombatRestUsage>,
    )
    private data class OvernightPass(
        val overnightRecords: List<OvernightRecord>,
        val forfeitures: List<OvernightForfeiture>,
        val passRecords: List<PassRecord>,
    )
    private data class Sources(
        val edh: EventDutyHoliday,
        val lcr: LeaveCombatRest,
        val op: OvernightPass,
        val profile: UserProfile?,
    )

    private fun build(
        year: Int,
        month: Int,
        selected: LocalDate?,
        draft: PlanDraft?,
        src: Sources,
    ): CalendarUiState {
        val (edh, lcr, op, profile) = src
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
        val trip = selected?.let { LeavePlanner.tripAround(it, lcr.leaveUsages, op.overnightRecords) }
        return CalendarUiState(
            year = year,
            month = month,
            days = days,
            selectedDate = selected,
            autoAddOffDuty = profile?.autoAddOffDuty == true,
            selectedTrip = trip?.let { TripSummary(it.start, it.end, it.totalDays, partsText(it.parts, lcr.leaveTypes)) },
            plan = draft?.let { buildPlanState(it, src) },
        )
    }

    /**
     * 계획 패널 상태. 잔여는 출타 관리 화면([com.kangminsang.hagoondori.ui.leave.LeaveManagementViewModel])과 같은
     * 방식으로 계산하되, 고른 기간과 겹치는 기록(이 계획이 바꿀 기록)은 빼고 계산한다.
     */
    private fun buildPlanState(draft: PlanDraft, src: Sources): LeavePlanState {
        val end = draft.end ?: return LeavePlanState(draft.start, null, emptyList(), emptyList(), emptyMap(), draft.editing)
        // 이 계획이 바꿀 기록(새 기간과 원래 기간에 겹치는 기록)은 잔여 계산에서 뺀다.
        val clearStart = draft.originalStart?.let { minOf(it, draft.start) } ?: draft.start
        val clearEnd = draft.originalEnd?.let { maxOf(it, end) } ?: end
        val usages = src.lcr.leaveUsages.filterNot { it.startDate <= clearEnd && it.endDate >= clearStart }
        val overnights = src.op.overnightRecords.filterNot { it.date <= clearEnd && it.endDate >= clearStart }
        val nextOvernightDate = src.profile?.firstOvernightDate?.let { first ->
            OvernightScheduleCalculator.buildSchedule(first, src.profile.overnightCycleWeeks, src.op.overnightRecords, src.op.forfeitures)
                .nextScheduledDate
        }
        val rows = buildList {
            add(PlanRow(PlanKind.Overnight, OutingPeriodCalculator.OVERNIGHT_LABEL, remaining = null))
            src.lcr.leaveTypes.forEach { type ->
                val summary = LeaveCalculator.summarize(
                    type, src.lcr.leaveGrants, usages, today,
                    overnightRecords = overnights,
                    nextOvernightDate = nextOvernightDate,
                )
                add(PlanRow(PlanKind.Leave(type.id), type.name, remaining = summary.remainingAfterPlanned))
            }
        }
        val parts = currentParts(draft.start, end, draft.parts, src)
        val dayLabels = LeavePlanner.layout(draft.start, parts).flatMap { segment ->
            val label = planKindDayLabel(segment.kind, src.lcr.leaveTypes)
            generateSequence(segment.start) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it <= segment.end }.map { it to label }.toList()
        }.toMap()
        return LeavePlanState(draft.start, end, rows, parts, dayLabels, draft.editing)
    }

    /** 패널 행 순서(외박 → 휴가 종류 표시 순서). */
    private fun planKinds(src: Sources): List<PlanKind> = listOf(PlanKind.Overnight) + src.lcr.leaveTypes.map { PlanKind.Leave(it.id) }

    /**
     * 행 순서에 맞춘 지금의 배분. 아직 채우지 않았으면([parts]가 null) 기간의 기존 기록으로 채운다.
     * 같은 종류가 떨어져 여러 번 나와도 행은 하나라 합친다.
     */
    private fun currentParts(start: LocalDate, end: LocalDate, parts: List<PlanPart>?, src: Sources): List<PlanPart> {
        val source = parts ?: LeavePlanner.existingParts(start, end, src.lcr.leaveUsages, src.op.overnightRecords)
        return planKinds(src).map { kind -> PlanPart(kind, source.filter { it.kind == kind }.sumOf { it.days }) }
    }

    /** [start]~[end]를 고르고 그 기간의 기존 기록으로 배분을 채운다. 기록이 있으면 그 기간을 수정 대상으로 기억한다. */
    private fun startPlan(start: LocalDate, end: LocalDate, editing: Boolean): PlanDraft {
        val src = sources.value
        val existing = src?.let { LeavePlanner.existingParts(start, end, it.lcr.leaveUsages, it.op.overnightRecords) }
        val hasRecords = editing || !existing.isNullOrEmpty()
        return PlanDraft(
            start = start,
            end = end,
            parts = src?.let { currentParts(start, end, existing, it) },
            originalStart = if (hasRecords) start else null,
            originalEnd = if (hasRecords) end else null,
            editing = editing,
        )
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

    // ---- 휴가 계획 ----
    // 달력에서 시작일·종료일을 차례로 눌러 전체 기간을 고르고, 패널에서 외박·휴가 종류별 일수를 나눈다.
    // 한 출타는 맞닿은 휴가·외박 기록 여러 개로 저장된다([LeavePlanner]).

    /**
     * 휴가 계획 모드에서 날짜를 눌렀을 때. 첫 탭은 시작일, 둘째 탭은 종료일(앞이면 서로 바꿈)이다.
     * 기간을 다 고른 뒤의 탭은 기간과 배분을 고친다([LeavePlanner.adjust]): 바깥을 누르면 늘어나고,
     * 처음·끝 날을 누르면 하루씩, 안쪽을 누르면 가까운 끝이 그날까지 줄어든다. 기간이 비면 선택을 지운다.
     */
    fun tapPlanDate(date: LocalDate) {
        val current = planDraft.value
        val end = current?.end
        planDraft.value = when {
            current == null -> PlanDraft(date, null)
            end == null -> {
                if (date < current.start) startPlan(date, current.start, editing = false)
                else startPlan(current.start, date, editing = false)
            }
            else -> {
                val src = sources.value ?: return
                val parts = currentParts(current.start, end, current.parts, src)
                LeavePlanner.adjust(current.start, end, parts, date)
                    ?.let { current.copy(start = it.start, end = it.end, parts = it.parts) }
            }
        }
    }

    /** 이미 저장된 출타를 고치려고 그 기간을 바로 고른다. 그 기간의 달로 넘어간다. */
    fun editTrip(start: LocalDate, end: LocalDate) {
        planDraft.value = startPlan(start, end, editing = true)
        yearMonth.value = start.year to start.monthNumber
    }

    /** 패널에서 한 행의 일수를 바꾼다. */
    fun setPlanDays(kind: PlanKind, days: Int) {
        val current = planDraft.value ?: return
        val end = current.end ?: return
        val src = sources.value ?: return
        val parts = currentParts(current.start, end, current.parts, src)
        planDraft.value = current.copy(parts = parts.map { if (it.kind == kind) it.copy(days = days.coerceAtLeast(0)) else it })
    }

    /** 패널의 배분을 통째로 바꾼다(자동 배분). */
    fun setPlanParts(parts: List<PlanPart>) {
        val current = planDraft.value ?: return
        if (current.end == null) return
        planDraft.value = current.copy(parts = parts)
    }

    fun clearPlan() {
        planDraft.value = null
    }

    /**
     * 고른 기간을 [parts]로 저장한다. 겹치는 기존 기록은 바꾸고, 기간 밖으로 삐져나온 부분은 잘라서 남긴다.
     * 기존 출타를 고친 경우엔 원래 기간까지 함께 정리해, 줄여서 빠진 날의 기록을 지운다.
     *
     * @param onResult 안내 문구 (예: "09-22~09-28 휴가 계획 저장 (외박1 포상3 연가3)")
     */
    fun savePlan(parts: List<PlanPart>, onResult: (String) -> Unit) {
        val draft = planDraft.value ?: return
        val end = draft.end ?: return
        viewModelScope.launch {
            toggleMutex.withLock {
                val changes = LeavePlanner.applyEdit(
                    draft.originalStart, draft.originalEnd, draft.start, end, parts,
                    leaveRepository.observeAllUsages().first(),
                    overnightRepository.observeRecords().first(),
                )
                applyChanges(changes)
                val types = leaveRepository.observeTypes().first()
                planDraft.value = null
                onResult("${rangeLabel(draft.start, end)} 휴가 계획 저장 (${partsText(parts, types)})")
            }
        }
    }

    /** 패널에서 고치던 기존 출타(원래 기간)를 지운다. */
    fun deleteEditedTrip(onResult: (String) -> Unit) {
        val draft = planDraft.value ?: return
        val start = draft.originalStart ?: return
        val end = draft.originalEnd ?: return
        planDraft.value = null
        deleteTrip(start, end, onResult)
    }

    /** [start]~[end] 출타의 휴가·외박 기록을 모두 지운다. */
    fun deleteTrip(start: LocalDate, end: LocalDate, onResult: (String) -> Unit) {
        viewModelScope.launch {
            toggleMutex.withLock {
                val usages = leaveRepository.observeAllUsages().first()
                val overnights = overnightRepository.observeRecords().first()
                val parts = LeavePlanner.existingParts(start, end, usages, overnights)
                applyChanges(
                    LeavePlanner.apply(
                        start, end, emptyList(),
                        leaveRepository.observeAllUsages().first(),
                        overnightRepository.observeRecords().first(),
                    ),
                )
                val types = leaveRepository.observeTypes().first()
                onResult("${rangeLabel(start, end)} 출타 삭제 (${partsText(parts, types)})")
            }
        }
    }

    private suspend fun applyChanges(changes: LeavePlanner.Changes) {
        changes.deleteUsages.forEach { leaveRepository.deleteUsage(it) }
        changes.deleteOvernights.forEach { overnightRepository.deleteRecord(it) }
        changes.addUsages.forEach { leaveRepository.addUsage(it.leaveTypeId, it.startDate, it.endDate, it.label) }
        changes.addOvernights.forEach { overnightRepository.addRecord(it.date, it.memo, it.endDate) }
    }
}

/** 안내 문구용 기간 표기: MM-dd~MM-dd (하루면 MM-dd). */
private fun rangeLabel(start: LocalDate, end: LocalDate): String =
    if (start == end) start.shortLabel() else "${start.shortLabel()}~${end.shortLabel()}"

/** 종류별 일수 요약: "외박1 포상3 연가3". 일수 0인 종류는 뺀다. 휴가 이름은 장치 라벨처럼 두 글자로 줄인다. */
internal fun partsText(parts: List<PlanPart>, types: List<LeaveType>): String =
    parts.filter { it.days > 0 }.joinToString(" ") { "${planKindShortName(it.kind, types)}${it.days}" }

internal fun planKindShortName(kind: PlanKind, types: List<LeaveType>): String = when (kind) {
    PlanKind.Overnight -> OutingPeriodCalculator.OVERNIGHT_LABEL
    is PlanKind.Leave -> types.firstOrNull { it.id == kind.typeId }?.name?.let(LeaveDeviceLabel::forTypeName) ?: "휴가"
}

/** 계획 중 달력 칸에 보이는 한 글자: 외박은 "외", 휴가는 장치 라벨의 첫 글자(포상 → "포", 정기휴가 → "연"). */
internal fun planKindDayLabel(kind: PlanKind, types: List<LeaveType>): String = when (kind) {
    PlanKind.Overnight -> "외"
    is PlanKind.Leave -> planKindShortName(kind, types).take(1)
}

/** 안내 문구용 날짜 표기: MM-dd (월·일 모두 두 자리). */
private fun LocalDate.shortLabel(): String = "%02d-%02d".format(monthNumber, dayOfMonth)
