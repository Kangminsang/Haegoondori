package com.kangminsang.hagoondori.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangminsang.hagoondori.core.calc.CombatRestCalculator
import com.kangminsang.hagoondori.core.calc.DDayCalculator
import com.kangminsang.hagoondori.core.calc.LeaveCalculator
import com.kangminsang.hagoondori.core.calc.OutingPeriodCalculator
import com.kangminsang.hagoondori.core.calc.OvernightScheduleCalculator
import com.kangminsang.hagoondori.core.calc.ServiceScheduleCalculator
import com.kangminsang.hagoondori.core.model.CombatRestGrant
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightForfeiture
import com.kangminsang.hagoondori.core.model.OvernightRecord
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.SyncState
import com.kangminsang.hagoondori.core.model.UserProfile
import com.kangminsang.hagoondori.data.remote.holiday.HolidayAutoRefresh
import com.kangminsang.hagoondori.data.repository.CombatRestRepository
import com.kangminsang.hagoondori.data.repository.LeaveRepository
import com.kangminsang.hagoondori.data.repository.OvernightRepository
import com.kangminsang.hagoondori.data.repository.PassRepository
import com.kangminsang.hagoondori.data.repository.ProfileRepository
import com.kangminsang.hagoondori.data.repository.SyncStateRepository
import com.kangminsang.hagoondori.util.AppClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 대시보드(F2) 상태 결합. 여러 Repository의 원본 Flow를 모아 core.calc의 순수
 * 계산기에 넘기고, 그 결과만 화면에 노출한다 - 계산 로직 자체는 여기 있지 않다.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val leaveRepository: LeaveRepository,
    private val combatRestRepository: CombatRestRepository,
    private val overnightRepository: OvernightRepository,
    private val passRepository: PassRepository,
    private val syncStateRepository: SyncStateRepository,
    private val holidayAutoRefresh: HolidayAutoRefresh,
) : ViewModel() {

    init {
        // 공휴일이 오래됐으면 조용히 갱신한다. 실패해도 내장 데이터가 있으므로 화면은 영향받지 않는다.
        viewModelScope.launch { holidayAutoRefresh.refreshIfStale() }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        profileRepository.observe(),
        combine(
            leaveRepository.observeTypes(),
            leaveRepository.observeAllGrants(),
            leaveRepository.observeAllUsages(),
            ::LeaveData,
        ),
        combine(combatRestRepository.observeGrants(), combatRestRepository.observeUsages(), ::CombatRestData),
        combine(overnightRepository.observeRecords(), overnightRepository.observeForfeitures(), ::OvernightData),
        combine(passRepository.observeAll(), syncStateRepository.observe(), ::PassAndSyncData),
    ) { profile, leaveData, combatRestData, overnightData, passAndSync ->
        buildUiState(profile, leaveData, combatRestData, overnightData, passAndSync)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private data class LeaveData(val types: List<LeaveType>, val grants: List<LeaveGrant>, val usages: List<LeaveUsage>)
    private data class CombatRestData(val grants: List<CombatRestGrant>, val usages: List<CombatRestUsage>)
    private data class OvernightData(val records: List<OvernightRecord>, val forfeitures: List<OvernightForfeiture>)
    private data class PassAndSyncData(val records: List<PassRecord>, val syncState: SyncState)

    private fun buildUiState(
        profile: UserProfile?,
        leaveData: LeaveData,
        combatRestData: CombatRestData,
        overnightData: OvernightData,
        passAndSync: PassAndSyncData,
    ): DashboardUiState {
        val today = AppClock.today()

        val overnightSchedule = profile?.firstOvernightDate?.let { first ->
            OvernightScheduleCalculator.buildSchedule(
                firstOvernightDate = first,
                cycleWeeks = profile.overnightCycleWeeks,
                records = overnightData.records,
                forfeitures = overnightData.forfeitures,
            )
        }

        val leaveSummaries = leaveData.types.map { type ->
            val s = LeaveCalculator.summarize(
                type, leaveData.grants, leaveData.usages, AppClock.today(),
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

        val nextOuting = OutingPeriodCalculator.next(
            today = today,
            leaveUsages = leaveData.usages,
            leaveTypes = leaveData.types,
            overnightRecords = overnightData.records,
            nextOvernightDate = overnightSchedule?.nextScheduledDate,
        )
        val nextPass = passAndSync.records.map { it.date }.filter { it >= today }.minOrNull()

        return DashboardUiState(
            isLoading = false,
            profile = profile,
            today = today,
            dischargeDDay = profile?.let { DDayCalculator.dDay(today, it.dischargeDate) },
            serviceProgressPercent = profile
                ?.let { DDayCalculator.serviceProgress(it.enlistmentDate, it.dischargeDate, today) }
                ?: 0.0,
            promotionMarkers = profile?.promotionDates?.entries.orEmpty().map { (rank, date) ->
                PromotionMarker(
                    label = rank.label,
                    progressPercent = DDayCalculator.promotionProgress(profile!!.enlistmentDate, profile.dischargeDate, date),
                    passed = date < today,
                )
            },
            nextPromotion = profile?.let {
                ServiceScheduleCalculator.nextPromotion(it.promotionDates, today)?.let { (rank, date) ->
                    NextPromotion(
                        label = rank.label,
                        dDay = DDayCalculator.dDay(today, date),
                        progressPercent = ServiceScheduleCalculator.progressToNextPromotion(
                            it.enlistmentDate, it.promotionDates, date, today,
                        ),
                    )
                }
            },
            leaveSummaries = leaveSummaries,
            combatRestSummary = combatRestSummary,
            nextOuting = nextOuting,
            nextPass = nextPass,
            syncState = passAndSync.syncState,
        )
    }
}
