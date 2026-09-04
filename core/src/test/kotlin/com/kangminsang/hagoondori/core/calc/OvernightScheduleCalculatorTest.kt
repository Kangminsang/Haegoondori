package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.OvernightForfeiture
import com.kangminsang.hagoondori.core.model.OvernightRecord
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 3.4.4절 차수 매칭 알고리즘의 정밀 테스트. 스펙이 "반드시 단위 테스트로 검증한다"고
 * 명시적으로 강조하는 부분이므로, 지연/선행/소멸이 뒤섞인 케이스까지 촘촘히 다룬다.
 */
class OvernightScheduleCalculatorTest {

    private val firstDate = LocalDate(2026, 1, 1)
    private val cycleWeeks = 6

    private fun slot(n: Int): LocalDate =
        OvernightScheduleCalculator.scheduledDate(firstDate, cycleWeeks, n)

    // ---------------------------------------------------------------------
    // scheduledDate: 격자는 기록/소멸과 무관하게 항상 고정이다 (3.4.2절 공식)
    // ---------------------------------------------------------------------

    @Test
    fun `scheduledDateFormula - grid is firstOvernightDate plus cycleWeeks times 7 times n regardless of records`() {
        assertEquals(LocalDate(2026, 1, 1), slot(0))
        assertEquals(LocalDate(2026, 2, 12), slot(1)) // +42일
        assertEquals(LocalDate(2026, 3, 26), slot(2)) // +84일
        assertEquals(LocalDate(2026, 5, 7), slot(3)) // +126일
        assertEquals(LocalDate(2026, 6, 18), slot(4)) // +168일

        // 기록이나 소멸을 아무리 섞어도 격자 자체(slot(n))는 바뀌지 않는다.
        val withRecordsAndForfeitures = OvernightScheduleCalculator.buildSchedule(
            firstOvernightDate = firstDate,
            cycleWeeks = cycleWeeks,
            records = listOf(OvernightRecord("r1", LocalDate(2026, 1, 20))),
            forfeitures = listOf(OvernightForfeiture("f1", slotIndex = 1)),
        )
        assertEquals(slot(3), OvernightScheduleCalculator.scheduledDate(firstDate, cycleWeeks, 3))
    }

    // ---------------------------------------------------------------------
    // 기본 매칭
    // ---------------------------------------------------------------------

    @Test
    fun `emptyRecords_nextSlotIsZero`() {
        val schedule = OvernightScheduleCalculator.buildSchedule(firstDate, cycleWeeks, emptyList(), emptyList())
        assertEquals(0, schedule.nextSlotIndex)
        assertEquals(firstDate, schedule.nextScheduledDate)
        assertEquals(emptyList<Any>(), schedule.matches)
    }

    @Test
    fun `noDelay_exactMatch - three records exactly on schedule`() {
        val records = listOf(
            OvernightRecord("r0", slot(0)),
            OvernightRecord("r1", slot(1)),
            OvernightRecord("r2", slot(2)),
        )
        val schedule = OvernightScheduleCalculator.buildSchedule(firstDate, cycleWeeks, records, emptyList())
        assertEquals(3, schedule.matches.size)
        schedule.matches.forEach { assertEquals(0, it.delayDays) }
        assertEquals(listOf(0, 1, 2), schedule.matches.map { it.slotIndex })
        assertEquals(3, schedule.nextSlotIndex)
        assertEquals(slot(3), schedule.nextScheduledDate)
    }

    @Test
    fun `singleDelay - one week late`() {
        val records = listOf(OvernightRecord("r0", slot(0).plusDays(7)))
        val schedule = OvernightScheduleCalculator.buildSchedule(firstDate, cycleWeeks, records, emptyList())
        assertEquals(7, schedule.matches.single().delayDays)
        assertEquals(1, schedule.nextSlotIndex)
    }

    @Test
    fun `singleEarly - three days early yields negative delayDays`() {
        val records = listOf(OvernightRecord("r0", slot(0).plusDays(-3)))
        val schedule = OvernightScheduleCalculator.buildSchedule(firstDate, cycleWeeks, records, emptyList())
        assertEquals(-3, schedule.matches.single().delayDays)
    }

    // ---------------------------------------------------------------------
    // 소멸
    // ---------------------------------------------------------------------

    @Test
    fun `singleForfeiture - slot 1 forfeited shifts effective slots to 0, 2, 3 and so on`() {
        val records = listOf(
            OvernightRecord("r0", slot(0)),
            OvernightRecord("r1", slot(2)),
        )
        val forfeitures = listOf(OvernightForfeiture("f1", slotIndex = 1))
        val schedule = OvernightScheduleCalculator.buildSchedule(firstDate, cycleWeeks, records, forfeitures)

        assertEquals(listOf(0, 2), schedule.matches.map { it.slotIndex })
        schedule.matches.forEach { assertEquals(0, it.delayDays) } // 소멸된 차수는 지연으로 잡히지 않는다
        assertEquals(3, schedule.nextSlotIndex)
        assertEquals(slot(3), schedule.nextScheduledDate)
        assertEquals(setOf(1), schedule.forfeitedSlots)
    }

