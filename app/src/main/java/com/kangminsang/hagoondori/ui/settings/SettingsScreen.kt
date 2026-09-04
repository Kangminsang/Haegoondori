package com.kangminsang.hagoondori.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * ⑤ 설정 (스펙 5.1/5.2절, F1/F15/F16/F17) - 복무 정보, 규정 수치, 공휴일 갱신,
 * 장치 미리보기 진입, 동기화 실행이 모두 이 화면에서 이루어진다.
 *
 * 지금은 네비게이션 뼈대 단계의 placeholder다. [onOpenDevicePreview]만 먼저
 * 연결해 둔다 - 장치 미리보기(F16)로 가는 라우트가 존재한다는 것을 네비게이션
 * 그래프 차원에서 미리 확정해 두기 위함이다.
 */
@Composable
fun SettingsScreen(
    onOpenDevicePreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("설정 — 준비 중", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onOpenDevicePreview) {
                Text("장치 미리보기")
            }
        }
    }
}
