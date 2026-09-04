package com.kangminsang.hagoondori.export

import android.content.Context
import com.kangminsang.hagoondori.core.export.CalendarSnapshot
import com.kangminsang.hagoondori.core.export.ExportAdapter
import com.kangminsang.hagoondori.core.export.ExportResult
import com.kangminsang.hagoondori.core.export.FailureReason
import com.kangminsang.hagoondori.core.payload.PayloadEncoder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * 개발/미리보기용 [ExportAdapter](스펙 6.3절). 실제 장치가 없어도 페이로드가
 * 명세와 일치하는지 눈으로 확인할 수 있도록, 앱 전용 외부 저장소에 그대로
 * `calendar.txt`로 저장한다. 항상 "연결됨"으로 취급한다.
 */
class FakeExportAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
) : ExportAdapter {

    override fun isDeviceConnected(): Boolean = true

    override fun export(snapshot: CalendarSnapshot): ExportResult = try {
        val text = PayloadEncoder.encode(snapshot)
        val dir = File(context.getExternalFilesDir(null), PREVIEW_DIR_NAME).apply { mkdirs() }
        File(dir, CALENDAR_FILE_NAME).writeText(text, Charsets.UTF_8)
        ExportResult.Success
    } catch (e: Exception) {
        ExportResult.Failure(FailureReason.WRITE_FAILED, e.message ?: "알 수 없는 오류로 저장하지 못했습니다")
    }

    companion object {
        private const val PREVIEW_DIR_NAME = "calendar_preview"
        private const val CALENDAR_FILE_NAME = "calendar.txt"
    }
}
