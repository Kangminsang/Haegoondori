package com.kangminsang.hagoondori.core.payload

import com.kangminsang.hagoondori.core.export.CalendarSnapshot
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.DutyType
import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.core.model.PromotionDates
import com.kangminsang.hagoondori.core.model.UserProfile
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PayloadDiffTest {

    private val rangeStart = LocalDate(2026, 8, 28)
    private val rangeEnd = LocalDate(2027, 3, 3)
    private val generatedAt = Instant.parse("2026-09-04T12:14:00Z")
    private val later = Instant.parse("2026-09-05T03:00:00Z")

    private val profile = UserProfile(
        enlistmentDate = LocalDate(2025, 3, 3),
        dischargeDate = LocalDate(2026, 9, 2),
        promotionDates = PromotionDates(),
        firstOvernightDate = null,
        wakeUpTime = LocalTime(5, 45),
        dinnerTime = LocalTime(17, 30),
        autoAddOffDuty = true,
    )

    private fun snapshot(
        at: Instant = generatedAt,
        events: List<Event> = emptyList(),
        duties: List<DutyAssignment> = emptyList(),
    ) = CalendarSnapshot(
        generatedAt = at,
        rangeStart = rangeStart,
        rangeEnd = rangeEnd,
        profile = profile,
        nextOvernightDate = null,
        holidays = emptyList(),
        events = events,
        leaveUsages = emptyList(),
        combatRestUsages = emptyList(),
        dutyAssignments = duties,
    )

    /** V/M/Z 줄을 포함한 페이로드 본문을 흉내 낸다. 비교에서는 V/M/Z가 무시돼야 한다. */
    private fun body(vararg lines: String): String =
        (listOf("V|1", "M|2026-09-04T21:14:00+09:00|2026-08-28|2027-03-03") + lines + "Z|0|00000000")
            .joinToString("\n", postfix = "\n")

    private val defaultP = "P|2025-03-03|2026-09-02||05:45|17:30"

    // -----------------------------------------------------------------
    // 변경 없음
    // -----------------------------------------------------------------

    @Test
    fun `payloads differing only in M and Z lines produce no changes`() {
        val before = PayloadEncoder.encode(snapshot(at = generatedAt))
        val after = PayloadEncoder.encode(snapshot(at = later))
        assertEquals(emptyList<String>(), PayloadDiff.describe(before, after))
    }

    @Test
    fun `add then remove the same duty leaves no changes against the original`() {
        val original = body(defaultP)
        val withDuty = body(defaultP, "D|2026-11-17|DUTY")
        val removedAgain = body(defaultP)

        // 추가 후 보낸 상태와 비교하면 삭제 한 건으로 보이지만, 처음 보낸 상태와 비교하면 변경이 없다.
        assertEquals(listOf("근무 삭제: 11-17 당직"), PayloadDiff.describe(withDuty, removedAgain))
        assertEquals(emptyList<String>(), PayloadDiff.describe(original, removedAgain))

        val encodedOriginal = PayloadEncoder.encode(snapshot())
        val encodedRemovedAgain = PayloadEncoder.encode(snapshot(at = later, duties = emptyList()))
        assertEquals(emptyList<String>(), PayloadDiff.describe(encodedOriginal, encodedRemovedAgain))
    }

    // -----------------------------------------------------------------
    // 근무
    // -----------------------------------------------------------------

    @Test
    fun `duty added is described as 근무 추가`() {
        val before = body(defaultP)
        val after = body(defaultP, "D|2026-11-17|DUTY")
        assertEquals(listOf("근무 추가: 11-17 당직"), PayloadDiff.describe(before, after))
    }

    @Test
    fun `duty removed is described as 근무 삭제`() {
        val before = body(defaultP, "D|2026-11-18|OFF_DUTY")
        val after = body(defaultP)
        assertEquals(listOf("근무 삭제: 11-18 비번"), PayloadDiff.describe(before, after))
    }

    @Test
    fun `encoder output differing by one duty yields exactly one 근무 추가 line`() {
        val before = PayloadEncoder.encode(snapshot())
        val after = PayloadEncoder.encode(
            snapshot(duties = listOf(DutyAssignment("d1", LocalDate(2026, 11, 17), DutyType.DUTY))),
        )
        assertEquals(listOf("근무 추가: 11-17 당직"), PayloadDiff.describe(before, after))
    }

    // -----------------------------------------------------------------
    // 일정
    // -----------------------------------------------------------------

    @Test
    fun `event title change is described as 일정 수정 with the old and new title`() {
        val before = body(defaultP, "E|2026-11-12|2026-11-12|회식|0")
        val after = body(defaultP, "E|2026-11-12|2026-11-12|회식 자리|0")
        assertEquals(listOf("일정 수정: 회식 자리 (회식 → 회식 자리)"), PayloadDiff.describe(before, after))
    }

    @Test
    fun `event date change shows the old and new period`() {
        val before = body(defaultP, "E|2026-11-12|2026-11-12|면회|1")
        val after = body(defaultP, "E|2026-11-13|2026-11-14|면회|1")
        assertEquals(listOf("일정 수정: 면회 (11-12 → 11-13~11-14)"), PayloadDiff.describe(before, after))
    }

    @Test
    fun `importance toggle is described both ways`() {
        val off = body(defaultP, "E|2026-11-12|2026-11-12|면회|0")
        val on = body(defaultP, "E|2026-11-12|2026-11-12|면회|1")
        assertEquals(listOf("일정 수정: 면회 (중요 표시 켬)"), PayloadDiff.describe(off, on))
        assertEquals(listOf("일정 수정: 면회 (중요 표시 끔)"), PayloadDiff.describe(on, off))
    }

    @Test
    fun `event added and removed without pairing`() {
        val before = body(defaultP, "E|2026-11-12|2026-11-12|면회|0")
        val after = body(defaultP, "E|2026-11-20|2026-11-20|진급 심사|0")
        assertEquals(
            listOf("일정 삭제: 면회 (11-12)", "일정 추가: 진급 심사 (11-20)"),
            PayloadDiff.describe(before, after),
        )
    }

    // -----------------------------------------------------------------
    // 휴가 / 외박
    // -----------------------------------------------------------------

    @Test
    fun `leave label change from 외박 to 포상 is a removal plus an addition`() {
        val before = body(defaultP, "L|2026-11-15|2026-11-16|외박")
        val after = body(defaultP, "L|2026-11-15|2026-11-16|포상")
        assertEquals(
            listOf("외박 삭제: 11-15~11-16", "휴가 추가: 포상 11-15~11-16"),
            PayloadDiff.describe(before, after),
        )
    }

    @Test
    fun `leave period change keeps the label prefix`() {
        val before = body(defaultP, "L|2026-12-01|2026-12-04|포상")
        val after = body(defaultP, "L|2026-12-01|2026-12-05|포상")
        assertEquals(listOf("휴가 수정: 포상 12-01~12-04 → 12-01~12-05"), PayloadDiff.describe(before, after))
    }

    @Test
    fun `overnight period change is paired by start date`() {
        val before = body(defaultP, "L|2026-11-15|2026-11-16|외박")
        val after = body(defaultP, "L|2026-11-15|2026-11-17|외박")
        assertEquals(listOf("외박 수정: 11-15~11-16 → 11-15~11-17"), PayloadDiff.describe(before, after))
    }

    @Test
    fun `next overnight date change is paired`() {
        val before = body(defaultP, "O|2026-11-15")
        val after = body(defaultP, "O|2026-11-22")
        assertEquals(listOf("다음 외박 예정일 변경: 11-15 → 11-22"), PayloadDiff.describe(before, after))
    }

    @Test
    fun `next overnight date added and removed`() {
        val none = body(defaultP)
        val withO = body(defaultP, "O|2026-11-22")
        assertEquals(listOf("다음 외박 예정일 추가: 11-22"), PayloadDiff.describe(none, withO))
        assertEquals(listOf("다음 외박 예정일 없어짐 (11-22)"), PayloadDiff.describe(withO, none))
    }

    // -----------------------------------------------------------------
    // 전투휴무 / 외출
    // -----------------------------------------------------------------

    @Test
    fun `combat rest added removed and changed`() {
        val none = body(defaultP)
        val added = body(defaultP, "C|2026-12-10|2026-12-11")
        assertEquals(listOf("전투휴무 추가: 12-10~12-11"), PayloadDiff.describe(none, added))
        assertEquals(listOf("전투휴무 삭제: 12-10~12-11"), PayloadDiff.describe(added, none))

        val changed = body(defaultP, "C|2026-12-10|2026-12-12")
        assertEquals(listOf("전투휴무 수정: 12-10~12-11 → 12-10~12-12"), PayloadDiff.describe(added, changed))
    }

    @Test
    fun `outing added and removed`() {
        val none = body(defaultP)
        val withG = body(defaultP, "G|2026-11-24")
        assertEquals(listOf("외출 추가: 11-24"), PayloadDiff.describe(none, withG))
        assertEquals(listOf("외출 삭제: 11-24"), PayloadDiff.describe(withG, none))
    }

    // -----------------------------------------------------------------
    // 복무 정보 (P)
    // -----------------------------------------------------------------

    @Test
    fun `profile field changes are described one line per field`() {
        val before = body("P|2025-03-03|2026-09-02|2026-10-16|05:45|17:30")
        val after = body("P|2025-03-09|2026-09-02||06:00|17:30")
        assertEquals(
            listOf(
                "입대일 변경: 03-03 → 03-09",
                "진급일 변경: 10-16 → 없음",
                "기상 시간 변경: 05:45 → 06:00",
            ),
            PayloadDiff.describe(before, after),
        )
    }

    @Test
    fun `promotion date appearing from an empty field shows 없음 as the old value`() {
        val before = body("P|2025-03-03|2026-09-02||05:45|17:30")
        val after = body("P|2025-03-03|2026-09-02|2026-10-16|05:45|17:30")
        assertEquals(listOf("진급일 변경: 없음 → 10-16"), PayloadDiff.describe(before, after))
    }

    @Test
    fun `malformed profile line is reported as unknown and the valid side as 복무 정보 변경`() {
        val before = body("P|2025-03-03|2026-09-02||05:45|17:30")
        val after = body("P|2025-03-03|2026-09-02||05:45")
        assertEquals(
            listOf("복무 정보 변경", "기타 변경: P|2025-03-03|2026-09-02||05:45"),
            PayloadDiff.describe(before, after),
        )
    }

    // -----------------------------------------------------------------
    // 공휴일 / 알 수 없는 종류
    // -----------------------------------------------------------------

    @Test
    fun `holiday rename is paired by date`() {
        val before = body(defaultP, "H|2026-09-25|추석")
        val after = body(defaultP, "H|2026-09-25|추석 연휴")
        assertEquals(listOf("공휴일 변경: 09-25 추석 → 추석 연휴"), PayloadDiff.describe(before, after))
    }

    @Test
    fun `unknown record kind is reported as 기타`() {
        val before = body(defaultP)
        val after = body(defaultP, "X|foo")
        assertEquals(listOf("기타 변경: X|foo"), PayloadDiff.describe(before, after))
        assertEquals(listOf("기타 삭제: X|foo"), PayloadDiff.describe(after, before))
    }

    // -----------------------------------------------------------------
    // parseMeta
    // -----------------------------------------------------------------

    @Test
    fun `parseMeta reads the M line of an encoded payload`() {
        val payload = PayloadEncoder.encode(snapshot())
        val meta = PayloadDiff.parseMeta(payload)
        assertEquals(
            PayloadMeta(generatedAt = generatedAt, rangeStart = rangeStart, rangeEnd = rangeEnd),
            meta,
        )
    }

    @Test
    fun `parseMeta returns null for garbage`() {
        assertNull(PayloadDiff.parseMeta(""))
        assertNull(PayloadDiff.parseMeta("hello\nworld\n"))
        assertNull(PayloadDiff.parseMeta("M|bad|2026-08-28|2027-03-03\n"))
        assertNull(PayloadDiff.parseMeta("M|2026-09-04T21:14:00+09:00|2026-08-28\n"))
    }
}
