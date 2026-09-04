package com.kangminsang.hagoondori.core.payload

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Crc32Test {

    @Test
    fun `empty input yields all-zero CRC`() {
        assertEquals("00000000", Crc32.compute(ByteArray(0)))
    }

    @Test
    fun `known CRC-32 check value for the ASCII string '123456789'`() {
        // CRC-32(ISO-HDLC, java.util.zip.CRC32이 구현하는 것과 동일한 다항식)의
        // 표준 검증 문자열 "123456789"에 대한 잘 알려진 체크값이다.
        assertEquals("CBF43926", Crc32.compute("123456789".toByteArray(Charsets.US_ASCII)))
    }

    @Test
    fun `output is always 8 uppercase hex digits, zero-padded`() {
        val result = Crc32.compute(byteArrayOf(0))
        assertEquals(8, result.length)
        assertTrue(result.matches(Regex("^[0-9A-F]{8}$")))
    }

    @Test
    fun `same input always yields the same value`() {
        val bytes = "군생활 관리 앱".toByteArray(Charsets.UTF_8)
        assertEquals(Crc32.compute(bytes), Crc32.compute(bytes))
    }

    @Test
    fun `different input generally yields a different value`() {
        val a = Crc32.compute("hello".toByteArray())
        val b = Crc32.compute("world".toByteArray())
        assertTrue(a != b)
    }
}
