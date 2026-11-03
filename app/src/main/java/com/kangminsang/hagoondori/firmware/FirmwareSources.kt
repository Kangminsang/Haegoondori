package com.kangminsang.hagoondori.firmware

import android.content.Context
import android.net.Uri
import com.kangminsang.hagoondori.core.firmware.InvalidUf2Exception
import com.kangminsang.hagoondori.core.firmware.Uf2Image
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 플래시할 펌웨어와 화면에 보여 줄 이름. */
data class FirmwareSource(val label: String, val image: Uf2Image)

/**
 * 펌웨어를 가져온다: 앱에 들어 있는 것(`assets/firmware/e-calendar.uf2`) 또는 사용자가 고른 UF2 파일.
 *
 * 내장 펌웨어는 `e-calendar.txt`의 첫 줄이 이름, 둘째 줄이 SHA-256이다. 해시가 다르면 빌드 실수이므로 쓰지 않는다.
 * 내장 펌웨어를 바꾸는 방법은 CLAUDE.md "장치 펌웨어 내장" 참고.
 */
@Singleton
class FirmwareSources @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun bundled(): FirmwareSource = withContext(Dispatchers.IO) {
        val (label, sha) = context.assets.open("$DIR/e-calendar.txt").bufferedReader().readLines()
        val image = Uf2Image.parse(context.assets.open("$DIR/e-calendar.uf2").use { it.readBytes() })
        if (image.sha256 != sha.trim()) throw InvalidUf2Exception("앱에 들어 있는 펌웨어가 손상됐습니다")
        FirmwareSource(label.trim(), image)
    }

    suspend fun fromFile(uri: Uri): FirmwareSource = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val out = input.readNBytes(MAX_FILE_BYTES + 1)
            if (out.size > MAX_FILE_BYTES) throw InvalidUf2Exception("파일이 너무 큽니다")
            out
        } ?: throw InvalidUf2Exception("파일을 열지 못했습니다")
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "선택한 파일"
        FirmwareSource(name, Uf2Image.parse(bytes))
    }

    private companion object {
        const val DIR = "firmware"

        /** 프로그램 영역 640KB를 UF2(페이지당 512바이트)로 담은 크기. */
        const val MAX_FILE_BYTES = 640 * 1024 * 2
    }
}
