package com.kangminsang.hagoondori.ui.leave

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kangminsang.hagoondori.core.calc.ConductPointCalculator
import com.kangminsang.hagoondori.core.model.ConductPoint
import com.kangminsang.hagoondori.core.model.ConductPointKind
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.Overtime
import com.kangminsang.hagoondori.ui.common.Button
import com.kangminsang.hagoondori.ui.common.Card
import com.kangminsang.hagoondori.ui.common.DateTextField
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.datetime.LocalDate

private fun ConductPointKind.label() = if (this == ConductPointKind.MERIT) "양호" else "과실"

private fun Overtime.dayLabel() = if (onHoliday) "휴일" else "평일"

/** 기록 입력 방식: 양호/과실 점수를 직접 넣거나, 초과근무 시간으로 양호를 계산한다. */
private enum class EntryMode(val label: String) { MERIT("양호"), DEMERIT("과실"), OVERTIME("초과근무") }

/**
 * 점수 탭: 양호·과실 점수 합계와 기록, 초과근무 양호 기록, 점수 전환 현황.
 * 양호 10점마다 포상휴가 1일, 과실 20점마다 외박제한 1회를 누적 기준으로 계산하고,
 * 아직 기록하지 않은 만큼을 버튼 한 번으로 휴가 부여 / 외박 차수 소멸 기록으로 남긴다.
 * 장치로 보내지 않는 개인 기록이다.
 */
