package com.kangminsang.hagoondori.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kangminsang.hagoondori.core.firmware.FirmwareFlasher
import com.kangminsang.hagoondori.firmware.DeviceFirmwareUpdater.Step
import com.kangminsang.hagoondori.ui.common.Button
import com.kangminsang.hagoondori.ui.common.Card

/**
 * 장치 펌웨어 - USB로 연결된 장치의 펌웨어를 앱에 들어 있는 것 또는 고른 UF2 파일로 바꾼다.
 * 장치의 일정 데이터(파일시스템)는 건드리지 않는다.
 */
@Composable
fun FirmwareSection(modifier: Modifier = Modifier, viewModel: FirmwareUpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bundledLabel by viewModel.bundledLabel.collectAsStateWithLifecycle()
    val running = state is FirmwareUpdateState.Running
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.chooseFile(uri)
    }

    // 쓰는 도중 화면이 꺼져도 작업은 계속되지만, 진행 상황을 볼 수 있게 켜 둔다.
    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("장치 펌웨어", style = MaterialTheme.typography.titleMedium)
            Text(
                "USB로 연결된 장치의 펌웨어를 바꿉니다. 장치의 일정 데이터는 지워지지 않습니다.\n" +
                    "앱에 들어 있는 펌웨어: ${bundledLabel ?: "없음"}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            Button(enabled = !running && bundledLabel != null, onClick = viewModel::chooseBundled) {
                Text("펌웨어 업데이트")
            }
            OutlinedButton(
                enabled = !running,
                onClick = { fileLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("UF2 파일에서 선택") }
        }
    }

    when (val s = state) {
        FirmwareUpdateState.Idle -> Unit
        is FirmwareUpdateState.Confirm -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("장치 펌웨어 업데이트") },
            text = {
                Text(
                    "${s.source.label}\nSHA-256 ${s.source.image.sha256.take(8)}… · ${s.source.image.sizeBytes / 1024}KB\n\n" +
                        "• 장치를 USB로 연결해 두세요. 다른 USB 시리얼 앱은 닫아 주세요.\n" +
                        "• 'USB 드라이브를 초기화' 창이 뜨면 취소를 누르세요. 초기화하지 마세요.\n" +
                        "• USB 접근 허용 창이 두 번 뜰 수 있습니다.\n" +
                        "• 끝날 때까지 케이블을 빼지 마세요.",
                )
            },
            confirmButton = { TextButton(onClick = viewModel::start) { Text("시작") } },
            dismissButton = { TextButton(onClick = viewModel::dismiss) { Text("취소") } },
        )
        is FirmwareUpdateState.Running -> AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("펌웨어 업데이트 중") },
            text = {
                Column {
                    Text(stepText(s.step))
                    val p = (s.step as? Step.Flashing)?.progress
                    if (p != null && p.total > 0) {
                        LinearProgressIndicator(
                            progress = { p.done.toFloat() / p.total },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                    }
                    Text("케이블을 빼지 마세요", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {},
        )
        is FirmwareUpdateState.Finished -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("완료") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = viewModel::dismiss) { Text("확인") } },
        )
        is FirmwareUpdateState.Failed -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("펌웨어 업데이트 실패") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = viewModel::dismiss) { Text("확인") } },
        )
    }
}

private fun stepText(step: Step): String = when (step) {
    Step.FindingDevice -> "장치를 찾는 중"
    Step.RequestingPermission -> "USB 접근을 허용해 주세요"
    Step.EnteringBootloader -> "장치를 업데이트 모드로 바꾸는 중 (화면을 그리는 중이면 30초쯤 걸립니다)"
    Step.WaitingForBootloader -> "업데이트 모드 장치를 기다리는 중"
    Step.Rebooting -> "장치를 다시 시작하는 중"
    is Step.Flashing -> when (step.progress.phase) {
        FirmwareFlasher.Phase.Comparing -> "현재 펌웨어와 비교하는 중"
        FirmwareFlasher.Phase.Writing -> "쓰는 중 (${step.progress.done}/${step.progress.total})"
        FirmwareFlasher.Phase.Verifying -> "검증하는 중"
    }
}
