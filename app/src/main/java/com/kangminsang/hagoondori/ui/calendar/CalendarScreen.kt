package com.kangminsang.hagoondori.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * ② 달력 (스펙 5.1/5.2절, F3) - 월간 달력 + 날짜별 항목 표시.
 * 휴가/전투휴무/외박/외출을 시각적으로 구분한다.
 */
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            MonthHeader(
                year = uiState.year,
                month = uiState.month,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
            )

            MonthGrid(
                days = uiState.days,
                selectedDate = uiState.selectedDate,
                onDayClick = viewModel::selectDate,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SelectedDayDetail(uiState.selectedDayInfo)
        }
    }
}

@Composable
private fun MonthHeader(year: Int, month: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "이전 달")
        }
        Text("${year}년 ${month}월", style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "다음 달")
        }
    }
}

@Composable
private fun SelectedDayDetail(day: CalendarDayInfo?) {
    if (day == null) {
        Text("날짜를 선택하면 상세 내용을 볼 수 있습니다", style = MaterialTheme.typography.bodyMedium)
        return
    }

    Column {
        Text("${day.date}", style = MaterialTheme.typography.titleMedium)

        if (!day.hasAnyMarker) {
            Text(
                "이 날은 등록된 일정이 없습니다",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            return
        }

        if (day.hasLeave) DetailLine("휴가 사용일")
        if (day.hasCombatRest) DetailLine("전투휴무 사용일 (영외 이동 불가)")
        if (day.hasOvernight) DetailLine("외박일")
        if (day.hasPass) DetailLine("외출일")
        day.dutyAssignments.forEach { DetailLine("근무: ${it.type.name}") }
        day.events.forEach { event ->
            DetailLine(if (event.isImportant) "★ ${event.title}" else event.title)
        }
    }
}

@Composable
private fun DetailLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
}
