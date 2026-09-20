package com.kangminsang.hagoondori.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
 */
@Composable
fun SyncStatusBanner(
    syncState: SyncState,
    now: Instant,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastSyncedAt = syncState.lastSyncedAt
    val staleDays = lastSyncedAt?.let { (now - it).inWholeDays }
    val isStale = lastSyncedAt == null || (staleDays != null && staleDays >= STALE_SYNC_DAYS)

    if (!syncState.hasPendingChanges && !isStale) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
    }
}
