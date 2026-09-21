package com.kangminsang.hagoondori.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.ui.duty.dutyTypeShortLabel
import com.kangminsang.hagoondori.ui.theme.Galmuri11
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * 달력 격자 한 칸. 장치 화면 설계서 v2(6장)의 셀 구성을 그대로 따른다.
 *
 * - 셀 위쪽 막대: 휴가·외박 = 적 실선, 전투휴무 = 적 점선, 외출 = 흑 점선(겹치면 이 순서로 하나만).
 * - 날짜 숫자: 주말·공휴일은 적, 지난 날은 흐리게, 오늘은 흑 반전 배지.
 * - 근무 칩: 당 = 흑 채움, 비 = 흑 테두리, 츄 = 적 채움 (한 셀에 하나, 당 > 비 > 츄).
 * - 선택한 날은 굵은 잉크 테두리(오늘 배지와 겹치지 않도록 반전을 쓰지 않는다).
 */
@Composable
fun DayCell(
    day: CalendarDayInfo,
    isSelected: Boolean,
    fillSelected: Boolean = false,
    isToday: Boolean,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val isWeekend = day.date.dayOfWeek == DayOfWeek.SATURDAY || day.date.dayOfWeek == DayOfWeek.SUNDAY
    val isPast = !isToday && day.date < today
    val numberColor = when {
        isToday -> colors.surface
        isWeekend || day.isHoliday -> colors.error
        else -> colors.onSurface
    }.let { if (isPast) it.copy(alpha = 0.7f) else it }

    Column(
        modifier = modifier
            .aspectRatio(0.85f)
            .then(if (isSelected && fillSelected) Modifier.background(colors.surfaceVariant) else Modifier)
            .then(if (isSelected) Modifier.border(2.dp, colors.onSurface) else Modifier)
            .clickable(onClick = onClick)
            .drawBehind { drawPeriodBar(day, colors.error, colors.onSurface) },
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            day.date.dayOfMonth.toString(),
            color = numberColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(start = 3.dp, top = 6.dp)
                .then(
                    if (isToday) Modifier.background(colors.onSurface).padding(horizontal = 4.dp)
                    else Modifier.padding(horizontal = 2.dp),
                ),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            day.dutyAssignments.map { it.type }.minOrNull()?.let { DutyChip(it) }
            if (day.events.isNotEmpty()) {
                Box(Modifier.padding(start = 3.dp).size(5.dp).background(colors.onSurface))
            }
        }
    }
}

private fun DrawScope.drawPeriodBar(day: CalendarDayInfo, red: Color, ink: Color) {
    val barHeight = 4.dp.toPx()
    when {
        day.hasLeave || day.hasOvernight -> drawRect(red, Offset.Zero, Size(size.width, barHeight))
        day.hasCombatRest -> drawDottedBar(red, barHeight, 2.dp.toPx())
        day.hasPass -> drawDottedBar(ink, barHeight, 2.dp.toPx())
    }
}

private fun DrawScope.drawDottedBar(color: Color, height: Float, dash: Float) {
    drawLine(
        color = color,
        start = Offset(0f, height / 2),
        end = Offset(size.width, height / 2),
        strokeWidth = height,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
    )
}

/** 근무 칩(장치와 같은 모양): 당 = 흑 채움, 비 = 흑 테두리, 츄 = 적 채움. */
@Composable
fun DutyChip(type: DutyType) {
    val colors = MaterialTheme.colorScheme
    val (fill, textColor, borderColor) = when (type) {
        DutyType.DUTY -> Triple(colors.onSurface, colors.surface, colors.onSurface)
        DutyType.OFF_DUTY -> Triple(colors.surface, colors.onSurface, colors.onSurface)
        DutyType.MESS -> Triple(colors.error, colors.surface, colors.error)
    }
    Box(
        modifier = Modifier.size(16.dp).background(fill).border(1.dp, borderColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            dutyTypeShortLabel(type),
            color = textColor,
            style = TextStyle(fontFamily = Galmuri11, fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold),
        )
    }
}

/** 범례용 기간 막대(셀 위쪽 막대와 같은 모양). */
@Composable
fun PeriodBarSample(color: Color, dotted: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 14.dp, height = 4.dp)
            .drawBehind {
                if (dotted) drawDottedBar(color, size.height, 2.dp.toPx()) else drawRect(color)
            },
    )
}
