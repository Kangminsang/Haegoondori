package com.kangminsang.hagoondori.ui.leave

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
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
import com.kangminsang.hagoondori.core.model.ConductConversionRecord
import com.kangminsang.hagoondori.core.model.ConductConversionType
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

private const val OVERTIME_CHIP = "초과근무"

/**
 * 점수 탭: 양호·과실 점수 합계와 기록, 초과근무 양호 기록, 점수 전환 현황.
 * 양호와 과실은 서로 상쇄하고 기한 없이 이월된다. 남은 양호 10점마다 포상휴가 1일, 남은 과실 20점마다 외박제한 1회가
 * 군기지도위원회 심의 대상이 된다. 전환은 자동이 아니며, 위원회가 정한 결과를 회의일과 함께 "위원회 결과 기록"으로 남기면
 * 그만큼의 점수가 차감된다. 휴가 부여와 외박 차수 소멸은 만들지 않으니 휴가·외박 탭에서 직접 기록한다.
 * 장치로 보내지 않는 개인 기록이다.
 */
@Composable
fun ConductPointTab(
    uiState: LeaveManagementUiState,
    onAdd: (date: LocalDate, kind: ConductPointKind, centiPoints: Int, reason: String, memo: String?, overtime: Overtime?) -> Unit,
    onUpdate: (record: ConductPoint, date: LocalDate, kind: ConductPointKind, centiPoints: Int, reason: String, memo: String?, overtime: Overtime?) -> Unit,
    onDelete: (ConductPoint) -> Unit,
    onAddConversion: (type: ConductConversionType, meetingDate: LocalDate, count: Int, memo: String?) -> Unit,
    onDeleteConversion: (ConductConversionRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = ConductPointCalculator.summarize(uiState.conductPoints)
    val conversion = ConductPointCalculator.conversion(summary, uiState.conductConversions)
    var showForm by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<ConductPoint?>(null) }
    var conversionToDelete by remember { mutableStateOf<ConductConversionRecord?>(null) }
    var showRewardForm by remember { mutableStateOf(false) }
    var showRestrictionForm by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("양호·과실 점수", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("양호 ${ConductPointCalculator.format(summary.merit)}점", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("과실 ${ConductPointCalculator.format(summary.demerit)}점", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                if (conversion.rewardLeaveDaysApplied > 0 || conversion.overnightRestrictionsApplied > 0) {
                    Text(
                        "위원회 결과로 차감된 점수: " + listOfNotNull(
                            conversion.rewardLeaveDaysApplied.takeIf { it > 0 }?.let {
                                "포상휴가 ${it}일(양호 −${ConductPointCalculator.format(it * ConductPointCalculator.CENTI_POINTS_PER_REWARD_LEAVE_DAY)}점)"
                            },
                            conversion.overnightRestrictionsApplied.takeIf { it > 0 }?.let {
                                "외박제한 ${it}회(과실 −${ConductPointCalculator.format(it * ConductPointCalculator.CENTI_POINTS_PER_OVERNIGHT_RESTRICTION)}점)"
                            },
                        ).joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                val balance = conversion.balance
                Text(
                    when {
                        balance > 0 -> "남은 점수 양호 ${ConductPointCalculator.format(balance)}점"
                        balance < 0 -> "남은 점수 과실 ${ConductPointCalculator.format(balance)}점"
                        else -> "남은 점수 0점"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "양호와 과실은 서로 상쇄하고, 기한 없이 다음 달로 이어져요.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "다음 포상휴가 1일 심의까지 양호 ${ConductPointCalculator.format(conversion.meritTowardNextRewardDay)}점",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "다음 외박제한 1회 심의까지 과실 ${ConductPointCalculator.format(conversion.demeritTowardNextRestriction)}점",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (conversion.rewardLeaveDaysPending > 0) {
            val pending = conversion.rewardLeaveDaysPending
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("포상휴가 심의 대상 ${pending}일", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "남은 양호로 포상휴가 ${pending}일을 심의받을 수 있어요. 포상과 외박제한은 군기지도위원회 회의를 거쳐 정해지니, 결과가 나오면 기록해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    TextButton(onClick = { showRewardForm = !showRewardForm }) {
                        Text(if (showRewardForm) "닫기" else "+ 위원회 결과 기록")
                    }
                    if (showRewardForm) {
                        CommitteeResultForm(
                            type = ConductConversionType.REWARD_LEAVE,
                            pending = pending,
                            onSubmit = { count, meetingDate, memo ->
                                onAddConversion(ConductConversionType.REWARD_LEAVE, meetingDate, count, memo)
                                showRewardForm = false
                            },
                        )
                    }
                }
            }
        }
        if (conversion.overnightRestrictionsPending > 0) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("외박제한 심의 대상 ${conversion.overnightRestrictionsPending}회", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "남은 과실로 외박제한 ${conversion.overnightRestrictionsPending}회를 심의받을 수 있어요. 포상과 외박제한은 군기지도위원회 회의를 거쳐 정해지니, 결과가 나오면 기록해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    TextButton(onClick = { showRestrictionForm = !showRestrictionForm }) {
                        Text(if (showRestrictionForm) "닫기" else "+ 위원회 결과 기록")
                    }
                    if (showRestrictionForm) {
                        CommitteeResultForm(
                            type = ConductConversionType.OVERNIGHT_RESTRICTION,
                            pending = conversion.overnightRestrictionsPending,
                            onSubmit = { count, meetingDate, memo ->
                                onAddConversion(ConductConversionType.OVERNIGHT_RESTRICTION, meetingDate, count, memo)
                                showRestrictionForm = false
                            },
                        )
                    }
                }
            }
        }
        TextButton(onClick = { showForm = !showForm; editingId = null }, modifier = Modifier.padding(top = 16.dp)) {
            Text(if (showForm) "닫기" else "+ 점수 기록 추가")
        }
        if (showForm) {
            ConductPointForm(
                holidays = uiState.holidays,
                records = uiState.conductPoints,
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
                        records = uiState.conductPoints,
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

        if (uiState.conductConversions.isNotEmpty()) {
            Text("위원회 결과 기록", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            uiState.conductConversions.sortedByDescending { it.meetingDate }.forEach { record ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        record.summaryText() + (record.memo?.let { " ($it)" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { conversionToDelete = record }) {
                        Icon(Icons.Filled.Close, contentDescription = "위원회 결과 기록 삭제")
                    }
                }
            }
        }

        conversionToDelete?.let { record ->
            AlertDialog(
                onDismissRequest = { conversionToDelete = null },
                title = { Text("위원회 결과 기록을 삭제할까요?") },
                text = { Text("${record.summaryText()} 기록을 지웁니다. 차감됐던 점수가 다시 남은 점수에 반영돼요. 휴가·외박 탭의 기록은 그대로예요.") },
                confirmButton = { Button(onClick = { onDeleteConversion(record); conversionToDelete = null }) { Text("삭제") } },
                dismissButton = { Button(onClick = { conversionToDelete = null }) { Text("취소") } },
            )
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

private fun ConductConversionType.unit() = if (this == ConductConversionType.REWARD_LEAVE) "일" else "회"

private fun ConductConversionType.centiPointsPerUnit() =
    if (this == ConductConversionType.REWARD_LEAVE) ConductPointCalculator.CENTI_POINTS_PER_REWARD_LEAVE_DAY
    else ConductPointCalculator.CENTI_POINTS_PER_OVERNIGHT_RESTRICTION

/** "2026-11-05 · 포상휴가 2일 · 양호 −20점" 형태의 한 줄 요약(메모 제외). */
private fun ConductConversionRecord.summaryText(): String {
    val name = if (type == ConductConversionType.REWARD_LEAVE) "포상휴가" else "외박제한"
    val kind = if (type == ConductConversionType.REWARD_LEAVE) "양호" else "과실"
    return "$meetingDate · $name $count${type.unit()} · $kind −${ConductPointCalculator.format(count * type.centiPointsPerUnit())}점"
}

/** 위원회가 정한 결과를 기록한다: 회의일 + 일수/횟수(1~[pending]) + 메모(선택). 휴가·외박 기록은 만들지 않는다. */
@Composable
private fun CommitteeResultForm(type: ConductConversionType, pending: Int, onSubmit: (count: Int, meetingDate: LocalDate, memo: String?) -> Unit) {
    val unit = type.unit()
    val isReward = type == ConductConversionType.REWARD_LEAVE
    var meetingDate by remember { mutableStateOf<LocalDate?>(AppClock.today()) }
    var countText by remember(pending) { mutableStateOf(pending.toString()) }
    var memo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val parsed = ConductPointCalculator.validateConversionCount(countText, pending, unit).count
    Column {
        DateTextField("회의일", meetingDate, { meetingDate = it }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = countText,
            onValueChange = { countText = it.filter(Char::isDigit).take(4) },
            label = { Text((if (isReward) "일수" else "횟수") + " (1~${pending}$unit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = memo,
            onValueChange = { memo = it },
            label = { Text("메모(선택)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Text(
            "기록하면 ${if (isReward) "양호" else "과실"} ${ConductPointCalculator.format((parsed ?: pending) * type.centiPointsPerUnit())}점이 차감돼요.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            if (isReward) "휴가 부여는 휴가 탭에서 직접 기록해 주세요." else "외박 차수 소멸은 외박 탭에서 직접 기록해 주세요.",
            style = MaterialTheme.typography.bodySmall,
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
        Button(
            onClick = {
                val d = meetingDate
                val input = ConductPointCalculator.validateConversionCount(countText, pending, unit)
                when {
                    d == null -> error = "회의일을 입력해 주세요."
                    input.error != null -> error = input.error
                    memo.trim().length > ConductPointCalculator.MAX_MEMO_LENGTH ->
                        error = "메모는 ${ConductPointCalculator.MAX_MEMO_LENGTH}자 이하로 입력해 주세요."
                    else -> { error = null; onSubmit(input.count!!, d, memo.trim().ifBlank { null }) }
                }
            },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("저장") }
    }
}

@Composable
private fun ConductPointForm(
    holidays: List<Holiday>,
    records: List<ConductPoint>,
    onSubmit: (date: LocalDate, kind: ConductPointKind, centiPoints: Int, reason: String, memo: String?, overtime: Overtime?) -> Unit,
    initial: ConductPoint? = null,
    submitText: String = "저장",
    onCancel: (() -> Unit)? = null,
) {
    var date by remember { mutableStateOf<LocalDate?>(initial?.date ?: AppClock.today()) }
    var kind by remember { mutableStateOf(initial?.kind ?: ConductPointKind.MERIT) }
    var isOvertime by remember { mutableStateOf(initial?.overtime != null) }
    var hoursText by remember { mutableStateOf(initial?.overtime?.let { ConductPointCalculator.formatHours(it.tenthHours) }.orEmpty()) }
    var overrideHoliday by remember { mutableStateOf(initial?.overtime?.onHoliday) }
    var pointsText by remember { mutableStateOf(initial?.centiPoints?.let(ConductPointCalculator::format).orEmpty()) }
    var reason by remember { mutableStateOf(initial?.reason.orEmpty()) }
    var memo by remember { mutableStateOf(initial?.memo.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    val currentDate = date
    val autoHoliday = currentDate?.let { ConductPointCalculator.isHolidayOvertime(it, holidays) } ?: false
    val onHoliday = overrideHoliday ?: autoHoliday
    val recentReasons = ConductPointCalculator.recentReasons(records, kind)
    val parsedHours = ConductPointCalculator.validateOvertimeHours(hoursText).tenthHours

    Column {
        DateTextField("날짜", date, { date = it; overrideHoliday = null }, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.padding(top = 4.dp)) {
            ConductPointKind.entries.forEach { k ->
                TextButton(onClick = { kind = k; isOvertime = false }) { Text((if (kind == k) "● " else "○ ") + k.label()) }
            }
        }
        if (isOvertime) {
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
            label = { Text(if (isOvertime) "사유(선택)" else "사유") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (kind == ConductPointKind.MERIT || recentReasons.isNotEmpty()) {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                if (kind == ConductPointKind.MERIT) {
                    FilterChip(
                        selected = isOvertime,
                        onClick = {
                            isOvertime = !isOvertime
                            if (isOvertime) reason = ""
                        },
                        label = { Text(OVERTIME_CHIP) },
                    )
                }
                recentReasons.forEach { r ->
                    FilterChip(
                        selected = !isOvertime && reason.trim() == r,
                        onClick = { isOvertime = false; reason = r },
                        label = { Text(r) },
                    )
                }
            }
        }
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
                if (isOvertime) {
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
