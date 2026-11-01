package com.kangminsang.hagoondori.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver.PendingResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.kangminsang.hagoondori.MainActivity
import com.kangminsang.hagoondori.R
import com.kangminsang.hagoondori.core.calc.DDayCalculator
import com.kangminsang.hagoondori.core.widget.WidgetMonthGrid
import com.kangminsang.hagoondori.core.widget.WidgetText
import com.kangminsang.hagoondori.data.repository.CombatRestRepository
import com.kangminsang.hagoondori.data.repository.DutyRepository
import com.kangminsang.hagoondori.data.repository.EventRepository
import com.kangminsang.hagoondori.data.repository.HolidayRepository
import com.kangminsang.hagoondori.data.repository.LeaveRepository
import com.kangminsang.hagoondori.data.repository.OvernightRepository
import com.kangminsang.hagoondori.data.repository.PassRepository
import com.kangminsang.hagoondori.data.repository.ProfileRepository
import com.kangminsang.hagoondori.ui.calendar.CalendarDayBuilder
import com.kangminsang.hagoondori.ui.calendar.CalendarSource
import com.kangminsang.hagoondori.util.AppClock
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import javax.inject.Inject

/**
 * 홈 화면 달력 위젯 제공자. 달력은 비트맵 한 장([CalendarWidgetBitmap])으로 그리고, 그 위에 투명 터치 영역을 얹는다.
 * 위젯마다 크기와 보고 있는 달이 다르므로 위젯별로 따로 그린다.
 *
 * - 보고 있는 달은 위젯 id별로 SharedPreferences(`widget_calendar`, `yyyy-MM`)에 둔다. ◀ ▶는 그 위젯의 달만 바꾼다.
 * - 자정 액션([WidgetActions.MIDNIGHT])이 오면 저장한 달을 모두 지워 이번 달로 돌아간다. 앱 재시작으로는 지우지 않는다.
 * - 날짜 칸과 달력 제목을 누르면 앱의 달력 탭을 연다([MainActivity.EXTRA_OPEN_CALENDAR]).
 *
 * 데이터는 Repository의 observe*().first()로 읽고, 계산은 앱 달력과 같은 [CalendarDayBuilder]에 맡긴다.
 * 읽기나 그리기에 실패하면 기록만 남기고 기존 화면을 그대로 둔다.
 */
