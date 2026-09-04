package com.kangminsang.hagoondori.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kangminsang.hagoondori.core.export.ExportResult
import kotlinx.coroutines.launch

/**
 * 장치 미리보기 화면(F16, 스펙 5.3절) - 장치와 같은 비율(800×480), 같은 규칙으로
 * 그려서 "보내기 전에 확인"할 수 있게 한다. 전체 갱신 26초가 걸리는 장치의 특성상
 * 사실상 필수 기능이다(5.3절).
 */
@Composable
fun DevicePreviewScreen(
    modifier: Modifier = Modifier,
    viewModel: DevicePreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("낮 테마", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = uiState.isNightTheme, onCheckedChange = { viewModel.toggleTheme() })
                Text("밤 테마", style = MaterialTheme.typography.bodyMedium)
            }

            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                when {
                    uiState.isLoading -> CircularProgressIndicator()
                    uiState.snapshot == null -> Text(
                        "복무 정보가 없어 미리볼 수 없습니다. 설정에서 먼저 입력해 주세요.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    else -> DevicePreviewRenderer(
                        snapshot = uiState.snapshot!!,
                        isNightTheme = uiState.isNightTheme,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.refresh() }) { Text("새로고침") }
                Button(onClick = {
                    viewModel.exportToPreviewFile { result ->
                        scope.launch {
                            val message = when (result) {
                                is ExportResult.Success -> "미리보기 파일로 저장했습니다 (앱 전용 저장소/calendar_preview)"
                                is ExportResult.Failure -> "저장 실패: ${result.message}"
                            }
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                }) { Text("페이로드 파일로 저장") }
            }

            Text(
                "이 화면은 장치 펌웨어의 근사치입니다 - 실제 픽셀 배치는 장치가 최종 결정합니다.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
