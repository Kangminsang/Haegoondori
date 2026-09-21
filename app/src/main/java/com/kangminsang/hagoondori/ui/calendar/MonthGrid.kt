package com.kangminsang.hagoondori.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.ui.common.rememberToday
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.datetime.LocalDate

private val WEEKDAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")
private const val COLUMNS = 7

/**
 * 요일 헤더 + 7열 날짜 격자(F3). 일요일이 첫 열이다.
 * 장치 설계서 v2(6장)처럼 칸 테두리 없이 각 주의 위쪽에만 점선을 긋고,
 * 요일 헤더의 일·토는 적색으로, 이전·다음 달 칸은 비워 둔다.
 *
 * 한 달치(최대 6주×7일=42칸) 정도는 가상화가 필요 없는 작은 규모이므로,
 * LazyVerticalGrid 대신 일반 Column/Row로 구성한다 - 이 화면을 스크롤 가능한
 * 상위 컨테이너 안에 넣어도(예: 선택한 날짜 상세 패널과 함께) 중첩 스크롤
 * 문제가 생기지 않는다.
 */
@Composable
fun MonthGrid(
    days: List<CalendarDayInfo>,
    selectedDates: Set<LocalDate>,
    /** 근무 입력 모드: 선택된 칸을 테두리에 더해 음영으로도 채워, 여러 칸이 골라졌음을 분명히 한다. */
    fillSelected: Boolean = false,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today by rememberToday()
    val ink = MaterialTheme.colorScheme.onSurface
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            WEEKDAY_LABELS.forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (index == 0 || index == 6) MaterialTheme.colorScheme.error else ink,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        days.chunked(COLUMNS).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = ink,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx())),
                        )
                    },
            ) {
                week.forEach { day ->
                    if (day.isCurrentMonth) {
                        DayCell(
                            day = day,
                            isSelected = day.date in selectedDates,
                            fillSelected = fillSelected,
                            isToday = day.date == today,
                            today = today,
                            onClick = { onDayClick(day.date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Box(Modifier.weight(1f).aspectRatio(0.85f))
                    }
                }
            }
        }

        MarkerLegend(modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
    }
}

/** 장치 하단 범례(설계서 5.1절)와 같은 순서·같은 기호. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarkerLegend(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LegendItem("당직") { DutyChip(DutyType.DUTY) }
        LegendItem("비번") { DutyChip(DutyType.OFF_DUTY) }
        LegendItem("식사당번") { DutyChip(DutyType.MESS) }
        LegendItem("휴가·외박") { PeriodBarSample(colors.error, dotted = false) }
        LegendItem("전투휴무") { PeriodBarSample(colors.error, dotted = true) }
        LegendItem("외출") { PeriodBarSample(colors.onSurface, dotted = true) }
        LegendItem("일정") { Box(Modifier.size(5.dp).background(colors.onSurface)) }
    }
}

@Composable
private fun LegendItem(label: String, marker: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        marker()
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
    }
}
