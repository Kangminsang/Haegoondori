package com.kangminsang.hagoondori

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

/**
 * 앱의 유일한 액티비티. 화면 전환은 전부 Compose Navigation으로 처리한다
 * (실제 네비게이션 그래프는 ui/nav/HagoondoriNavHost.kt에서 붙인다).
 *
 * 지금은 스캐폴딩 단계 placeholder이며, DI/Room/Navigation이 갖춰지는 대로
 * `HagoondoriTheme { HagoondoriNavHost() }`로 교체된다.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                PlaceholderScreen()
            }
        }
    }
}

@Composable
private fun PlaceholderScreen() {
    Scaffold { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("하고운도리 — 뼈대 준비 중")
            }
        }
    }
}
