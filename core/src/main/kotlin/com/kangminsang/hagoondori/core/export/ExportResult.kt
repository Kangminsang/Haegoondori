package com.kangminsang.hagoondori.core.export

/** [ExportAdapter.export] 실패 사유 (스펙 6.2절). */
enum class FailureReason {
    /** 장치 미연결 */
    DEVICE_NOT_CONNECTED,

    /** 저장소 접근 권한 없음 */
    PERMISSION_DENIED,

    /** 쓰기 중 오류 (케이블 분리 등) */
    WRITE_FAILED,

    /** 페이로드 크기 초과 */
    PAYLOAD_TOO_LARGE,
}

/** [ExportAdapter.export]의 결과. */
sealed class ExportResult {
    data object Success : ExportResult()
    data class Failure(val reason: FailureReason, val message: String) : ExportResult()
}
