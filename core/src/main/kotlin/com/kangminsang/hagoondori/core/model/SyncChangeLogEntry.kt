package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.Instant

/**
 * 순 차이 방식 이전에 기록된 변경 하나(스펙 4.15절, F18). 지금은 "업데이트 전 변경"으로만 보여준다.
 * 새 미반영 변경은 페이로드 차이에서 계산하므로 이 표에 더 쌓이지 않는다.
 */
data class SyncChangeLogEntry(
    val id: String,
    val description: String,
    val occurredAt: Instant,
)
