package com.kangminsang.hagoondori

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        enableEdgeToEdge()
        setContent {
            HagoondoriTheme {
                HagoondoriNavHost()
            }
        }
    }
}
