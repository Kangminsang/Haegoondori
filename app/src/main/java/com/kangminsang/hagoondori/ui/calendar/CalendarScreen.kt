package com.kangminsang.hagoondori.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Edit
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** 달력 화면 모드: 보기, 근무·외출 탭 입력(F6), 휴가 계획(기간을 고르고 종류별로 나눔). */
enum class CalendarMode { VIEW, DUTY_INPUT, LEAVE_PLAN }

/**
 * ② 달력 (스펙 5.1/5.2절, F3) - 월간 달력 + 날짜별 항목 표시, 근무·외출 탭 입력 모드(F6), 휴가 계획 모드.
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
    // 휴가 계획 모드: 시작일·종료일을 차례로 눌러 기간을 고른 뒤 아래 패널에서 외박·휴가 종류별 일수를 나눈다.
    var mode by remember { mutableStateOf(CalendarMode.VIEW) }
    var selectedType by remember { mutableStateOf(DutyType.DUTY) }
    var isPassMode by remember { mutableStateOf(false) }
    var addingEvent by remember { mutableStateOf(false) }
    /** 수정 중인 일반 일정의 id. 날짜를 바꾸거나 입력 모드에 들어가면 비운다. */
    var editingEventId by remember { mutableStateOf<String?>(null) }
    /** 방금 한 토글·계획 저장·출타 삭제의 안내 문구. 모드를 바꾸면 비운다. */
    var lastAction by remember { mutableStateOf<String?>(null) }

    // 선택한 날짜가 바뀌면 수정 중이던 일정 폼은 닫는다(다른 날의 일정을 가리키지 않도록).
    LaunchedEffect(uiState.selectedDate) {
        editingEventId = null
    }

    // 위젯에서 날짜를 골랐다면 근무 입력 모드를 끄고(방금 한 입력 안내도 버린다) 그 날짜를 보여 준다.
    LaunchedEffect(openRequest) {
        val request = openRequest ?: return@LaunchedEffect
        mode = CalendarMode.VIEW
        viewModel.clearPlan()
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
                listOf(
                    CalendarMode.VIEW to "보기",
                    CalendarMode.DUTY_INPUT to "근무·외출 입력",
                    CalendarMode.LEAVE_PLAN to "휴가 계획",
                ).forEach { (chipMode, label) ->
                    FilterChip(
                        selected = mode == chipMode,
                        onClick = {
                            if (mode != chipMode) {
                                mode = chipMode
                                lastAction = null
                                editingEventId = null
                                viewModel.clearPlan()
                            }
                        },
                        label = { Text(label) },
                    )
                }
            }

            if (mode == CalendarMode.DUTY_INPUT) {
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
                selectedDates = when (mode) {
                    CalendarMode.VIEW -> setOfNotNull(uiState.selectedDate)
                    CalendarMode.DUTY_INPUT -> emptySet()
                    CalendarMode.LEAVE_PLAN -> uiState.plan?.let { planDates(it.start, it.end) }.orEmpty()
                },
                fillSelected = mode == CalendarMode.LEAVE_PLAN,
                dayLabels = if (mode == CalendarMode.LEAVE_PLAN) uiState.plan?.dayLabels.orEmpty() else emptyMap(),
                onDayClick = { date ->
                    when (mode) {
                        CalendarMode.VIEW -> viewModel.selectDate(date)
                        CalendarMode.DUTY_INPUT ->
                            if (isPassMode) viewModel.togglePass(date) { lastAction = it }
                            else viewModel.toggleDuty(date, selectedType) { lastAction = it }
                        CalendarMode.LEAVE_PLAN -> viewModel.tapPlanDate(date)
                    }
                },
                onSwipePrevious = viewModel::previousMonth,
                onSwipeNext = viewModel::nextMonth,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            if (mode == CalendarMode.DUTY_INPUT) {
                InputGuide(
                    lastAction = lastAction,
                    isPassMode = isPassMode,
                    selectedType = selectedType,
                    autoAddOffDuty = uiState.autoAddOffDuty,
                )
            } else if (mode == CalendarMode.LEAVE_PLAN) {
                LastActionBanner(lastAction)
                val plan = uiState.plan
                val planEnd = plan?.end
                if (plan == null || planEnd == null) {
                    Text(
                        if (plan == null) "출타 시작일을 누르세요" else "${plan.start.slashLabel()} 시작 · 종료일을 누르세요 (다음 달은 넘겨서 고릅니다)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    Text(
                        if (plan.brush != null) {
                            "날짜를 누르면 고른 종류로 바뀝니다. 같은 종류를 다시 누르면 비워지고, 처음·끝 날이면 기간에서 빠집니다. 기간 바깥을 누르면 그 종류로 늘어납니다"
                        } else {
                            "종류를 고르고 날짜를 누르면 그 종류로 바뀝니다. 종류를 고르지 않으면 기간 바깥을 눌러 늘리고, 처음·끝 날을 눌러 줄입니다"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    LeavePlanPanel(
                        start = plan.start,
                        end = planEnd,
                        rows = plan.rows,
                        dayKinds = plan.dayKinds,
                        brush = plan.brush,
                        onSelectBrush = viewModel::selectPlanBrush,
                        onAutoFill = viewModel::autoFillPlan,
                        onSave = { viewModel.savePlan { lastAction = it } },
                        onCancel = {
                            viewModel.clearPlan()
                            if (plan.editing) mode = CalendarMode.VIEW
                        },
                        onRestart = viewModel::clearPlan,
                        onDeleteTrip = if (plan.editing) {
                            { viewModel.deleteEditedTrip { lastAction = it } }
                        } else {
                            null
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        "같은 종류가 이어진 날마다 기록 하나로 저장됩니다. 기간과 겹친 기존 휴가·외박은 이 계획으로 바뀌고, 기간 밖 부분은 남습니다. 수정 중 줄여서 빠진 날의 기록은 지워집니다",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            } else {
                LastActionBanner(lastAction)
                SelectedDayDetail(
                    uiState.selectedDayInfo,
                    tripLine = {
                        uiState.selectedTrip?.let { trip ->
                            TripLine(
                                trip,
                                onEdit = {
                                    mode = CalendarMode.LEAVE_PLAN
                                    lastAction = null
                                    editingEventId = null
                                    viewModel.editTrip(trip.start, trip.end)
                                },
                                onDelete = { viewModel.deleteTrip(trip.start, trip.end) { lastAction = it } },
                            )
                        }
                    },
                    onDeleteEvent = viewModel::deleteEvent,
                    onDeletePass = viewModel::deletePass,
                    onDeleteDuty = viewModel::deleteDuty,
                    addingEvent = addingEvent,
                    onToggleAddEvent = { addingEvent = !addingEvent; editingEventId = null },
                    editingEventId = editingEventId,
                    onEditEvent = { id ->
                        editingEventId = id
                        if (id != null) addingEvent = false
                    },
                    onUpdateEvent = viewModel::updateEvent,
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
        LastActionBanner(lastAction)
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

/** 방금 한 동작의 한 줄 안내(있을 때만). */
@Composable
private fun LastActionBanner(lastAction: String?) {
    if (lastAction == null) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text("방금: $lastAction", style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * 보기 모드에서 고른 날이 속한 출타: "이번 출타: 9/22~9/28 7일 (외박1 포상3 연가3)" + [수정] [삭제] 아이콘.
 * 같은 상세 영역의 일정 줄([EventLine])과 같은 모양이고, 삭제는 확인 다이얼로그를 거친다.
 */
@Composable
private fun TripLine(trip: TripSummary, onEdit: () -> Unit, onDelete: () -> Unit) {
    var confirmingDelete by remember(trip) { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "이번 출타: ${trip.start.slashLabel()}~${trip.end.slashLabel()} ${trip.totalDays}일 (${trip.partsText})",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "출타 수정")
        }
        IconButton(onClick = { confirmingDelete = true }) {
            Icon(Icons.Filled.Close, contentDescription = "출타 삭제")
        }
    }
    if (confirmingDelete) {
        DeleteTripDialog(
            message = "${trip.start.slashLabel()}~${trip.end.slashLabel()} 출타의 휴가·외박 기록을 모두 지웁니다.",
            onConfirm = { confirmingDelete = false; onDelete() },
            onDismiss = { confirmingDelete = false },
        )
    }
}

/** 휴가 계획 기간의 날짜 집합(달력 칠하기용). 종료일을 아직 안 골랐으면 시작일 하나. */
private fun planDates(start: LocalDate, end: LocalDate?): Set<LocalDate> {
    if (end == null) return setOf(start)
    val result = mutableSetOf<LocalDate>()
    var day = start
    while (day <= end) {
        result += day
        day = day.plus(1, DateTimeUnit.DAY)
    }
    return result
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
    /** 고른 날이 속한 출타 줄. 휴가·외박 줄 바로 아래에 그린다. */
    tripLine: @Composable () -> Unit,
    onDeleteEvent: (Event) -> Unit,
    onDeletePass: (PassRecord) -> Unit,
    onDeleteDuty: (DutyAssignment) -> Unit,
    addingEvent: Boolean,
    onToggleAddEvent: () -> Unit,
    editingEventId: String?,
    /** 일정 수정 폼을 연다(id). null이면 닫는다. */
    onEditEvent: (String?) -> Unit,
    onUpdateEvent: (Event) -> Unit,
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
        tripLine()
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
            if (event.id == editingEventId) {
                EventEditForm(
                    event = event,
                    onUpdate = onUpdateEvent,
                    onClose = { onEditEvent(null) },
                )
            } else {
                EventLine(
                    event,
                    onEdit = { onEditEvent(event.id) },
                    onDelete = { onDeleteEvent(event) },
                )
            }
        }
    }
}

private fun passTypeLabel(type: PassType): String = if (type == PassType.WEEKDAY) "평일" else "휴일"

@Composable
private fun DetailLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun EventLine(event: Event, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (event.isImportant) "★ ${event.title}" else event.title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "일정 수정")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Close, contentDescription = "일정 삭제")
        }
    }
}

/** 일반 일정 저장 가능 여부: 제목이 비어 있지 않고 경고 이상이 아니며, 시작일이 있고 종료일이 시작일 이후다. */
private fun isEventFormValid(title: String, startDate: LocalDate?, endDate: LocalDate?): Boolean =
    title.isNotBlank() &&
        TitleValidator.validate(title) !is TitleValidation.Invalid &&
        startDate != null &&
        (endDate == null || endDate >= startDate)

/**
 * 일정 추가·수정 폼의 공통 입력 칸(제목, 시작일, 종료일, 메모, 중요 표시).
 * 시작일 칸만 호출하는 쪽에서 정한다(추가는 선택한 날짜의 읽기 전용 표시, 수정은 날짜 선택 필드).
 */
@Composable
private fun EventFormFields(
    title: String,
    onTitleChange: (String) -> Unit,
    startDateField: @Composable () -> Unit,
    endDate: LocalDate?,
    onEndDateChange: (LocalDate?) -> Unit,
    memo: String,
    onMemoChange: (String) -> Unit,
    isImportant: Boolean,
    onImportantChange: (Boolean) -> Unit,
) {
    val titleValidation = TitleValidator.validate(title)

    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
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

    startDateField()
    DateTextField(
        "종료일(선택, 여러 날에 걸친 일정만)",
        endDate,
        onEndDateChange,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    // 날짜 선택기는 값을 비울 수 없으므로, 여러 날 일정을 하루짜리로 되돌릴 수 있게 따로 지운다.
    if (endDate != null) {
        TextButton(onClick = { onEndDateChange(null) }) { Text("종료일 지우기") }
    }
    OutlinedTextField(
        value = memo,
        onValueChange = onMemoChange,
        label = { Text("메모(선택, 장치에는 표시 안 됨)") },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = isImportant, onCheckedChange = onImportantChange)
        Text("중요 일정으로 표시")
    }
}

/** 기존 일반 일정 수정 폼. 시작일·종료일·제목·메모·중요 표시를 고칠 수 있다. */
@Composable
private fun EventEditForm(
    event: Event,
    onUpdate: (Event) -> Unit,
    onClose: () -> Unit,
) {
    var title by remember(event.id) { mutableStateOf(event.title) }
    var startDate by remember(event.id) { mutableStateOf<LocalDate?>(event.startDate) }
    var endDate by remember(event.id) { mutableStateOf(event.endDate) }
    var memo by remember(event.id) { mutableStateOf(event.memo.orEmpty()) }
    var isImportant by remember(event.id) { mutableStateOf(event.isImportant) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface)
            .padding(8.dp),
    ) {
        Text("일정 수정", style = MaterialTheme.typography.titleSmall)
        EventFormFields(
            title = title,
            onTitleChange = { title = it },
            startDateField = {
                DateTextField(
                    "시작일",
                    startDate,
                    { startDate = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            },
            endDate = endDate,
            onEndDateChange = { endDate = it },
            memo = memo,
            onMemoChange = { memo = it },
            isImportant = isImportant,
            onImportantChange = { isImportant = it },
        )
        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                enabled = isEventFormValid(title, startDate, endDate),
                onClick = {
                    startDate?.let { start ->
                        onUpdate(
                            event.copy(
                                title = title.trim(),
                                startDate = start,
                                endDate = endDate,
                                isImportant = isImportant,
                                memo = memo.ifBlank { null },
                            ),
                        )
                        onClose()
                    }
                },
            ) { Text("수정 저장") }
            TextButton(onClick = onClose) { Text("취소") }
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

            EventFormFields(
                title = title,
                onTitleChange = { title = it },
                startDateField = { DateTextFieldReadOnlyLabel(selectedDate, modifier = Modifier.padding(top = 8.dp)) },
                endDate = endDate,
                onEndDateChange = { endDate = it },
                memo = memo,
                onMemoChange = { memo = it },
                isImportant = isImportant,
                onImportantChange = { isImportant = it },
            )
            Button(
                enabled = isEventFormValid(title, selectedDate, endDate),
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
