package com.kangminsang.hagoondori.core.payload

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PayloadVerifierTest {

    private fun payload(body: String = "V|1\nH|2026-09-25|추석\n"): String {
        val bytes = body.toByteArray(Charsets.UTF_8)
        return body + "Z|${bytes.size}|${Crc32.compute(bytes)}\n"
    }

    private fun verify(text: String) = PayloadVerifier.verify(text.toByteArray(Charsets.UTF_8))

    @Test
    fun `정상 페이로드는 Valid`() = assertEquals(VerifyResult.Valid, verify(payload()))

    @Test
    fun `0바이트 파일은 Empty`() = assertEquals(VerifyResult.Empty, PayloadVerifier.verify(ByteArray(0)))

    @Test
    fun `Z 줄이 없이 잘린 파일은 MissingTerminator`() =
        assertEquals(VerifyResult.MissingTerminator, verify("V|1\nH|2026-09-25|추"))

    @Test
    fun `Z 줄의 CRC가 중간에서 잘리면 손상으로 거부`() =
        assertEquals(VerifyResult.CrcMismatch, verify(payload().dropLast(6)))

    @Test
    fun `Z 줄이 바이트 수 필드에서 잘리면 손상으로 거부`() =
        assertEquals(VerifyResult.ByteCountMismatch, verify(payload().substringBeforeLast("|").substringBeforeLast("|")))

    @Test
    fun `내용이 바뀌면 CrcMismatch`() {
        val text = payload().replace("추석", "설날")
        // 한글 두 글자 교체라 바이트 수는 같고 CRC만 달라진다
        assertEquals(VerifyResult.CrcMismatch, verify(text))
    }

    @Test
    fun `바이트 수가 다르면 ByteCountMismatch`() {
        val text = payload("V|1\n") + ""
        assertEquals(VerifyResult.ByteCountMismatch, verify("V|1\nX|extra\n" + text.substringAfter("V|1\n")))
    }

    @Test
    fun `이전 파일 잔여분이 뒤에 남으면 거부`() {
        // 새 페이로드가 옛 파일보다 짧을 때 절단 없이 덮어쓰면 옛 꼬리가 남는다
        assertEquals(VerifyResult.MissingTerminator, verify(payload() + "E|2026-09-12|2026-09-12|면회|1\n"))
    }
}
