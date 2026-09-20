package com.kangminsang.hagoondori.ui.leave

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.kangminsang.hagoondori.ui.common.Button
import com.kangminsang.hagoondori.ui.common.Card
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
import androidx.compose.ui.unit.dp
import com.kangminsang.hagoondori.core.calc.LeaveCalculator
import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.ui.common.DateTextField
import com.kangminsang.hagoondori.ui.dashboard.LeaveTypeSummary
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** 휴가 탭 (F7~F9): 정기/포상/위로휴가별 부여/사용/잔여, 포상 상한 경고. 종류는 미리 채워져 있다. */
@Composable
fun LeaveTab(
    uiState: LeaveManagementUiState,
    onAddGrant: (leaveTypeId: String, days: Int, grantedDate: LocalDate, reason: String?, expiryDate: LocalDate?) -> Unit,
    onMoveType: (leaveTypeId: String, delta: Int) -> Unit,
    onDeleteGrant: (LeaveGrant) -> Unit,
    onAddUsage: (leaveTypeId: String, startDate: LocalDate, endDate: LocalDate, label: String?) -> Unit,
    onDeleteUsage: (LeaveUsage) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        itemsIndexed(uiState.leaveSummaries, key = { _, it -> it.type.id }) { index, summary ->
            LeaveTypeCard(
                summary = summary,
                canMoveUp = index > 0,
                canMoveDown = index < uiState.leaveSummaries.lastIndex,
                onMove = { delta -> onMoveType(summary.type.id, delta) },
                usages = uiState.leaveUsages.filter { it.leaveTypeId == summary.type.id },
                onAddGrant = { days, date, reason, expiry -> onAddGrant(summary.type.id, days, date, reason, expiry) },
                onDeleteGrant = onDeleteGrant,
                onAddUsage = { start, end, label -> onAddUsage(summary.type.id, start, end, label) },
                onDeleteUsage = onDeleteUsage,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun LeaveTypeCard(
    summary: LeaveTypeSummary,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (delta: Int) -> Unit,
    usages: List<LeaveUsage>,
    onAddGrant: (days: Int, grantedDate: LocalDate, reason: String?, expiryDate: LocalDate?) -> Unit,
    onDeleteGrant: (LeaveGrant) -> Unit,
    onAddUsage: (startDate: LocalDate, endDate: LocalDate, label: String?) -> Unit,
    onDeleteUsage: (LeaveUsage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(summary.type.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "위로 이동")
                }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "아래로 이동")
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                // 상한이 있는 종류(포상휴가)는 상한도 함께 보여 준다. 부여받은 만큼만 잔여가 생긴다.
                summary.type.cap?.let { Text("상한 ${it}일", style = MaterialTheme.typography.bodyMedium) }
                Text("부여 ${summary.granted}일", style = MaterialTheme.typography.bodyMedium)
                Text("사용 ${summary.used}일", style = MaterialTheme.typography.bodyMedium)
                Text("잔여 ${summary.remaining}일", style = MaterialTheme.typography.bodyMedium)
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

            var showGrantForm by remember { mutableStateOf(false) }
            var showUsageForm by remember { mutableStateOf(false) }

            Row(modifier = Modifier.padding(top = 8.dp)) {
                if (summary.type.fixedDays == null) {
                    TextButton(onClick = { showGrantForm = !showGrantForm; showUsageForm = false }) { Text("+ 부여 기록") }
                }
                TextButton(onClick = { showUsageForm = !showUsageForm; showGrantForm = false }) { Text("+ 사용 기록") }
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
                usages.sortedByDescending { it.startDate }.forEach { usage ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${usage.startDate} ~ ${usage.endDate}" + (usage.label?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onDeleteUsage(usage) }) {
                            Icon(Icons.Filled.Close, contentDescription = "삭제")
                        }
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
    val today = AppClock.today()
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
private fun UsageForm(onSubmit: (start: LocalDate, end: LocalDate, label: String?) -> Unit) {
    var start by remember { mutableStateOf<LocalDate?>(AppClock.today()) }
    var end by remember { mutableStateOf<LocalDate?>(AppClock.today()) }
    var label by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        DateTextField("시작일", start, { start = it }, modifier = Modifier.fillMaxWidth())
        DateTextField("종료일(포함)", end, { end = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("이름(선택)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Button(
            onClick = {
                val startDate = start
                val endDate = end
                if (startDate != null && endDate != null && endDate >= startDate) {
                    onSubmit(startDate, endDate, label.ifBlank { null })
                }
            },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("저장") }
    }
}
