package com.kangminsang.hagoondori.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.kangminsang.hagoondori.ui.common.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import com.kangminsang.hagoondori.core.model.DutyType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.PassType
import com.kangminsang.hagoondori.ui.duty.dutyTypeLabel
import com.kangminsang.hagoondori.core.validation.TitleValidation
import com.kangminsang.hagoondori.core.validation.TitleValidator
import com.kangminsang.hagoondori.ui.common.DateTextField
import kotlinx.datetime.LocalDate

/**
 * ② 달력 (스펙 5.1/5.2절, F3) - 월간 달력 + 날짜별 항목 표시, 그리고 근무·외출 탭 입력 모드(F6).
 * 휴가/전투휴무/외박/외출을 시각적으로 구분한다.
 */
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
    /** 홈 화면 위젯 등에서 온 열기 요청. 처리하면 [onOpenRequestHandled]로 되돌려 반복되지 않게 한다. */
    openRequest: CalendarOpenRequest? = null,
    onOpenRequestHandled: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 근무·외출 입력 모드(F6): 입력할 종류를 고른 뒤 날짜를 누르면 바로 저장되고, 이미 있는 날을 다시 누르면 지워진다(토글).
    // 예전의 여러 날짜 선택 후 일괄 저장 방식은 빠뜨리기 쉬워 이 방식으로 바꿨다.
    // 예전의 별도 '근무입력' 탭은 이 달력과 격자·기능이 겹쳐 이 모드로 합쳤다.
    var inputMode by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(DutyType.DUTY) }
    var isPassMode by remember { mutableStateOf(false) }
    var addingEvent by remember { mutableStateOf(false) }
    /** 입력 모드에서 방금 한 토글의 안내 문구. 입력 모드를 나가면 비운다. */
    var lastAction by remember { mutableStateOf<String?>(null) }

    // 위젯에서 날짜를 골랐다면 근무 입력 모드를 끄고(방금 한 입력 안내도 버린다) 그 날짜를 보여 준다.
    LaunchedEffect(openRequest) {
        val request = openRequest ?: return@LaunchedEffect
        inputMode = false
        lastAction = null
        request.date?.let(viewModel::showDate)
        onOpenRequestHandled()
    }

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

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !inputMode,
                    onClick = { inputMode = false; lastAction = null },
                    label = { Text("보기") },
                )
                FilterChip(
                    selected = inputMode,
                    onClick = { inputMode = true },
                    label = { Text("근무·외출 입력") },
                )
            }

            if (inputMode) {
                TypeChoiceRow(
                    selectedType = selectedType,
                    isPassMode = isPassMode,
                    onSelectDuty = { selectedType = it; isPassMode = false },
                    onSelectPass = { isPassMode = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }

            MonthGrid(
                days = uiState.days,
                selectedDates = if (inputMode) emptySet() else setOfNotNull(uiState.selectedDate),
                fillSelected = false,
                onDayClick = { date ->
                    if (inputMode) {
                        if (isPassMode) viewModel.togglePass(date) { lastAction = it }
                        else viewModel.toggleDuty(date, selectedType) { lastAction = it }
                    } else {
                        viewModel.selectDate(date)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            if (inputMode) {
                InputGuide(
                    lastAction = lastAction,
                    isPassMode = isPassMode,
                    selectedType = selectedType,
                    autoAddOffDuty = uiState.autoAddOffDuty,
                )
            } else {
                SelectedDayDetail(
                    uiState.selectedDayInfo,
                    onDeleteEvent = viewModel::deleteEvent,
                    onDeletePass = viewModel::deletePass,
                    onDeleteDuty = viewModel::deleteDuty,
                    addingEvent = addingEvent,
                    onToggleAddEvent = { addingEvent = !addingEvent },
                )

                uiState.selectedDate?.let { selected ->
                    AddEventSection(
                        expanded = addingEvent,
                        onClose = { addingEvent = false },
                        selectedDate = selected,
                        onAddEvent = viewModel::addEvent,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

/** 입력 모드에서 고르는 종류 칩(당직/비번/츄라이/외출). 달력 위에 둬서 날짜를 누르기 전에 고른다. */
@Composable
private fun TypeChoiceRow(
    selectedType: DutyType,
    isPassMode: Boolean,
    onSelectDuty: (DutyType) -> Unit,
    onSelectPass: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text("입력할 종류", style = MaterialTheme.typography.bodySmall)
        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DutyType.entries.forEach { type ->
                FilterChip(
                    selected = !isPassMode && selectedType == type,
                    onClick = { onSelectDuty(type) },
                    label = { Text(dutyTypeLabel(type)) },
                )
            }
            FilterChip(selected = isPassMode, onClick = onSelectPass, label = { Text("외출") })
        }
    }
}

/** 입력 모드 하단 안내: 방금 한 동작(있으면)과 토글 방식, 종류별 자동 추가 규칙. */
@Composable
private fun InputGuide(
    lastAction: String?,
    isPassMode: Boolean,
    selectedType: DutyType,
    autoAddOffDuty: Boolean,
) {
    Column {
        if (lastAction != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("방금: $lastAction", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(
            "날짜를 누르면 고른 종류가 바로 저장되고, 이미 있는 날을 다시 누르면 지워집니다",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (isPassMode) {
            Text(
                "평일/휴일은 날짜별로 자동 판정됩니다",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else if (selectedType == DutyType.DUTY && autoAddOffDuty) {
            Text(
                "당직을 넣으면 다음 날 비번도 함께 들어가고, 당직을 지우면 그 비번도 함께 지워집니다",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
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
private fun SelectedDayDetail(
    day: CalendarDayInfo?,
    onDeleteEvent: (Event) -> Unit,
    onDeletePass: (PassRecord) -> Unit,
    onDeleteDuty: (DutyAssignment) -> Unit,
    addingEvent: Boolean,
    onToggleAddEvent: () -> Unit,
) {
    if (day == null) {
        Text("날짜를 선택하면 상세 내용을 볼 수 있습니다", style = MaterialTheme.typography.bodyMedium)
        return
    }

    Column {
        // 일정 추가 버튼은 날짜 옆에 붙여 상세 영역을 간결하게 유지한다.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${day.date}", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onToggleAddEvent) {
                Text(if (addingEvent) "닫기" else "+ 일정 추가")
            }
        }

        if (!day.hasAnyMarker) {
            Text(
                "이 날은 등록된 일정이 없습니다",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        day.leaveNames.forEach { DetailLine("$it 사용일") }
        if (day.hasCombatRest) DetailLine("전투휴무 사용일")
        if (day.hasOvernight) DetailLine("외박일")
        day.passRecords.forEach { record ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "외출일 (${passTypeLabel(record.type)})",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onDeletePass(record) }) {
                    Icon(Icons.Filled.Close, contentDescription = "외출 기록 삭제")
                }
            }
        }
        day.dutyAssignments.forEach { assignment ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "근무: ${dutyTypeLabel(assignment.type)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onDeleteDuty(assignment) }) {
                    Icon(Icons.Filled.Close, contentDescription = "근무 취소")
                }
            }
        }
        day.events.forEach { event ->
            EventLine(event, onDelete = { onDeleteEvent(event) })
        }
    }
}

private fun passTypeLabel(type: PassType): String = if (type == PassType.WEEKDAY) "평일" else "휴일"

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
    expanded: Boolean,
    onClose: () -> Unit,
    selectedDate: LocalDate,
    onAddEvent: (title: String, startDate: LocalDate, endDate: LocalDate?, isImportant: Boolean, memo: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
                    onClose()
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
