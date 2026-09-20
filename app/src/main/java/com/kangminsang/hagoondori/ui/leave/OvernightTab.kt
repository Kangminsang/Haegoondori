package com.kangminsang.hagoondori.ui.leave

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.kangminsang.hagoondori.ui.common.Button
import com.kangminsang.hagoondori.ui.common.Card
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

/** 외박 탭 (F11~F13): 다음 예정일/D-day, 차수 이력, 차수 소멸 처리. */
@Composable
fun OvernightTab(
    uiState: LeaveManagementUiState,
    onAddRecord: (date: LocalDate, endDate: LocalDate, memo: String?) -> Unit,
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
                }
            }

            if (schedule.matches.isNotEmpty()) {
                Text("외박 이력", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                schedule.matches.sortedByDescending { it.record.date }.forEach { match ->
                    val period = if (match.record.endDate == match.record.date) "${match.record.date}" else "${match.record.date} ~ ${match.record.endDate}"
                    Text("${match.slotIndex + 1}차 · $period", style = MaterialTheme.typography.bodyMedium)
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
            RecordForm(onSubmit = { date, endDate, memo -> onAddRecord(date, endDate, memo); showRecordForm = false })
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
private fun RecordForm(onSubmit: (date: LocalDate, endDate: LocalDate, memo: String?) -> Unit) {
    var date by remember { mutableStateOf<LocalDate?>(AppClock.today()) }
    var endDate by remember { mutableStateOf<LocalDate?>(AppClock.today()) }
    var memo by remember { mutableStateOf("") }
    val start = date
    val end = endDate

    Column {
        DateTextField("외박 시작일 (6주 차수 기준일)", date, { date = it; if (endDate == null || it == null || endDate!! < it) endDate = it }, modifier = Modifier.fillMaxWidth())
        DateTextField("종료일(포함, 휴가와 이어 쓰면 휴가 끝나는 날)", endDate, { endDate = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(value = memo, onValueChange = { memo = it }, label = { Text("메모(선택)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Button(
            enabled = start != null && end != null && end >= start,
            onClick = { if (start != null && end != null) onSubmit(start, end, memo.ifBlank { null }) },
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
