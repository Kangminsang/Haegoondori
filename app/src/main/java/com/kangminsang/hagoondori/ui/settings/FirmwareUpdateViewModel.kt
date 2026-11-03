package com.kangminsang.hagoondori.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangminsang.hagoondori.firmware.DeviceFirmwareUpdater
import com.kangminsang.hagoondori.firmware.FirmwareSource
import com.kangminsang.hagoondori.firmware.FirmwareSources
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FirmwareUpdateState {
    data object Idle : FirmwareUpdateState
    data class Confirm(val source: FirmwareSource) : FirmwareUpdateState
    data class Running(val step: DeviceFirmwareUpdater.Step) : FirmwareUpdateState
    data class Finished(val message: String) : FirmwareUpdateState
    data class Failed(val message: String) : FirmwareUpdateState
}

/** 설정의 "장치 펌웨어" 카드: 펌웨어 고르기 → 확인 → 업데이트 진행. */
@HiltViewModel
class FirmwareUpdateViewModel @Inject constructor(
    private val sources: FirmwareSources,
    private val updater: DeviceFirmwareUpdater,
) : ViewModel() {
    private val _state = MutableStateFlow<FirmwareUpdateState>(FirmwareUpdateState.Idle)
    val state: StateFlow<FirmwareUpdateState> = _state.asStateFlow()

    private val _bundledLabel = MutableStateFlow<String?>(null)
    val bundledLabel: StateFlow<String?> = _bundledLabel.asStateFlow()

    init {
        viewModelScope.launch { _bundledLabel.value = runCatching { sources.bundled().label }.getOrNull() }
    }

    fun chooseBundled() = choose { sources.bundled() }

    fun chooseFile(uri: Uri) = choose { sources.fromFile(uri) }

    fun start() {
        val source = (_state.value as? FirmwareUpdateState.Confirm)?.source ?: return
        _state.value = FirmwareUpdateState.Running(DeviceFirmwareUpdater.Step.FindingDevice)
        viewModelScope.launch {
            _state.value = try {
                val changed = updater.update(source.image) { _state.value = FirmwareUpdateState.Running(it) }
                FirmwareUpdateState.Finished(
                    if (changed == 0) "장치에 이미 같은 펌웨어가 있습니다. 장치를 다시 시작했습니다."
                    else "펌웨어를 업데이트했습니다. 장치가 다시 시작됩니다.",
                )
            } catch (e: Exception) {
                FirmwareUpdateState.Failed(
                    (e.message ?: "알 수 없는 오류") +
                        "\n\n장치가 업데이트 모드에 남아 있으면 다시 시도할 때 이어서 진행합니다. " +
                        "계속 실패하면 장치의 RESET 버튼을 눌러 주세요.",
                )
            }
        }
    }

    fun dismiss() {
        if (_state.value !is FirmwareUpdateState.Running) _state.value = FirmwareUpdateState.Idle
    }

    private fun choose(load: suspend () -> FirmwareSource) {
        if (_state.value is FirmwareUpdateState.Running) return
        viewModelScope.launch {
            _state.value = try {
                FirmwareUpdateState.Confirm(load())
            } catch (e: Exception) {
                FirmwareUpdateState.Failed(e.message ?: "펌웨어 파일을 읽지 못했습니다")
            }
        }
    }
}
