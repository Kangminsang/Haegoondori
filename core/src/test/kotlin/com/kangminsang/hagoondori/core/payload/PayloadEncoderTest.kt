package com.kangminsang.hagoondori.core.payload

import com.kangminsang.hagoondori.core.export.CalendarSnapshot
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.UserProfile
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PayloadEncoderTest {

    private val rangeStart = LocalDate(2026, 8, 28)
    private val rangeEnd = LocalDate(2027, 3, 3)

    // 스펙 7.2절 예시와 동일한 시각: UTC 12:14 == KST(UTC+9) 21:14
    private val generatedAt = Instant.parse("2026-09-04T12:14:00Z")

    private fun baseProfile(promotionDate: LocalDate? = null) = UserProfile(
        enlistmentDate = LocalDate(2025, 3, 3),
        dischargeDate = LocalDate(2026, 9, 2),
        promotionDate = promotionDate,
        firstOvernightDate = null,
        wakeUpTime = LocalTime(5, 45),
        dinnerTime = LocalTime(17, 30),
        autoAddOffDuty = true,
    )

    private fun minimalSnapshot(profile: UserProfile = baseProfile()) = CalendarSnapshot(
        generatedAt = generatedAt,
        rangeStart = rangeStart,
        rangeEnd = rangeEnd,
        profile = profile,
        nextOvernightDate = null,
        holidays = emptyList(),
        events = emptyList(),
        leaveUsages = emptyList(),
        combatRestUsages = emptyList(),
        dutyAssignments = emptyList(),
    )

    // -----------------------------------------------------------------
    // 최소 데이터셋: V/M/P만
    // -----------------------------------------------------------------

    @Test
    fun `minimal snapshot encodes V, M, P lines only, then Z`() {
        val payload = PayloadEncoder.encode(minimalSnapshot())
        val lines = payload.trimEnd('\n').split("\n")

        assertEquals("V|1", lines[0])
        assertEquals("M|2026-09-04T21:14:00+09:00|2026-08-28|2027-03-03", lines[1])
        assertEquals("P|2025-03-03|2026-09-02||05:45|17:30", lines[2]) // promotionDate 없음 -> 빈 문자열
        assertTrue(lines[3].startsWith("Z|"))
        assertEquals(4, lines.size)
    }

    @Test
    fun `promotionDate present is encoded as a plain date field`() {
        val payload = PayloadEncoder.encode(minimalSnapshot(baseProfile(promotionDate = LocalDate(2026, 10, 16))))
        val profileLine = payload.split("\n").first { it.startsWith("P|") }
        assertEquals("P|2025-03-03|2026-09-02|2026-10-16|05:45|17:30", profileLine)
    }

    @Test
    fun `nextOvernightDate null omits the O line entirely`() {
        val payload = PayloadEncoder.encode(minimalSnapshot())
        assertFalse(payload.lines().any { it.startsWith("O|") })
    }

    // -----------------------------------------------------------------
    // 전체 필드 (스펙 7.2절 예시와 동일한 데이터)
    // -----------------------------------------------------------------

    @Test
    fun `full snapshot matches the spec section 7_2 example line by line`() {
        val snapshot = CalendarSnapshot(
            generatedAt = generatedAt,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            profile = baseProfile(promotionDate = LocalDate(2026, 10, 16)),
            nextOvernightDate = LocalDate(2026, 9, 12),
            holidays = listOf(
                Holiday(LocalDate(2026, 9, 25), "추석"),
                Holiday(LocalDate(2026, 10, 3), "개천절"),
            ),
            events = listOf(
                Event("e1", "면회", LocalDate(2026, 9, 12), isImportant = true),
                Event("e2", "진급 심사", LocalDate(2026, 9, 18), isImportant = false),
            ),
            leaveUsages = listOf(
                LeaveUsage("l1", "annual", LocalDate(2026, 8, 30), LocalDate(2026, 9, 2), "정기휴가"),
            ),
            combatRestUsages = listOf(
                CombatRestUsage("c1", LocalDate(2026, 9, 20), LocalDate(2026, 9, 20)),
            ),
            dutyAssignments = listOf(
                DutyAssignment("d1", LocalDate(2026, 9, 5), DutyType.DUTY),
                DutyAssignment("d2", LocalDate(2026, 9, 6), DutyType.OFF_DUTY),
                DutyAssignment("d3", LocalDate(2026, 9, 9), DutyType.MESS),
            ),
        )

        val payload = PayloadEncoder.encode(snapshot)
        val lines = payload.trimEnd('\n').split("\n")

        val expectedBodyLines = listOf(
            "V|1",
            "M|2026-09-04T21:14:00+09:00|2026-08-28|2027-03-03",
            "P|2025-03-03|2026-09-02|2026-10-16|05:45|17:30",
            "O|2026-09-12",
            "H|2026-09-25|추석",
            "H|2026-10-03|개천절",
            "E|2026-09-12|2026-09-12|면회|1",
            "E|2026-09-18|2026-09-18|진급 심사|0",
            "L|2026-08-30|2026-09-02|정기휴가",
            "C|2026-09-20|2026-09-20",
            "D|2026-09-05|DUTY",
            "D|2026-09-06|OFF_DUTY",
            "D|2026-09-09|MESS",
        )
        assertEquals(expectedBodyLines, lines.dropLast(1))
        assertTrue(lines.last().startsWith("Z|"))
    }

    @Test
    fun `single-day event fills endDate with startDate`() {
        val snapshot = minimalSnapshot().copy(
            events = listOf(Event("e1", "면회", LocalDate(2026, 9, 12))),
        )
        val eventLine = PayloadEncoder.encode(snapshot).lines().first { it.startsWith("E|") }
        assertEquals("E|2026-09-12|2026-09-12|면회|0", eventLine)
    }

    // -----------------------------------------------------------------
    // 전송 범위 필터링 (스펙 7.4절)
    // -----------------------------------------------------------------

    @Test
    fun `records entirely outside the transfer range are excluded`() {
        val snapshot = minimalSnapshot().copy(
            holidays = listOf(
                Holiday(rangeStart.minusDays(1), "범위 밖(하루 전)"),
                Holiday(rangeStart, "범위 안(시작 경계)"),
                Holiday(rangeEnd, "범위 안(끝 경계)"),
                Holiday(rangeEnd.plusDays(1), "범위 밖(하루 후)"),
            ),
        )
        val holidayNames = PayloadEncoder.encode(snapshot).lines()
            .filter { it.startsWith("H|") }
            .map { it.substringAfterLast("|") }

        assertEquals(listOf("범위 안(시작 경계)", "범위 안(끝 경계)"), holidayNames)
    }

    @Test
    fun `a leave usage overlapping the range boundary is included even if it starts before the range`() {
        val snapshot = minimalSnapshot().copy(
            leaveUsages = listOf(
                LeaveUsage("l1", "annual", rangeStart.minusDays(3), rangeStart.plusDays(1), "연가"),
            ),
        )
        assertTrue(PayloadEncoder.encode(snapshot).lines().any { it.startsWith("L|") })
    }

    // -----------------------------------------------------------------
    // 무결성 (Z 줄)
    // -----------------------------------------------------------------

    @Test
    fun `Z line byte count and CRC32 match the body that precedes it, and only LF is used`() {
        val payload = PayloadEncoder.encode(minimalSnapshot())
        assertFalse(payload.contains("\r\n"))
        assertFalse(payload.contains("\r"))

        val zLineStart = payload.lastIndexOf("Z|")
        val body = payload.substring(0, zLineStart)
        val bodyBytes = body.toByteArray(Charsets.UTF_8)

        val zLine = payload.substring(zLineStart).trimEnd('\n')
        val (_, byteCountStr, crc) = zLine.split("|")

        assertEquals(bodyBytes.size, byteCountStr.toInt())
        assertEquals(Crc32.compute(bodyBytes), crc)
    }

    @Test
    fun `Z line byte count reflects UTF-8 multi-byte length of Korean text, not char count`() {
        val snapshot = minimalSnapshot().copy(
            holidays = listOf(Holiday(LocalDate(2026, 9, 25), "추석")),
        )
        val payload = PayloadEncoder.encode(snapshot)
        val zLineStart = payload.lastIndexOf("Z|")
        val body = payload.substring(0, zLineStart)
        val declaredByteCount = payload.substring(zLineStart).trimEnd('\n').split("|")[1].toInt()

        assertEquals(body.toByteArray(Charsets.UTF_8).size, declaredByteCount)
        // "추석"은 한글 2글자, UTF-8로는 6바이트 - char 길이와 byte 길이가 다르다는 것을 재확인
        assertTrue(body.toByteArray(Charsets.UTF_8).size > body.length)
    }

    // -----------------------------------------------------------------
    // 방어적 검증
    // -----------------------------------------------------------------

    @Test
    fun `a pipe character in a free-text field is rejected defensively even if the model itself allowed it`() {
        // Holiday.name은 모델 생성자 수준에서 '|'를 막지 않으므로, 인코더가 방어선이 되어야 한다.
        val snapshot = minimalSnapshot().copy(
            holidays = listOf(Holiday(LocalDate(2026, 9, 25), "추석|사고")),
        )
        assertThrows(IllegalArgumentException::class.java) { PayloadEncoder.encode(snapshot) }
    }
}

private fun LocalDate.minusDays(days: Int): LocalDate =
    kotlinx.datetime.LocalDate.fromEpochDays(this.toEpochDays() - days)

private fun LocalDate.plusDays(days: Int): LocalDate =
    kotlinx.datetime.LocalDate.fromEpochDays(this.toEpochDays() + days)
