package com.kangminsang.hagoondori.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.UserProfile
import com.kangminsang.hagoondori.data.remote.holiday.HolidayFetchResult
import com.kangminsang.hagoondori.data.remote.holiday.HolidayRemoteRepository
import com.kangminsang.hagoondori.data.repository.HolidayRepository
import com.kangminsang.hagoondori.data.repository.ProfileRepository
import com.kangminsang.hagoondori.data.repository.SyncStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 설정(F1/F15) 상태 결합 및 저장/공휴일 갱신 처리. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val holidayRepository: HolidayRepository,
    private val holidayRemoteRepository: HolidayRemoteRepository,
    private val syncStateRepository: SyncStateRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        profileRepository.observe(),
        holidayRepository.observeAll(),
        syncStateRepository.observe(),
    ) { profile, holidays, syncState ->
        SettingsUiState(profile = profile, holidays = holidays.sortedBy { it.date }, syncState = syncState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch { profileRepository.save(profile) }
    }

    /** 공휴일 갱신(F15) - 올해/내년을 API로 시도하고, 실패하면 자동으로 내장 데이터로 대체된다. */
    fun refreshHolidays(onResult: (List<HolidayFetchResult>) -> Unit) {
        viewModelScope.launch { onResult(holidayRemoteRepository.refreshCurrentAndNextYear()) }
    }

    /** 공휴일 확보 우선순위의 최후 수단: 사용자 수동 입력(4.14절). */
    fun addManualHoliday(holiday: Holiday) {
        viewModelScope.launch { holidayRepository.addManual(holiday) }
    }

    fun deleteHoliday(holiday: Holiday) {
        viewModelScope.launch { holidayRepository.delete(holiday) }
    }
}
