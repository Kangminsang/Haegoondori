package com.kangminsang.hagoondori

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.kangminsang.hagoondori.ui.calendar.CalendarOpenRequest
import com.kangminsang.hagoondori.ui.nav.HagoondoriNavHost
import com.kangminsang.hagoondori.ui.theme.HagoondoriTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.datetime.LocalDate

/**
 * 앱의 유일한 액티비티. 화면 전환은 전부 Compose Navigation([HagoondoriNavHost])으로
 * 처리한다. 홈 화면 달력 위젯이 보낸 달력 열기 요청([EXTRA_OPEN_CALENDAR])도 여기서 받는다.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** 아직 화면이 처리하지 않은 달력 열기 요청. 처리되면 화면이 null로 되돌린다. */
    private val calendarRequest = mutableStateOf<CalendarOpenRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 회전 등으로 다시 만들어질 때는 같은 인텐트가 또 오므로, 처음 만들어질 때만 읽는다.
        if (savedInstanceState == null) {
            takeCalendarRequest(intent)?.let { calendarRequest.value = it }
        }
        // 앱은 항상 밝은 종이 톤이므로 시스템 다크 모드와 무관하게 시스템 바 아이콘을 어둡게 고정한다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            HagoondoriTheme {
                HagoondoriNavHost(
                    calendarRequest = calendarRequest.value,
                    onCalendarRequestHandled = { calendarRequest.value = null },
                )
            }
        }
    }

    /** 앱이 떠 있을 때 위젯을 누르면(singleTop) 새 화면을 쌓지 않고 여기로 온다. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takeCalendarRequest(intent)?.let { calendarRequest.value = it }
    }

    /**
     * 인텐트에서 달력 열기 요청을 꺼낸다. 꺼낸 뒤에는 extras를 지워, 같은 인텐트로 다시 만들어져도 반복되지 않게 한다.
     */
    private fun takeCalendarRequest(intent: Intent): CalendarOpenRequest? {
        if (!intent.getBooleanExtra(EXTRA_OPEN_CALENDAR, false)) return null
        val date = intent.getStringExtra(EXTRA_OPEN_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        intent.removeExtra(EXTRA_OPEN_CALENDAR)
        intent.removeExtra(EXTRA_OPEN_DATE)
        return CalendarOpenRequest(date)
    }

    companion object {
        /** 달력 탭을 열라는 표시(boolean). 위젯의 날짜 칸과 달력 제목 탭이 붙인다. */
        const val EXTRA_OPEN_CALENDAR = "com.kangminsang.hagoondori.extra.OPEN_CALENDAR"

        /** 함께 고를 날짜(ISO 문자열, 예: 2026-11-08). 없으면 달만 연다. */
        const val EXTRA_OPEN_DATE = "com.kangminsang.hagoondori.extra.OPEN_DATE"
    }
}
