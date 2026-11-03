package com.kangminsang.hagoondori.core.firmware

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class Uf2ImageTest {

    @Test
    fun `pages are keyed by flash address in order`() {
        val pages = firmwarePages(5)
        val image = Uf2Image.parse(uf2(pages.entries.reversed().associate { it.key to it.value }))
        assertEquals(pages.keys.toList(), image.pages.keys.toList())
        assertTrue(image.pages.getValue(Uf2Image.FLASH_BASE).contentEquals(pages.getValue(Uf2Image.FLASH_BASE)))
        assertEquals(64, image.sha256.length)
    }

    @Test
    fun `filesystem area is refused`() {
        val pages = firmwarePages(2) + (Uf2Image.PROGRAM_END to ByteArray(256))
        val e = assertThrows<InvalidUf2Exception> { Uf2Image.parse(uf2(pages)) }
        assertTrue(e.message!!.contains("데이터 영역"))
    }

    @Test
    fun `program area ends where the E-calendar filesystem starts`() {
        assertEquals(0x100A0000L, Uf2Image.PROGRAM_END)
    }

    @Test
    fun `other chip families and partial images are refused`() {
        assertThrows<InvalidUf2Exception> { Uf2Image.parse(uf2(firmwarePages(2), family = 0xE48BFF59L)) }
        val noBoot = firmwarePages(3).filterKeys { it != Uf2Image.FLASH_BASE }
        assertThrows<InvalidUf2Exception> { Uf2Image.parse(uf2(noBoot)) }
    }

    @Test
    fun `non UF2 data is refused`() {
        assertThrows<InvalidUf2Exception> { Uf2Image.parse(ByteArray(0)) }
        assertThrows<InvalidUf2Exception> { Uf2Image.parse(ByteArray(512)) }
        assertThrows<InvalidUf2Exception> { Uf2Image.parse("hello".toByteArray()) }
    }

    @Test
    fun `blocks not meant for main flash are skipped`() {
        val image = Uf2Image.parse(uf2(firmwarePages(2)) + uf2(mapOf(0x20000000L to ByteArray(256)), flags = 1))
        assertEquals(2, image.pages.size)
    }
}
