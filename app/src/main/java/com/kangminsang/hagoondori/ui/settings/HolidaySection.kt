package com.kangminsang.hagoondori.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.data.remote.holiday.HolidayFetchResult
import com.kangminsang.hagoondori.ui.common.DateTextField
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.datetime.LocalDate

/**
 * 공휴일 갱신(F15, 스펙 4.14절) - 확보 우선순위(API → 내장 데이터 → 수동 입력)를
 * 그대로 화면에 드러낸다: 갱신 버튼은 자동으로 최선의 경로를 시도하고, 그래도
 * 부족한 부분(주로 음력 기반 공휴일)은 수동으로 추가할 수 있게 한다.
 */
@Composable
fun HolidaySection(
    holidays: List<Holiday>,
    onRefresh: ((List<HolidayFetchResult>) -> Unit) -> Unit,
    hasApiKey: Boolean,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onAddManual: (Holiday) -> Unit,
    onDelete: (Holiday) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isRefreshing by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var showManualForm by remember { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("공휴일", style = MaterialTheme.typography.titleMedium)

            val refresh = {
                isRefreshing = true
                onRefresh { results ->
                    isRefreshing = false
                    resultMessage = summarize(results)
                }
            }

            ApiKeyInput(
                hasKey = hasApiKey,
                onSave = { key -> onSaveApiKey(key); refresh() },
                onClear = onClearApiKey,
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    enabled = !isRefreshing,
                    onClick = { refresh() },
                ) { Text("공휴일 갱신") }
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.padding(start = 12.dp).size(20.dp))
                }
            }
            resultMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }

            TextButton(onClick = { showManualForm = !showManualForm }, modifier = Modifier.padding(top = 8.dp)) {
                Text(if (showManualForm) "닫기" else "+ 수동으로 공휴일 추가 (설날·추석 등)")
            }
            if (showManualForm) {
                ManualHolidayForm(onSubmit = { holiday -> onAddManual(holiday); showManualForm = false })
            }

            if (holidays.isNotEmpty()) {
                Text("등록된 공휴일 (${holidays.size}건)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                val today = AppClock.today()
                holidays.filter { it.date >= today }.take(15).forEach { holiday ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${holiday.date} · ${holiday.name}" + (if (holiday.isSubstitute) " (대체)" else ""),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onDelete(holiday) }) {
                            Icon(Icons.Filled.Close, contentDescription = "삭제")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 공공데이터포털 서비스키 입력. 키는 이 기기의 앱 전용 저장소에만 보관되고 화면에는 가려서
 * 보인다. 저장하면 바로 공휴일을 한 번 조회해 키가 맞는지 확인한다.
 */
@Composable
private fun ApiKeyInput(hasKey: Boolean, onSave: (String) -> Unit, onClear: () -> Unit) {
    var keyText by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            if (hasKey) "공공데이터포털 키가 저장되어 있습니다 (앱을 켤 때 자동으로 공휴일을 갱신합니다)"
            else "공공데이터포털(특일 정보) 서비스키를 입력하면 공휴일을 자동으로 받아옵니다",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = keyText,
            onValueChange = { keyText = it },
            label = { Text(if (hasKey) "새 키로 바꾸려면 입력" else "서비스키") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        Row {
            TextButton(
                enabled = keyText.isNotBlank(),
                onClick = { onSave(keyText); keyText = "" },
            ) { Text("키 저장") }
            if (hasKey) TextButton(onClick = onClear) { Text("키 삭제") }
        }
    }
}

private fun summarize(results: List<HolidayFetchResult>): String =
    results.joinToString(" / ") { result ->
        when (result) {
            is HolidayFetchResult.Success -> "${result.year}년 ${result.count}건 조회 성공"
            is HolidayFetchResult.UsedBuiltInFallback -> "${result.year}년: 내장 데이터로 대체 (${result.reason})"
        }
    }

@Composable
private fun ManualHolidayForm(onSubmit: (Holiday) -> Unit) {
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var name by remember { mutableStateOf("") }
    var isSubstitute by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        DateTextField("날짜", date, { date = it }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("이름 (예: 설날)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isSubstitute, onCheckedChange = { isSubstitute = it })
            Text("대체공휴일")
        }
        Button(
            onClick = { val d = date; if (d != null && name.isNotBlank()) onSubmit(Holiday(d, name.trim(), isSubstitute)) },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("추가") }
    }
}
