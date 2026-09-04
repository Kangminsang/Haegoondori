package com.kangminsang.hagoondori.export

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.kangminsang.hagoondori.core.export.CalendarSnapshot
import com.kangminsang.hagoondori.core.export.ExportAdapter
import com.kangminsang.hagoondori.core.export.ExportResult
import com.kangminsang.hagoondori.core.export.FailureReason
import com.kangminsang.hagoondori.core.payload.PayloadEncoder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 실제 e-ink 장치로 내보내는 [ExportAdapter](F17, 스펙 7장/8.3절).
 *
 * 장치는 USB 대용량 저장장치(MSC)로 인식되므로, 안드로이드에서는 이를 블록
 * 디바이스로 직접 열 수 없고 대신 사용자가 설정 화면에서 `ACTION_OPEN_DOCUMENT_TREE`로
 * 장치가 마운트된 폴더에 대한 접근 권한을 한 번 부여해야 한다([DeviceStorageAccess]가
 * 그 권한을 영구 보관한다). "연결됨" 판정은 그 저장된 권한이 여전히 유효한지로 갈음한다.
 *
 * **주의(8.3절 검증 절차)**: USB MSC 인식/마운트 거동은 기기·OS·장치 펌웨어에 따라
 * 달라 이 코드는 로컬 실기기에서 반드시 검증해야 한다. 만약 이 방식이 기대대로
 * 동작하지 않으면(예: 특정 기기가 MSC를 파일시스템으로 노출하지 않는 경우),
 * 6장의 격리 설계 덕분에 [ExportAdapter] 구현체만 시리얼(CDC) 통신 등으로
 * 교체하면 되고 core/7.2절 페이로드 형식은 그대로 유지된다.
 */
class RealExportAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceStorageAccess: DeviceStorageAccess,
) : ExportAdapter {

    override fun isDeviceConnected(): Boolean {
        val treeUri = deviceStorageAccess.savedTreeUri ?: return false
        return runCatching { DocumentFile.fromTreeUri(context, treeUri)?.exists() == true }.getOrDefault(false)
    }

    override fun export(snapshot: CalendarSnapshot): ExportResult {
        val treeUri = deviceStorageAccess.savedTreeUri
            ?: return ExportResult.Failure(
                FailureReason.DEVICE_NOT_CONNECTED,
                "장치 저장소가 아직 연결되지 않았습니다. 설정에서 장치 폴더를 선택해 주세요.",
            )

        val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
        if (treeDoc == null || !treeDoc.exists()) {
            return ExportResult.Failure(FailureReason.DEVICE_NOT_CONNECTED, "장치 저장소에 접근할 수 없습니다")
        }

        return try {
            val bytes = PayloadEncoder.encode(snapshot).toByteArray(Charsets.UTF_8)

            // "새로 만들며(기존 내용 절단) 처음부터 쓴다"(7.3절) - 기존 파일을 지우고 새로 만든다.
            treeDoc.findFile(CALENDAR_FILE_NAME)?.delete()
            val newFile = treeDoc.createFile("text/plain", CALENDAR_FILE_NAME)
                ?: return ExportResult.Failure(FailureReason.WRITE_FAILED, "calendar.txt 파일을 생성하지 못했습니다")

            val outputStream = context.contentResolver.openOutputStream(newFile.uri, "wt")
                ?: return ExportResult.Failure(FailureReason.PERMISSION_DENIED, "파일을 쓰기 모드로 열 수 없습니다")

            outputStream.use { stream ->
                stream.write(bytes)
                stream.flush()
            }

            ExportResult.Success
        } catch (e: SecurityException) {
            ExportResult.Failure(FailureReason.PERMISSION_DENIED, e.message ?: "저장소 접근 권한이 없습니다")
        } catch (e: Exception) {
            ExportResult.Failure(FailureReason.WRITE_FAILED, e.message ?: "쓰기 중 오류가 발생했습니다 (케이블 분리 등)")
        }
    }

    companion object {
        private const val CALENDAR_FILE_NAME = "calendar.txt"
    }
}