@Composable
fun ConductPointTab(
    uiState: LeaveManagementUiState,
    onAdd: (date: LocalDate, kind: ConductPointKind, centiPoints: Int, reason: String, memo: String?, overtime: Overtime?) -> Unit,
    onUpdate: (record: ConductPoint, date: LocalDate, kind: ConductPointKind, centiPoints: Int, reason: String, memo: String?, overtime: Overtime?) -> Unit,
    onDelete: (ConductPoint) -> Unit,
    onGrantRewardLeave: (days: Int) -> Unit,
    onAddOvernightRestriction: (slotIndex: Int, reason: String?, recordedDate: LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = ConductPointCalculator.summarize(uiState.conductPoints)
    val conversion = ConductPointCalculator.conversion(summary, uiState.leaveGrants, uiState.overnightForfeitures)
    val rewardLeaveType = ConductPointCalculator.findRewardLeaveType(uiState.leaveTypes)
    var showForm by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<ConductPoint?>(null) }

    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("양호·과실 점수", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("양호 ${ConductPointCalculator.format(summary.merit)}점", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("과실 ${ConductPointCalculator.format(summary.demerit)}점", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                val net = summary.net
                Text(
                    "합계 " + (if (net > 0) "+" else if (net < 0) "−" else "") + "${ConductPointCalculator.format(net)}점",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "포상휴가 ${conversion.rewardLeaveDaysEarned}일 (다음 1일까지 ${ConductPointCalculator.format(conversion.meritTowardNext)}점)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "외박제한 ${conversion.overnightRestrictionsEarned}회 (다음 1회까지 ${ConductPointCalculator.format(conversion.demeritTowardNext)}점)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (conversion.rewardLeaveDaysPending > 0) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("받을 수 있는 포상휴가 ${conversion.rewardLeaveDaysPending}일", style = MaterialTheme.typography.titleMedium)
                    if (rewardLeaveType == null) {
                        Text("포상휴가 종류가 없어요. 휴가 탭에서 추가해 주세요.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        Button(onClick = { onGrantRewardLeave(conversion.rewardLeaveDaysPending) }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("포상휴가 ${conversion.rewardLeaveDaysPending}일 부여 기록")
                        }
                    }
                }
            }
        }
        if (conversion.overnightRestrictionsPending > 0) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("제한할 외박 ${conversion.overnightRestrictionsPending}회", style = MaterialTheme.typography.titleMedium)
                    if (uiState.upcomingSlotOptions.isEmpty()) {
                        Text("설정에서 첫 외박일을 입력하면 외박 차수에 기록할 수 있어요.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        RestrictionChooser(
                            options = uiState.upcomingSlotOptions,
                            onSubmit = { slotIndex -> onAddOvernightRestriction(slotIndex, ConductPointCalculator.OVERNIGHT_RESTRICTION_REASON, AppClock.today()) },
                        )
                    }
                }
            }
        }
        if (conversion.rewardLeaveOverApplied) {
            Text("이미 기록한 포상휴가가 지금 점수로 받을 수 있는 일수보다 많아요.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
        if (conversion.overnightRestrictionOverApplied) {
            Text("이미 기록한 외박제한이 지금 점수로 받을 수 있는 횟수보다 많아요.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }

        TextButton(onClick = { showForm = !showForm; editingId = null }, modifier = Modifier.padding(top = 16.dp)) {
            Text(if (showForm) "닫기" else "+ 점수 기록 추가")
        }
        if (showForm) {
            ConductPointForm(
                holidays = uiState.holidays,
                onSubmit = { date, kind, points, reason, memo, overtime ->
                    onAdd(date, kind, points, reason, memo, overtime)
                    showForm = false
                },
            )
        }

        if (uiState.conductPoints.isNotEmpty()) {
            Text("기록", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            uiState.conductPoints.sortedByDescending { it.date }.forEach { record ->
                val sign = if (record.kind == ConductPointKind.MERIT) "+" else "−"
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${record.date} · ${record.kindLabel()} · $sign${ConductPointCalculator.format(record.centiPoints)}점 · ${record.reason}" +
                            (record.memo?.let { " ($it)" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { editingId = if (editingId == record.id) null else record.id; showForm = false }) {
                        Icon(Icons.Filled.Edit, contentDescription = "점수 기록 수정")
                    }
                    IconButton(onClick = { toDelete = record }) {
                        Icon(Icons.Filled.Close, contentDescription = "점수 기록 삭제")
                    }
                }
                if (editingId == record.id) {
                    ConductPointForm(
                        initial = record,
                        holidays = uiState.holidays,
                        submitText = "수정 저장",
                        onCancel = { editingId = null },
                        onSubmit = { date, kind, points, reason, memo, overtime ->
                            onUpdate(record, date, kind, points, reason, memo, overtime)
                            editingId = null
                        },
                    )
                }
            }
        }

        toDelete?.let { record ->
            AlertDialog(
                onDismissRequest = { toDelete = null },
                title = { Text("점수 기록을 삭제할까요?") },
                text = { Text("${record.date} · ${record.kindLabel()} ${ConductPointCalculator.format(record.centiPoints)}점 · ${record.reason} 기록을 지웁니다.") },
                confirmButton = { Button(onClick = { onDelete(record); toDelete = null }) { Text("삭제") } },
                dismissButton = { Button(onClick = { toDelete = null }) { Text("취소") } },
            )
        }
    }
}

private fun ConductPoint.kindLabel() = overtime?.let { "초과근무(${it.dayLabel()} ${ConductPointCalculator.formatHours(it.tenthHours)}시간)" } ?: kind.label()

/** 외박제한을 적용할 예정 차수를 날짜로 고른다(차수 번호를 직접 입력하지 않는다). */
@Composable
private fun RestrictionChooser(options: List<UpcomingSlotOption>, onSubmit: (slotIndex: Int) -> Unit) {
    var selected by remember(options) { mutableStateOf(options.first()) }
    Column {
        Text("어느 차수를 제한할까요? (예정일에서 고릅니다)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        options.forEach { option ->
            TextButton(onClick = { selected = option }) {
                Text((if (selected == option) "● " else "○ ") + "${option.slotIndex + 1}차 (${option.scheduledDate})")
            }
        }
        Button(onClick = { onSubmit(selected.slotIndex) }, modifier = Modifier.padding(top = 4.dp)) { Text("외박제한 기록") }
    }
}

@Composable
private fun ConductPointForm(
    holidays: List<Holiday>,
    onSubmit: (date: LocalDate, kind: ConductPointKind, centiPoints: Int, reason: String, memo: String?, overtime: Overtime?) -> Unit,
    initial: ConductPoint? = null,
    submitText: String = "저장",
    onCancel: (() -> Unit)? = null,
) {
    var date by remember { mutableStateOf<LocalDate?>(initial?.date ?: AppClock.today()) }
    var mode by remember {
        mutableStateOf(
            when {
                initial?.overtime != null -> EntryMode.OVERTIME
                initial?.kind == ConductPointKind.DEMERIT -> EntryMode.DEMERIT
                else -> EntryMode.MERIT
            },
        )
    }
    var hoursText by remember { mutableStateOf(initial?.overtime?.let { ConductPointCalculator.formatHours(it.tenthHours) }.orEmpty()) }
    var overrideHoliday by remember { mutableStateOf(initial?.overtime?.onHoliday) }
    var pointsText by remember { mutableStateOf(initial?.centiPoints?.let(ConductPointCalculator::format).orEmpty()) }
    var reason by remember { mutableStateOf(initial?.reason.orEmpty()) }
    var memo by remember { mutableStateOf(initial?.memo.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    val currentDate = date
    val autoHoliday = currentDate?.let { ConductPointCalculator.isHolidayOvertime(it, holidays) } ?: false
    val onHoliday = overrideHoliday ?: autoHoliday
    val parsedHours = ConductPointCalculator.validateOvertimeHours(hoursText).tenthHours

    Column {
        DateTextField("날짜", date, { date = it; overrideHoliday = null }, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.padding(top = 4.dp)) {
            EntryMode.entries.forEach { m ->
                TextButton(onClick = { mode = m }) { Text((if (mode == m) "● " else "○ ") + m.label) }
            }
        }
        if (mode == EntryMode.OVERTIME) {
            OutlinedTextField(
                value = hoursText,
                onValueChange = { hoursText = it.filter { c -> c.isDigit() || c == '.' }.take(4) },
                label = { Text("시간 (예: 1.5)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "자동 판정: ${if (autoHoliday) "휴일" else "평일"} (직접 바꾸려면 아래에서 선택)",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row {
                listOf(false to "평일", true to "휴일").forEach { (holiday, label) ->
                    TextButton(onClick = { overrideHoliday = holiday }) { Text((if (onHoliday == holiday) "● " else "○ ") + label) }
                }
            }
            parsedHours?.let {
                Text(
                    "→ 양호 ${ConductPointCalculator.format(ConductPointCalculator.overtimeCentiPoints(it, onHoliday))}점",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            OutlinedTextField(
                value = pointsText,
                onValueChange = { pointsText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                label = { Text("점수 (예: 0.25)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text(if (mode == EntryMode.OVERTIME) "사유(선택)" else "사유") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = memo,
            onValueChange = { memo = it },
            label = { Text("메모(선택)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = {
                val d = date
                if (mode == EntryMode.OVERTIME) {
                    val hours = ConductPointCalculator.validateOvertimeHours(hoursText)
                    val tenth = hours.tenthHours
                    val trimmedReason = reason.trim()
                    error = when {
                        d == null -> "날짜를 입력해 주세요."
                        tenth == null -> hours.error
                        trimmedReason.length > ConductPointCalculator.MAX_REASON_LENGTH ->
                            "사유는 ${ConductPointCalculator.MAX_REASON_LENGTH}자 이하로 입력해 주세요."
                        memo.trim().length > ConductPointCalculator.MAX_MEMO_LENGTH ->
                            "메모는 ${ConductPointCalculator.MAX_MEMO_LENGTH}자 이하로 입력해 주세요."
                        else -> {
                            onSubmit(
                                d, ConductPointKind.MERIT, ConductPointCalculator.overtimeCentiPoints(tenth, onHoliday),
                                trimmedReason.ifEmpty { ConductPointCalculator.overtimeReason(tenth, onHoliday) },
                                memo.trim().ifBlank { null }, Overtime(tenth, onHoliday),
                            )
                            null
                        }
                    }
                } else {
                    val input = ConductPointCalculator.validate(pointsText, reason, memo)
                    when {
                        d == null -> error = "날짜를 입력해 주세요."
                        input.error != null -> error = input.error
                        else -> {
                            error = null
                            val kind = if (mode == EntryMode.DEMERIT) ConductPointKind.DEMERIT else ConductPointKind.MERIT
                            onSubmit(d, kind, input.centiPoints!!, input.reason, memo.trim().ifBlank { null }, null)
                        }
                    }
                }
            }) { Text(submitText) }
            if (onCancel != null) {
                TextButton(onClick = onCancel) { Text("취소") }
            }
        }
    }
}
