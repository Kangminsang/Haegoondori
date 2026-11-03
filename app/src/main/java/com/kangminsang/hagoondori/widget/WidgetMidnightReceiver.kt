package com.kangminsang.hagoondori.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 자정 알람을 받는 수신자(앱 내부 전용, exported=false). 알람은 하나만 걸려 있으므로 여기서 다음 자정을 다시 걸고
 * 달력·요약 위젯 제공자에 자정 액션을 보낸다. 직접 그리지는 않는다.
 */
@AndroidEntryPoint
class WidgetMidnightReceiver : BroadcastReceiver() {

    @Inject lateinit var widgetRefresher: WidgetRefresher

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetActions.MIDNIGHT) return
        widgetRefresher.onMidnight()
    }
}
