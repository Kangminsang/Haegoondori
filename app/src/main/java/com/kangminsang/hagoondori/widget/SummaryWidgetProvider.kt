package com.kangminsang.hagoondori.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver.PendingResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kangminsang.hagoondori.core.widget.WidgetSummary
import com.kangminsang.hagoondori.core.widget.WidgetSummaryBuilder
import com.kangminsang.hagoondori.data.repository.DutyRepository
import com.kangminsang.hagoondori.data.repository.LeaveRepository
import com.kangminsang.hagoondori.data.repository.OvernightRepository
import com.kangminsang.hagoondori.data.repository.PassRepository
import com.kangminsang.hagoondori.data.repository.ProfileRepository
import com.kangminsang.hagoondori.util.AppClock
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 홈 화면 요약 위젯 제공자. 데이터는 Repository의 observe*().first()로 읽고, 계산은 core의
 * [WidgetSummaryBuilder]에 맡긴다. 읽기나 그리기에 실패하면 기록만 남기고 기존 화면을 그대로 둔다.
 *
 * 갱신은 시스템 액션(APPWIDGET_UPDATE, 시각·시간대 변경, 패키지 교체)과 앱 내부 액션
 * ([ACTION_REFRESH], [ACTION_MIDNIGHT]) 두 가지로 들어온다.
 */
@AndroidEntryPoint
class SummaryWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var leaveRepository: LeaveRepository
    @Inject lateinit var overnightRepository: OvernightRepository
    @Inject lateinit var passRepository: PassRepository
    @Inject lateinit var dutyRepository: DutyRepository
    @Inject lateinit var widgetRenderer: WidgetRenderer
    @Inject lateinit var widgetRefresher: WidgetRefresher

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        widgetRefresher.scheduleMidnight()
        renderAsync(context, goAsync(), appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 먼저 부모를 호출해 Hilt 주입과 시스템 위젯 액션(APPWIDGET_UPDATE → onUpdate 등)을 처리한다.
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_MIDNIGHT,
            ACTION_REFRESH,
            Intent.ACTION_TIME_CHANGED, // android.intent.action.TIME_SET (시계 수동 변경)
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // 앱 업데이트나 강제 종료로 알람이 사라질 수 있어 갱신할 때마다 다시 건다. 같은 알람 하나만 남는다.
                widgetRefresher.scheduleMidnight()
                renderAsync(context, goAsync(), null)
            }
        }
    }

    override fun onDisabled(context: Context) {
        // 달력 위젯이 남아 있으면 자정 알람은 그대로 둔다.
        widgetRefresher.cancelMidnightIfNoWidgets()
    }

    /** [appWidgetIds]가 null이면 이 제공자의 위젯 전부를 그린다. */
    private fun renderAsync(context: Context, pending: PendingResult, appWidgetIds: IntArray?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = appWidgetIds
                    ?: manager.getAppWidgetIds(ComponentName(context, SummaryWidgetProvider::class.java))
                if (ids.isEmpty()) return@launch
                val summary = loadSummary()
                manager.updateAppWidget(ids, widgetRenderer.render(summary))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "요약 위젯 갱신에 실패해 이전 화면을 유지합니다", e)
            } finally {
                pending.finish()
            }
        }
    }

    /** 프로필이 없으면 null(안내 화면). 있으면 대시보드와 같은 입력으로 요약을 만든다. */
    private suspend fun loadSummary(): WidgetSummary? {
        val profile = profileRepository.observe().first() ?: return null
        return WidgetSummaryBuilder.build(
            profile = profile,
            today = AppClock.today(),
            leaveTypes = leaveRepository.observeTypes().first(),
            leaveGrants = leaveRepository.observeAllGrants().first(),
            leaveUsages = leaveRepository.observeAllUsages().first(),
            overnightRecords = overnightRepository.observeRecords().first(),
            overnightForfeitures = overnightRepository.observeForfeitures().first(),
            passRecords = passRepository.observeAll().first(),
            duties = dutyRepository.observeAll().first(),
        )
    }

    companion object {
        const val ACTION_REFRESH = WidgetActions.REFRESH

        /**
         * 이전 버전이 이 제공자로 걸어 둔 자정 알람이 한 번 울려도 처리하려고 남겨 둔다. 지금은 [WidgetMidnightReceiver]가 보낸다.
         */
        const val ACTION_MIDNIGHT = WidgetActions.MIDNIGHT
        private const val TAG = "SummaryWidget"
    }
}
