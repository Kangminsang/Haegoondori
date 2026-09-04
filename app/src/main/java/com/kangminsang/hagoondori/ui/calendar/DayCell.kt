package com.kangminsang.hagoondori.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kangminsang.hagoondori.ui.theme.CombatRestTint
import com.kangminsang.hagoondori.ui.theme.LeaveTint
import com.kangminsang.hagoondori.ui.theme.OvernightTint
import com.kangminsang.hagoondori.ui.theme.PassTint

/**
 * 달력 격자 한 칸. 휴가/전투휴무/외박/외출을 각각 다른 색 점으로 시각적으로
 * 구분한다(스펙 5.1절 - 전투휴무는 영외 이동이 불가능해 휴가와 활용 방법이 전혀
 * 다르므로, 3.3.3절이 요구하는 대로 색까지 분리한다).
 */
@Composable
fun DayCell(day: CalendarDayInfo, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val contentColor = when {
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        day.isHoliday -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(day.date.dayOfMonth.toString(), color = contentColor, style = MaterialTheme.typography.bodyMedium)
        if (day.dutyAssignments.isNotEmpty()) {
            Text(
                day.dutyAssignments.first().type.name.take(1),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            if (day.hasLeave) MarkerDot(LeaveTint)
            if (day.hasCombatRest) MarkerDot(CombatRestTint)
            if (day.hasOvernight) MarkerDot(OvernightTint)
            if (day.hasPass) MarkerDot(PassTint)
            if (day.events.isNotEmpty()) MarkerDot(MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun MarkerDot(color: Color) {
    Box(
        modifier = Modifier
            .padding(horizontal = 1.dp)
            .size(5.dp)
            .clip(CircleShape)
            .background(color),
    )
}
