package com.kangminsang.hagoondori.core.firmware

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * BOOTSEL 상태 RP2040의 PICOBOOT 인터페이스(벤더 클래스)의 벌크 엔드포인트 한 쌍.
 * 길이 0을 주고받는 것은 명령 완료 확인(ack)이다. 실패하면 [IOException]을 던진다.
 */
interface PicobootTransport {
    fun bulkOut(data: ByteArray)

    /** 최대 [length]바이트를 받는다. 받은 만큼만 돌려준다. */
    fun bulkIn(length: Int): ByteArray
}

class PicobootException(message: String) : IOException(message)

/**
 * RP2040 부트롬의 PICOBOOT 명령(RP2040 데이터시트 2.8.5, picotool과 같은 순서).
 * 32바이트 명령 → (데이터) → 반대 방향 길이 0 ack.
 */
class Picoboot(private val transport: PicobootTransport) {
    private var token = 0

    fun exclusiveAccess(exclusive: Boolean) = command(CMD_EXCLUSIVE_ACCESS, byteArrayOf(if (exclusive) 1 else 0))

    /** 플래시 읽기·지우기·쓰기 전에 필요하다. XIP 상태로 읽으면 부트롬 작업 처리기가 멈춘다. */
    fun exitXip() = command(CMD_EXIT_XIP)

    fun read(address: Long, size: Int): ByteArray = command(CMD_READ, range(address, size), size)

    fun erase(address: Long, size: Int) = command(CMD_FLASH_ERASE, range(address, size))

    fun write(address: Long, data: ByteArray) = command(CMD_WRITE, range(address, data.size), data.size, data)

    /** [delayMs] 뒤 플래시의 프로그램으로 재부팅한다(pc 0 = 일반 부팅). */
    fun reboot(delayMs: Int) {
        val args = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0).putInt(STACK_TOP).putInt(delayMs).array()
        command(CMD_REBOOT, args)
    }

    private fun range(address: Long, size: Int): ByteArray =
        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(address.toInt()).putInt(size).array()

    private fun command(id: Int, args: ByteArray = ByteArray(0), length: Int = 0, payload: ByteArray? = null): ByteArray {
        token++
        val packet = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(MAGIC).putInt(token).put(id.toByte()).put(args.size.toByte()).putShort(0).putInt(length)
            .put(args).array()
        transport.bulkOut(packet)
        if (id and 0x80 != 0) {
            val data = transport.bulkIn(length)
            if (data.size != length) throw PicobootException("읽기가 중간에 끊겼습니다 (${data.size}/$length)")
            transport.bulkOut(ByteArray(0))
            return data
        }
        payload?.let { transport.bulkOut(it) }
        transport.bulkIn(0)
        return ByteArray(0)
    }

    private companion object {
        const val MAGIC = 0x431FD10B
        const val CMD_EXCLUSIVE_ACCESS = 0x01
        const val CMD_REBOOT = 0x02
        const val CMD_FLASH_ERASE = 0x03
        const val CMD_READ = 0x84
        const val CMD_WRITE = 0x05
        const val CMD_EXIT_XIP = 0x06
        const val STACK_TOP = 0x20042000
    }
}
