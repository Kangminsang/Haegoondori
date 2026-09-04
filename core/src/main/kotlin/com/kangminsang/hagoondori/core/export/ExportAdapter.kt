package com.kangminsang.hagoondori.core.export

/**
 * 장치로 데이터를 내보내는 경계 인터페이스 (스펙 6.2절).
 *
 * **이 파일을 포함해 core 계층 코드 어디에도 USB, 파일 포맷, 장치 관련 단어가
 * 등장해서는 안 된다**(6.4절 규칙 1). 실제 구현(USB/SAF로 `calendar.txt`를 쓰는 것,
 * 혹은 개발용으로 로컬 파일에 저장하는 것)은 app 계층의 몫이다.
 */
interface ExportAdapter {
    fun isDeviceConnected(): Boolean
    fun export(snapshot: CalendarSnapshot): ExportResult
}
