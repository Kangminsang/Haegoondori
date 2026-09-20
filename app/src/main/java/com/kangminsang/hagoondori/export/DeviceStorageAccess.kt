package com.kangminsang.hagoondori.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 장치가 USB 대용량 저장장치(MSC)로 마운트된 폴더에 대한 SAF(Storage Access
 * Framework) 접근 권한을 관리한다(스펙 8.3절 검증 절차 2/3).
 *
 * 사용자가 설정 화면에서 `ACTION_OPEN_DOCUMENT_TREE`로 장치 폴더를 한 번 선택하면,
 * 그 권한을 영구적으로("persistable") 저장해 앱을 다시 켜도 재선택할 필요가 없게
 * 한다. [com.kangminsang.hagoondori.core.export.ExportAdapter]는 순수 인터페이스라
 * Uri/SAF 같은 Android 개념을 몰라야 하므로, 이 클래스가 그 경계를 담당한다.
 */
@Singleton
class DeviceStorageAccess @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    val savedTreeUri: Uri?
        get() = prefs.getString(KEY_TREE_URI, null)?.let(Uri::parse)

    /** SAF 폴더 선택 결과로 받은 [uri]에 대한 영구 권한을 확보하고 저장한다. */
    fun persist(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        prefs.edit { putString(KEY_TREE_URI, uri.toString()) }
    }

    /**
     * 저장된 폴더에 대한 쓰기 권한을 시스템이 아직 갖고 있는지. USB 장치를 뽑았다 다시 꽂으면 안드로이드가
     * 이동식 볼륨에 대한 영구 권한을 회수하므로, uri 문자열이 저장돼 있어도 이 값은 false가 될 수 있다.
     */
    fun hasPersistedWritePermission(): Boolean {
        val saved = savedTreeUri ?: return false
        return context.contentResolver.persistedUriPermissions.any { it.uri == saved && it.isWritePermission }
    }

    fun clear() {
        prefs.edit { remove(KEY_TREE_URI) }
    }

    private companion object {
        const val PREFS_NAME = "device_storage_access"
        const val KEY_TREE_URI = "tree_uri"
    }
}
