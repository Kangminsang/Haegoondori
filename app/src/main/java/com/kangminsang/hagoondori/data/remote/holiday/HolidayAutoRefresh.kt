package com.kangminsang.hagoondori.data.remote.holiday

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱을 켤 때 공휴일이 오래됐으면 특일 정보 API로 자동 갱신한다. 설정 화면의 "공휴일 갱신"
 * 버튼을 누르지 않아도 올해·내년 공휴일(대체공휴일 포함)이 최신으로 유지된다.
 *
 * 마지막으로 **API 조회가 성공한** 시각만 기록한다. 키가 없거나 네트워크가 안 되어 내장
 * 데이터로 대체된 경우는 성공으로 치지 않으므로, 다음에 앱을 켤 때 다시 시도한다.
 */
@Singleton
class HolidayAutoRefresh @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remote: HolidayRemoteRepository,
    private val apiKeyStore: HolidayApiKeyStore,
) {
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val mutex = Mutex()

    suspend fun refreshIfStale() {
        if (!apiKeyStore.hasKey()) return
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (now - prefs.getLong(KEY_LAST_SUCCESS, 0L) < STALE_AFTER_MS) return
            val results = remote.refreshCurrentAndNextYear()
            if (results.all { it is HolidayFetchResult.Success }) {
                prefs.edit { putLong(KEY_LAST_SUCCESS, now) }
            }
        }
    }

    private companion object {
        const val PREFS_NAME = "holiday_auto_refresh"
        const val KEY_LAST_SUCCESS = "last_success_ms"
        const val STALE_AFTER_MS = 7L * 24 * 60 * 60 * 1000
    }
}
