package com.kangminsang.hagoondori.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.LocalTime

/** 분 선택 칸의 간격. 5분 단위가 아닌 기존 값은 그대로 두되, 칸에는 선택 표시가 없다. */
private const val MINUTE_STEP = 5
private const val GRID_COLUMNS = 6

/**
 * [LocalTime]을 입력받는 필드. 표시 필드는 읽기 전용이며, 탭하면 시(0~23)와 분(5분 단위)을
 * 버튼 격자에서 고르는 다이얼로그가 뜬다([DateTextField]와 같은 패턴). 기본 시계판 TimePicker는
 * 숫자가 작고 24시간 안팎 원이 겹쳐 고르기 어려워 쓰지 않는다.
 */
@Composable
fun TimeTextField(
    label: String,
    value: LocalTime?,
    onValueChange: (LocalTime?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                showPicker = true
            }
        }
    }

    OutlinedTextField(
        value = value?.let(::formatTime) ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = "시각 선택") },
        interactionSource = interactionSource,
        singleLine = true,
        modifier = modifier,
    )

    if (showPicker) {
        TimeGridDialog(
            title = label,
            initial = value,
            onDismissRequest = { showPicker = false },
            onConfirm = {
                onValueChange(it)
                showPicker = false
            },
        )
    }
}

private fun formatTime(time: LocalTime) = "%02d:%02d".format(time.hour, time.minute)

@Composable
private fun TimeGridDialog(
    title: String,
    initial: LocalTime?,
    onDismissRequest: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    var hour by remember { mutableIntStateOf(initial?.hour ?: -1) }
    var minute by remember { mutableIntStateOf(initial?.minute ?: 0) }

    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.padding(16.dp).widthIn(max = 480.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (hour >= 0) formatTime(LocalTime(hour, minute)) else "--:%02d".format(minute),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                Text("시", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                ChoiceGrid(values = (0..23).toList(), selected = hour, onSelect = { hour = it })

                Text("분", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                ChoiceGrid(values = (0 until 60 step MINUTE_STEP).toList(), selected = minute, onSelect = { minute = it })

                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissRequest) { Text("취소") }
                    TextButton(onClick = { onConfirm(LocalTime(hour, minute)) }, enabled = hour >= 0) { Text("확인") }
                }
            }
        }
    }
}

/** 두 자리 숫자 버튼을 [GRID_COLUMNS]개씩 줄지어 놓는다. 고른 칸은 채워서 보여준다. */
@Composable
private fun ChoiceGrid(values: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        values.chunked(GRID_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { v ->
                    val isSelected = v == selected
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                            .clickable { onSelect(v) },
                    ) {
                        if (isSelected) {
                            Surface(
                                color = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("%02d".format(v), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        } else {
                            Text("%02d".format(v), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
