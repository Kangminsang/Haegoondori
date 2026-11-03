package com.kangminsang.hagoondori.core.firmware

/**
 * [Uf2Image]를 장치 플래시에 쓴다. E-calendar `tools/picoboot_flash.py`(실기기 검증, 2026-11-02)와 같은 절차다.
 *
 * - 내용이 다른 4KB 섹터만 지우고 다시 쓴다. 이미 같으면 아무것도 쓰지 않는다.
 * - 첫 섹터(boot2)는 맨 먼저 지우고 맨 마지막에 쓴다. 중간에 끊기면 boot2 검사에 실패해 장치가
 *   BOOTSEL로 돌아오므로, 섞인 펌웨어로 부팅하는 일 없이 다시 시도할 수 있다.
 * - 전체를 다시 읽어 맞을 때만 재부팅한다.
 */
class FirmwareFlasher(private val boot: Picoboot) {

    enum class Phase { Comparing, Writing, Verifying }

    /** [done]/[total]은 해당 단계의 섹터 수다. */
    data class Progress(val phase: Phase, val done: Int, val total: Int)

    /** @return 다시 쓴 섹터 수(0이면 이미 같은 펌웨어였다). */
    fun flash(image: Uf2Image, onProgress: (Progress) -> Unit = {}): Int {
        val sectors = image.pages.keys.groupBy { it and (SECTOR_SIZE - 1).toLong().inv() }.toSortedMap()
        boot.exclusiveAccess(true)
        boot.exitXip()

        val changed = sectors.entries.filterIndexed { i, (_, addrs) ->
            onProgress(Progress(Phase.Comparing, i, sectors.size))
            !matches(addrs, image)
        }.map { it.key }

        changed.forEachIndexed { i, sector ->
            onProgress(Progress(Phase.Writing, i, changed.size))
            boot.erase(sector, SECTOR_SIZE)
            if (sector != Uf2Image.FLASH_BASE) writePages(sectors.getValue(sector), image)
        }
        if (Uf2Image.FLASH_BASE in changed) writePages(sectors.getValue(Uf2Image.FLASH_BASE), image)
        onProgress(Progress(Phase.Writing, changed.size, changed.size))

        sectors.values.forEachIndexed { i, addrs ->
            onProgress(Progress(Phase.Verifying, i, sectors.size))
            if (!matches(addrs, image)) {
                throw PicobootException("검증 실패 (0x${addrs.first().toString(16)}). 장치를 그대로 두고 다시 시도해 주세요")
            }
        }
        onProgress(Progress(Phase.Verifying, sectors.size, sectors.size))
        return changed.size
    }

    /** 플래시 후 장치를 새 펌웨어로 재부팅한다. 장치가 ack 전에 버스에서 사라질 수 있어 그 실패는 무시한다. */
    fun reboot() {
        runCatching { boot.reboot(REBOOT_DELAY_MS) }
    }

    private fun writePages(addrs: List<Long>, image: Uf2Image) {
        for (addr in addrs) boot.write(addr, image.pages.getValue(addr))
    }

    /** 섹터 안 UF2 페이지들이 플래시와 같은지. 한 번에 읽고 페이지별로 비교한다. */
    private fun matches(addrs: List<Long>, image: Uf2Image): Boolean {
        val first = addrs.first()
        val data = boot.read(first, (addrs.last() - first).toInt() + Uf2Image.PAGE_SIZE)
        return addrs.all { addr ->
            val off = (addr - first).toInt()
            data.copyOfRange(off, off + Uf2Image.PAGE_SIZE).contentEquals(image.pages.getValue(addr))
        }
    }

    private companion object {
        const val SECTOR_SIZE = 4096
        const val REBOOT_DELAY_MS = 500
    }
}
