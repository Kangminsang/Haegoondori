package com.kangminsang.hagoondori.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
 * - 날짜 숫자: 주말·공휴일은 적, 지난 날은 흐리게, 오늘은 흑 반전 배지. 일정이 있으면 숫자 오른쪽에 흑 점.
 * - 근무 칩: 당 = 흑 채움, 비 = 흑 테두리, 츄 = 적 채움 (겹치면 모두 표시, 당 > 비 > 츄 순; 3개면 작게).
 * - 출타 종류 글자(포·연·외): 적색, 칸 아래쪽 가운데. 근무 칩이 있는 날은 칩 줄 오른쪽 끝.
 * - 선택한 날은 굵은 잉크 테두리(오늘 배지와 겹치지 않도록 반전을 쓰지 않는다).
 */
@Composable
fun DayCell(
    day: CalendarDayInfo,
    isSelected: Boolean,
    fillSelected: Boolean = false,
    /** 이 날의 출타 종류 한 글자(저장된 휴가·외박, 휴가 계획 중에는 계획). null이면 표시하지 않는다. */
    kindLabel: String? = null,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            if (day.events.isNotEmpty()) {
                Box(Modifier.padding(start = 3.dp, top = 6.dp).size(5.dp).background(colors.onSurface))
            }
        }
        val duties = day.dutyAssignments.map { it.type }.distinct().sorted()
        // 종류 글자와 근무 칩이 함께 있으면 글자를 칩 줄 오른쪽 끝에 둔다 - 칸 아래쪽에 따로 두면 0.85 비율의
        // 좁은 칸(360dp 폭 기기에서 높이 약 55dp)에서 숫자·칩·글자 세 줄이 다 들어가지 않는다.
        // 칩 자리가 좁아지므로 2개부터 작은 칩을 쓴다.
        val labelInChipRow = kindLabel != null && duties.isNotEmpty()
        val crowded = duties.size >= 3 || (labelInChipRow && duties.size >= 2)
        val tiny = labelInChipRow && duties.size >= 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (tiny) 1.dp else if (crowded) 2.dp else 4.dp, top = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            duties.forEach {
                when {
                    tiny -> DutyChip(it, size = 11.dp, fontSize = 8.sp)
                    crowded -> DutyChip(it, size = 13.dp, fontSize = 9.sp)
                    else -> DutyChip(it)
                }
            }
            if (labelInChipRow) {
                Spacer(Modifier.weight(1f))
                Text(
                    kindLabel!!,
                    color = colors.error,
                    style = if (tiny) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 2.dp),
                )
            }
        }
        if (kindLabel != null && !labelInChipRow) {
            // 칸 아래쪽 가운데에 붙인다. 휴가·외박 막대와 같은 적색, 날짜 숫자와 같은 갈무리11 크기(굵게)로 써서
            // 칸 안에서 숫자보다 튀지 않게 한다.
            Spacer(Modifier.weight(1f))
            Text(
                kindLabel,
                color = colors.error,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            )
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
fun DutyChip(type: DutyType, size: Dp = 16.dp, fontSize: TextUnit = 11.sp) {
    val colors = MaterialTheme.colorScheme
    val (fill, textColor, borderColor) = when (type) {
        DutyType.DUTY -> Triple(colors.onSurface, colors.surface, colors.onSurface)
        DutyType.OFF_DUTY -> Triple(colors.surface, colors.onSurface, colors.onSurface)
        DutyType.MESS -> Triple(colors.error, colors.surface, colors.error)
    }
    Box(
        modifier = Modifier.size(size).background(fill).border(1.dp, borderColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            dutyTypeShortLabel(type),
            color = textColor,
            style = TextStyle(fontFamily = Galmuri11, fontSize = fontSize, lineHeight = fontSize, fontWeight = FontWeight.Bold),
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
