package com.kangminsang.hagoondori.export

import com.kangminsang.hagoondori.core.export.ExportAdapter
import com.kangminsang.hagoondori.core.export.ExportResult
import com.kangminsang.hagoondori.core.export.FailureReason
import com.kangminsang.hagoondori.data.repository.SyncStateRepository
import com.kangminsang.hagoondori.di.FakeAdapter
import com.kangminsang.hagoondori.di.RealAdapter
import com.kangminsang.hagoondori.util.AppClock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "스냅샷 빌드 → 내보내기 → 성공 시 동기화 상태 갱신" 흐름의 단일 진입점(F17/F18).
 * 성공했을 때만 [SyncStateRepository.markSynced]가 불려 "동기화 안 된 변경" 배너가
 * 거짓으로 사라지는 일이 없도록 한다.
 */
@Singleton
class DeviceSyncService @Inject constructor(
    private val snapshotBuilder: SnapshotBuilder,
    private val syncStateRepository: SyncStateRepository,
    @RealAdapter private val realAdapter: ExportAdapter,
    @FakeAdapter private val fakeAdapter: ExportAdapter,
) {
    suspend fun syncToDevice(): ExportResult {
        val snapshot = snapshotBuilder.build() ?: return NO_PROFILE
        val result = realAdapter.export(snapshot)
        if (result is ExportResult.Success) syncStateRepository.markSynced(AppClock.now())
        return result
    }

    /** 실기기 없이 페이로드를 앱 내부 저장소에 남긴다. 동기화 상태는 바꾸지 않는다. */
    suspend fun exportPreview(): ExportResult {
        val snapshot = snapshotBuilder.build() ?: return NO_PROFILE
        return fakeAdapter.export(snapshot)
    }

    private companion object {
        val NO_PROFILE = ExportResult.Failure(FailureReason.WRITE_FAILED, "복무 정보를 먼저 입력해 주세요")
    }
}