    @Test
    fun `consecutiveForfeitures - two slots in a row forfeited`() {
        val records = listOf(OvernightRecord("r0", slot(0)))
        val forfeitures = listOf(
            OvernightForfeiture("f1", slotIndex = 1),
            OvernightForfeiture("f2", slotIndex = 2),
        )
        val schedule = OvernightScheduleCalculator.buildSchedule(firstDate, cycleWeeks, records, forfeitures)

        assertEquals(0, schedule.matches.single().slotIndex)
        // 1, 2가 모두 소멸됐으므로 다음은 3
        assertEquals(3, schedule.nextSlotIndex)
        assertEquals(slot(3), schedule.nextScheduledDate)
    }

    @Test
    fun `forfeitureAfterAllRecords - a future forfeited slot is skipped when computing the next slot`() {
        val records = listOf(OvernightRecord("r0", slot(0)))
        val forfeitures = listOf(OvernightForfeiture("f1", slotIndex = 1)) // 아직 도달 안 한 미래 차수
        val schedule = OvernightScheduleCalculator.buildSchedule(firstDate, cycleWeeks, records, forfeitures)

        assertEquals(0, schedule.matches.single().slotIndex)
        assertEquals(2, schedule.nextSlotIndex) // 1은 소멸이라 건너뛰고 2
        assertEquals(slot(2), schedule.nextScheduledDate)
    }

    // ---------------------------------------------------------------------
    // 지연 + 선행 + 소멸 혼합 (스펙이 명시적으로 강조하는 케이스)
    // ---------------------------------------------------------------------

    @Test
    fun `mixedDelayEarlyForfeiture - slot0 delayed, slot1 early, slot2 forfeited, slot3 exact`() {
        val forfeitures = listOf(OvernightForfeiture("f1", slotIndex = 2))

        // slot0(2026-01-01)보다 10일 늦게, slot1(2026-02-12)보다 3일 이르게, slot3(2026-05-07)에 정확히
        val actualForSlot0 = slot(0).plusDays(10) // 2026-01-11
        val actualForSlot1 = slot(1).plusDays(-3) // 2026-02-09
        val actualForSlot3 = slot(3) // 2026-05-07

        val records = listOf(
            OvernightRecord("r0", actualForSlot0),
            OvernightRecord("r1", actualForSlot1),
            OvernightRecord("r2", actualForSlot3),
        )

        val schedule = OvernightScheduleCalculator.buildSchedule(firstDate, cycleWeeks, records, forfeitures)

        assertEquals(3, schedule.matches.size)

        val bySlot = schedule.matches.associateBy { it.slotIndex }
        assertEquals(setOf(0, 1, 3), bySlot.keys) // slot2는 소멸이라 매칭에 나타나지 않음

        assertEquals(10, bySlot.getValue(0).delayDays)
        assertEquals(slot(0), bySlot.getValue(0).scheduledDate)

        assertEquals(-3, bySlot.getValue(1).delayDays)
        assertEquals(slot(1), bySlot.getValue(1).scheduledDate)

        assertEquals(0, bySlot.getValue(3).delayDays)
        assertEquals(slot(3), bySlot.getValue(3).scheduledDate)

        // 다음 차수는 4 (0,1,3 사용, 2는 소멸)
        assertEquals(4, schedule.nextSlotIndex)
        assertEquals(slot(4), schedule.nextScheduledDate)
        assertEquals(setOf(2), schedule.forfeitedSlots)
    }

    @Test
    fun `mixedDelayEarlyForfeiture - input order does not matter, records are sorted internally by date`() {
        val forfeitures = listOf(OvernightForfeiture("f1", slotIndex = 2))
        val actualForSlot0 = slot(0).plusDays(10)
        val actualForSlot1 = slot(1).plusDays(-3)
        val actualForSlot3 = slot(3)

        // 위 테스트와 동일한 데이터를, 이번엔 날짜 역순(최신 먼저)으로 넣는다.
        val recordsReversed = listOf(
            OvernightRecord("r2", actualForSlot3),
            OvernightRecord("r1", actualForSlot1),
            OvernightRecord("r0", actualForSlot0),
        )

        val schedule = OvernightScheduleCalculator.buildSchedule(firstDate, cycleWeeks, recordsReversed, forfeitures)

        val bySlot = schedule.matches.associateBy { it.slotIndex }
        assertEquals(setOf(0, 1, 3), bySlot.keys)
        assertEquals(10, bySlot.getValue(0).delayDays)
        assertEquals(-3, bySlot.getValue(1).delayDays)
        assertEquals(0, bySlot.getValue(3).delayDays)
        assertEquals(4, schedule.nextSlotIndex)
    }
}

/** 테스트 가독성을 위한 작은 날짜 유틸. n일 뒤(음수면 n일 전) 날짜를 반환한다. */
private fun LocalDate.plusDays(days: Int): LocalDate =
    kotlinx.datetime.LocalDate.fromEpochDays(this.toEpochDays() + days)
