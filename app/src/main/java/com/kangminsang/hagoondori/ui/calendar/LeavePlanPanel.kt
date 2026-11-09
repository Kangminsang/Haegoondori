package com.kangminsang.hagoondori.ui.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kangminsang.hagoondori.core.calc.LeavePlanner
import com.kangminsang.hagoondori.core.calc.PlanKind
import com.kangminsang.hagoondori.core.calc.PlanSegment
import com.kangminsang.hagoondori.core.payload.LeaveDeviceLabel
import com.kangminsang.hagoondori.ui.common.Button
import kotlinx.datetime.LocalDate

/**
 * 휴가 계획 배분 패널. 행(외박 + 휴가 종류)을 눌러 칠할 종류를 고르면, 달력 칸을 누를 때 그 날이 그 종류로 칠해진다.
 * 행마다 칠한 일수와 잔여를 보이고, 잔여를 넘기는 종류는 경고만 띄운다. 모든 날을 칠해야(미배분 0일) 저장할 수 있다.
 * 배치는 ViewModel이 들고 있다 - 달력 칸을 눌러 칠하거나 기간을 고치기 때문이다.
 *
 * @param onDeleteTrip 기존 출타를 고치는 중일 때만 넘긴다. [이 출타 삭제]를 보이고, 누르면 확인 다이얼로그를 띄운다.
 */
@Composable
fun LeavePlanPanel(
    start: LocalDate,
    end: LocalDate,
    rows: List<PlanRow>,
    dayKinds: List<PlanKind?>,
    brush: PlanKind?,
    onSelectBrush: (PlanKind) -> Unit,
    onAutoFill: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRestart: () -> Unit,
    onDeleteTrip: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val totalDays = LeavePlanner.dayCount(start, end)
    val counts = LeavePlanner.counts(dayKinds)
    val unassigned = LeavePlanner.unassigned(dayKinds)
    var confirmingDelete by remember(start, end) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface)
            .padding(8.dp),
    ) {
        Text("${start.slashLabel()}~${end.slashLabel()} · ${totalDays}일", style = MaterialTheme.typography.titleSmall)
        Text(
            "칠할 종류",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        )

        rows.forEach { row ->
            BrushRow(row, days = counts[row.kind] ?: 0, selected = row.kind == brush, onClick = { onSelectBrush(row.kind) })
        }

        if (unassigned > 0) {
            Text(
                "미배분 ${unassigned}일",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        val preview = LeavePlanner.segmentsOf(start, dayKinds)
        if (preview.isNotEmpty()) {
            Text(
                preview.joinToString(" · ") { previewText(it, rows) },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // 보조 동작은 글자 버튼으로 모으고, 다른 입력 폼처럼 [저장] 잉크 버튼 옆에 [취소]를 둔다.
        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(enabled = unassigned > 0, onClick = onAutoFill) { Text("자동 배분") }
            TextButton(onClick = onRestart) { Text("새로 고르기") }
            if (onDeleteTrip != null) {
                TextButton(onClick = { confirmingDelete = true }) { Text("이 출타 삭제") }
            }
        }
        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(enabled = unassigned == 0, onClick = onSave) { Text("저장") }
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

/**
 * 칠할 종류 한 행: 이름, 칠한 일수, 잔여(외박은 없음). 고른 행은 오늘 배지·당직 칩처럼 잉크로 반전한다.
 * 다시 누르면 고르기가 풀린다.
 */
@Composable
private fun BrushRow(row: PlanRow, days: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val textColor = if (selected) colors.surface else colors.onSurface
    val remaining = row.remaining
    val over = remaining != null && days > remaining
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .then(if (selected) Modifier.background(colors.onSurface) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.name, style = MaterialTheme.typography.bodyMedium, color = textColor, modifier = Modifier.weight(1f))
        Text(
            "${days}일",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp),
        )
        Text(
            when {
                remaining == null -> ""
                over -> "잔여 ${remaining}일 · ${days - remaining}일 초과"
                else -> "잔여 ${remaining}일"
            },
            style = MaterialTheme.typography.bodySmall,
            // 반전된 잉크 바탕 위에서는 빨강이 잘 안 보여 바탕색 글자로 두고, "초과" 문구로 알린다.
            color = if (over && !selected) colors.error else textColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(120.dp),
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
