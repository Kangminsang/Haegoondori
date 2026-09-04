package com.kangminsang.hagoondori.ui.duty

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * ④ 근무 일괄 입력 (스펙 5.1/5.2절, F6) - 당직표를 다중 선택으로 한 번에 입력.
 * 당직표가 한 달치가 한 번에 나온다는 근무 환경 특성상, 이 화면의 입력 부담이
 * 사용성을 좌우한다(5.2절 F6 비고).
 *
 * 지금은 네비게이션 뼈대 단계의 placeholder다.
 */
@Composable
fun DutyBulkInputScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text("근무 일괄 입력 — 준비 중", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
