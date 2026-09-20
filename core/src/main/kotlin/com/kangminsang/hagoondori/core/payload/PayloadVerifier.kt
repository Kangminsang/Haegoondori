package com.kangminsang.hagoondori.core.payload

/** [PayloadVerifier.verify]의 결과. [Valid] 외에는 장치가 기존 데이터를 유지하는 경우다. */
sealed class VerifyResult {
    data object Valid : VerifyResult()

    /** 0바이트 파일 - FAT에서 전송이 중단되면 이렇게 잘린다(펌웨어 스펙 2.3절). */
    data object Empty : VerifyResult()

    /** `Z` 줄이 없음 - 불완전 전송. */
    data object MissingTerminator : VerifyResult()

    /** `Z` 줄 형식이 깨졌거나 바이트 수가 실제와 다름 - 손상. */
    data object ByteCountMismatch : VerifyResult()

    data object CrcMismatch : VerifyResult()
}

/**
 * 장치 펌웨어의 무결성 검증(펌웨어 스펙 5.3/5.4절 2~4단계)을 그대로 재현한다.
 * 쓰기 직후 다시 읽어 장치가 받아들일 파일인지 확인하고, 이 검증 규칙을 앱 쪽 테스트로
 * 고정하는 용도다. 규칙: 파일 끝의 `Z|바이트수|CRC32` 줄을 찾고, 그 줄 직전까지의
 * 바이트 수와 CRC32가 일치해야 한다.
 */
object PayloadVerifier {

    fun verify(bytes: ByteArray): VerifyResult {
        if (bytes.isEmpty()) return VerifyResult.Empty

        // 마지막 줄의 시작 위치: 끝의 LF 하나를 제외하고 그 앞의 마지막 LF 다음.
        val end = if (bytes.last() == LF) bytes.size - 1 else bytes.size
        var lineStart = end
        while (lineStart > 0 && bytes[lineStart - 1] != LF) lineStart--

        if (lineStart >= end || bytes[lineStart] != 'Z'.code.toByte()) return VerifyResult.MissingTerminator

        val zLine = String(bytes, lineStart, end - lineStart, Charsets.UTF_8)
        val fields = zLine.split('|')
        if (fields.size != 3) return VerifyResult.ByteCountMismatch
        val declaredBytes = fields[1].toIntOrNull() ?: return VerifyResult.ByteCountMismatch

        if (declaredBytes != lineStart) return VerifyResult.ByteCountMismatch

        val actualCrc = Crc32.compute(bytes.copyOfRange(0, lineStart))
        return if (actualCrc == fields[2]) VerifyResult.Valid else VerifyResult.CrcMismatch
    }

    private const val LF = '\n'.code.toByte()
}
