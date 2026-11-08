package com.kangminsang.hagoondori.ui.leave

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangminsang.hagoondori.core.calc.CombatRestCalculator
import com.kangminsang.hagoondori.core.calc.LeaveCalculator
import com.kangminsang.hagoondori.core.calc.ConductPointCalculator
import com.kangminsang.hagoondori.core.calc.OvernightScheduleCalculator
import com.kangminsang.hagoondori.core.calc.PassCalculator
import com.kangminsang.hagoondori.core.model.CombatRestGrant
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.ConductPoint
import com.kangminsang.hagoondori.core.model.Overtime
import com.kangminsang.hagoondori.core.model.ConductPointKind
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OverflowBehavior
import com.kangminsang.hagoondori.core.model.OvernightForfeiture
import com.kangminsang.hagoondori.core.model.OvernightRecord
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.PassType
import com.kangminsang.hagoondori.core.model.UserProfile
import com.kangminsang.hagoondori.data.repository.CombatRestRepository
import com.kangminsang.hagoondori.data.repository.ConductPointRepository
import com.kangminsang.hagoondori.data.repository.HolidayRepository
import com.kangminsang.hagoondori.data.repository.LeaveRepository
import com.kangminsang.hagoondori.data.repository.OvernightRepository
import com.kangminsang.hagoondori.data.repository.PassRepository
import com.kangminsang.hagoondori.data.repository.ProfileRepository
import com.kangminsang.hagoondori.util.AppClock
import com.kangminsang.hagoondori.ui.dashboard.LeaveTypeSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * 출타 관리(F7~F14) 상태 결합 및 입력 처리. 4개 탭(휴가/전투휴무/외박/외출)이
 * 이 하나의 ViewModel을 공유한다 - 서로 다른 자원이지만 한 화면에서 오가며
 * 확인하는 경우가 많고(예: 포상휴가를 부여하면 전투휴무 전환분이 바로 바뀜),
 * 상태를 나누면 오히려 그 연관성이 화면에서 끊겨 보인다.
 */
