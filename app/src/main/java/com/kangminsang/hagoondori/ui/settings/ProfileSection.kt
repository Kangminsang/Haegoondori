package com.kangminsang.hagoondori.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import com.kangminsang.hagoondori.ui.common.Button
import com.kangminsang.hagoondori.ui.common.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kangminsang.hagoondori.core.model.UserProfile
import com.kangminsang.hagoondori.ui.common.DateTextField
import com.kangminsang.hagoondori.ui.common.TimeTextField
import kotlinx.datetime.LocalTime

/**
 * 복무 정보/규정 설정 (F1). [UserProfile]의 모든 필드가 여기서 정해진다 - 이 앱의
 * 모든 계산의 전제가 되는 화면이다.
 */
@Composable
fun ProfileSection(profile: UserProfile?, onSave: (UserProfile) -> Unit, modifier: Modifier = Modifier) {
    var enlistmentDate by remember(profile) { mutableStateOf(profile?.enlistmentDate) }
    var dischargeDate by remember(profile) { mutableStateOf(profile?.dischargeDate) }
    var promotionDate by remember(profile) { mutableStateOf(profile?.promotionDate) }
    var firstOvernightDate by remember(profile) { mutableStateOf(profile?.firstOvernightDate) }
    var cycleWeeksText by remember(profile) { mutableStateOf((profile?.overnightCycleWeeks ?: 6).toString()) }
    var weekdayPassText by remember(profile) { mutableStateOf((profile?.weekdayPassPerMonth ?: 2).toString()) }
    var holidayPassText by remember(profile) { mutableStateOf((profile?.holidayPassPerMonth ?: 1).toString()) }
    var wakeUpTime by remember(profile) { mutableStateOf(profile?.wakeUpTime ?: LocalTime(6, 0)) }
    var dinnerTime by remember(profile) { mutableStateOf(profile?.dinnerTime ?: LocalTime(18, 0)) }
    var autoAddOffDuty by remember(profile) { mutableStateOf(profile?.autoAddOffDuty ?: true) }

    var showFirstOvernightChangeWarning by remember { mutableStateOf(false) }
    var pendingProfile by remember { mutableStateOf<UserProfile?>(null) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("복무 정보", style = MaterialTheme.typography.titleMedium)

            DateTextField("입대일", enlistmentDate, { enlistmentDate = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            DateTextField("전역일", dischargeDate, { dischargeDate = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            DateTextField("진급 예정일(선택)", promotionDate, { promotionDate = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

            DateTextField("첫 외박일(선택)", firstOvernightDate, { firstOvernightDate = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Text(
                "첫 외박일은 6주 주기 격자의 기산점입니다. 이미 소멸 처리한 차수가 있는 " +
                    "상태에서 바꾸면 그 이력의 의미가 달라지니 주의하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )

            OutlinedTextField(
                value = cycleWeeksText,
                onValueChange = { cycleWeeksText = it.filter(Char::isDigit) },
                label = { Text("외박 주기(주), 기본 6") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = weekdayPassText,
                    onValueChange = { weekdayPassText = it.filter(Char::isDigit) },
                    label = { Text("월 평일 외출") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = holidayPassText,
                    onValueChange = { holidayPassText = it.filter(Char::isDigit) },
                    label = { Text("월 휴일 외출") },
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
            }

            Row(modifier = Modifier.padding(top = 8.dp)) {
                TimeTextField("총기상 시각", wakeUpTime, { it?.let { t -> wakeUpTime = t } }, modifier = Modifier.weight(1f))
                TimeTextField("석식 시각", dinnerTime, { it?.let { t -> dinnerTime = t } }, modifier = Modifier.weight(1f).padding(start = 8.dp))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("당직 입력 시 다음날 비번 자동 추가", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = autoAddOffDuty, onCheckedChange = { autoAddOffDuty = it })
            }

            Button(
                onClick = {
                    val enlistment = enlistmentDate
                    val discharge = dischargeDate
                    val cycleWeeks = cycleWeeksText.toIntOrNull()
                    val weekdayPass = weekdayPassText.toIntOrNull()
                    val holidayPass = holidayPassText.toIntOrNull()
                    if (enlistment == null || discharge == null || cycleWeeks == null || cycleWeeks <= 0 ||
                        weekdayPass == null || holidayPass == null
                    ) {
                        return@Button
                    }
                    val newProfile = UserProfile(
                        enlistmentDate = enlistment,
                        dischargeDate = discharge,
                        promotionDate = promotionDate,
                        firstOvernightDate = firstOvernightDate,
                        overnightCycleWeeks = cycleWeeks,
                        weekdayPassPerMonth = weekdayPass,
                        holidayPassPerMonth = holidayPass,
                        wakeUpTime = wakeUpTime,
                        dinnerTime = dinnerTime,
                        autoAddOffDuty = autoAddOffDuty,
                    )
                    val firstOvernightChanged = profile != null && profile.firstOvernightDate != firstOvernightDate
                    if (firstOvernightChanged) {
                        pendingProfile = newProfile
                        showFirstOvernightChangeWarning = true
                    } else {
                        onSave(newProfile)
                    }
                },
                modifier = Modifier.padding(top = 16.dp),
            ) { Text("저장") }
        }
    }

    if (showFirstOvernightChangeWarning) {
        AlertDialog(
            onDismissRequest = { showFirstOvernightChangeWarning = false },
            title = { Text("첫 외박일을 변경할까요?") },
            text = {
                Text(
                    "첫 외박일은 6주 주기 차수 번호의 기준점입니다. 지금 바꾸면 이전에 " +
                        "저장된 외박/소멸 기록의 차수 번호 의미가 달라집니다. 계속할까요?",
                )
            },
            confirmButton = {
                Button(onClick = {
                    pendingProfile?.let(onSave)
                    showFirstOvernightChangeWarning = false
                }) { Text("변경") }
            },
            dismissButton = {
                Button(onClick = { showFirstOvernightChangeWarning = false }) { Text("취소") }
            },
        )
    }
}
