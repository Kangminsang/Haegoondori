package com.kangminsang.hagoondori.ui.duty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kangminsang.hagoondori.core.calc.HolidayJudge
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.ui.calendar.CalendarGridBuilder
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * ④ 근무 일괄 입력 (스펙 5.1/5.2절, F6) - 당직표를 다중 선택으로 한 번에 입력.
 * 날짜를 여러 개 고른 뒤 근무 종류 하나를 선택해 한 번에 저장한다.
 */
@Composable
fun DutyBulkInputScreen(
    modifier: Modifier = Modifier,
    viewModel: DutyBulkInputViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember { AppClock.today() }

    var year by remember { mutableStateOf(today.year) }
    var month by remember { mutableStateOf(today.monthNumber) }
    var selectedDates by remember { mutableStateOf(setOf<LocalDate>()) }
    var selectedType by remember { mutableStateOf(DutyType.DUTY) }
    var isPassMode by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { if (month == 1) { year -= 1; month = 12 } else month -= 1 }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "이전 달")
                }
                Text("${year}년 ${month}월", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { if (month == 12) { year += 1; month = 1 } else month += 1 }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "다음 달")
                }
            }

            Text(
                "여러 날짜를 탭해서 선택한 뒤, 근무 종류(또는 외출)를 골라 한 번에 저장하세요",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            DutyMonthGrid(
                year = year,
                month = month,
                holidays = uiState.holidays,
                existingAssignments = uiState.existingAssignments,
                existingPasses = uiState.existingPasses,
                selectedDates = selectedDates,
                onToggleDate = { date ->
                    selectedDates = if (date in selectedDates) selectedDates - date else selectedDates + date
                },
            )

            Row(modifier = Modifier.padding(top = 16.dp)) {
                DutyType.entries.forEach { type ->
                    FilterChip(
                        selected = !isPassMode && selectedType == type,
                        onClick = { selectedType = type; isPassMode = false },
                        label = { Text(dutyTypeLabel(type)) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                FilterChip(
                    selected = isPassMode,
                    onClick = { isPassMode = true },
                    label = { Text("외출") },
                )
            }

            if (isPassMode) {
                Text(
                    "평일/휴일은 날짜별로 자동 판정됩니다. 이미 외출이 기록된 날짜는 건너뜁니다",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (!isPassMode && selectedType == DutyType.DUTY && uiState.profile?.autoAddOffDuty == true) {
                Text(
                    "당직으로 저장하면 선택한 날짜들의 다음 날이 비번으로 자동 추가됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Button(
                enabled = selectedDates.isNotEmpty(),
                onClick = {
                    val onResult = { saved: Int, skipped: Int ->
                        val message = if (skipped > 0) {
                            "${saved}건 저장, ${skipped}건은 이미 있어 건너뜀"
                        } else {
                            "${saved}건 저장했습니다"
                        }
                        selectedDates = emptySet()
                        scope.launch { snackbarHostState.showSnackbar(message) }
                        Unit
                    }
                    if (isPassMode) viewModel.submitPass(selectedDates, onResult) else viewModel.submit(selectedDates, selectedType, onResult)
                },
                modifier = Modifier.padding(top = 16.dp),
            ) { Text("${selectedDates.size}건 일괄 저장") }
        }
    }
}


@Composable
private fun DutyMonthGrid(
    year: Int,
    month: Int,
    holidays: List<Holiday>,
    existingAssignments: List<DutyAssignment>,
    existingPasses: List<PassRecord>,
    selectedDates: Set<LocalDate>,
    onToggleDate: (LocalDate) -> Unit,
) {
    val gridDates = remember(year, month) { CalendarGridBuilder.buildGrid(year, month) }
    val assignmentsByDate = remember(existingAssignments) { existingAssignments.groupBy { it.date } }
    val passDates = remember(existingPasses) { existingPasses.map { it.date }.toSet() }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach { label ->
                Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            }
        }
        gridDates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val isCurrentMonth = date.year == year && date.monthNumber == month
                    val isSelected = date in selectedDates
                    val isHoliday = HolidayJudge.isHoliday(date, holidays)
                    val existing = assignmentsByDate[date].orEmpty()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.8f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isCurrentMonth) 0.4f else 0.15f),
                            )
                            .clickable { onToggleDate(date) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val textColor = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            isHoliday -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Text(date.dayOfMonth.toString(), color = textColor, style = MaterialTheme.typography.bodyMedium)
                        val marks = existing.map { dutyTypeShortLabel(it.type) } + if (date in passDates) listOf("외") else emptyList()
                        if (marks.isNotEmpty()) {
                            Text(
                                marks.joinToString("·"),
                                color = textColor,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}
