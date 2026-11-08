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
import com.kangminsang.hagoondori.ui.common.Button
import com.kangminsang.hagoondori.ui.common.Card
import com.kangminsang.hagoondori.ui.common.DateTextField
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.datetime.LocalDate

private fun ConductPointKind.label() = if (this == ConductPointKind.MERIT) "양호" else "과실"

/** 점수 탭: 양호·과실 점수 합계와 기록. 장치로 보내지 않는 개인 기록이다. */
@Composable
fun ConductPointTab(
    uiState: LeaveManagementUiState,
    onAdd: (date: LocalDate, kind: ConductPointKind, points: Int, reason: String, memo: String?) -> Unit,
    onUpdate: (record: ConductPoint, date: LocalDate, kind: ConductPointKind, points: Int, reason: String, memo: String?) -> Unit,
    onDelete: (ConductPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = ConductPointCalculator.summarize(uiState.conductPoints)
    var showForm by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<ConductPoint?>(null) }

    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("양호·과실 점수", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("양호 ${summary.merit}점", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("과실 ${summary.demerit}점", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                val net = summary.net
                Text(
                    "합계 " + (if (net > 0) "+" else if (net < 0) "−" else "") + "${kotlin.math.abs(net)}점",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        TextButton(onClick = { showForm = !showForm; editingId = null }, modifier = Modifier.padding(top = 16.dp)) {
            Text(if (showForm) "닫기" else "+ 점수 기록 추가")
        }
        if (showForm) {
            ConductPointForm(
                onSubmit = { date, kind, points, reason, memo ->
                    onAdd(date, kind, points, reason, memo)
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
                        "${record.date} · ${record.kind.label()} · $sign${record.points}점 · ${record.reason}" +
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
                        submitText = "수정 저장",
                        onCancel = { editingId = null },
                        onSubmit = { date, kind, points, reason, memo ->
                            onUpdate(record, date, kind, points, reason, memo)
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
                text = { Text("${record.date} · ${record.kind.label()} ${record.points}점 · ${record.reason} 기록을 지웁니다.") },
                confirmButton = { Button(onClick = { onDelete(record); toDelete = null }) { Text("삭제") } },
                dismissButton = { Button(onClick = { toDelete = null }) { Text("취소") } },
            )
        }
    }
}

@Composable
private fun ConductPointForm(
    onSubmit: (date: LocalDate, kind: ConductPointKind, points: Int, reason: String, memo: String?) -> Unit,
    initial: ConductPoint? = null,
    submitText: String = "저장",
    onCancel: (() -> Unit)? = null,
) {
    var date by remember { mutableStateOf<LocalDate?>(initial?.date ?: AppClock.today()) }
    var kind by remember { mutableStateOf(initial?.kind ?: ConductPointKind.MERIT) }
    var pointsText by remember { mutableStateOf(initial?.points?.toString().orEmpty()) }
    var reason by remember { mutableStateOf(initial?.reason.orEmpty()) }
    var memo by remember { mutableStateOf(initial?.memo.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    Column {
        DateTextField("날짜", date, { date = it }, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.padding(top = 4.dp)) {
            ConductPointKind.entries.forEach { k ->
                TextButton(onClick = { kind = k }) { Text((if (kind == k) "● " else "○ ") + k.label()) }
            }
        }
        OutlinedTextField(
            value = pointsText,
            onValueChange = { pointsText = it.filter(Char::isDigit).take(3) },
            label = { Text("점수") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("사유") },
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
                val input = ConductPointCalculator.validate(pointsText, reason, memo)
                when {
                    d == null -> error = "날짜를 입력해 주세요."
                    input.error != null -> error = input.error
                    else -> {
                        error = null
                        onSubmit(d, kind, input.points!!, input.reason, memo.trim().ifBlank { null })
                    }
                }
            }) { Text(submitText) }
            if (onCancel != null) {
                TextButton(onClick = onCancel) { Text("취소") }
            }
        }
    }
}
