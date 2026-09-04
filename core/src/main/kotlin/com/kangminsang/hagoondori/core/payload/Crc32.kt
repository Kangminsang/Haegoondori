package com.kangminsang.hagoondori.core.payload

import java.util.zip.CRC32

/**
 * 페이로드 무결성 검증용 CRC32 (스펙 7.3절). `java.util.zip.CRC32`는 순수 JDK API로,
 * Android나 특정 플랫폼에 종속되지 않으므로 core 모듈에서 그대로 사용할 수 있다.
 */
object Crc32 {
    /** 대문자 16진수 8자리로 포맷된 CRC32 값. 예: `"3F2A9C11"`. */
    fun compute(bytes: ByteArray): String {
        val crc = CRC32()
        crc.update(bytes)
        return "%08X".format(crc.value)
    }
}
