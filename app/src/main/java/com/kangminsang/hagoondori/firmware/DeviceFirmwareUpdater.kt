package com.kangminsang.hagoondori.firmware

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import com.kangminsang.hagoondori.core.firmware.FirmwareFlasher
import com.kangminsang.hagoondori.core.firmware.Picoboot
import com.kangminsang.hagoondori.core.firmware.PicobootException
import com.kangminsang.hagoondori.core.firmware.PicobootTransport
import com.kangminsang.hagoondori.core.firmware.Uf2Image
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** 화면에 그대로 보여 줄 실패 사유. */
class FirmwareUpdateException(message: String) : IOException(message)

/**
 * 휴대폰에 USB로 연결된 장치(E-calendar, RP2040)의 펌웨어를 바꾼다.
 *
 * 1. 장치가 평소 상태(MicroPython)면 USB 시리얼(REPL)로 `machine.bootloader()`를 보내 BOOTSEL로 전환한다.
 * 2. BOOTSEL 장치의 PICOBOOT 인터페이스로 [FirmwareFlasher]가 쓰고 검증한 뒤 재부팅한다.
 *
 * Android는 BOOTSEL 드라이브(RPI-RP2)를 마운트하지 못하므로 UF2 파일 복사 대신 PICOBOOT를 쓴다
 * (picotool과 같은 경로). 장치의 일정 데이터가 있는 파일시스템 영역은 [Uf2Image]가 거부한다.
 */
