package com.kangminsang.hagoondori.ui.leave

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.kangminsang.hagoondori.ui.common.Button
import com.kangminsang.hagoondori.ui.common.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kangminsang.hagoondori.core.calc.LeaveCalculator
import com.kangminsang.hagoondori.core.calc.LeaveTypeError
import com.kangminsang.hagoondori.core.calc.LeaveTypeValidator
import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OverflowBehavior
import com.kangminsang.hagoondori.core.payload.LeaveDeviceLabel
import com.kangminsang.hagoondori.ui.common.DateTextField
import com.kangminsang.hagoondori.ui.dashboard.LeaveTypeSummary
import com.kangminsang.hagoondori.ui.common.rememberToday
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** 휴가 탭 (F7~F9): 정기/포상/위로휴가별 부여/사용/잔여, 포상 상한 경고. 기본 3종은 새 설치 때 채워지고, 종류는 추가·수정할 수 있다(삭제는 없다). */
@Composable
fun LeaveTab(
    uiState: LeaveManagementUiState,
    onAddType: (name: String, cap: Int?, overflowBehavior: OverflowBehavior, fixedDays: Int?) -> Unit,
    onUpdateType: (LeaveType) -> Unit,
    onAddGrant: (leaveTypeId: String, days: Int, grantedDate: LocalDate, reason: String?, expiryDate: LocalDate?) -> Unit,
    onMoveType: (leaveTypeId: String, delta: Int) -> Unit,
    onDeleteGrant: (LeaveGrant) -> Unit,
    onAddUsage: (leaveTypeId: String, startDate: LocalDate, endDate: LocalDate, label: String?) -> Unit,
    onUpdateUsage: (usage: LeaveUsage, startDate: LocalDate, endDate: LocalDate, label: String?) -> Unit,
    onDeleteUsage: (LeaveUsage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddTypeForm by remember { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        itemsIndexed(uiState.leaveSummaries, key = { _, it -> it.type.id }) { index, summary ->
            LeaveTypeCard(
                summary = summary,
                canMoveUp = index > 0,
                canMoveDown = index < uiState.leaveSummaries.lastIndex,
                onMove = { delta -> onMoveType(summary.type.id, delta) },
                otherTypeNames = uiState.leaveTypes.filter { it.id != summary.type.id }.map { it.name },
                onUpdateType = onUpdateType,
                usages = uiState.leaveUsages.filter { it.leaveTypeId == summary.type.id },
                onAddGrant = { days, date, reason, expiry -> onAddGrant(summary.type.id, days, date, reason, expiry) },
                onDeleteGrant = onDeleteGrant,
                onAddUsage = { start, end, label -> onAddUsage(summary.type.id, start, end, label) },
                onUpdateUsage = onUpdateUsage,
                onDeleteUsage = onDeleteUsage,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item(key = "add-leave-type") {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (showAddTypeForm) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            LeaveTypeForm(
                                initialType = null,
                                otherNames = uiState.leaveTypes.map { it.name },
                                grantCount = 0,
                                submitText = "저장",
                                onCancel = { showAddTypeForm = false },
                                onSubmit = { name, cap, overflow, fixedDays ->
                                    onAddType(name, cap, overflow, fixedDays)
                                    showAddTypeForm = false
                                },
                            )
                        }
                    }
                } else {
                    TextButton(onClick = { showAddTypeForm = true }) { Text("+ 휴가 종류 추가") }
                }
            }
        }
    }
}

