package com.kangminsang.hagoondori.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.kangminsang.hagoondori.ui.common.Button
import com.kangminsang.hagoondori.ui.common.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kangminsang.hagoondori.core.export.ExportResult
import com.kangminsang.hagoondori.export.DeviceFolderCheck
import com.kangminsang.hagoondori.ui.common.SyncStatusBanner
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.coroutines.launch

/**
 * ⑤ 설정 (스펙 5.1/5.2절, F1/F15/F17) - 복무 정보, 규정 수치, 공휴일 갱신,
 * 동기화 실행이 모두 이 화면에서 이루어진다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    /** true가 되면 "장치 연동" 카드가 보이도록 스크롤한 뒤 [onScrollHandled]로 알린다(대시보드 배너에서 넘어온 경우). */
    scrollToDeviceSection: Boolean = false,
    onScrollHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var hasHolidayApiKey by remember { mutableStateOf(viewModel.hasHolidayApiKey()) }
    var isFolderSelected by remember { mutableStateOf(viewModel.isDeviceFolderSelected()) }
    val deviceSectionRequester = remember { BringIntoViewRequester() }
    var scrollRequest by remember { mutableStateOf(0) }

    LaunchedEffect(scrollToDeviceSection) {
        if (scrollToDeviceSection) {
            scrollRequest++
            onScrollHandled()
        }
    }
    // 레이아웃이 끝난 뒤에 스크롤해야 카드 위치가 잡힌다.
    LaunchedEffect(scrollRequest) {
        if (scrollRequest > 0) {
            withFrameNanos { }
            deviceSectionRequester.bringIntoView()
        }
    }

    val openTreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            when (val check = viewModel.onDeviceFolderSelected(uri)) {
                is DeviceFolderCheck.Result.Ok -> {
                    isFolderSelected = true
                    scope.launch { snackbarHostState.showSnackbar("장치 폴더를 선택했습니다") }
                }
                is DeviceFolderCheck.Result.Rejected ->
                    scope.launch { snackbarHostState.showSnackbar(check.message) }
            }
        }
    }

    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SyncStatusBanner(
                syncState = uiState.syncState,
                now = AppClock.now(),
                onSyncNow = {
                    if (!isFolderSelected) {
                        // 폴더 없이는 동기화할 수 없으니 폴더 선택 버튼이 있는 카드로 안내한다.
                        scrollRequest++
                        scope.launch { snackbarHostState.showSnackbar("먼저 장치 폴더를 선택해 주세요") }
                    } else {
                        viewModel.syncToDevice { result ->
                            scope.launch { snackbarHostState.showSnackbar(exportResultMessage(result)) }
                        }
                    }
                },
            )

            ProfileSection(profile = uiState.profile, onSave = viewModel::saveProfile, modifier = Modifier.fillMaxWidth())

            HolidaySection(
                holidays = uiState.holidays,
                onRefresh = viewModel::refreshHolidays,
                hasApiKey = hasHolidayApiKey,
                onSaveApiKey = { key -> viewModel.saveHolidayApiKey(key); hasHolidayApiKey = true },
                onClearApiKey = { viewModel.clearHolidayApiKey(); hasHolidayApiKey = false },
                onAddManual = viewModel::addManualHoliday,
                onDelete = viewModel::deleteHoliday,
                modifier = Modifier.fillMaxWidth(),
            )

            Card(modifier = Modifier.fillMaxWidth().bringIntoViewRequester(deviceSectionRequester)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("장치 연동", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (isFolderSelected) "장치 폴더가 연결되어 있습니다" else "아직 장치 폴더를 선택하지 않았습니다",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                    OutlinedButton(onClick = { openTreeLauncher.launch(null) }) {
                        Text(if (isFolderSelected) "장치 폴더 다시 선택" else "장치 폴더 선택")
                    }
                    Button(
                        enabled = isFolderSelected,
                        onClick = {
                            viewModel.syncToDevice { result ->
                                scope.launch { snackbarHostState.showSnackbar(exportResultMessage(result)) }
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text("지금 동기화") }
                }
            }
        }
    }
}

private fun exportResultMessage(result: ExportResult): String = when (result) {
    is ExportResult.Success -> "동기화를 완료했습니다"
    is ExportResult.Failure -> "동기화 실패: ${result.message}"
}
