package com.kangminsang.hagoondori.ui.settings

import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.SyncChangeLogEntry
import com.kangminsang.hagoondori.core.model.SyncState
import com.kangminsang.hagoondori.core.model.UserProfile

/** ⑤ 설정(스펙 5.1/5.2절) 화면 상태 - 복무정보/공휴일/동기화가 모두 여기 모인다. */
data class SettingsUiState(
    val profile: UserProfile? = null,
    val holidays: List<Holiday> = emptyList(),
    val syncState: SyncState = SyncState(),
    val syncChangeLog: List<SyncChangeLogEntry> = emptyList(),
)
