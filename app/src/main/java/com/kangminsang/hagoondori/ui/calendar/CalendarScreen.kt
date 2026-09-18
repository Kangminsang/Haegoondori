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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.core.validation.TitleValidation
import com.kangminsang.hagoondori.core.validation.TitleValidator
import com.kangminsang.hagoondori.ui.common.DateTextField
import kotlinx.datetime.LocalDate

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

            SelectedDayDetail(uiState.selectedDayInfo, onDeleteEvent = viewModel::deleteEvent)

            uiState.selectedDate?.let { selected ->
                AddEventSection(
                    selectedDate = selected,
                    onAddEvent = viewModel::addEvent,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
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
private fun SelectedDayDetail(day: CalendarDayInfo?, onDeleteEvent: (Event) -> Unit) {
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
            EventLine(event, onDelete = { onDeleteEvent(event) })
        }
    }
}

@Composable
private fun DetailLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun EventLine(event: Event, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (event.isImportant) "★ ${event.title}" else event.title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Close, contentDescription = "일정 삭제")
        }
    }
}

/** 일반 일정 추가 폼(F4, 스펙 4.12절). 선택한 날짜를 기본 시작일로 채운다. */
@Composable
private fun AddEventSection(
    selectedDate: LocalDate,
    onAddEvent: (title: String, startDate: LocalDate, endDate: LocalDate?, isImportant: Boolean, memo: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "일정 추가 닫기" else "+ 일정 추가")
        }
        if (expanded) {
            var title by remember { mutableStateOf("") }
            var endDate by remember(selectedDate) { mutableStateOf<LocalDate?>(null) }
            var isImportant by remember { mutableStateOf(false) }
            var memo by remember { mutableStateOf("") }
            val titleValidation = TitleValidator.validate(title)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("제목 (최대 12자)") },
                isError = titleValidation is TitleValidation.Invalid,
                modifier = Modifier.fillMaxWidth(),
            )
            when (titleValidation) {
                is TitleValidation.Invalid -> Text(
                    titleValidation.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 2.dp),
                )
                is TitleValidation.Warning -> Text(
                    titleValidation.message,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
                TitleValidation.Ok -> Unit
            }

            DateTextFieldReadOnlyLabel(selectedDate, modifier = Modifier.padding(top = 8.dp))
            DateTextField(
                "종료일(선택, 여러 날에 걸친 일정만)",
                endDate,
                { endDate = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("메모(선택, 장치에는 표시 안 됨)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isImportant, onCheckedChange = { isImportant = it })
                Text("중요 일정으로 표시")
            }
            val endDateValid = endDate == null || endDate!! >= selectedDate
            Button(
                enabled = title.isNotBlank() && titleValidation !is TitleValidation.Invalid && endDateValid,
                onClick = {
                    onAddEvent(title.trim(), selectedDate, endDate, isImportant, memo.ifBlank { null })
                    title = ""
                    endDate = null
                    isImportant = false
                    memo = ""
                    expanded = false
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("저장") }
        }
    }
}

@Composable
private fun DateTextFieldReadOnlyLabel(date: LocalDate, modifier: Modifier = Modifier) {
    Text("시작일: $date", style = MaterialTheme.typography.bodyMedium, modifier = modifier)
}
