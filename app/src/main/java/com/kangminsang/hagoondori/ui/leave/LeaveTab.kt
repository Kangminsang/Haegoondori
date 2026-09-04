package com.kangminsang.hagoondori.ui.leave

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kangminsang.hagoondori.core.model.OverflowBehavior
import com.kangminsang.hagoondori.ui.common.DateTextField
import com.kangminsang.hagoondori.ui.dashboard.LeaveTypeSummary
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.datetime.LocalDate

/** 휴가 탭 (F7~F9): 종류별 부여/사용/잔여, 포상 상한 경고. */
@Composable
fun LeaveTab(
    uiState: LeaveManagementUiState,
    onAddType: (name: String, cap: Int?, overflowBehavior: OverflowBehavior) -> Unit,
    onAddGrant: (leaveTypeId: String, days: Int, grantedDate: LocalDate, reason: String?) -> Unit,
    onAddUsage: (leaveTypeId: String, startDate: LocalDate, endDate: LocalDate, label: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item { AddLeaveTypeSection(onAddType, modifier = Modifier.padding(16.dp)) }

        items(uiState.leaveSummaries, key = { it.type.id }) { summary ->
            LeaveTypeCard(
                summary = summary,
                onAddGrant = { days, date, reason -> onAddGrant(summary.type.id, days, date, reason) },
                onAddUsage = { start, end, label -> onAddUsage(summary.type.id, start, end, label) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun AddLeaveTypeSection(
    onAddType: (name: String, cap: Int?, overflowBehavior: OverflowBehavior) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "휴가 종류 추가 닫기" else "+ 휴가 종류 추가")
        }
        if (expanded) {
            var name by remember { mutableStateOf("") }
            var capText by remember { mutableStateOf("") }
            var overflow by remember { mutableStateOf(OverflowBehavior.NONE) }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("종류 이름 (예: 포상휴가)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = capText,
                onValueChange = { capText = it.filter(Char::isDigit) },
                label = { Text("상한 일수 (없으면 비워둠, 포상휴가는 17)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                OverflowBehavior.entries.forEach { behavior ->
                    TextButton(onClick = { overflow = behavior }) {
                        Text((if (behavior == overflow) "● " else "○ ") + behavior.name)
                    }
                }
            }
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAddType(name.trim(), capText.toIntOrNull(), overflow)
                        name = ""
                        capText = ""
                        overflow = OverflowBehavior.NONE
                        expanded = false
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("추가") }
        }
    }
}

@Composable
private fun LeaveTypeCard(
    summary: LeaveTypeSummary,
    onAddGrant: (days: Int, grantedDate: LocalDate, reason: String?) -> Unit,
    onAddUsage: (startDate: LocalDate, endDate: LocalDate, label: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(summary.type.name, style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("부여 ${summary.granted}일", style = MaterialTheme.typography.bodyMedium)
                Text("사용 ${summary.used}일", style = MaterialTheme.typography.bodyMedium)
                Text("잔여 ${summary.remaining}일", style = MaterialTheme.typography.bodyMedium)
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
                TextButton(onClick = { showGrantForm = !showGrantForm; showUsageForm = false }) { Text("+ 부여 기록") }
                TextButton(onClick = { showUsageForm = !showUsageForm; showGrantForm = false }) { Text("+ 사용 기록") }
            }

            if (showGrantForm) {
                GrantForm(onSubmit = { days, date, reason -> onAddGrant(days, date, reason); showGrantForm = false })
            }
            if (showUsageForm) {
                UsageForm(onSubmit = { start, end, label -> onAddUsage(start, end, label); showUsageForm = false })
            }
        }
    }
}

@Composable
private fun GrantForm(onSubmit: (days: Int, date: LocalDate, reason: String?) -> Unit) {
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
        OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("사유(선택)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Button(
            onClick = {
                val days = daysText.toIntOrNull()
                val grantedDate = date
                if (days != null && days > 0 && grantedDate != null) {
                    onSubmit(days, grantedDate, reason.ifBlank { null })
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
