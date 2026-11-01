package com.kangminsang.hagoondori.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.kangminsang.hagoondori.MainActivity
import com.kangminsang.hagoondori.R
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.core.widget.WidgetSummary
import com.kangminsang.hagoondori.core.widget.WidgetText
import com.kangminsang.hagoondori.ui.duty.dutyTypeShortLabel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * [WidgetSummary]를 [RemoteViews]로 바꾼다. 크기별 레이아웃 셋을 크기 Map으로 묶어
 * 시스템이 실제 크기에 맞는 것을 고르게 한다. 요약이 없으면(프로필 미입력) 안내 레이아웃 하나만 쓴다.
 *
 * RemoteViews가 지원하는 뷰(FrameLayout, LinearLayout, TextView, ProgressBar, ImageView)만 쓰는
 * 레이아웃과 짝을 이룬다 - 레이아웃에 없는 id를 참조하면 크래시가 나므로 id는 각 레이아웃에서 확인한다.
 */
class WidgetRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val packageName: String = context.packageName

    /** 앱을 여는 PendingIntent. 위젯 전체를 누르면 이것이 실행된다. */
    private val openApp: PendingIntent by lazy {
        PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            // 홈 앱 아이콘과 같은 인텐트라, 앱이 떠 있으면 새 화면을 쌓지 않고 기존 작업으로 돌아간다.
            Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun render(summary: WidgetSummary?): RemoteViews {
        if (summary == null) {
            return RemoteViews(packageName, R.layout.widget_summary_empty).apply {
                setOnClickPendingIntent(R.id.widget_root, openApp)
            }
        }
        return RemoteViews(
            mapOf(
                SIZE_SMALL to small(summary),
                SIZE_WIDE to wide(summary),
                SIZE_LARGE to large(summary),
            ),
        )
    }

    private fun small(summary: WidgetSummary): RemoteViews =
        RemoteViews(packageName, R.layout.widget_summary_small).apply {
            bindTitle(summary)
            bindGauge(summary)
            setOnClickPendingIntent(R.id.widget_root, openApp)
        }

    private fun wide(summary: WidgetSummary): RemoteViews =
        RemoteViews(packageName, R.layout.widget_summary_wide).apply {
            bindTitle(summary)
            bindDuties(summary)
            bindGauge(summary)
            val promotion = summary.nextPromotion
            setViewVisibility(R.id.promotion, if (promotion == null) View.GONE else View.VISIBLE)
            if (promotion != null) setTextViewText(R.id.promotion, WidgetText.promotionLine(promotion))
            setOnClickPendingIntent(R.id.widget_root, openApp)
        }

    private fun large(summary: WidgetSummary): RemoteViews =
        RemoteViews(packageName, R.layout.widget_summary_large).apply {
            bindTitle(summary)
            bindDuties(summary)
            bindGauge(summary)
            bindRow(R.id.row_promotion, R.id.value_promotion, summary.nextPromotion?.let(WidgetText::promotionLine))
            bindRow(R.id.row_outing, R.id.value_outing, summary.nextOuting?.let(WidgetText::outingLine))
            bindRow(R.id.row_pass, R.id.value_pass, summary.nextPass?.let(WidgetText::passLine))
            bindRow(R.id.row_leave, R.id.value_leave, WidgetText.leaveRemainingLine(summary.leaveRemaining))
            setOnClickPendingIntent(R.id.widget_root, openApp)
        }

    private fun RemoteViews.bindTitle(summary: WidgetSummary) {
        setTextViewText(R.id.title, WidgetText.dischargeTitle(summary.dischargeDDay))
    }

    private fun RemoteViews.bindGauge(summary: WidgetSummary) {
        val percent = WidgetText.progressPercent(summary.serviceProgressPercent)
        setProgressBar(R.id.gauge, GAUGE_MAX, percent, false)
        setTextViewText(R.id.percent, WidgetText.progressText(summary.serviceProgressPercent))
    }

    /**
     * 오늘 근무 칩. 칩 3개는 레이아웃에 고정돼 있고 있는 종류만 보인다. 순서는 레이아웃의 배치
     * (당 → 비 → 츄)를 따르며, [WidgetSummaryBuilder]가 이 순서로 정렬해 넘기므로 입력 순서와 같다.
     */
    private fun RemoteViews.bindDuties(summary: WidgetSummary) {
        setViewVisibility(R.id.duty_row, if (summary.todayDuties.isEmpty()) View.GONE else View.VISIBLE)
        DutyType.entries.forEach { type ->
            val chip = chipId(type)
            setViewVisibility(chip, if (type in summary.todayDuties) View.VISIBLE else View.GONE)
            setTextViewText(chip, dutyTypeShortLabel(type))
        }
    }

    private fun RemoteViews.bindRow(rowId: Int, valueId: Int, text: String?) {
        setViewVisibility(rowId, if (text == null) View.GONE else View.VISIBLE)
        if (text != null) setTextViewText(valueId, text)
    }

    private fun chipId(type: DutyType): Int = when (type) {
        DutyType.DUTY -> R.id.chip_duty
        DutyType.OFF_DUTY -> R.id.chip_off_duty
        DutyType.MESS -> R.id.chip_mess
    }

    private companion object {
        const val REQUEST_OPEN_APP = 1000
        const val GAUGE_MAX = 100

        // RemoteViews 크기 Map의 키. 실제 위젯 크기가 키 이상인 후보 중 가장 큰 레이아웃을 시스템이 고른다.
        val SIZE_SMALL = SizeF(110f, 40f)
        val SIZE_WIDE = SizeF(250f, 40f)
        val SIZE_LARGE = SizeF(250f, 110f)
    }
}
