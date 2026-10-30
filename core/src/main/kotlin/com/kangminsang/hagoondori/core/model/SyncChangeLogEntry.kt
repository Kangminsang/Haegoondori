package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.Instant

/**
 * 마지막 동기화 이후 발생한 변경 하나(스펙 4.15절, F18). [SyncState.pendingChangeCount]는
 * "몇 건"만 보여주지만, 이 목록은 "무엇이" 바뀌었는지 사용자가 확인할 수 있게 한다.
 */
data class SyncChangeLogEntry(
    val id: String,
    val description: String,
    val occurredAt: Instant,
)
