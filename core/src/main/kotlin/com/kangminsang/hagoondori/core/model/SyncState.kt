package com.kangminsang.hagoondori.core.model

import kotlinx.datetime.Instant

/**
 * 동기화 상태 (스펙 4.15절). 앱 전체에서 단 1개만 존재한다.
 *
 * 미반영 변경은 더 이상 이 값으로 세지 않는다. 마지막으로 보낸 페이로드와 지금 데이터의 순 차이로
 * 계산하며(앱의 PendingChangesTracker), 그 기준 페이로드는 앱 쪽 저장소에 둔다. 이 앱의 가장 큰 실패
 * 시나리오는 "앱에서 고쳤는데 동기화를 잊어 장치가 옛 정보를 보여주는 것"이다(F18, 5.4절).
 */
data class SyncState(
    /** 마지막 성공 동기화 시각. 한 번도 동기화하지 않았다면 없다 */
    val lastSyncedAt: Instant? = null,
    /**
     * 예전 방식(변경 발생마다 1씩 증가)이 남긴 건수. 새 방식에서는 올리지 않으며 미반영 건수로 쓰지 않는다.
     * 동기화 성공 시 0으로 초기화한다.
     */
    val pendingChangeCount: Int = 0,
) {
    init {
        require(pendingChangeCount >= 0) { "pendingChangeCount는 음수일 수 없다: $pendingChangeCount" }
    }

    val hasPendingChanges: Boolean get() = pendingChangeCount > 0
}
