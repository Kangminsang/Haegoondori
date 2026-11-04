package com.kangminsang.hagoondori.data.repository

import android.util.Log
import com.kangminsang.hagoondori.core.export.TransmissionRange
import com.kangminsang.hagoondori.core.model.SyncChangeLogEntry
import com.kangminsang.hagoondori.core.payload.PayloadDiff
import com.kangminsang.hagoondori.core.payload.PayloadEncoder
import com.kangminsang.hagoondori.core.payload.PayloadMeta
import com.kangminsang.hagoondori.data.local.entity.SyncStateEntity
import com.kangminsang.hagoondori.export.SnapshotBuilder
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 동기화하지 않은 변경사항. [changes]는 마지막으로 장치에 보낸 페이로드와 지금 데이터의 순 차이를
 * 사람이 읽는 문장으로 옮긴 것이다. [legacy]는 순 차이 방식 이전에 쌓인 변경 내역으로, "업데이트 전 변경"으로만 보여준다.
 */
data class PendingChanges(
    val changes: List<String> = emptyList(),
    val legacy: List<SyncChangeLogEntry> = emptyList(),
) {
    val count: Int get() = changes.size + legacy.size
    val hasAny: Boolean get() = count > 0
}

/**
 * "동기화하지 않은 변경"을 계산한다. 변경 발생마다 건수를 세지 않고, 마지막으로 보낸 페이로드([SyncStateEntity.syncedPayload])와
 * 지금 데이터로 만든 페이로드를 비교한다. 그래서 근무를 추가했다가 지우면 0건이다.
 *
 * 데이터 Repository에는 주입하지 않는다(SnapshotBuilder가 Repository들에 의존하므로 순환이 생긴다).
 */
@Singleton
class PendingChangesTracker @Inject constructor(
    private val snapshotBuilder: SnapshotBuilder,
    private val syncStateRepository: SyncStateRepository,
) {
    /** 예전 버전에서 올라온 뒤 기준 페이로드를 채우는 작업이 여러 수집자에서 겹쳐 두 번 돌지 않도록 막는다. */
    private val upgradeMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<PendingChanges> = combine(
        syncStateRepository.changes,
        syncStateRepository.observeEntity(),
        syncStateRepository.observeChangeLog(),
    ) { _, entity, legacy -> entity to legacy }
        .mapLatest { (entity, legacy) ->
            try {
                compute(entity, legacy)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "미반영 변경 계산에 실패해 예전 변경 내역만 보여 준다", e)
                PendingChanges(legacy = legacy)
            }
        }
        .flowOn(Dispatchers.Default)

    private suspend fun compute(entity: SyncStateEntity?, legacy: List<SyncChangeLogEntry>): PendingChanges {
        val lastSyncedAt = entity?.lastSyncedAt
        var baseline = entity?.syncedPayload

        // 예전 버전에서 올라온 직후: 기준 페이로드가 없지만 동기화한 적은 있다. 마지막 동기화 시각의 범위와 시각으로
        // 지금 데이터를 한 번 만들어 기준으로 삼는다. 이후 변경은 이 기준과의 차이로 잡힌다.
        if (baseline == null && lastSyncedAt != null) {
            baseline = upgradeMutex.withLock {
                syncStateRepository.syncedPayload() ?: buildUpgradeBaseline(lastSyncedAt)?.also {
                    syncStateRepository.setBaseline(it)
                }
            }
            // 복무 정보가 없으면 기준을 만들 수 없다.
            if (baseline == null) return PendingChanges(legacy = legacy)
        }

        // 한 번도 동기화하지 않았다: 빈 장치에서 시작한 것으로 보고 지금 데이터 전부를 변경으로 본다.
        if (baseline == null) {
            val current = buildPayload(meta = null) ?: return PendingChanges(legacy = legacy)
            return PendingChanges(changes = PayloadDiff.describe("", current), legacy = legacy)
        }

        // 기준 페이로드와 같은 생성 시각·범위로 지금 데이터를 인코딩해야, 날짜 계산(진급 예정일 등)이 어긋나지 않는다.
        val meta = PayloadDiff.parseMeta(baseline)
        val current = buildPayload(meta) ?: return PendingChanges(legacy = legacy)
        return PendingChanges(changes = PayloadDiff.describe(baseline, current), legacy = legacy)
    }

    /** 지금 데이터로 페이로드 전문을 만든다. 복무 정보가 없으면 null. [meta]가 없으면 지금 시각과 오늘 기준 범위를 쓴다. */
    private suspend fun buildPayload(meta: PayloadMeta?): String? {
        val snapshot = if (meta == null) {
            snapshotBuilder.build()
        } else {
            snapshotBuilder.build(generatedAt = meta.generatedAt, rangeStart = meta.rangeStart, rangeEnd = meta.rangeEnd)
        } ?: return null
        return PayloadEncoder.encode(snapshot)
    }

    /** [syncedAt] 시점의 전송 범위(그 날짜가 속한 달 ~ +180일이 속한 달 말일)로 지금 데이터를 인코딩한다. */
    private suspend fun buildUpgradeBaseline(syncedAt: Instant): String? {
        val syncedDate: LocalDate = syncedAt.toLocalDateTime(AppClock.timeZone).date
        val snapshot = snapshotBuilder.build(
            generatedAt = syncedAt,
            rangeStart = TransmissionRange.start(syncedDate),
            rangeEnd = TransmissionRange.end(syncedDate),
        ) ?: return null
        return PayloadEncoder.encode(snapshot)
    }

    private companion object {
        const val TAG = "PendingChangesTracker"
    }
}
