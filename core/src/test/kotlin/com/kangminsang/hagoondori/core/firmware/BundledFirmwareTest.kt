package com.kangminsang.hagoondori.core.firmware

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** 앱에 내장한 장치 펌웨어(app/src/main/assets/firmware)가 그대로 쓸 수 있는 상태인지. */
class BundledFirmwareTest {
    private val dir = File("../app/src/main/assets/firmware")

    @Test
    fun `bundled UF2 parses and matches its recorded SHA-256`() {
        val (label, sha) = File(dir, "e-calendar.txt").readLines()
        val image = Uf2Image.parse(File(dir, "e-calendar.uf2").readBytes())
        assertEquals(sha.trim(), image.sha256, "e-calendar.txt의 해시를 새 UF2에 맞춰 주세요")
        assertTrue(label.isNotBlank())
        assertTrue(image.pages.keys.last() < Uf2Image.PROGRAM_END)
    }
}
