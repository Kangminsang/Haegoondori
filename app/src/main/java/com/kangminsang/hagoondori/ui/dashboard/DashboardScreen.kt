package com.kangminsang.hagoondori.ui.dashboard

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
 * ① 대시보드 (스펙 5.1/5.2절, F2) - 앱을 여는 주된 이유.
 *
 * 지금은 네비게이션 뼈대 단계의 placeholder다. 실제 내용(D-day/진행률 게이지,
 * 휴가·전투휴무 요약, 다음 외박 D-day, 이번 달 외출 잔여, 동기화 상태 배너)은
 * DashboardViewModel과 함께 다음 단계에서 채운다.
 */
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text("대시보드 — 준비 중", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
