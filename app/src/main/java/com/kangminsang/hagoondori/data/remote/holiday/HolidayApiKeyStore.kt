package com.kangminsang.hagoondori.data.remote.holiday

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 공공데이터포털 서비스키를 사용자가 앱에서 직접 입력해 이 기기에만 보관한다. 키를 빌드에 넣으면
 * APK에서 그대로 꺼낼 수 있어, 앱을 배포하거나 APK를 공유했을 때 키가 노출된다. 여기서는 앱
 * 전용 저장소(다른 앱이 읽을 수 없고, 백업도 꺼져 있다)에만 두고 소스·APK에는 넣지 않는다.
 */
@Singleton
class HolidayApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    fun get(): String = prefs.getString(KEY_SERVICE_KEY, "").orEmpty()

    fun hasKey(): Boolean = get().isNotBlank()

    /**
     * Retrofit이 서비스키를 다시 URL 인코딩하므로 Decoding 키가 필요하다. 포털의 "Encoding"
     * 키(`%`가 들어 있음)를 붙여 넣어도 동작하도록 여기서 디코딩해 저장한다.
     */
    fun save(rawKey: String) {
        val trimmed = rawKey.trim()
        val decoded = if ('%' in trimmed) URLDecoder.decode(trimmed, "UTF-8") else trimmed
        prefs.edit { putString(KEY_SERVICE_KEY, decoded) }
    }

    fun clear() {
        prefs.edit { remove(KEY_SERVICE_KEY) }
    }

    private companion object {
        const val PREFS_NAME = "holiday_api_key"
        const val KEY_SERVICE_KEY = "service_key"
    }
}
