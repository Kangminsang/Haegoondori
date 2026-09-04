package com.kangminsang.hagoondori.ui.leave

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
 * ③ 출타 관리 (스펙 5.1/5.2절, F7~F14) - 휴가/전투휴무/외박/외출 4개 탭.
 * 포상 상한 경고, 전투휴무 전환분·부여분 분리 표시, 외박 차수 현황(지연/선행/소멸),
 * 외출 월별 사용 현황을 모두 여기서 다룬다.
 *
 * 지금은 네비게이션 뼈대 단계의 placeholder다.
 */
@Composable
fun LeaveManagementScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text("출타 관리 — 준비 중", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
