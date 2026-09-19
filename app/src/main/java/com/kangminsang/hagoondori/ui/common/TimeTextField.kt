package com.kangminsang.hagoondori.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.LocalTime

/**
 * 안드로이드 기본 시각 선택 다이얼로그([TimePicker])로 [LocalTime]을 입력받는 필드.
 * 표시 필드는 읽기 전용이며, 탭하면 다이얼로그가 뜬다([DateTextField]와 같은 패턴).
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        value = value?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = "시각 선택") },
        interactionSource = interactionSource,
        singleLine = true,
        modifier = modifier,
    )

    if (showPicker) {
        val initial = value ?: LocalTime(0, 0)
        val timePickerState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { showPicker = false },
            onConfirm = {
                onValueChange(LocalTime(timePickerState.hour, timePickerState.minute))
                showPicker = false
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

/**
 * Material3(1.3.1 기준)는 [TimePicker]만 제공하고 [DatePickerDialog]와 달리
 * 다이얼로그 래퍼는 제공하지 않으므로 직접 감싼다.
 */
@Composable
private fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                content()
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissRequest) { Text("취소") }
                    TextButton(onClick = onConfirm) { Text("확인") }
                }
            }
        }
    }
}
