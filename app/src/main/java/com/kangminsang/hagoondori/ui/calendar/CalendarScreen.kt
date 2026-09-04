package com.kangminsang.hagoondori.ui.calendar

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
 * ② 달력 (스펙 5.1/5.2절, F3) - 월간 달력 + 날짜별 항목(휴가/전투휴무/외박/외출) 표시.
 *
 * 지금은 네비게이션 뼈대 단계의 placeholder다.
 */
@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text("달력 — 준비 중", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
