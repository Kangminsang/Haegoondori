package com.kangminsang.hagoondori.ui.leave

import androidx.compose.foundation.layout.Arrangement
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
import com.kangminsang.hagoondori.core.calc.PassCalculator
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.PassType
import com.kangminsang.hagoondori.ui.common.DateTextField
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.datetime.LocalDate

/** 외출 탭 (F14): 이번 달 평일/휴일 잔여, 기록 추가. 이월이 없으므로 매달 새로 리셋된다. */
@Composable
fun PassTab(
    uiState: LeaveManagementUiState,
    onAddRecord: (date: LocalDate, explicitType: PassType?, memo: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = uiState.profile
    val today = AppClock.today()

    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (profile == null) {
            Text("설정에서 복무 정보를 먼저 입력해 주세요.", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        val weekdayRemaining = PassCalculator.remainingInMonth(profile, uiState.passRecords, today.year, today.monthNumber, PassType.WEEKDAY)
        val holidayRemaining = PassCalculator.remainingInMonth(profile, uiState.passRecords, today.year, today.monthNumber, PassType.HOLIDAY)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("${today.year}년 ${today.monthNumber}월 외출 잔여", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("평일 ${weekdayRemaining}회 (월 ${profile.weekdayPassPerMonth}회)", style = MaterialTheme.typography.bodyMedium)
                    Text("휴일 ${holidayRemaining}회 (월 ${profile.holidayPassPerMonth}회)", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        var showForm by remember { mutableStateOf(false) }
        TextButton(onClick = { showForm = !showForm }, modifier = Modifier.padding(top = 16.dp)) {
            Text(if (showForm) "닫기" else "+ 외출 기록 추가")
        }
        if (showForm) {
            AddPassRecordForm(
                holidays = uiState.holidays,
                onSubmit = { date, type, memo -> onAddRecord(date, type, memo); showForm = false },
            )
        }

        if (uiState.passRecords.isNotEmpty()) {
            Text("최근 기록", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            uiState.passRecords.sortedByDescending { it.date }.take(20).forEach { record ->
                val label = if (record.type == PassType.WEEKDAY) "평일" else "휴일"
                Text("${record.date} · $label", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun AddPassRecordForm(
    holidays: List<Holiday>,
    onSubmit: (date: LocalDate, type: PassType?, memo: String?) -> Unit,
) {
    var date by remember { mutableStateOf<LocalDate?>(AppClock.today()) }
    var memo by remember { mutableStateOf("") }
    val autoType = date?.let { PassCalculator.classifyType(it, holidays) }
    var overrideType by remember { mutableStateOf<PassType?>(null) }

    Column {
        DateTextField("외출 날짜", date, { date = it; overrideType = null }, modifier = Modifier.fillMaxWidth())
        Text(
            "자동 판정: ${if (autoType == PassType.WEEKDAY) "평일" else "휴일"} (직접 바꾸려면 아래에서 선택)",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(modifier = Modifier.padding(top = 4.dp)) {
            PassType.entries.forEach { type ->
                TextButton(onClick = { overrideType = type }) {
                    val label = if (type == PassType.WEEKDAY) "평일" else "휴일"
                    Text((if ((overrideType ?: autoType) == type) "● " else "○ ") + label)
                }
            }
        }
        OutlinedTextField(value = memo, onValueChange = { memo = it }, label = { Text("메모(선택)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Button(
            onClick = { date?.let { onSubmit(it, overrideType, memo.ifBlank { null }) } },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("저장") }
    }
}
