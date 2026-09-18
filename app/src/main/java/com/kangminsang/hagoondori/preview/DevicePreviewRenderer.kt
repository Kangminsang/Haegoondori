package com.kangminsang.hagoondori.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.kangminsang.hagoondori.core.calc.HolidayJudge
import com.kangminsang.hagoondori.core.export.CalendarSnapshot
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.ui.calendar.CalendarGridBuilder
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.datetime.LocalDate

/** 장치 화면 논리 해상도(스펙 1.4절: 800×480). */
private const val LOGICAL_WIDTH = 800f
private const val LOGICAL_HEIGHT = 480f

/**
 * [snapshot]을 장치와 같은 비율(800×480), 같은 규칙으로 그린다(F16, 스펙 5.3절).
 *
 * 여기서 그리는 것은 어디까지나 **미리보기 근사치**다 - 실제 장치 펌웨어의 정확한
 * 픽셀 레이아웃은 이 코드가 아니라 임베디드 쪽 구현이 결정한다. 이 렌더러의
 * 목적은 "보내기 전에 대략이라도 확인"이며(장치 갱신 26초가 걸리므로), 데이터
 * 누락이나 형식 오류를 조기에 발견하는 데 있다.
 *
 * 실제 장치는 흑·적·백 3색만 쓸 수 있으므로, 이 렌더러도 그 제약을 그대로 따른다 -
 * [DevicePalette]가 정의하는 배경/전경/강조 색 세 가지만 사용한다.
 */
@Composable
fun DevicePreviewRenderer(
    snapshot: CalendarSnapshot,
    isNightTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val today = remember { AppClock.today() }
    val textMeasurer = rememberTextMeasurer()
    val palette = if (isNightTheme) DevicePalette.NIGHT else DevicePalette.DAY

    Canvas(modifier = modifier.aspectRatio(LOGICAL_WIDTH / LOGICAL_HEIGHT)) {
        val scaleFactor = size.width / LOGICAL_WIDTH
        scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
            drawDeviceScreen(textMeasurer, palette, snapshot, today)
        }
    }
}

/**
 * [sizeSp]는 이 렌더러의 800×480 논리 좌표계 단위다 - 실제 sp가 아니다.
 * `drawText`는 폰트 크기를 기기 실제 밀도로 측정하지만, 이 함수를 감싸는
 * `scale(scaleFactor)` 캔버스 변환은 그 측정 이후에 다시 한번 곱해지므로,
 * 밀도를 미리 나눠 상쇄하지 않으면 텍스트만 다른 도형보다 밀도배만큼 더 커져
 * 겹쳐 보인다(실기기 밀도가 높을수록 심해짐).
 */
private fun DrawScope.textStyle(color: Color, sizeSp: Float) =
    TextStyle(color = color, fontSize = TextUnit(sizeSp / density, TextUnitType.Sp))

private fun DrawScope.drawDeviceScreen(
    textMeasurer: TextMeasurer,
    palette: DevicePalette,
    snapshot: CalendarSnapshot,
    today: LocalDate,
) {
    drawRect(color = palette.background, size = Size(LOGICAL_WIDTH, LOGICAL_HEIGHT))

    drawHeader(textMeasurer, palette, snapshot, today)
    drawBattery(palette, x = 750f, y = 10f)
    drawCalendarGrid(textMeasurer, palette, snapshot, today, originX = 400f, originY = 56f)
}

private fun DrawScope.drawHeader(
    textMeasurer: TextMeasurer,
    palette: DevicePalette,
    snapshot: CalendarSnapshot,
    today: LocalDate,
) {
    val weekdayNames = listOf("월", "화", "수", "목", "금", "토", "일")
    val weekday = weekdayNames[today.dayOfWeek.value - 1]
    drawText(textMeasurer, "${today} ($weekday)", topLeft = Offset(16f, 12f), style = textStyle(palette.foreground, 20f))

    val dDay = PreviewDisplayCalc.dischargeDDay(snapshot, today)
    val dDayText = if (dDay >= 0) "전역 D-$dDay" else "전역 D+${-dDay}"
    drawText(textMeasurer, dDayText, topLeft = Offset(16f, 44f), style = textStyle(palette.foreground, 44f))

    // 진행률 바 + 진급 마커
    val barLeft = 16f
    val barTop = 116f
    val barWidth = 360f
    val barHeight = 16f
    drawRect(color = palette.foreground, topLeft = Offset(barLeft, barTop), size = Size(barWidth, barHeight), style = Stroke(width = 2f))
    val progress = (PreviewDisplayCalc.serviceProgressPercent(snapshot, today) / 100.0).toFloat()
    drawRect(color = palette.foreground, topLeft = Offset(barLeft, barTop), size = Size(barWidth * progress, barHeight))
    PreviewDisplayCalc.promotionMarkerPercent(snapshot, today)?.let { markerPercent ->
        val markerX = barLeft + barWidth * (markerPercent / 100.0).toFloat()
        drawLine(
            color = palette.accent,
            start = Offset(markerX, barTop - 4f),
            end = Offset(markerX, barTop + barHeight + 4f),
            strokeWidth = 3f,
        )
    }

    var lineY = 144f
    PreviewDisplayCalc.promotionDDay(snapshot, today)?.let { pDay ->
        val text = if (pDay >= 0) "진급 D-$pDay" else "진급 D+${-pDay}"
        drawText(textMeasurer, text, topLeft = Offset(16f, lineY), style = textStyle(palette.foreground, 16f))
        lineY += 22f
    }
    PreviewDisplayCalc.leaveDDay(snapshot, today)?.let { leave ->
        val prefix = if (leave.isOngoing) "휴가 종료까지" else "휴가까지"
        val dText = if (leave.dDay >= 0) "D-${leave.dDay}" else "D+${-leave.dDay}"
        drawText(textMeasurer, "$prefix $dText (${leave.label})", topLeft = Offset(16f, lineY), style = textStyle(palette.foreground, 16f))
        lineY += 22f
    }
    PreviewDisplayCalc.nextOvernightDDay(snapshot, today)?.let { oDay ->
        val text = if (oDay >= 0) "외박 D-$oDay" else "외박 D+${-oDay}"
        drawText(textMeasurer, text, topLeft = Offset(16f, lineY), style = textStyle(palette.foreground, 16f))
        lineY += 22f
    }

    lineY += 10f
    PreviewDisplayCalc.upcomingEventDDays(snapshot, today).forEach { event ->
        val mark = if (event.isImportant) "★" else "•"
        val dText = if (event.dDay >= 0) "D-${event.dDay}" else "D+${-event.dDay}"
        drawText(textMeasurer, "$mark ${event.title} ($dText)", topLeft = Offset(16f, lineY), style = textStyle(palette.foreground, 15f))
        lineY += 20f
    }
}

