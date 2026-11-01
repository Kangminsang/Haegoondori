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
 * 요약 위젯을 다시 그리도록 요청한다. 데이터 저장소([com.kangminsang.hagoondori.data.repository.SyncStateRepository]
 * 등)에서 호출하므로 Repository에는 의존하지 않는다 - 읽기는 위젯 제공자가 직접 한다.
 *
 * - [refresh]: 위젯이 있을 때만 제공자에 명시적 브로드캐스트를 보낸다.
 * - [scheduleMidnight]/[cancelMidnight]: 다음 Asia/Seoul 자정에 한 번 깨워 날짜가 바뀐 D-day를 다시 계산한다.
 *   정확 알람 권한을 쓰지 않으므로 기기 절전 상태에서는 자정 몇 분 뒤에 올 수 있다.
 */
@Singleton
class WidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun refresh() {
        if (!hasWidgets()) return
        context.sendBroadcast(
            Intent(context, SummaryWidgetProvider::class.java).setAction(SummaryWidgetProvider.ACTION_REFRESH),
        )
    }

    /** 위젯이 있을 때만 다음 자정 알람을 건다. 같은 PendingIntent를 쓰므로 다시 불러도 한 개만 남는다. */
    fun scheduleMidnight() {
        if (!hasWidgets()) return
        val nextMidnight = AppClock.today().plus(1, DateTimeUnit.DAY).atStartOfDayIn(AppClock.timeZone)
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMidnight.toEpochMilliseconds(), midnightIntent())
    }

    fun cancelMidnight() {
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_MIDNIGHT,
            midnightIntentData(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pending)
        pending.cancel()
    }

    private fun hasWidgets(): Boolean =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, SummaryWidgetProvider::class.java))
            .isNotEmpty()

    private fun midnightIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_MIDNIGHT,
            midnightIntentData(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun midnightIntentData(): Intent =
        Intent(context, SummaryWidgetProvider::class.java).setAction(SummaryWidgetProvider.ACTION_MIDNIGHT)

    private companion object {
        const val REQUEST_MIDNIGHT = 1001
    }
}
