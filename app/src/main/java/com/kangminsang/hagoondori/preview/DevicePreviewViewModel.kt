package com.kangminsang.hagoondori.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangminsang.hagoondori.core.export.CalendarSnapshot
import com.kangminsang.hagoondori.core.export.ExportAdapter
import com.kangminsang.hagoondori.core.export.ExportResult
import com.kangminsang.hagoondori.core.export.FailureReason
import com.kangminsang.hagoondori.di.FakeAdapter
import com.kangminsang.hagoondori.di.RealAdapter
import com.kangminsang.hagoondori.export.SnapshotBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevicePreviewUiState(
    val isLoading: Boolean = true,
    val snapshot: CalendarSnapshot? = null,
    val isNightTheme: Boolean = false,
)

/**
 * 장치 미리보기(F16) 상태. 대시보드/출타관리처럼 Repository Flow를 실시간
 * 구독하지 않고, 화면 진입 시 한 번 [SnapshotBuilder]로 스냅샷을 떠서 보여준다 -
 * 미리보기는 "지금 이 순간 내보내면 어떤 모습일지"를 보는 것이므로, 실시간
 * 반영보다는 명시적인 새로고침이 더 알맞다.
 */
@HiltViewModel
class DevicePreviewViewModel @Inject constructor(
    private val snapshotBuilder: SnapshotBuilder,
    @RealAdapter private val realExportAdapter: ExportAdapter,
    @FakeAdapter private val fakeExportAdapter: ExportAdapter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicePreviewUiState())
    val uiState: StateFlow<DevicePreviewUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val snapshot = snapshotBuilder.build()
            _uiState.value = _uiState.value.copy(isLoading = false, snapshot = snapshot)
        }
    }

    fun toggleTheme() {
        _uiState.value = _uiState.value.copy(isNightTheme = !_uiState.value.isNightTheme)
    }

    /** 실기기 없이 페이로드를 눈으로 확인하기 위해 앱 내부 저장소로 내보낸다. */
    fun exportToPreviewFile(onResult: (ExportResult) -> Unit) {
        val snapshot = _uiState.value.snapshot
        if (snapshot == null) {
            onResult(ExportResult.Failure(FailureReason.WRITE_FAILED, "복무 정보를 먼저 입력해 주세요"))
            return
        }
        onResult(fakeExportAdapter.export(snapshot))
    }

    fun isRealDeviceConnected(): Boolean = realExportAdapter.isDeviceConnected()
}