/** 배터리 잔량 표시(스펙 7.1절 - 장치가 ADC로 측정). 미리보기에서는 값을 실측할 수 없어 임의 값으로 형태만 재현한다. */
private fun DrawScope.drawBattery(palette: DevicePalette, x: Float, y: Float) {
    val width = 34f
    val height = 16f
    drawRect(color = palette.foreground, topLeft = Offset(x, y), size = Size(width, height), style = Stroke(width = 2f))
    drawRect(color = palette.foreground, topLeft = Offset(x + width, y + 4f), size = Size(3f, height - 8f))
    val mockLevel = 0.7f // 실제 값은 장치가 ADC로 측정한다 - 미리보기는 형태만 보여준다.
    drawRect(color = palette.foreground, topLeft = Offset(x + 2f, y + 2f), size = Size((width - 4f) * mockLevel, height - 4f))
}

private fun DrawScope.drawCalendarGrid(
    textMeasurer: TextMeasurer,
    palette: DevicePalette,
    snapshot: CalendarSnapshot,
    today: LocalDate,
    originX: Float,
    originY: Float,
) {
    val cellWidth = 54f
    val cellHeight = 55f
    val weekdayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

    weekdayLabels.forEachIndexed { index, label ->
        drawText(
            textMeasurer,
            label,
            topLeft = Offset(originX + index * cellWidth + 18f, originY - 24f),
            style = textStyle(palette.foreground, 14f),
        )
    }

    val gridDates = CalendarGridBuilder.buildGrid(today.year, today.monthNumber)
    val eventsByDate = snapshot.events.groupBy { it.startDate }
    val leaveDates = snapshot.leaveUsages.flatMap { usage -> dateRange(usage.startDate, usage.endDate) }.toSet()
    val combatRestDates = snapshot.combatRestUsages.flatMap { usage -> dateRange(usage.startDate, usage.endDate) }.toSet()
    val dutyByDate = snapshot.dutyAssignments.groupBy { it.date }

    gridDates.forEachIndexed { index, date ->
        val row = index / 7
        val col = index % 7
        val cellX = originX + col * cellWidth
        val cellY = originY + row * cellHeight
        val isCurrentMonth = date.monthNumber == today.monthNumber && date.year == today.year
        val isHoliday = HolidayJudge.isHoliday(date, snapshot.holidays)
        val isToday = date == today
        val isNextOvernight = date == snapshot.nextOvernightDate

        if (isToday) {
            drawRect(color = palette.foreground, topLeft = Offset(cellX, cellY), size = Size(cellWidth - 4f, cellHeight - 4f), style = Stroke(width = 2f))
        }
        if (isNextOvernight) {
            drawRect(color = palette.accent, topLeft = Offset(cellX + 1f, cellY + 1f), size = Size(cellWidth - 6f, cellHeight - 6f), style = Stroke(width = 2f))
        }

        val numberColor = when {
            !isCurrentMonth -> palette.foreground.copy(alpha = 0.35f)
            isHoliday -> palette.accent
            else -> palette.foreground
        }
        drawText(textMeasurer, date.dayOfMonth.toString(), topLeft = Offset(cellX + 4f, cellY + 2f), style = textStyle(numberColor, 15f))

        val hasMark = date in leaveDates || date in combatRestDates || eventsByDate.containsKey(date)
        if (hasMark) {
            drawCircle(color = palette.foreground, radius = 2.5f, center = Offset(cellX + cellWidth - 10f, cellY + 8f))
        }

        dutyByDate[date]?.firstOrNull()?.let { duty ->
            val letter = when (duty.type) {
                DutyType.DUTY -> "당"
                DutyType.OFF_DUTY -> "비"
                DutyType.MESS -> "츄"
            }
            drawText(textMeasurer, letter, topLeft = Offset(cellX + 4f, cellY + 30f), style = textStyle(palette.foreground, 14f))
        }
    }
}

private fun dateRange(start: LocalDate, end: LocalDate): List<LocalDate> {
    val result = mutableListOf<LocalDate>()
    var cursor = start
    while (cursor <= end) {
        result += cursor
        cursor = LocalDate.fromEpochDays(cursor.toEpochDays() + 1)
    }
    return result
}
