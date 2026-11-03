package com.kangminsang.hagoondori.core.firmware

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/** UF2 파일이 장치용 펌웨어가 아닐 때. [message]는 화면에 그대로 보여 줄 문장이다. */
class InvalidUf2Exception(message: String) : Exception(message)

/**
 * 장치(RP2040) 펌웨어 UF2를 256바이트 플래시 페이지로 푼 것.
 *
 * 프로그램 영역 밖(장치의 FAT 파일시스템: calendar.txt, /data)을 건드리는 페이지가 하나라도 있거나
 * 첫 섹터(boot2)가 없으면 받지 않는다. 플래시가 그 영역을 지우지 않는다는 보장은 여기서 나온다.
 */
class Uf2Image private constructor(
    /** 플래시 주소 → 256바이트, 주소 오름차순. */
    val pages: Map<Long, ByteArray>,
    val sha256: String,
    val sizeBytes: Int,
) {
    companion object {
        const val FLASH_BASE = 0x10000000L

        /** E-calendar `mpconfigboard.cmake`: 2MB 플래시 중 위쪽 1408KB가 파일시스템이다. */
        const val PROGRAM_END = FLASH_BASE + 2 * 1024 * 1024 - 1408 * 1024

        const val PAGE_SIZE = 256
        private const val BLOCK_SIZE = 512
        private const val MAGIC_START0 = 0x0A324655L
        private const val MAGIC_START1 = 0x9E5D5157L
        private const val MAGIC_END = 0x0AB16F30L
        private const val FLAG_NOT_MAIN_FLASH = 0x1L
        private const val FLAG_FAMILY_ID = 0x2000L
        private const val RP2040_FAMILY_ID = 0xE48BFF56L

        fun parse(bytes: ByteArray): Uf2Image {
            if (bytes.isEmpty() || bytes.size % BLOCK_SIZE != 0) {
                throw InvalidUf2Exception("UF2 파일이 아닙니다 (크기 ${bytes.size}바이트)")
            }
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            fun u32(offset: Int) = buf.getInt(offset).toLong() and 0xFFFFFFFFL

            val pages = sortedMapOf<Long, ByteArray>()
            for (block in 0 until bytes.size / BLOCK_SIZE) {
                val off = block * BLOCK_SIZE
                if (u32(off) != MAGIC_START0 || u32(off + 4) != MAGIC_START1 || u32(off + 508) != MAGIC_END) {
                    throw InvalidUf2Exception("UF2 파일이 손상됐습니다 (${block}번째 블록)")
                }
                val flags = u32(off + 8)
                if (flags and FLAG_NOT_MAIN_FLASH != 0L) continue
                if (flags and FLAG_FAMILY_ID != 0L && u32(off + 28) != RP2040_FAMILY_ID) {
                    throw InvalidUf2Exception("RP2040용 펌웨어가 아닙니다")
                }
                val addr = u32(off + 12)
                val size = u32(off + 16)
                if (size != PAGE_SIZE.toLong() || addr % PAGE_SIZE != 0L) {
                    throw InvalidUf2Exception("지원하지 않는 UF2 형식입니다 (${block}번째 블록)")
                }
                if (addr < FLASH_BASE || addr >= PROGRAM_END) {
                    throw InvalidUf2Exception(
                        "장치 데이터 영역을 덮어쓰는 파일이라 거부했습니다 (주소 0x${addr.toString(16)})",
                    )
                }
                if (pages.put(addr, bytes.copyOfRange(off + 32, off + 32 + PAGE_SIZE)) != null) {
                    throw InvalidUf2Exception("같은 주소가 두 번 들어 있는 UF2입니다")
                }
            }
            if (FLASH_BASE !in pages) {
                throw InvalidUf2Exception("전체 펌웨어가 아닙니다 (부트 섹터가 없음)")
            }
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return Uf2Image(pages, digest.joinToString("") { "%02x".format(it) }, bytes.size)
        }
    }
}
