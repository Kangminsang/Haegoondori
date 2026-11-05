package com.kangminsang.hagoondori.ui.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
 */
@Composable
fun LeavePlanPanel(
    start: LocalDate,
    end: LocalDate,
    rows: List<PlanRow>,
    initialParts: List<PlanPart>,
    onSave: (List<PlanPart>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalDays = LeavePlanner.dayCount(start, end)
    // 기간이 바뀌면 그 기간의 기존 기록으로 다시 채운다. 같은 종류가 떨어져 여러 번 나와도 행은 하나라 합친다.
    val days = remember(start, end) {
        mutableStateMapOf<PlanKind, Int>().apply {
            initialParts.forEach { merge(it.kind, it.days, Int::plus) }
        }
    }
    val parts = rows.map { PlanPart(it.kind, days[it.kind] ?: 0) }
    val assigned = parts.sumOf { it.days }
    val left = totalDays - assigned

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
                TextButton(enabled = value > 0, onClick = { days[row.kind] = value - 1 }) { Text("−") }
                Text("$value", textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
                TextButton(enabled = left > 0, onClick = { days[row.kind] = value + 1 }) { Text("+") }
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

        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                LeavePlanner.autoFill(totalDays, rows.map { it.kind to (it.remaining ?: 0) })
                    .forEach { days[it.kind] = it.days }
            }) { Text("자동 배분") }
            TextButton(onClick = onCancel) { Text("취소") }
            Button(enabled = left == 0, onClick = { onSave(parts) }) { Text("저장") }
        }
    }
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
