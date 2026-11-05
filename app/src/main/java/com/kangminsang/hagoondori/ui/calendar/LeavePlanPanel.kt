package com.kangminsang.hagoondori.ui.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kangminsang.hagoondori.core.calc.LeavePlanner
import com.kangminsang.hagoondori.core.calc.PlanKind
import com.kangminsang.hagoondori.core.calc.PlanPart
import com.kangminsang.hagoondori.core.calc.PlanSegment
import com.kangminsang.hagoondori.core.payload.LeaveDeviceLabel
import com.kangminsang.hagoondori.ui.common.Button
import kotlinx.datetime.LocalDate

/**
 * 휴가 계획 배분 패널. 고른 기간의 일수를 외박과 휴가 종류별로 나눈다. 날짜는 행 순서대로 이어 붙는다
 * (외박 → 휴가 종류 표시 순서). 합계가 기간 일수와 같아야 저장할 수 있고, 잔여를 넘기는 종류는 경고만 띄운다.
 * 배분은 ViewModel이 들고 있다 - 달력 칸을 눌러 기간을 고치면 배분도 함께 바뀌기 때문이다.
 *
 * @param onDeleteTrip 기존 출타를 고치는 중일 때만 넘긴다. [이 출타 삭제]를 보이고, 누르면 확인 다이얼로그를 띄운다.
 */
@Composable
fun LeavePlanPanel(
    start: LocalDate,
    end: LocalDate,
    rows: List<PlanRow>,
    parts: List<PlanPart>,
    onChangeDays: (PlanKind, Int) -> Unit,
    onAutoFill: (List<PlanPart>) -> Unit,
    onSave: (List<PlanPart>) -> Unit,
    onCancel: () -> Unit,
    onRestart: () -> Unit,
    onDeleteTrip: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val totalDays = LeavePlanner.dayCount(start, end)
    val days = parts.associate { it.kind to it.days }
    val assigned = parts.sumOf { it.days }
    val left = totalDays - assigned
    var confirmingDelete by remember(start, end) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface)
            .padding(8.dp),
    ) {
        Text("${start.slashLabel()}~${end.slashLabel()} · ${totalDays}일", style = MaterialTheme.typography.titleSmall)

        rows.forEach { row ->
            val value = days[row.kind] ?: 0
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(row.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                // 휴가 관리의 위/아래 이동처럼 아이콘 버튼으로 늘리고 줄인다.
                IconButton(enabled = value > 0, onClick = { onChangeDays(row.kind, value - 1) }) {
                    Icon(Icons.Filled.Remove, contentDescription = "${row.name} 하루 빼기")
                }
                Text(
                    "$value",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(28.dp),
                )
                IconButton(enabled = left > 0, onClick = { onChangeDays(row.kind, value + 1) }) {
                    Icon(Icons.Filled.Add, contentDescription = "${row.name} 하루 더하기")
                }
                val remaining = row.remaining
                Text(
                    when {
                        remaining == null -> ""
                        value > remaining -> "잔여 ${remaining}일 · ${value - remaining}일 초과"
                        else -> "잔여 ${remaining}일"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remaining != null && value > remaining) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(110.dp),
                )
            }
        }

        Text(
            "남은 일수 $left",
            style = MaterialTheme.typography.bodyMedium,
            color = if (left != 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
        val preview = LeavePlanner.layout(start, parts)
        if (preview.isNotEmpty()) {
            Text(
                preview.joinToString(" · ") { previewText(it, rows) },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // 보조 동작은 글자 버튼으로 모으고, 다른 입력 폼처럼 [저장] 잉크 버튼 옆에 [취소]를 둔다.
        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                onAutoFill(LeavePlanner.autoFill(totalDays, rows.map { it.kind to (it.remaining ?: 0) }))
            }) { Text("자동 배분") }
            TextButton(onClick = onRestart) { Text("새로 고르기") }
            if (onDeleteTrip != null) {
                TextButton(onClick = { confirmingDelete = true }) { Text("이 출타 삭제") }
            }
        }
        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(enabled = left == 0, onClick = { onSave(parts) }) { Text("저장") }
            TextButton(onClick = onCancel, modifier = Modifier.padding(start = 8.dp)) { Text("취소") }
        }
    }

    if (confirmingDelete && onDeleteTrip != null) {
        // 기간을 고치던 중이어도 지우는 것은 고치기 전 출타다.
        DeleteTripDialog(
            message = "고치기 전 출타의 휴가·외박 기록을 모두 지웁니다.",
            onConfirm = { confirmingDelete = false; onDeleteTrip() },
            onDismiss = { confirmingDelete = false },
        )
    }
}

/** 출타 삭제 확인. 외박 기록 삭제와 같은 모양의 다이얼로그(잉크 버튼 두 개)를 쓴다. */
@Composable
internal fun DeleteTripDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("이 출타를 삭제할까요?") },
        text = { Text(message) },
        confirmButton = { Button(onClick = onConfirm) { Text("삭제") } },
        dismissButton = { Button(onClick = onDismiss) { Text("취소") } },
    )
}

/** 미리보기 한 구간: "포상 09-23~25"(같은 달), "연가 09-29~10-01"(달을 넘김), "외박 09-22"(하루). */
private fun previewText(segment: PlanSegment, rows: List<PlanRow>): String {
    val name = when (val kind = segment.kind) {
        PlanKind.Overnight -> "외박"
        is PlanKind.Leave -> rows.firstOrNull { it.kind == kind }?.name?.let(LeaveDeviceLabel::forTypeName) ?: "휴가"
    }
    val s = segment.start
    val e = segment.end
    val dates = when {
        s == e -> s.dashLabel()
        s.monthNumber == e.monthNumber && s.year == e.year -> "${s.dashLabel()}~%02d".format(e.dayOfMonth)
        else -> "${s.dashLabel()}~${e.dashLabel()}"
    }
    return "$name $dates"
}

private fun LocalDate.dashLabel(): String = "%02d-%02d".format(monthNumber, dayOfMonth)

/** 화면 제목용 날짜: 9/22. */
internal fun LocalDate.slashLabel(): String = "$monthNumber/$dayOfMonth"