@HiltViewModel
class LeaveManagementViewModel @Inject constructor(
    private val leaveRepository: LeaveRepository,
    private val combatRestRepository: CombatRestRepository,
    private val overnightRepository: OvernightRepository,
    private val passRepository: PassRepository,
    private val conductPointRepository: ConductPointRepository,
    private val profileRepository: ProfileRepository,
    private val holidayRepository: HolidayRepository,
) : ViewModel() {

    val uiState: StateFlow<LeaveManagementUiState> = combine(
        combine(
            profileRepository.observe(),
            combine(leaveRepository.observeTypes(), leaveRepository.observeAllGrants(), leaveRepository.observeAllUsages(), ::LeaveData),
            combine(combatRestRepository.observeGrants(), combatRestRepository.observeUsages(), ::CombatRestData),
            combine(overnightRepository.observeRecords(), overnightRepository.observeForfeitures(), ::OvernightData),
            combine(passRepository.observeAll(), holidayRepository.observeAll(), AppClock.todayFlow(), ::PassHolidayData),
        ) { profile, leaveData, combatRestData, overnightData, passHoliday ->
            build(profile, leaveData, combatRestData, overnightData, passHoliday)
        },
        conductPointRepository.observeAll(),
    ) { state, conductPoints -> state.copy(conductPoints = conductPoints) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeaveManagementUiState())

    private data class LeaveData(val types: List<LeaveType>, val grants: List<LeaveGrant>, val usages: List<LeaveUsage>)
    private data class CombatRestData(val grants: List<CombatRestGrant>, val usages: List<CombatRestUsage>)
    private data class OvernightData(val records: List<OvernightRecord>, val forfeitures: List<OvernightForfeiture>)
    private data class PassHolidayData(val records: List<PassRecord>, val holidays: List<Holiday>, val today: LocalDate)

    private fun build(
        profile: UserProfile?,
        leaveData: LeaveData,
        combatRestData: CombatRestData,
        overnightData: OvernightData,
        passHoliday: PassHolidayData,
    ): LeaveManagementUiState {
        val overnightSchedule = profile?.firstOvernightDate?.let { first ->
            OvernightScheduleCalculator.buildSchedule(first, profile.overnightCycleWeeks, overnightData.records, overnightData.forfeitures)
        }

        val leaveSummaries = leaveData.types.map { type ->
            val s = LeaveCalculator.summarize(
                type, leaveData.grants, leaveData.usages, passHoliday.today,
                overnightRecords = overnightData.records,
                nextOvernightDate = overnightSchedule?.nextScheduledDate,
            )
            LeaveTypeSummary(
                type = type,
                granted = s.granted,
                used = s.used,
                expired = s.expired,
                remaining = s.remaining,
                planned = s.planned,
                remainingAfterPlanned = s.remainingAfterPlanned,
                grantStatuses = s.grants,
                remainingCapCapacity = LeaveCalculator.remainingCapCapacity(type, leaveData.grants),
            )
        }

        val combatRestSummary = CombatRestCalculator.summarizeAcrossLeaveTypes(
            leaveTypes = leaveData.types,
            leaveGrants = leaveData.grants,
            restGrants = combatRestData.grants,
            restUsages = combatRestData.usages,
        )

        val upcomingSlotOptions = if (profile?.firstOvernightDate != null && overnightSchedule != null) {
            val forfeitedSlots = overnightData.forfeitures.map { it.slotIndex }.toSet()
            OvernightScheduleCalculator.effectiveSlots(forfeitedSlots)
                .dropWhile { it < overnightSchedule.nextSlotIndex }
                .take(UPCOMING_SLOT_OPTION_COUNT)
                .map { slot ->
                    UpcomingSlotOption(
                        slotIndex = slot,
                        scheduledDate = OvernightScheduleCalculator.scheduledDate(profile.firstOvernightDate!!, profile.overnightCycleWeeks, slot),
                    )
                }
                .toList()
        } else {
            emptyList()
        }

        val remainingOvernights = if (profile?.firstOvernightDate != null && overnightSchedule != null) {
            OvernightScheduleCalculator.remainingUntilDischarge(
                firstOvernightDate = profile.firstOvernightDate!!,
                cycleWeeks = profile.overnightCycleWeeks,
                schedule = overnightSchedule,
                dischargeDate = profile.dischargeDate,
                today = passHoliday.today,
            )
        } else {
            emptyList()
        }

        return LeaveManagementUiState(
            profile = profile,
            leaveTypes = leaveData.types,
            leaveGrants = leaveData.grants,
            leaveUsages = leaveData.usages,
            leaveSummaries = leaveSummaries,
            combatRestGrants = combatRestData.grants,
            combatRestUsages = combatRestData.usages,
            combatRestSummary = combatRestSummary,
            overnightRecords = overnightData.records,
            overnightForfeitures = overnightData.forfeitures,
            overnightSchedule = overnightSchedule,
            passRecords = passHoliday.records,
            holidays = passHoliday.holidays,
            upcomingSlotOptions = upcomingSlotOptions,
            remainingOvernights = remainingOvernights,
        )
    }

    // ---- 휴가 ----

    fun addLeaveGrant(leaveTypeId: String, days: Int, grantedDate: LocalDate, reason: String?, expiryDate: LocalDate?) {
        viewModelScope.launch { leaveRepository.addGrant(leaveTypeId, days, grantedDate, reason, expiryDate) }
    }

    /** 휴가 종류 카드를 [delta](-1=위, +1=아래)만큼 옮긴다. 이미 끝이면 아무 일도 하지 않는다. */
    fun moveLeaveType(leaveTypeId: String, delta: Int) {
        val ids = uiState.value.leaveTypes.map { it.id }.toMutableList()
        val from = ids.indexOf(leaveTypeId)
        val to = from + delta
        if (from < 0 || to !in ids.indices) return
        ids.add(to, ids.removeAt(from))
        viewModelScope.launch { leaveRepository.reorderTypes(ids) }
    }

    /** 휴가 종류를 새로 만든다. 입력 검증은 호출하는 화면이 저장 전에 끝낸다. */
    fun addLeaveType(name: String, cap: Int?, overflowBehavior: OverflowBehavior, fixedDays: Int?) {
        viewModelScope.launch { leaveRepository.createType(name, cap, overflowBehavior, fixedDays) }
    }

    /** 휴가 종류의 이름·상한·초과 처리·고정 일수를 고친다. 표시 순서는 그대로 둔다. */
    fun updateLeaveType(type: LeaveType) {
        viewModelScope.launch { leaveRepository.updateType(type) }
    }

    fun deleteLeaveGrant(grant: LeaveGrant) {
        viewModelScope.launch { leaveRepository.deleteGrant(grant) }
    }

    fun addLeaveUsage(leaveTypeId: String, startDate: LocalDate, endDate: LocalDate, label: String?) {
        viewModelScope.launch { leaveRepository.addUsage(leaveTypeId, startDate, endDate, label) }
    }

    fun updateLeaveUsage(usage: LeaveUsage, startDate: LocalDate, endDate: LocalDate, label: String?) {
        viewModelScope.launch { leaveRepository.updateUsage(usage.copy(startDate = startDate, endDate = endDate, label = label)) }
    }

    fun deleteLeaveUsage(usage: LeaveUsage) {
        viewModelScope.launch { leaveRepository.deleteUsage(usage) }
    }

    // ---- 전투휴무 ----

    fun addCombatRestGrant(days: Int, grantedDate: LocalDate, reason: String?) {
        viewModelScope.launch { combatRestRepository.addGrant(days, grantedDate, reason) }
    }

    fun addCombatRestUsage(startDate: LocalDate, endDate: LocalDate, memo: String?) {
        viewModelScope.launch { combatRestRepository.addUsage(startDate, endDate, memo) }
    }

    fun deleteCombatRestUsage(usage: CombatRestUsage) {
        viewModelScope.launch { combatRestRepository.deleteUsage(usage) }
    }

    // ---- 외박 ----

    fun addOvernightRecord(date: LocalDate, endDate: LocalDate, memo: String?) {
        viewModelScope.launch { overnightRepository.addRecord(date, memo, endDate) }
    }

    fun updateOvernightRecord(record: OvernightRecord, date: LocalDate, endDate: LocalDate, memo: String?) {
        viewModelScope.launch { overnightRepository.updateRecord(record.copy(date = date, endDate = endDate, memo = memo)) }
    }

    fun deleteOvernightRecord(record: OvernightRecord) {
        viewModelScope.launch { overnightRepository.deleteRecord(record) }
    }

    /**
     * 차수 소멸 처리(F13). 호출부(화면)가 예정일 목록에서 고른 [slotIndex]만
     * 받는다 - 사용자가 차수 번호를 직접 입력하지 않는다(4.10절 요구사항).
     */
    fun addOvernightForfeiture(slotIndex: Int, reason: String?, recordedDate: LocalDate?) {
        viewModelScope.launch { overnightRepository.addForfeiture(slotIndex, reason, recordedDate) }
    }

    // ---- 외출 ----

    fun addPassRecord(date: LocalDate, explicitType: PassType?, memo: String?) {
        viewModelScope.launch {
            val holidays = holidayRepository.getAll()
            val type = explicitType ?: PassCalculator.classifyType(date, holidays)
            passRepository.addRecord(date, type, memo)
        }
    }

    // ---- 점수 ----

    fun addConductPoint(date: LocalDate, kind: ConductPointKind, centiPoints: Int, reason: String, memo: String?, overtime: Overtime? = null) {
        viewModelScope.launch { conductPointRepository.add(date, kind, centiPoints, reason, memo, overtime) }
    }

    fun updateConductPoint(
        record: ConductPoint,
        date: LocalDate,
        kind: ConductPointKind,
        centiPoints: Int,
        reason: String,
        memo: String?,
        overtime: Overtime? = null,
    ) {
        viewModelScope.launch {
            conductPointRepository.update(
                record.copy(date = date, kind = kind, centiPoints = centiPoints, reason = reason, memo = memo, overtime = overtime),
            )
        }
    }

    /** 군기지도위원회가 정한 포상휴가 [days]일을 [meetingDate] 회의일로 부여 기록한다. 상한 초과 처리는 기존 부여 경로가 맡는다. */
    fun grantRewardLeaveFromPoints(days: Int, meetingDate: LocalDate) {
        viewModelScope.launch {
            val type = ConductPointCalculator.findRewardLeaveType(uiState.value.leaveTypes) ?: return@launch
            leaveRepository.addGrant(type.id, days, meetingDate, ConductPointCalculator.REWARD_CONVERSION_REASON)
        }
    }

    fun deleteConductPoint(record: ConductPoint) {
        viewModelScope.launch { conductPointRepository.delete(record) }
    }

    private companion object {
        /** 차수 소멸 처리(F13) UI에 보여줄 예정 차수 개수. */
        const val UPCOMING_SLOT_OPTION_COUNT = 5
    }
}
