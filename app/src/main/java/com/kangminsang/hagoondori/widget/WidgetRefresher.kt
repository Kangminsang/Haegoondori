package com.kangminsang.hagoondori.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.kangminsang.hagoondori.util.AppClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 홈 화면 위젯(요약·달력)을 다시 그리도록 요청한다. 데이터 저장소([com.kangminsang.hagoondori.data.repository.SyncStateRepository]
 * 등)에서 호출하므로 Repository에는 의존하지 않는다 - 읽기는 위젯 제공자가 직접 한다.
 *
 * - [refresh]: 위젯이 있는 제공자마다 명시적 브로드캐스트를 보낸다.
 * - [scheduleMidnight]/[cancelMidnightIfNoWidgets]: 자정 알람은 하나만 쓴다. 알람이 울리면 [WidgetMidnightReceiver]가
 *   [onMidnight]를 호출해 다음 자정을 다시 걸고 두 제공자에 자정 액션을 보낸다.
 *   정확 알람 권한을 쓰지 않으므로 기기 절전 상태에서는 자정 몇 분 뒤에 올 수 있다.
 */
@Singleton
class WidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun refresh() {
        for (provider in PROVIDERS) {
            if (hasWidgets(provider)) {
                context.sendBroadcast(Intent(context, provider).setAction(WidgetActions.REFRESH))
            }
        }
    }

    /** 위젯이 있을 때만 다음 자정 알람을 건다. 같은 PendingIntent를 쓰므로 다시 불러도 한 개만 남는다. */
    fun scheduleMidnight() {
        if (!hasAnyWidget()) return
        val nextMidnight = AppClock.today().plus(1, DateTimeUnit.DAY).atStartOfDayIn(AppClock.timeZone)
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMidnight.toEpochMilliseconds(), midnightIntent())
    }

    /** 두 제공자에 남은 위젯이 하나도 없을 때만 자정 알람을 취소한다. 제공자의 onDisabled에서 부른다. */
    fun cancelMidnightIfNoWidgets() {
        if (hasAnyWidget()) return
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_MIDNIGHT,
            midnightIntentData(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pending)
        pending.cancel()
    }

    /** 자정 알람이 울렸을 때: 다음 자정을 다시 걸고, 두 제공자에 자정 액션을 보낸다. */
    fun onMidnight() {
        if (!hasAnyWidget()) return
        scheduleMidnight()
        for (provider in PROVIDERS) {
            context.sendBroadcast(Intent(context, provider).setAction(WidgetActions.MIDNIGHT))
        }
    }

    private fun hasAnyWidget(): Boolean = PROVIDERS.any { hasWidgets(it) }

    private fun hasWidgets(provider: Class<*>): Boolean =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, provider))
            .isNotEmpty()

    private fun midnightIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_MIDNIGHT,
            midnightIntentData(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun midnightIntentData(): Intent =
        Intent(context, WidgetMidnightReceiver::class.java).setAction(WidgetActions.MIDNIGHT)

    private companion object {
        const val REQUEST_MIDNIGHT = 1001

        /** 위젯 제공자 목록. 새 제공자를 만들면 여기에도 넣는다. */
        val PROVIDERS: List<Class<*>> = listOf(SummaryWidgetProvider::class.java, CalendarWidgetProvider::class.java)
    }
}

/** 위젯 제공자들이 같이 쓰는 앱 내부 액션. 시스템 액션은 각 제공자가 직접 받는다. */
object WidgetActions {
    const val REFRESH = "com.kangminsang.hagoondori.widget.action.REFRESH"
    const val MIDNIGHT = "com.kangminsang.hagoondori.widget.action.MIDNIGHT"
}
