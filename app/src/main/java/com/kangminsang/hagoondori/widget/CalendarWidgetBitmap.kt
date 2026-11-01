package com.kangminsang.hagoondori.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.text.TextPaint
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.kangminsang.hagoondori.R
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.core.widget.WidgetCellLines
import com.kangminsang.hagoondori.core.widget.WidgetMonthGrid
import com.kangminsang.hagoondori.ui.calendar.CalendarDayInfo
import com.kangminsang.hagoondori.ui.duty.dutyTypeShortLabel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 달력 위젯 한 장의 입력. 칸 정보는 앱 달력과 같은 [CalendarDayInfo]를 쓴다. */
class CalendarWidgetModel(
    val year: Int,
    val month: Int,
    val today: LocalDate,
    /** 이 달의 날짜별 정보. 키가 없는 날은 빈 칸으로 그린다. */
    val days: Map<LocalDate, CalendarDayInfo>,
    /** 헤더 오른쪽의 `D-231`. 프로필이 없으면 null이라 생략한다. */
    val dDayText: String?,
)

/**
 * 달력 위젯 전체를 비트맵 한 장으로 그린다. 칸마다 뷰를 만들지 않는다. 이 비트맵 위에 얹는 투명 터치 영역
 * (res/layout/widget_calendar.xml)과 기하가 정확히 같아야 한다. 아래 상수는 그 XML의 dp 값과 일치해야 한다.
 *
 * - 헤더 [HEADER_DP]dp, 요일 줄 [WEEKDAY_DP]dp. 나머지 높이를 주 수로 균등 분할한다.
 * - 칸 폭 = 전체폭 / 7. 바깥 패딩과 좌우 안쪽 여백은 없다.
 * - ◀ 중심 x = [NAV_CENTER_DP]dp, ▶ 중심 x = 전체폭 - [NAV_CENTER_DP]dp.
 *
 * 글자는 [WidgetTextBitmap]과 같은 Galmuri 글꼴([WidgetFont])로 그린다. 바탕 테두리는 그리지 않는다(홈 앱이 모서리를 자른다).
 * 비트맵은 RGB_565(불투명 3색이라 충분)로 만든다.
 */
