package com.kangminsang.hagoondori.core.firmware

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** UF2 블록을 만든다. [pages]의 키는 플래시 주소. */
internal fun uf2(pages: Map<Long, ByteArray>, family: Long? = 0xE48BFF56L, flags: Long = 0): ByteArray {
    val out = ByteBuffer.allocate(pages.size * 512).order(ByteOrder.LITTLE_ENDIAN)
    pages.entries.forEachIndexed { i, (addr, data) ->
        val base = i * 512
        out.putInt(base, 0x0A324655).putInt(base + 4, 0x9E5D5157.toInt())
        out.putInt(base + 8, (flags or if (family != null) 0x2000L else 0L).toInt())
        out.putInt(base + 12, addr.toInt()).putInt(base + 16, 256)
        out.putInt(base + 20, i).putInt(base + 24, pages.size).putInt(base + 28, (family ?: 0L).toInt())
        out.position(base + 32)
        out.put(data)
        out.putInt(base + 508, 0x0AB16F30)
    }
    return out.array()
}

/** 주소마다 다른 내용의 페이지 [count]개(플래시 시작부터 연속). */
internal fun firmwarePages(count: Int, seed: Int = 1): Map<Long, ByteArray> =
    (0 until count).associate { i ->
        Uf2Image.FLASH_BASE + i * 256L to ByteArray(256) { ((i * 7 + it + seed) % 251).toByte() }
    }

/**
 * 부트롬 흉내: 지우면 0xFF, 쓰기는 지운 페이지에만 허용. 명령 순서를 [log]에 남긴다.
 * [failOnWrite]번째 페이지 쓰기에서 USB가 끊긴 것처럼 실패한다.
 */
private class FakeBoard(var failOnWrite: Int? = null) : PicobootTransport {
    val flash = ByteArray(2 * 1024 * 1024) { 0xEE.toByte() }
    val log = mutableListOf<Pair<Int, Long>>()
    var writes = 0
    var corruptAfterWrite: Long? = null
    private var pending: Triple<Int, Long, Int>? = null

    override fun bulkOut(data: ByteArray) {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        if (data.size == 32 && buf.getInt(0) == 0x431FD10B) {
            val id = data[8].toInt() and 0xFF
            val addr = buf.getInt(16).toLong() and 0xFFFFFFFFL
            val size = buf.getInt(20)
            pending = Triple(id, addr, size)
            log += id to addr
            if (id == 0x03) {
                check(addr % 4096 == 0L && size == 4096 && addr < Uf2Image.PROGRAM_END)
                flash.fill(0xFF.toByte(), off(addr), off(addr) + size)
            }
            return
        }
        val (id, addr, size) = pending ?: error("data without command")
        if (id == 0x05 && data.isNotEmpty()) {
            check(data.size == size && size == 256)
            check((0 until size).all { flash[off(addr) + it] == 0xFF.toByte() }) { "write to unerased page" }
            data.copyInto(flash, off(addr))
            writes++
            if (corruptAfterWrite == addr) flash[off(addr)] = (flash[off(addr)] + 1).toByte()
            if (writes == failOnWrite) throw IOException("cable pulled")
        }
    }

    override fun bulkIn(length: Int): ByteArray {
        val (id, addr, size) = pending ?: error("read without command")
        if (length == 0) return ByteArray(0)
        check(id == 0x84 && length == size)
        return flash.copyOfRange(off(addr), off(addr) + size)
    }

    fun install(pages: Map<Long, ByteArray>) = pages.forEach { (addr, data) -> data.copyInto(flash, off(addr)) }

    fun holds(pages: Map<Long, ByteArray>) =
        pages.all { (addr, data) -> flash.copyOfRange(off(addr), off(addr) + 256).contentEquals(data) }

    private fun off(addr: Long) = (addr - Uf2Image.FLASH_BASE).toInt()
}

class FirmwareFlasherTest {
    private val pages = firmwarePages(160)         // 10 sectors of 16 pages
    private val image = Uf2Image.parse(uf2(pages))

    @Test
    fun `fresh board gets every sector, boot sector erased first and written last`() {
        val board = FakeBoard()
        board.install(mapOf(Uf2Image.FLASH_BASE to pages.getValue(Uf2Image.FLASH_BASE)))
        assertEquals(10, FirmwareFlasher(Picoboot(board)).flash(image))
        assertTrue(board.holds(pages))
        val erases = board.log.filter { it.first == 0x03 }.map { it.second }
        val writes = board.log.filter { it.first == 0x05 }.map { it.second }
        assertEquals(Uf2Image.FLASH_BASE, erases.first())
        assertEquals((0 until 16).map { Uf2Image.FLASH_BASE + it * 256L }, writes.takeLast(16))
    }

    @Test
    fun `filesystem area is never touched`() {
        val board = FakeBoard()
        FirmwareFlasher(Picoboot(board)).flash(image)
        val fs = (Uf2Image.PROGRAM_END - Uf2Image.FLASH_BASE).toInt()
        assertTrue(board.flash.copyOfRange(fs, board.flash.size).all { it == 0xEE.toByte() })
    }

    @Test
    fun `same firmware writes nothing`() {
        val board = FakeBoard()
        board.install(pages)
        assertEquals(0, FirmwareFlasher(Picoboot(board)).flash(image))
        assertTrue(board.log.none { it.first == 0x03 || it.first == 0x05 })
    }

    @Test
    fun `only changed sectors are rewritten`() {
        val board = FakeBoard()
        board.install(pages)
        val changed = pages.toMutableMap()
        changed[Uf2Image.FLASH_BASE + 4096 * 3 + 256] = ByteArray(256) { 9 }
        assertEquals(1, FirmwareFlasher(Picoboot(board)).flash(Uf2Image.parse(uf2(changed))))
        assertEquals(listOf(Uf2Image.FLASH_BASE + 4096 * 3), board.log.filter { it.first == 0x03 }.map { it.second })
        assertTrue(board.holds(changed))
    }

    @Test
    fun `an interrupted flash leaves the boot sector erased and a retry finishes it`() {
        val board = FakeBoard(failOnWrite = 15)
        assertThrows<IOException> { FirmwareFlasher(Picoboot(board)).flash(image) }
        assertTrue((0 until 256).all { board.flash[it] == 0xFF.toByte() })
        board.failOnWrite = null
        FirmwareFlasher(Picoboot(board)).flash(image)
        assertTrue(board.holds(pages))
    }

    @Test
    fun `verify failure is reported and progress reaches the end otherwise`() {
        val board = FakeBoard()
        board.corruptAfterWrite = Uf2Image.FLASH_BASE + 4096 * 2
        assertThrows<PicobootException> { FirmwareFlasher(Picoboot(board)).flash(image) }

        val events = mutableListOf<FirmwareFlasher.Progress>()
        FirmwareFlasher(Picoboot(FakeBoard())).flash(image) { events += it }
        assertEquals(FirmwareFlasher.Progress(FirmwareFlasher.Phase.Verifying, 10, 10), events.last())
    }

    @Test
    fun `reads and writes follow the PICOBOOT handshake`() {
        val board = FakeBoard()
        FirmwareFlasher(Picoboot(board)).flash(image)
        assertEquals(listOf(0x01, 0x06), board.log.take(2).map { it.first })
        assertArrayEquals(pages.getValue(Uf2Image.FLASH_BASE), board.flash.copyOfRange(0, 256))
    }
}
