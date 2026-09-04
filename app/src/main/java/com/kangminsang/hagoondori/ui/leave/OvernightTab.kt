package com.kangminsang.hagoondori.ui.leave

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.kangminsang.hagoondori.core.calc.DDayCalculator
import com.kangminsang.hagoondori.ui.common.DateTextField
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.datetime.LocalDate

/** 외박 탭 (F11~F13): 다음 예정일/D-day, 지연·선행 상태, 차수 이력, 차수 소멸 처리. */
@Composable
fun OvernightTab(
    uiState: LeaveManagementUiState,
    onAddRecord: (date: LocalDate, memo: String?) -> Unit,
    onAddForfeiture: (slotIndex: Int, reason: String?, recordedDate: LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (uiState.profile?.firstOvernightDate == null) {
            Text(
                "설정에서 첫 외박일을 입력하면 6주 주기 차수를 계산해 보여줍니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        val schedule = uiState.overnightSchedule
        if (schedule != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("다음 외박 (${schedule.nextSlotIndex + 1}차)", style = MaterialTheme.typography.titleMedium)
                    val today = AppClock.today()
                    val dDay = DDayCalculator.dDay(today, schedule.nextScheduledDate)
                    Text(
                        "${schedule.nextScheduledDate} (${if (dDay >= 0) "D-$dDay" else "D+${-dDay}"})",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    val lastDelay = schedule.matches.lastOrNull()?.delayDays
                    if (lastDelay != null && lastDelay != 0) {
                        val text = if (lastDelay > 0) "직전 외박이 예정보다 ${lastDelay}일 지연됐습니다" else "직전 외박을 ${-lastDelay}일 앞당겨 사용했습니다"
                        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            if (schedule.matches.isNotEmpty()) {
                Text("외박 이력", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                schedule.matches.sortedByDescending { it.record.date }.forEach { match ->
                    val delayText = when {
                        match.delayDays > 0 -> " (${match.delayDays}일 지연)"
                        match.delayDays < 0 -> " (${-match.delayDays}일 선행)"
                        else -> ""
                    }
                    Text("${match.slotIndex + 1}차 · ${match.record.date}$delayText", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (schedule.forfeitedSlots.isNotEmpty()) {
                Text("소멸된 차수", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                uiState.overnightForfeitures.sortedBy { it.slotIndex }.forEach { forfeiture ->
                    Text(
                        "${forfeiture.slotIndex + 1}차" + (forfeiture.reason?.let { " - $it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        var showRecordForm by remember { mutableStateOf(false) }
        var showForfeitureForm by remember { mutableStateOf(false) }

        TextButton(onClick = { showRecordForm = !showRecordForm; showForfeitureForm = false }, modifier = Modifier.padding(top = 16.dp)) {
            Text("+ 외박 기록 추가")
        }
        if (showRecordForm) {
            RecordForm(onSubmit = { date, memo -> onAddRecord(date, memo); showRecordForm = false })
        }

        TextButton(onClick = { showForfeitureForm = !showForfeitureForm; showRecordForm = false }) {
            Text("+ 차수 소멸 처리 (징계)")
        }
        if (showForfeitureForm) {
            ForfeitureForm(
                options = uiState.upcomingSlotOptions,
                onSubmit = { slotIndex, reason -> onAddForfeiture(slotIndex, reason, AppClock.today()); showForfeitureForm = false },
            )
        }
    }
}

@Composable
private fun RecordForm(onSubmit: (date: LocalDate, memo: String?) -> Unit) {
    var date by remember { mutableStateOf<LocalDate?>(AppClock.today()) }
    var memo by remember { mutableStateOf("") }

    Column {
        DateTextField("외박일", date, { date = it }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = memo, onValueChange = { memo = it }, label = { Text("메모(선택)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Button(
            onClick = { date?.let { onSubmit(it, memo.ifBlank { null }) } },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("저장") }
    }
}

@Composable
private fun ForfeitureForm(options: List<UpcomingSlotOption>, onSubmit: (slotIndex: Int, reason: String?) -> Unit) {
    if (options.isEmpty()) {
        Text("소멸 처리할 예정 차수가 없습니다", style = MaterialTheme.typography.bodySmall)
        return
    }
    var selected by remember(options) { mutableStateOf(options.first()) }
    var reason by remember { mutableStateOf("") }

    Column {
        Text("어느 차수를 소멸시킬까요? (차수 번호가 아니라 예정일에서 고릅니다)", style = MaterialTheme.typography.bodySmall)
        options.forEach { option ->
            Row(modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = { selected = option }) {
                    Text((if (selected == option) "● " else "○ ") + "${option.slotIndex + 1}차 (${option.scheduledDate})")
                }
            }
        }
        OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("사유(선택)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Button(onClick = { onSubmit(selected.slotIndex, reason.ifBlank { null }) }, modifier = Modifier.padding(top = 8.dp)) {
            Text("소멸 처리")
        }
    }
}
