package com.kangminsang.hagoondori.ui.leave

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.ui.common.DateTextField
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.datetime.LocalDate

/**
 * 전투휴무 탭 (F10): 잔여를 전환분(계산)/부여분(저장)으로 나눠 보여준다(3.3.3절) -
 * 포상 기록을 나중에 고쳐도 이 화면의 숫자가 왜 바뀌는지 사용자가 이해할 수 있어야 한다.
 */
@Composable
fun CombatRestTab(
    uiState: LeaveManagementUiState,
    onAddGrant: (days: Int, grantedDate: LocalDate, reason: String?) -> Unit,
    onAddUsage: (startDate: LocalDate, endDate: LocalDate, memo: String?) -> Unit,
    onDeleteUsage: (CombatRestUsage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = uiState.combatRestSummary

    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("전투휴무 잔여", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${summary.remaining}일 (전환 ${summary.convertedFromLeave}일 + 부여 ${summary.directGranted}일)",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "사용 ${summary.totalUsed}일",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        var showGrantForm by remember { mutableStateOf(false) }
        var showUsageForm by remember { mutableStateOf(false) }

        TextButton(onClick = { showGrantForm = !showGrantForm; showUsageForm = false }) { Text("+ 직접 부여 기록") }
        if (showGrantForm) {
            CombatRestGrantForm(onSubmit = { days, date, reason -> onAddGrant(days, date, reason); showGrantForm = false })
        }

        TextButton(onClick = { showUsageForm = !showUsageForm; showGrantForm = false }) { Text("+ 사용 기록") }
        if (showUsageForm) {
            CombatRestUsageForm(onSubmit = { start, end, memo -> onAddUsage(start, end, memo); showUsageForm = false })
        }

        if (uiState.combatRestUsages.isNotEmpty()) {
            Text(
                "최근 사용 기록",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
            uiState.combatRestUsages.sortedByDescending { it.startDate }.forEach { usage ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${usage.startDate} ~ ${usage.endDate}" + (usage.memo?.let { " · $it" } ?: ""),
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

@Composable
private fun CombatRestGrantForm(onSubmit: (days: Int, date: LocalDate, reason: String?) -> Unit) {
    var daysText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(AppClock.today()) }
    var reason by remember { mutableStateOf("") }

    Column {
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
                if (days != null && days > 0 && grantedDate != null) onSubmit(days, grantedDate, reason.ifBlank { null })
            },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("저장") }
    }
}

@Composable
private fun CombatRestUsageForm(onSubmit: (start: LocalDate, end: LocalDate, memo: String?) -> Unit) {
    var start by remember { mutableStateOf<LocalDate?>(AppClock.today()) }
    var end by remember { mutableStateOf<LocalDate?>(AppClock.today()) }
    var memo by remember { mutableStateOf("") }

    Column {
        DateTextField("시작일", start, { start = it }, modifier = Modifier.fillMaxWidth())
        DateTextField("종료일(포함)", end, { end = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(value = memo, onValueChange = { memo = it }, label = { Text("메모(선택)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Button(
            onClick = {
                val startDate = start
                val endDate = end
                if (startDate != null && endDate != null && endDate >= startDate) onSubmit(startDate, endDate, memo.ifBlank { null })
            },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("저장") }
    }
}
