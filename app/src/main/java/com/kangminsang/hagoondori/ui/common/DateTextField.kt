package com.kangminsang.hagoondori.ui.common

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate

/**
 * `YYYY-MM-DD` 텍스트 입력으로 [LocalDate]를 받는 간단한 필드. 입력 중에는 아직
 * 완성되지 않은 문자열도 자유롭게 타이핑할 수 있게 두고, 파싱 가능한 값이 나올
 * 때만 [onValueChange]로 전달한다. 형식이 아직 유효하지 않은 동안(빈 값이
 * 아닌 한)에는 에러 상태로 표시한다.
 */
@Composable
fun DateTextField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(value?.toString() ?: "") }
    val isError = text.isNotBlank() && runCatching { LocalDate.parse(text) }.isFailure

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            onValueChange(runCatching { LocalDate.parse(newText) }.getOrNull())
        },
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") },
        isError = isError,
        singleLine = true,
        modifier = modifier,
    )
}