@AndroidEntryPoint
class CalendarWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var eventRepository: EventRepository
    @Inject lateinit var leaveRepository: LeaveRepository
    @Inject lateinit var combatRestRepository: CombatRestRepository
    @Inject lateinit var overnightRepository: OvernightRepository
    @Inject lateinit var passRepository: PassRepository
    @Inject lateinit var dutyRepository: DutyRepository
    @Inject lateinit var holidayRepository: HolidayRepository
    @Inject lateinit var calendarBitmap: CalendarWidgetBitmap
    @Inject lateinit var widgetRefresher: WidgetRefresher

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        widgetRefresher.scheduleMidnight()
        renderAsync(context, goAsync(), appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        // 크기가 바뀌면 비트맵 크기도 바뀌어야 하므로 이 위젯만 다시 그린다.
        renderAsync(context, goAsync(), intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 먼저 부모를 호출해 Hilt 주입과 시스템 위젯 액션(APPWIDGET_UPDATE → onUpdate 등)을 처리한다.
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PREV, ACTION_NEXT -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
                shiftMonth(context, widgetId, if (intent.action == ACTION_NEXT) 1 else -1)
                renderAsync(context, goAsync(), intArrayOf(widgetId))
            }
            WidgetActions.MIDNIGHT -> {
                // 자정이 지나면 저장한 달을 모두 지워 모든 위젯이 이번 달로 돌아간다. 알람은 WidgetMidnightReceiver가 다시 건다.
                prefs(context).edit().clear().apply()
                renderAsync(context, goAsync(), null)
            }
            WidgetActions.REFRESH,
            Intent.ACTION_TIME_CHANGED, // android.intent.action.TIME_SET (시계 수동 변경)
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // 앱 업데이트나 강제 종료로 알람이 사라질 수 있어 갱신할 때마다 다시 건다. 같은 알람 하나만 남는다.
                widgetRefresher.scheduleMidnight()
                renderAsync(context, goAsync(), null)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val editor = prefs(context).edit()
        appWidgetIds.forEach { editor.remove(monthKey(it)) }
        editor.apply()
    }

    override fun onDisabled(context: Context) {
        // 요약 위젯이 남아 있으면 자정 알람은 그대로 둔다.
        widgetRefresher.cancelMidnightIfNoWidgets()
    }

    /** [appWidgetIds]가 null이면 이 제공자의 위젯 전부를 그린다. */
    private fun renderAsync(context: Context, pending: PendingResult, appWidgetIds: IntArray?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = appWidgetIds
                    ?: manager.getAppWidgetIds(ComponentName(context, CalendarWidgetProvider::class.java))
                if (ids.isEmpty()) return@launch
                val today = AppClock.today()
                val source = loadSource()
                val dDayText = profileRepository.observe().first()?.let {
                    WidgetText.formatDDay(DDayCalculator.dDay(today, it.dischargeDate))
                }
                for (widgetId in ids) {
                    try {
                        drawWidget(context, manager, widgetId, source, dDayText, today)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // 한 위젯이 실패해도 나머지는 계속 그린다. 실패한 위젯은 이전 화면을 유지한다.
                        Log.w(TAG, "달력 위젯($widgetId) 갱신에 실패해 이전 화면을 유지합니다", e)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "달력 위젯 데이터를 읽지 못해 이전 화면을 유지합니다", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun drawWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        source: CalendarSource,
        dDayText: String?,
        today: LocalDate,
    ) {
        val month = shownMonth(context, widgetId, today)
        val model = CalendarWidgetModel(
            year = month.year,
            month = month.monthNumber,
            today = today,
            days = CalendarDayBuilder.build(month.year, month.monthNumber, source).associateBy { it.date },
            dDayText = dDayText,
        )
        val (widthDp, heightDp) = widgetSizeDp(context, manager, widgetId)
        val bitmap = calendarBitmap.draw(model, widthDp, heightDp)
        manager.updateAppWidget(widgetId, buildViews(context, widgetId, bitmap, month))
    }

    /** 앱 달력과 같은 원본 목록을 한 번 읽는다. 위젯 여러 개가 같은 목록을 쓴다. */
    private suspend fun loadSource(): CalendarSource = CalendarSource(
        events = eventRepository.observeAll().first(),
        duties = dutyRepository.observeAll().first(),
        holidays = holidayRepository.observeAll().first(),
        leaveUsages = leaveRepository.observeAllUsages().first(),
        leaveTypes = leaveRepository.observeTypes().first(),
        combatRestUsages = combatRestRepository.observeUsages().first(),
        overnightRecords = overnightRepository.observeRecords().first(),
        passRecords = passRepository.observeAll().first(),
    )

    /**
     * 위젯 크기(dp). 세로 방향이면 폭은 최소, 높이는 최대를, 가로 방향이면 폭은 최대, 높이는 최소를 쓴다.
     * 옵션 값이 0 이하이면 그 치수만 기본값(320×440dp)을 쓴다.
     */
    private fun widgetSizeDp(context: Context, manager: AppWidgetManager, widgetId: Int): Pair<Float, Float> {
        val options = manager.getAppWidgetOptions(widgetId)
        val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        val (widthKey, heightKey) = if (portrait) {
            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH to AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
        } else {
            AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH to AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
        }
        val width = options.getInt(widthKey).takeIf { it > 0 } ?: DEFAULT_WIDTH_DP
        val height = options.getInt(heightKey).takeIf { it > 0 } ?: DEFAULT_HEIGHT_DP
        return width.toFloat() to height.toFloat()
    }

    /** 달력 위젯 RemoteViews. 달력 그림은 ImageView 한 장이고, 칸은 날짜가 있는 칸에만 클릭을 건다. */
    private fun buildViews(context: Context, widgetId: Int, bitmap: Bitmap, month: LocalDate): RemoteViews {
        val packageName = context.packageName
        val leading = WidgetMonthGrid.leadingBlanks(month.year, month.monthNumber)
        val daysInMonth = WidgetMonthGrid.daysInMonth(month.year, month.monthNumber)
        val weeks = WidgetMonthGrid.weekCount(month.year, month.monthNumber)
        return RemoteViews(packageName, R.layout.widget_calendar).apply {
            setImageViewBitmap(R.id.calendar_image, bitmap)
            setContentDescription(R.id.calendar_image, "${month.year}년 ${month.monthNumber}월 달력")
            setOnClickPendingIntent(R.id.nav_prev, broadcast(context, widgetId, ACTION_PREV, "prev"))
            setOnClickPendingIntent(R.id.nav_next, broadcast(context, widgetId, ACTION_NEXT, "next"))
            setOnClickPendingIntent(R.id.nav_title, openCalendar(context, widgetId, date = null, tag = "title"))
            WEEK_IDS.forEachIndexed { week, weekId ->
                removeAllViews(weekId)
                setViewVisibility(weekId, if (week < weeks) View.VISIBLE else View.GONE)
                if (week >= weeks) return@forEachIndexed
                for (column in 0 until WidgetMonthGrid.COLUMNS) {
                    val day = week * WidgetMonthGrid.COLUMNS + column - leading + 1
                    val cell = RemoteViews(packageName, R.layout.widget_calendar_cell)
                    // 앞뒤 달 칸과 빈 칸에는 클릭을 걸지 않는다.
                    if (day in 1..daysInMonth) {
                        val date = LocalDate(month.year, month.monthNumber, day)
                        cell.setOnClickPendingIntent(R.id.cell_root, openCalendar(context, widgetId, date, tag = "day/$date"))
                    }
                    addView(weekId, cell)
                }
            }
        }
    }

    private fun broadcast(context: Context, widgetId: Int, action: String, tag: String): PendingIntent {
        val intent = Intent(context, CalendarWidgetProvider::class.java)
            .setAction(action)
            .setData(widgetUri(widgetId, tag))
            .putExtra(EXTRA_WIDGET_ID, widgetId)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** 앱의 달력 탭을 연다. [date]가 있으면 그 날짜의 달을 보이고 날짜를 고른다. */
    private fun openCalendar(context: Context, widgetId: Int, date: LocalDate?, tag: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setData(widgetUri(widgetId, tag))
            .putExtra(MainActivity.EXTRA_OPEN_CALENDAR, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (date != null) intent.putExtra(MainActivity.EXTRA_OPEN_DATE, date.toString())
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * PendingIntent를 구분하는 Uri. 위젯 id·동작·날짜([tag]에 포함)가 달라야 같은 Intent로 뭉치지 않는다.
     */
    private fun widgetUri(widgetId: Int, tag: String): Uri =
        Uri.parse("hagoondori://widget/calendar/$widgetId/$tag")

    private fun shownMonth(context: Context, widgetId: Int, today: LocalDate): LocalDate {
        val stored = prefs(context).getString(monthKey(widgetId), null)
        return parseMonth(stored) ?: LocalDate(today.year, today.monthNumber, 1)
    }

    private fun shiftMonth(context: Context, widgetId: Int, delta: Int) {
        val current = shownMonth(context, widgetId, AppClock.today())
        val next = current.plus(delta, DateTimeUnit.MONTH)
        prefs(context).edit().putString(monthKey(widgetId), formatMonth(next)).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun monthKey(widgetId: Int): String = "month_$widgetId"

    companion object {
        const val ACTION_PREV = "com.kangminsang.hagoondori.widget.action.CALENDAR_PREV"
        const val ACTION_NEXT = "com.kangminsang.hagoondori.widget.action.CALENDAR_NEXT"
        const val EXTRA_WIDGET_ID = "com.kangminsang.hagoondori.widget.extra.WIDGET_ID"

        private const val PREFS_NAME = "widget_calendar"
        private const val TAG = "CalendarWidget"
        private const val DEFAULT_WIDTH_DP = 320
        private const val DEFAULT_HEIGHT_DP = 440

        /** 주 행 id. 6주까지 두고 쓰지 않는 행은 GONE으로 숨긴다. */
        private val WEEK_IDS = intArrayOf(R.id.week_1, R.id.week_2, R.id.week_3, R.id.week_4, R.id.week_5, R.id.week_6)

        /** `yyyy-MM` 문자열을 그 달의 1일로 바꾼다. 형식이 틀리면 null. */
        internal fun parseMonth(value: String?): LocalDate? {
            val match = value?.let { Regex("""(\d{4})-(\d{2})""").matchEntire(it) } ?: return null
            return runCatching {
                LocalDate(match.groupValues[1].toInt(), match.groupValues[2].toInt(), 1)
            }.getOrNull()
        }

        internal fun formatMonth(date: LocalDate): String =
            "%04d-%02d".format(date.year, date.monthNumber)
    }
}