@Composable
private fun LeaveTypeCard(
    summary: LeaveTypeSummary,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (delta: Int) -> Unit,
    otherTypeNames: List<String>,
    onUpdateType: (LeaveType) -> Unit,
    usages: List<LeaveUsage>,
    onAddGrant: (days: Int, grantedDate: LocalDate, reason: String?, expiryDate: LocalDate?) -> Unit,
    onDeleteGrant: (LeaveGrant) -> Unit,
    onAddUsage: (startDate: LocalDate, endDate: LocalDate, label: String?) -> Unit,
    onUpdateUsage: (usage: LeaveUsage, startDate: LocalDate, endDate: LocalDate, label: String?) -> Unit,
    onDeleteUsage: (LeaveUsage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            var showEditForm by remember { mutableStateOf(false) }
            var showGrantForm by remember { mutableStateOf(false) }
            var showUsageForm by remember { mutableStateOf(false) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(summary.type.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { showEditForm = !showEditForm; showGrantForm = false; showUsageForm = false }) {
                    Icon(Icons.Filled.Edit, contentDescription = "휴가 종류 수정")
                }
                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "위로 이동")
                }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "아래로 이동")
                }
            }
            if (showEditForm) {
                LeaveTypeForm(
                    initialType = summary.type,
                    otherNames = otherTypeNames,
                    grantCount = summary.grantStatuses.size,
                    submitText = "수정 저장",
                    onCancel = { showEditForm = false },
                    onSubmit = { name, cap, overflow, fixedDays ->
                        onUpdateType(summary.type.copy(name = name, cap = cap, overflowBehavior = overflow, fixedDays = fixedDays))
                        showEditForm = false
                    },
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                // 상한이 있는 종류(포상휴가)는 상한도 함께 보여 준다. 부여받은 만큼만 잔여가 생긴다.
                summary.type.cap?.let { Text("상한 ${it}일", style = MaterialTheme.typography.bodyMedium) }
                Text("부여 ${summary.granted}일", style = MaterialTheme.typography.bodyMedium)
                Text("사용 ${summary.used}일", style = MaterialTheme.typography.bodyMedium)
                Text("잔여 ${summary.remaining}일", style = MaterialTheme.typography.bodyMedium)
            }
            if (summary.planned > 0) {
                Text(
                    "예정 ${summary.planned}일 (모두 쓰면 ${summary.remainingAfterPlanned}일 남음)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (summary.expired > 0) {
                Text(
                    "유효 기간이 지나 ${summary.expired}일 소멸",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            val capCapacity = summary.remainingCapCapacity
            if (capCapacity != null && capCapacity <= 3) {
                Text(
                    "상한까지 ${capCapacity}일 남음 - 초과분은 전투휴무로 전환될 수 있습니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Row(modifier = Modifier.padding(top = 8.dp)) {
                if (summary.type.fixedDays == null) {
                    TextButton(onClick = { showGrantForm = !showGrantForm; showUsageForm = false; showEditForm = false }) { Text("+ 부여 기록") }
                }
                TextButton(onClick = { showUsageForm = !showUsageForm; showGrantForm = false; showEditForm = false }) { Text("+ 사용 기록") }
            }

            if (showGrantForm) {
                GrantForm(onSubmit = { days, date, reason, expiry -> onAddGrant(days, date, reason, expiry); showGrantForm = false })
            }
            if (showUsageForm) {
                UsageForm(onSubmit = { start, end, label -> onAddUsage(start, end, label); showUsageForm = false })
            }

            if (summary.grantStatuses.isNotEmpty()) {
                Text(
                    "부여 내역",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                summary.grantStatuses.sortedByDescending { it.grant.grantedDate }.forEach { status ->
                    GrantRow(status, onDelete = { onDeleteGrant(status.grant) })
                }
            }

            if (usages.isNotEmpty()) {
                Text(
                    "최근 사용 기록",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                var editingUsageId by remember { mutableStateOf<String?>(null) }
                usages.sortedByDescending { it.startDate }.forEach { usage ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${usage.startDate} ~ ${usage.endDate}" + (usage.label?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { editingUsageId = if (editingUsageId == usage.id) null else usage.id }) {
                            Icon(Icons.Filled.Edit, contentDescription = "수정")
                        }
                        IconButton(onClick = { onDeleteUsage(usage) }) {
                            Icon(Icons.Filled.Close, contentDescription = "삭제")
                        }
                    }
                    if (editingUsageId == usage.id) {
                        UsageForm(
                            initialStart = usage.startDate,
                            initialEnd = usage.endDate,
                            initialLabel = usage.label.orEmpty(),
                            submitText = "수정 저장",
                            onCancel = { editingUsageId = null },
                            onSubmit = { start, end, label ->
                                onUpdateUsage(usage, start, end, label)
                                editingUsageId = null
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 부여 한 건: 날짜·일수·사유, 남은 일수, 유효 기간(만료 여부). */
@Composable
private fun GrantRow(status: LeaveCalculator.GrantStatus, onDelete: () -> Unit) {
    val grant = status.grant
    val today by rememberToday()
    val expiry = grant.expiryDate
    val isExpired = expiry != null && expiry < today
    val statusText = when {
        isExpired -> "만료됨(${expiry}) · ${status.expiredDays}일 소멸"
        expiry != null -> "${expiry}까지 (D-${today.daysUntil(expiry)}) · 남음 ${status.remainingDays}일"
        else -> "기한 없음 · 남음 ${status.remainingDays}일"
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(
                "${grant.grantedDate} · ${grant.days}일" + (grant.reason?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Close, contentDescription = "부여 기록 삭제")
        }
    }
}

/**
 * 휴가 종류 추가·수정 폼. [initialType]이 null이면 새 종류, 있으면 그 값을 채워 수정한다.
 * 저장 버튼을 누를 때 [LeaveTypeValidator]로 검사하고, 통과한 값만 [onSubmit]으로 넘긴다.
 */
@Composable
private fun LeaveTypeForm(
    initialType: LeaveType?,
    otherNames: List<String>,
    grantCount: Int,
    submitText: String,
    onCancel: () -> Unit,
    onSubmit: (name: String, cap: Int?, overflowBehavior: OverflowBehavior, fixedDays: Int?) -> Unit,
) {
    var name by remember(initialType) { mutableStateOf(initialType?.name.orEmpty()) }
    var fixedMode by remember(initialType) { mutableStateOf(initialType?.fixedDays != null) }
    var capText by remember(initialType) { mutableStateOf(initialType?.cap?.toString().orEmpty()) }
    var fixedText by remember(initialType) { mutableStateOf(initialType?.fixedDays?.toString().orEmpty()) }
    var convertToCombatRest by remember(initialType) {
        mutableStateOf(initialType == null || initialType.overflowBehavior == OverflowBehavior.CONVERT_TO_COMBAT_REST)
    }
    var errors by remember(initialType) { mutableStateOf<List<LeaveTypeError>>(emptyList()) }

    // 입력했는데 숫자가 아니거나 0이면 0으로 넘겨 검증기가 INVALID_* 오류를 내게 한다. 빈칸만 null(없음)이다.
    val capValue = if (fixedMode) null else capText.toDaysOrNull()
    val fixedValue: Int? = if (fixedMode) fixedText.toDaysOrNull() ?: 0 else null

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("이름") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (name.isNotBlank()) {
            Text(
                "장치 표시 이름: ${LeaveDeviceLabel.forTypeName(name)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Text("총량 방식", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LeaveTypeRadio(text = "부여 기록 합산", selected = !fixedMode, onSelect = { fixedMode = false })
            LeaveTypeRadio(text = "고정 일수", selected = fixedMode, onSelect = { fixedMode = true })
        }

        if (fixedMode) {
            OutlinedTextField(
                value = fixedText,
                onValueChange = { fixedText = it.filter(Char::isDigit) },
                label = { Text("총 일수") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        } else {
            OutlinedTextField(
                value = capText,
                onValueChange = { capText = it.filter(Char::isDigit) },
                label = { Text("상한 일수(선택)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (capText.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = convertToCombatRest, role = Role.Checkbox, onValueChange = { convertToCombatRest = it }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = convertToCombatRest, onCheckedChange = null)
                    Text("상한 초과분을 전투휴무로 전환", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // 수정할 때만, 해당할 때 나오는 안내. 저장 전에 결과가 어떻게 바뀌는지 알려 준다.
        if (initialType != null) {
            if (capValue != initialType.cap) {
                Text(
                    "! 상한을 바꾸면 전투휴무 전환분이 다시 계산됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (initialType.fixedDays == null && fixedMode && grantCount > 0) {
                Text(
                    "! 고정 일수로 바꾸면 기존 부여 기록 ${grantCount}건은",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "  잔여 계산에서 빠집니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        errors.forEach { error ->
            Text(
                error.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    val result = LeaveTypeValidator.validate(name, capValue, fixedValue, otherNames)
                    errors = result.errors
                    if (result.isValid) {
                        // 상한이 없으면 초과 처리도 없다. 상한이 있으면 체크 여부를 따르되, 기존이 REJECT이고 체크를 풀었으면 REJECT를 유지한다.
                        val overflow = when {
                            capValue == null -> OverflowBehavior.NONE
                            convertToCombatRest -> OverflowBehavior.CONVERT_TO_COMBAT_REST
                            initialType?.overflowBehavior == OverflowBehavior.REJECT -> OverflowBehavior.REJECT
                            else -> OverflowBehavior.NONE
                        }
                        onSubmit(name.trim(), capValue, overflow, fixedValue)
                    }
                },
            ) { Text(submitText) }
            TextButton(onClick = onCancel, modifier = Modifier.padding(start = 8.dp)) { Text("취소") }
        }
    }
}

@Composable
private fun LeaveTypeRadio(text: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(end = 16.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** 숫자 입력 한 칸을 일수로 바꾼다. 빈칸은 null(없음), 숫자가 아니면 0(검증기가 오류로 잡는다). */
private fun String.toDaysOrNull(): Int? = if (isBlank()) null else toIntOrNull() ?: 0

@Composable
private fun GrantForm(onSubmit: (days: Int, date: LocalDate, reason: String?, expiryDate: LocalDate?) -> Unit) {
    var expiry by remember { mutableStateOf<LocalDate?>(null) }
    var daysText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(AppClock.today()) }
    var reason by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = daysText,
            onValueChange = { daysText = it.filter(Char::isDigit) },
            label = { Text("부여 일수") },
            modifier = Modifier.fillMaxWidth(),
        )
        DateTextField("부여일", date, { date = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("어떤 휴가인지(예: 사격 우수, 선택)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        DateTextField("유효 기간 마지막 날(선택)", expiry, { expiry = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Button(
            onClick = {
                val days = daysText.toIntOrNull()
                val grantedDate = date
                if (days != null && days > 0 && grantedDate != null && (expiry == null || expiry!! >= grantedDate)) {
                    onSubmit(days, grantedDate, reason.ifBlank { null }, expiry)
                }
            },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("저장") }
    }
}

@Composable
private fun UsageForm(
    onSubmit: (start: LocalDate, end: LocalDate, label: String?) -> Unit,
    initialStart: LocalDate = AppClock.today(),
    initialEnd: LocalDate = AppClock.today(),
    initialLabel: String = "",
    submitText: String = "저장",
    onCancel: (() -> Unit)? = null,
) {
    var start by remember { mutableStateOf<LocalDate?>(initialStart) }
    var end by remember { mutableStateOf<LocalDate?>(initialEnd) }
    var label by remember { mutableStateOf(initialLabel) }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        DateTextField("시작일", start, { start = it }, modifier = Modifier.fillMaxWidth())
        DateTextField("종료일(포함)", end, { end = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("이름(선택)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    val startDate = start
                    val endDate = end
                    if (startDate != null && endDate != null && endDate >= startDate) {
                        onSubmit(startDate, endDate, label.ifBlank { null })
                    }
                },
            ) { Text(submitText) }
            if (onCancel != null) {
                TextButton(onClick = onCancel, modifier = Modifier.padding(start = 8.dp)) { Text("취소") }
            }
        }
    }
}
