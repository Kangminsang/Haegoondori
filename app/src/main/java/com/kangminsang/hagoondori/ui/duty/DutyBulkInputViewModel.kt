package com.kangminsang.hagoondori.ui.duty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.data.repository.DutyRepository
import com.kangminsang.hagoondori.data.repository.HolidayRepository
import com.kangminsang.hagoondori.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import javax.inject.Inject

/**
 * 근무 일괄 입력(F6). 당직표가 한 달치가 한 번에 나오는 근무 환경 특성상, 다중
 * 선택으로 한 번에 저장하는 것이 이 화면의 핵심이다(스펙 4.13절).
 *
 * 월 이동/날짜 선택/근무 종류 선택 같은 화면 전용 상태는 Composable에서
 * `remember`로 들고, 이 ViewModel은 저장에 필요한 데이터(복무 정보, 공휴일,
 * 기존 배정)와 저장 동작만 담당한다.
 */
@HiltViewModel
class DutyBulkInputViewModel @Inject constructor(
    private val dutyRepository: DutyRepository,
    private val profileRepository: ProfileRepository,
    private val holidayRepository: HolidayRepository,
) : ViewModel() {

    val uiState: StateFlow<DutyBulkInputUiState> = combine(
        profileRepository.observe(),
        holidayRepository.observeAll(),
        dutyRepository.observeAll(),
    ) { profile, holidays, assignments ->
        DutyBulkInputUiState(profile, holidays, assignments)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DutyBulkInputUiState())

    /**
     * [dates]를 [type]으로 일괄 저장한다. 이미 있는 `(date, type)` 조합은 조용히
     * 건너뛴다. [type]이 DUTY이고 [com.kangminsang.hagoondori.core.model.UserProfile.autoAddOffDuty]가
     * 켜져 있으면, 선택한 날짜들의 **다음 날**을 OFF_DUTY로도 자동 추가한다.
     *
     * @param onResult (저장된 건수, 건너뛴 건수)를 전달한다 - 화면이 이걸로 안내 문구를 만든다.
     */
    fun submit(dates: Set<LocalDate>, type: DutyType, onResult: (savedCount: Int, skippedCount: Int) -> Unit) {
        if (dates.isEmpty()) {
            onResult(0, 0)
            return
        }
        viewModelScope.launch {
            val primarySaved = dutyRepository.bulkAssign(dates, type)
            val primarySkipped = dates.size - primarySaved

            var autoAddedOffDutyCount = 0
            val profile = profileRepository.get()
            if (type == DutyType.DUTY && profile?.autoAddOffDuty == true) {
                val nextDayDates = dates.map { it.plus(1, DateTimeUnit.DAY) }
                autoAddedOffDutyCount = dutyRepository.bulkAssign(nextDayDates, DutyType.OFF_DUTY)
            }

            // 자동 추가된 비번은 저장 건수에는 합산하되, "건너뜀" 안내는 사용자가 직접
            // 고른 날짜(dates) 기준으로만 계산한다 - 자동 추가분의 건너뜀은 부수 효과일 뿐이다.
            onResult(primarySaved + autoAddedOffDutyCount, primarySkipped)
        }
    }
}
