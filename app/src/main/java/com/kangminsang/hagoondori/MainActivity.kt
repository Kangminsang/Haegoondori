package com.kangminsang.hagoondori

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.kangminsang.hagoondori.ui.nav.HagoondoriNavHost
import com.kangminsang.hagoondori.ui.theme.HagoondoriTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 앱의 유일한 액티비티. 화면 전환은 전부 Compose Navigation([HagoondoriNavHost])으로
 * 처리한다.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 앱은 항상 밝은 종이 톤이므로 시스템 다크 모드와 무관하게 시스템 바 아이콘을 어둡게 고정한다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            HagoondoriTheme {
                HagoondoriNavHost()
            }
        }
    }
}
