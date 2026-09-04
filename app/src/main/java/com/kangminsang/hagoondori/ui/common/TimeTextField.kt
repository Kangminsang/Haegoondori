package com.kangminsang.hagoondori.ui.common

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalTime

/** `HH:MM` 텍스트 입력으로 [LocalTime]을 받는 간단한 필드([DateTextField]와 같은 패턴). */
@Composable
fun TimeTextField(
    label: String,
    value: LocalTime?,
    onValueChange: (LocalTime?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(value?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "") }
    val isError = text.isNotBlank() && parseTime(text) == null

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            onValueChange(parseTime(newText))
        },
        label = { Text(label) },
        placeholder = { Text("HH:MM") },
        isError = isError,
        singleLine = true,
        modifier = modifier,
    )
}

private fun parseTime(text: String): LocalTime? {
    val parts = text.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return runCatching { LocalTime(hour, minute) }.getOrNull()
}
