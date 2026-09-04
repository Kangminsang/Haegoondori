package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.Instant

/**
 * 동기화 상태 (스펙 4.15절). 앱 전체에서 단 1개만 존재한다.
 *
 * 데이터 변경 시마다 [pendingChangeCount]를 증가시키고, 동기화 성공 시 0으로 초기화한다.
 * 이 앱의 가장 큰 실패 시나리오는 "앱에서 고쳤는데 동기화를 잊어 장치가 옛 정보를
 * 보여주는 것"이다(F18, 5.4절) — 이 값이 그 상태를 사용자에게 드러내는 근거가 된다.
 */
data class SyncState(
    /** 마지막 성공 동기화 시각. 한 번도 동기화하지 않았다면 없다 */
    val lastSyncedAt: Instant? = null,
    /** 마지막 동기화 이후 발생한 변경 건수 */
    val pendingChangeCount: Int = 0,
) {
    init {
        require(pendingChangeCount >= 0) { "pendingChangeCount는 음수일 수 없다: $pendingChangeCount" }
    }

    val hasPendingChanges: Boolean get() = pendingChangeCount > 0
}
