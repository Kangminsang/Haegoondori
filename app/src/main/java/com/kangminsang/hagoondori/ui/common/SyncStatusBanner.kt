package com.kangminsang.hagoondori.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kangminsang.hagoondori.core.model.SyncChangeLogEntry
import com.kangminsang.hagoondori.core.model.SyncState
import kotlinx.datetime.Instant

/** 마지막 동기화 후 이만큼 지나면 별도 안내를 띄운다(스펙 5.4절 - 달이 바뀌면 달력이 달라지므로). */
private const val STALE_SYNC_DAYS = 14

/**
 * 동기화 상태 배너(F18, 스펙 5.4절).
 *
 * 이 앱의 가장 큰 실패 시나리오는 "앱에서 고쳤는데 동기화를 잊어 장치가 옛 정보를
 * 보여주는 것"이다 - 그래서 미반영 변경이 있거나 마지막 동기화가 14일을 넘으면
 * 눈에 띄게 경고하고 즉시 동기화 버튼을 제공한다. 둘 다 해당하지 않으면 배너
 * 자체를 그리지 않는다.
 *
 * 건수만으로는 "무엇이" 바뀌었는지 알 수 없다는 피드백이 있어, [changeLog]를 펼쳐
 * 각 변경의 설명을 확인할 수 있게 한다.
 */
@Composable
fun SyncStatusBanner(
    syncState: SyncState,
    now: Instant,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier,
    changeLog: List<SyncChangeLogEntry> = emptyList(),
) {
    val lastSyncedAt = syncState.lastSyncedAt
    val staleDays = lastSyncedAt?.let { (now - it).inWholeDays }
    val isStale = lastSyncedAt == null || (staleDays != null && staleDays >= STALE_SYNC_DAYS)

    if (!syncState.hasPendingChanges && !isStale) return

    var showChangeLog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    if (syncState.hasPendingChanges) {
                        Text(
                            "동기화하지 않은 변경사항이 ${syncState.pendingChangeCount}건 있습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    if (isStale) {
                        val message = if (lastSyncedAt == null) {
                            "아직 한 번도 동기화하지 않았습니다"
                        } else {
                            "마지막 동기화 후 ${staleDays}일이 지났습니다"
                        }
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                TextButton(onClick = onSyncNow) { Text("지금 동기화") }
            }

            if (syncState.hasPendingChanges && changeLog.isNotEmpty()) {
                TextButton(onClick = { showChangeLog = !showChangeLog }) {
                    Text(if (showChangeLog) "변경사항 숨기기" else "변경사항 보기")
                }
                if (showChangeLog) {
                    Divider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    changeLog.forEach { entry ->
                        ChangeLogLine(entry, now)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeLogLine(entry: SyncChangeLogEntry, now: Instant) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            entry.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        Text(
            relativeTimeLabel(entry.occurredAt, now),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

/** "방금", "12분 전", "3시간 전", "2일 전"처럼 변경 시각을 짧게 표시한다. */
private fun relativeTimeLabel(occurredAt: Instant, now: Instant): String {
    val elapsed = now - occurredAt
    return when {
        elapsed.inWholeMinutes < 1 -> "방금"
        elapsed.inWholeHours < 1 -> "${elapsed.inWholeMinutes}분 전"
        elapsed.inWholeDays < 1 -> "${elapsed.inWholeHours}시간 전"
        else -> "${elapsed.inWholeDays}일 전"
    }
}