@Singleton
class DeviceFirmwareUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    sealed interface Step {
        data object FindingDevice : Step
        data object RequestingPermission : Step
        data object EnteringBootloader : Step
        data object WaitingForBootloader : Step
        data class Flashing(val progress: FirmwareFlasher.Progress) : Step
        data object Rebooting : Step
    }

    private val usb get() = context.getSystemService(UsbManager::class.java)

    /** 디버그 앱(.debug)과 릴리즈 앱이 함께 설치돼도 서로의 권한 응답을 받지 않도록 패키지 이름을 붙인다. */
    private val permissionAction get() = "${context.packageName}.USB_PERMISSION"

    /** @return 다시 쓴 섹터 수(0이면 이미 같은 펌웨어였다). */
    suspend fun update(image: Uf2Image, onStep: (Step) -> Unit): Int = withContext(Dispatchers.IO) {
        onStep(Step.FindingDevice)
        var device = findDevice() ?: throw FirmwareUpdateException("장치를 찾지 못했습니다. USB 케이블로 연결돼 있는지 확인해 주세요")
        if (device.productId != PID_BOOTSEL) {
            onStep(Step.RequestingPermission)
            requirePermission(device)
            onStep(Step.EnteringBootloader)
            enterBootloader(device)
            onStep(Step.WaitingForBootloader)
            device = waitForBootsel()
        }
        onStep(Step.RequestingPermission)
        requirePermission(device)
        val connection = usb.openDevice(device) ?: throw FirmwareUpdateException("장치를 열지 못했습니다")
        try {
            val flasher = FirmwareFlasher(Picoboot(openPicoboot(device, connection)))
            val changed = try {
                flasher.flash(image) { onStep(Step.Flashing(it)) }
            } catch (e: PicobootException) {
                throw FirmwareUpdateException(e.message ?: "플래시 실패")
            }
            onStep(Step.Rebooting)
            flasher.reboot()
            changed
        } finally {
            connection.close()
        }
    }

    private fun findDevice(): UsbDevice? =
        usb.deviceList.values.filter { it.vendorId == VID_RASPBERRY_PI }
            .minByOrNull { if (it.productId == PID_BOOTSEL) 0 else 1 }

    private suspend fun waitForBootsel(): UsbDevice {
        repeat(BOOTSEL_WAIT_MS / POLL_MS) {
            usb.deviceList.values.firstOrNull { it.vendorId == VID_RASPBERRY_PI && it.productId == PID_BOOTSEL }
                ?.let { return it }
            delay(POLL_MS.toLong())
        }
        throw FirmwareUpdateException("장치가 업데이트 모드로 바뀌지 않았습니다. 장치를 다시 연결한 뒤 시도해 주세요")
    }

    private suspend fun requirePermission(device: UsbDevice) {
        if (usb.hasPermission(device)) return
        suspendCancellableCoroutine { cont ->
            val done = AtomicBoolean(false)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (done.compareAndSet(false, true)) {
                        context.unregisterReceiver(this)
                        cont.resume(Unit)
                    }
                }
            }
            ContextCompat.registerReceiver(context, receiver, IntentFilter(permissionAction), ContextCompat.RECEIVER_NOT_EXPORTED)
            cont.invokeOnCancellation { if (done.compareAndSet(false, true)) context.unregisterReceiver(receiver) }
            val intent = Intent(permissionAction).setPackage(context.packageName)
            usb.requestPermission(
                device,
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        }
        // 일부 기기는 허용해도 EXTRA_PERMISSION_GRANTED를 빠뜨리므로 결과는 다시 묻는다.
        if (!usb.hasPermission(device)) throw FirmwareUpdateException("USB 접근이 허용되지 않았습니다")
    }

    /**
     * REPL에 Ctrl-C를 보내 캘린더 루프를 멈추고 `>>>`가 보이면 BOOTSEL로 재부팅시킨다. 화면 갱신(약 20초) 중이면
     * 프롬프트가 늦게 나오므로 Ctrl-C를 되풀이한다.
     */
    private fun enterBootloader(device: UsbDevice) {
        val connection = usb.openDevice(device) ?: throw FirmwareUpdateException("장치를 열지 못했습니다")
        try {
            val data = device.interfaces().firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA }
                ?: throw FirmwareUpdateException("장치의 USB 시리얼을 찾지 못했습니다")
            if (!connection.claimInterface(data, true)) {
                throw FirmwareUpdateException("USB 시리얼을 다른 앱이 쓰고 있습니다. 시리얼 터미널 앱을 닫아 주세요")
            }
            device.interfaces().firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_COMM }?.let { comm ->
                connection.claimInterface(comm, true)
                connection.controlTransfer(0x21, SET_CONTROL_LINE_STATE, 0x03, comm.id, null, 0, TIMEOUT_MS)  // DTR, RTS
            }
            val inEp = data.bulk(UsbConstants.USB_DIR_IN)
            val outEp = data.bulk(UsbConstants.USB_DIR_OUT)
            val seen = StringBuilder()
            val buf = ByteArray(inEp.maxPacketSize)
            val deadline = System.currentTimeMillis() + PROMPT_WAIT_MS
            while (">>> " !in seen) {
                if (System.currentTimeMillis() > deadline) {
                    throw FirmwareUpdateException("장치가 응답하지 않습니다. 장치를 다시 연결한 뒤 시도해 주세요")
                }
                seen.clear()
                connection.write(outEp, byteArrayOf(3, 3))
                val until = System.currentTimeMillis() + 1000
                while (System.currentTimeMillis() < until && ">>> " !in seen) {
                    val n = connection.bulkTransfer(inEp, buf, buf.size, 200)
                    if (n > 0) seen.append(String(buf, 0, n, Charsets.UTF_8))
                }
            }
            connection.write(outEp, "import machine; machine.bootloader()\r".toByteArray())
        } finally {
            connection.close()
        }
    }

    private fun openPicoboot(device: UsbDevice, connection: UsbDeviceConnection): PicobootTransport {
        val iface = device.interfaces().firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC }
            ?: throw FirmwareUpdateException("업데이트 모드 장치에서 PICOBOOT를 찾지 못했습니다")
        if (!connection.claimInterface(iface, true)) throw FirmwareUpdateException("장치를 열지 못했습니다")
        // PICOBOOT_IF_RESET: 이전에 끊긴 명령이 남아 있어도 처음 상태로 되돌린다.
        connection.controlTransfer(0x41, PICOBOOT_IF_RESET, 0, iface.id, null, 0, TIMEOUT_MS)
        return UsbPicobootTransport(connection, iface.bulk(UsbConstants.USB_DIR_IN), iface.bulk(UsbConstants.USB_DIR_OUT))
    }

    private fun UsbDeviceConnection.write(ep: UsbEndpoint, data: ByteArray) {
        if (bulkTransfer(ep, data, data.size, TIMEOUT_MS) != data.size) {
            throw FirmwareUpdateException("장치에 명령을 보내지 못했습니다")
        }
    }

    private class UsbPicobootTransport(
        private val connection: UsbDeviceConnection,
        private val inEp: UsbEndpoint,
        private val outEp: UsbEndpoint,
    ) : PicobootTransport {
        override fun bulkOut(data: ByteArray) {
            if (connection.bulkTransfer(outEp, data, data.size, TIMEOUT_MS) != data.size) {
                throw PicobootException("장치와 통신이 끊겼습니다 (쓰기)")
            }
        }

        override fun bulkIn(length: Int): ByteArray {
            // ack(길이 0)도 picotool처럼 1바이트 버퍼로 받는다: 장치가 보내는 빈 패킷이면 0이 돌아온다.
            val buf = ByteArray(maxOf(length, 1))
            val n = connection.bulkTransfer(inEp, buf, buf.size, TIMEOUT_MS)
            if (n < 0 || n > length) throw PicobootException("장치와 통신이 끊겼습니다 (읽기)")
            return buf.copyOf(n)
        }
    }

    private companion object {
        const val VID_RASPBERRY_PI = 0x2E8A
        const val PID_BOOTSEL = 0x0003
        const val SET_CONTROL_LINE_STATE = 0x22
        const val PICOBOOT_IF_RESET = 0x41
        const val TIMEOUT_MS = 5000
        const val PROMPT_WAIT_MS = 90_000  // 새 데이터·저녁 갱신이 이어지면 화면 갱신이 여러 번 연달아 온다
        const val BOOTSEL_WAIT_MS = 20_000
        const val POLL_MS = 250

        fun UsbDevice.interfaces(): List<UsbInterface> = (0 until interfaceCount).map(::getInterface)

        fun UsbInterface.bulk(direction: Int): UsbEndpoint =
            (0 until endpointCount).map(::getEndpoint)
                .firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == direction }
                ?: throw FirmwareUpdateException("장치의 USB 구성이 예상과 다릅니다")
    }
}
