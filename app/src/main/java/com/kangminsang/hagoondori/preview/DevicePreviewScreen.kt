package com.kangminsang.hagoondori.preview

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
 * 장치 미리보기 화면(F16, 스펙 5.3절) - 장치와 같은 비율(800×480), 같은 규칙으로
 * 그려서 "보내기 전에 확인"할 수 있게 한다. 전체 갱신 26초가 걸리는 장치의 특성상
 * 사실상 필수 기능이다.
 *
 * 지금은 네비게이션 뼈대 단계의 placeholder다. 실제 800×480 Canvas 렌더러
 * ([DevicePreviewRenderer])는 동기화 계층과 함께 나중에 채운다.
 */
@Composable
fun DevicePreviewScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text("장치 미리보기 — 준비 중", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
