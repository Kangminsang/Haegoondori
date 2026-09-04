package com.kangminsang.hagoondori.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate

private val WEEKDAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")
private const val COLUMNS = 7

/**
 * 요일 헤더 + 7열 날짜 격자(F3). 일요일이 첫 열이다.
 *
 * 한 달치(최대 6주×7일=42칸) 정도는 가상화가 필요 없는 작은 규모이므로,
 * LazyVerticalGrid 대신 일반 Column/Row로 구성한다 - 이 화면을 스크롤 가능한
 * 상위 컨테이너 안에 넣어도(예: 선택한 날짜 상세 패널과 함께) 중첩 스크롤
 * 문제가 생기지 않는다.
 */
@Composable
fun MonthGrid(
    days: List<CalendarDayInfo>,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            WEEKDAY_LABELS.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        days.chunked(COLUMNS).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        isSelected = day.date == selectedDate,
                        onClick = { onDayClick(day.date) },
                        modifier = Modifier.weight(1f).padding(2.dp),
                    )
                }
            }
        }
    }
}