class CalendarWidgetBitmap @Inject constructor(
    @ApplicationContext private val context: Context,
    private val textBitmap: WidgetTextBitmap,
) {

    /**
     * [widthDp]×[heightDp] 위젯을 그린다. 비트맵이 RemoteViews 메모리 제한([화면 폭×높이×4×1.5바이트])을 넘으면
     * 같은 비율로 줄인다. 글자 크기도 같은 비율로 줄어 칸 안의 배치가 유지된다.
     */
    fun draw(model: CalendarWidgetModel, widthDp: Float, heightDp: Float): Bitmap {
        val metrics = context.resources.displayMetrics
        val naturalWidth = widthDp * metrics.density
        val naturalHeight = heightDp * metrics.density
        // RGB_565는 픽셀당 2바이트다. 제한(화면 폭×높이×4×1.5바이트)은 픽셀 수로 화면 픽셀 수×3이다.
        val maxPixels = metrics.widthPixels.toDouble() * metrics.heightPixels * 3
        val shrink = min(1.0, sqrt(maxPixels / (naturalWidth.toDouble() * naturalHeight))).toFloat()
        val width = (naturalWidth * shrink).roundToInt().coerceAtLeast(1)
        val height = (naturalHeight * shrink).roundToInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmap.density = metrics.densityDpi
        val frame = Frame(
            canvas = Canvas(bitmap),
            widthDp = widthDp,
            heightDp = heightDp,
            width = width.toFloat(),
            height = height.toFloat(),
            shrink = shrink,
        )
        drawAll(frame, model)
        return bitmap
    }

    /**
     * 그리는 동안 쓰는 좌표 도구. [x]·[y]는 dp를 비트맵 픽셀로 바꾼다. x와 y의 배율은 실제 비트맵 크기에서 따로 구해
     * 헤더·요일 줄 같은 dp 높이가 정확히 맞게 한다.
     */
    private inner class Frame(
        val canvas: Canvas,
        val widthDp: Float,
        val heightDp: Float,
        val width: Float,
        val height: Float,
        private val shrink: Float,
    ) {
        private val metrics = context.resources.displayMetrics
        private val unitX = width / widthDp
        val unitY = height / heightDp

        fun x(dp: Float): Float = dp * unitX
        fun y(dp: Float): Float = dp * unitY

        /** sp를 픽셀로 바꾼다. 비트맵을 줄였다면 글자도 같은 비율로 줄인다. */
        fun sp(value: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, metrics) * shrink

        fun colorOf(colorRes: Int): Int = ContextCompat.getColor(context, colorRes)

        fun textPaint(font: WidgetFont, sizeSp: Float, colorRes: Int, alpha: Int = 255): TextPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = textBitmap.typeface(font)
                textSize = sp(sizeSp)
                color = colorOf(colorRes)
                this.alpha = alpha
                isFakeBoldText = font.fakeBold
            }

        fun fillPaint(colorRes: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = colorOf(colorRes)
        }

        fun strokePaint(colorRes: Int, widthPx: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = widthPx
            color = colorOf(colorRes)
        }
    }

    private fun drawAll(f: Frame, model: CalendarWidgetModel) {
        f.canvas.drawColor(f.colorOf(R.color.widget_paper))
        val weeks = WidgetMonthGrid.weekCount(model.year, model.month)
        val gridTop = f.y(GRID_TOP_DP)
        val rowHeight = (f.height - gridTop) / weeks
        val cellWidth = f.width / WidgetMonthGrid.COLUMNS

        drawHeader(f, model)
        drawWeekdays(f)

        // 요일 줄 아래의 잉크 선. 요일 줄 안쪽 맨 아래에 그려 칸 격자 위치는 바꾸지 않는다.
        f.canvas.drawRect(0f, f.y(GRID_TOP_DP - WEEKDAY_RULE_DP), f.width, gridTop, f.fillPaint(R.color.widget_ink))

        // 주 사이의 회색 가로선. 마지막 주 아래에는 긋지 않는다.
        val rule = f.fillPaint(R.color.widget_gray)
        for (week in 1 until weeks) {
            val top = gridTop + week * rowHeight
            f.canvas.drawRect(0f, top, f.width, top + max(1f, f.y(RULE_DP)), rule)
        }

        for (day in 1..WidgetMonthGrid.daysInMonth(model.year, model.month)) {
            val info = model.days[LocalDate(model.year, model.month, day)] ?: continue
            val row = WidgetMonthGrid.rowOf(model.year, model.month, day)
            val column = WidgetMonthGrid.columnOf(model.year, model.month, day)
            drawCell(
                f = f,
                info = info,
                today = model.today,
                left = column * cellWidth,
                top = gridTop + row * rowHeight,
                cellWidth = cellWidth,
                rowHeight = rowHeight,
                cellWidthDp = f.widthDp / WidgetMonthGrid.COLUMNS,
            )
        }
    }

    private fun drawHeader(f: Frame, model: CalendarWidgetModel) {
        val centerY = f.y(HEADER_DP / 2)
        val ink = f.fillPaint(R.color.widget_ink)
        val prevX = f.x(NAV_CENTER_DP)
        val nextX = f.width - f.x(NAV_CENTER_DP)
        f.canvas.drawPath(triangle(f, prevX, centerY, pointsLeft = true), ink)
        f.canvas.drawPath(triangle(f, nextX, centerY, pointsLeft = false), ink)

        val titlePaint = f.textPaint(WidgetFont.TITLE, 18f, R.color.widget_ink)
        val title = "${model.year}년 ${model.month}월"
        val titleWidth = titlePaint.measureText(title)
        val titleLeft = (f.width - titleWidth) / 2f
        f.canvas.drawText(title, titleLeft, baselineAt(titlePaint, centerY), titlePaint)

        val dDay = model.dDayText ?: return
        // D-day는 ▶ 바로 왼쪽에 쓴다. 제목과 겹치면 글자를 한 단계씩 줄이고, 그래도 안 들어가면 생략한다.
        val right = nextX - f.x(TRIANGLE_HALF_WIDTH_DP + DDAY_GAP_DP)
        for (size in DDAY_SIZES_SP) {
            val paint = f.textPaint(WidgetFont.TITLE, size, R.color.widget_ink)
            val left = right - paint.measureText(dDay)
            if (left >= titleLeft + titleWidth + f.x(TITLE_DDAY_GAP_DP)) {
                f.canvas.drawText(dDay, left, baselineAt(paint, centerY), paint)
                return
            }
        }
    }

    private fun triangle(f: Frame, centerX: Float, centerY: Float, pointsLeft: Boolean): Path {
        val halfWidth = f.x(TRIANGLE_HALF_WIDTH_DP)
        val halfHeight = f.y(TRIANGLE_HALF_HEIGHT_DP)
        val tipX = if (pointsLeft) centerX - halfWidth else centerX + halfWidth
        val baseX = if (pointsLeft) centerX + halfWidth else centerX - halfWidth
        return Path().apply {
            moveTo(tipX, centerY)
            lineTo(baseX, centerY - halfHeight)
            lineTo(baseX, centerY + halfHeight)
            close()
        }
    }

    private fun drawWeekdays(f: Frame) {
        val cellWidth = f.width / WidgetMonthGrid.COLUMNS
        val centerY = f.y(HEADER_DP + WEEKDAY_DP / 2)
        WEEKDAY_LABELS.forEachIndexed { index, label ->
            val colorRes = if (index == 0 || index == 6) R.color.widget_red else R.color.widget_ink
            val paint = f.textPaint(WidgetFont.BODY, 12f, colorRes)
            val x = (index + 0.5f) * cellWidth - paint.measureText(label) / 2f
            f.canvas.drawText(label, x, baselineAt(paint, centerY), paint)
        }
    }

    /**
     * 칸 하나. 위쪽 막대 → 날짜 → 근무 칩 → 글자 줄 순서로 그린다. 칸 바깥으로 넘치는 것은 clip으로 자른다.
     */
    private fun drawCell(
        f: Frame,
        info: CalendarDayInfo,
        today: LocalDate,
        left: Float,
        top: Float,
        cellWidth: Float,
        rowHeight: Float,
        cellWidthDp: Float,
    ) {
        val canvas = f.canvas
        val right = left + cellWidth
        val bottom = top + rowHeight
        canvas.save()
        canvas.clipRect(left, top, right, bottom)

        drawPeriodBar(f, info, left, top, right)

        val isToday = info.date == today
        val isPast = !isToday && info.date < today
        val isWeekend = info.date.dayOfWeek == DayOfWeek.SATURDAY || info.date.dayOfWeek == DayOfWeek.SUNDAY
        val numberColor = when {
            isToday -> R.color.widget_paper
            isWeekend || info.isHoliday -> R.color.widget_red
            else -> R.color.widget_ink
        }
        val numberPaint = f.textPaint(WidgetFont.BODY, 14f, numberColor, alpha = if (isPast) PAST_ALPHA else OPAQUE)
        val numberText = info.date.dayOfMonth.toString()
        val numberWidth = numberPaint.measureText(numberText)
        val numberLineTop = top + f.y(NUMBER_TOP_DP)
        val numberLineHeight = numberPaint.fontMetrics.let { it.descent - it.ascent }
        val numberBottom = numberLineTop + numberLineHeight
        var numberX = left + f.x(NUMBER_START_DP)
        if (isToday) {
            // 오늘은 잉크 배지 위에 종이색 글자를 쓴다.
            val badgeLeft = left + f.x(TODAY_BADGE_START_DP)
            numberX = badgeLeft + f.x(TODAY_BADGE_PAD_DP)
            canvas.drawRect(badgeLeft, numberLineTop, numberX + numberWidth + f.x(TODAY_BADGE_PAD_DP), numberBottom, f.fillPaint(R.color.widget_ink))
        }
        canvas.drawText(numberText, numberX, numberLineTop - numberPaint.fontMetrics.ascent, numberPaint)

        drawDutyChips(f, info, left, numberBottom, cellWidthDp)
        drawCellLines(f, info, left, numberBottom, bottom)

        canvas.restore()
    }

    private fun drawPeriodBar(f: Frame, info: CalendarDayInfo, left: Float, top: Float, right: Float) {
        val barHeight = f.y(PERIOD_BAR_DP)
        when {
            info.hasLeave || info.hasOvernight ->
                f.canvas.drawRect(left, top, right, top + barHeight, f.fillPaint(R.color.widget_red))
            info.hasCombatRest -> drawDashedBar(f, R.color.widget_red, left, top, right, barHeight)
            info.hasPass -> drawDashedBar(f, R.color.widget_ink, left, top, right, barHeight)
        }
    }

    private fun drawDashedBar(f: Frame, colorRes: Int, left: Float, top: Float, right: Float, height: Float) {
        val paint = f.strokePaint(colorRes, height).apply {
            pathEffect = DashPathEffect(floatArrayOf(f.x(DASH_DP), f.x(DASH_DP)), 0f)
        }
        val y = top + height / 2
        f.canvas.drawLine(left, y, right, y, paint)
    }

    /**
     * 근무 칩. 당 = 잉크 채움, 비 = 종이 채움 + 잉크 테두리, 츄 = 빨강 채움. 칩은 붙여서 나란히 놓는다(당 > 비 > 츄 순).
     * 세 개가 칸 폭에 모자라면 작게 그린다.
     */
    private fun drawDutyChips(f: Frame, info: CalendarDayInfo, left: Float, numberBottom: Float, cellWidthDp: Float) {
        val duties = info.dutyAssignments.map { it.type }.distinct().sorted()
        if (duties.isEmpty()) return
        val small = duties.size * CHIP_DP + 2 * CHIP_START_DP > cellWidthDp
        val sizeDp = if (small) CHIP_SMALL_DP else CHIP_DP
        val chipTop = numberBottom + f.y(CHIP_TOP_GAP_DP)
        val chipWidth = f.x(sizeDp)
        val chipHeight = f.y(sizeDp)
        duties.forEachIndexed { index, type ->
            drawChip(f, type, left + f.x(CHIP_START_DP) + index * chipWidth, chipTop, chipWidth, chipHeight, small)
        }
    }

    private fun drawChip(f: Frame, type: DutyType, chipLeft: Float, chipTop: Float, width: Float, height: Float, small: Boolean) {
        // (채움, 글자, 테두리). 테두리가 null이면 채움만 그린다.
        val (fillRes, textRes, borderRes) = when (type) {
            DutyType.DUTY -> Triple(R.color.widget_ink, R.color.widget_paper, null)
            DutyType.OFF_DUTY -> Triple(R.color.widget_paper, R.color.widget_ink, R.color.widget_ink)
            DutyType.MESS -> Triple(R.color.widget_red, R.color.widget_paper, null)
        }
        val chipRight = chipLeft + width
        val chipBottom = chipTop + height
        f.canvas.drawRect(chipLeft, chipTop, chipRight, chipBottom, f.fillPaint(fillRes))
        if (borderRes != null) {
            val border = f.strokePaint(borderRes, f.x(BORDER_DP))
            val inset = border.strokeWidth / 2f
            f.canvas.drawRect(chipLeft + inset, chipTop + inset, chipRight - inset, chipBottom - inset, border)
        }
        val label = dutyTypeShortLabel(type)
        val paint = f.textPaint(WidgetFont.BOLD, if (small) CHIP_SMALL_SP else CHIP_SP, textRes)
        f.canvas.drawText(label, chipLeft + (width - paint.measureText(label)) / 2f, baselineAt(paint, chipTop + height / 2), paint)
    }

    /**
     * 칩 아래 글자 줄. 휴가·외박(빨강) → 일정 제목(잉크) → 외출(잉크) 순이다. 칸 높이에 들어가는 만큼만 쓰고,
     * 못 보여 준 줄 수는 마지막 줄 끝에 `+N`으로 붙인다.
     */
    private fun drawCellLines(f: Frame, info: CalendarDayInfo, left: Float, numberBottom: Float, bottom: Float) {
        val lines = buildList {
            info.startingLeaveNames.forEach { add(it to R.color.widget_red) }
            if (info.overnightStarts) add(OVERNIGHT_LABEL to R.color.widget_red)
            info.events.forEach { add(it.title to R.color.widget_ink) }
            if (info.hasPass) add(PASS_LABEL to R.color.widget_ink)
        }
        if (lines.isEmpty()) return

        val textTop = numberBottom + f.y(CHIP_TOP_GAP_DP + CHIP_DP + TEXT_GAP_DP)
        val availableDp = (bottom - f.y(TEXT_BOTTOM_DP) - textTop) / f.unitY
        val plan = WidgetCellLines.plan(lines.size, WidgetCellLines.maxLines(availableDp, TEXT_LINE_DP))
        val lineHeight = f.y(TEXT_LINE_DP)
        for (index in 0 until plan.shown) {
            val (text, colorRes) = lines[index]
            val shownText = if (index == plan.shown - 1) text + WidgetCellLines.suffix(plan.hidden) else text
            val paint = f.textPaint(WidgetFont.BODY, TEXT_SP, colorRes)
            f.canvas.drawText(shownText, left + f.x(TEXT_INSET_DP), textTop + index * lineHeight - paint.fontMetrics.ascent, paint)
        }
    }

    /** 글자 한 줄을 [centerY]에 세로 가운데 맞출 때의 baseline. */
    private fun baselineAt(paint: Paint, centerY: Float): Float {
        val metrics = paint.fontMetrics
        return centerY - (metrics.ascent + metrics.descent) / 2f
    }

    private companion object {
        // 기하(dp). res/layout/widget_calendar.xml의 높이·여백과 같아야 한다.
        const val HEADER_DP = 40f
        const val WEEKDAY_DP = 22f
        const val GRID_TOP_DP = HEADER_DP + WEEKDAY_DP
        const val WEEKDAY_RULE_DP = 1.5f
        const val RULE_DP = 1f
        const val NAV_CENTER_DP = 28f
        const val TRIANGLE_HALF_WIDTH_DP = 5f
        const val TRIANGLE_HALF_HEIGHT_DP = 6f
        const val DDAY_GAP_DP = 6f
        const val TITLE_DDAY_GAP_DP = 4f
        val DDAY_SIZES_SP = listOf(14f, 12f, 11f, 10f)

        const val PERIOD_BAR_DP = 4f
        const val DASH_DP = 2f
        const val NUMBER_TOP_DP = 6f
        const val NUMBER_START_DP = 5f
        const val TODAY_BADGE_START_DP = 3f
        const val TODAY_BADGE_PAD_DP = 4f
        const val PAST_ALPHA = 178 // 70%
        const val OPAQUE = 255

        const val CHIP_START_DP = 3f
        const val CHIP_TOP_GAP_DP = 3f
        const val CHIP_DP = 16f
        const val CHIP_SMALL_DP = 13f
        const val CHIP_SP = 11f
        const val CHIP_SMALL_SP = 9f
        const val BORDER_DP = 1f

        const val TEXT_GAP_DP = 2f
        const val TEXT_INSET_DP = 3f
        const val TEXT_LINE_DP = 13f
        const val TEXT_BOTTOM_DP = 2f
        const val TEXT_SP = 10f

        const val OVERNIGHT_LABEL = "외박"
        const val PASS_LABEL = "외출"

        val WEEKDAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")
    }
}
