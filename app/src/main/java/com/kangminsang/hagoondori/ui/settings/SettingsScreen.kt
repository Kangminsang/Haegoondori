package com.kangminsang.hagoondori.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kangminsang.hagoondori.ui.common.SyncStatusBanner
import com.kangminsang.hagoondori.util.AppClock

/**
 * ⑤ 설정 (스펙 5.1/5.2절, F1/F15/F16/F17) - 복무 정보, 규정 수치, 공휴일 갱신,
 * 장치 미리보기 진입, 동기화 실행이 모두 이 화면에서 이루어진다.
 *
 * F17(동기화 실행)은 [com.kangminsang.hagoondori.export.SnapshotBuilder]/
 * `ExportAdapter`와 함께 다음 단계에서 채운다 - 지금은 상태 배너만 보여준다.
 */
@Composable
fun SettingsScreen(
    onOpenDevicePreview: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SyncStatusBanner(syncState = uiState.syncState, now = AppClock.now(), onSyncNow = {})

            ProfileSection(profile = uiState.profile, onSave = viewModel::saveProfile, modifier = Modifier.fillMaxWidth())

            HolidaySection(
                holidays = uiState.holidays,
                onRefresh = viewModel::refreshHolidays,
                onAddManual = viewModel::addManualHoliday,
                onDelete = viewModel::deleteHoliday,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("장치 연동", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onOpenDevicePreview) { Text("장치 미리보기") }
        }
    }
}
