package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightRecord
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LeavePlannerTest {

    private fun d(m: Int, day: Int) = LocalDate(2026, m, day)
    private val reward = PlanKind.Leave("reward")
    private val regular = PlanKind.Leave("regular")
    private val overnight = PlanKind.Overnight

    /** [kind]를 [n]일. */
    private fun days(kind: PlanKind?, n: Int): List<PlanKind?> = List(n) { kind }

    /** 외박1 포상3 연가3, 9/22~9/28. */
    private val trip = days(overnight, 1) + days(reward, 3) + days(regular, 3)

    // ---- segmentsOf / counts ----

    @Test
    fun `contiguous days of the same kind form one segment each`() {
        assertEquals(
            listOf(
                PlanSegment(overnight, d(9, 22), d(9, 22)),
                PlanSegment(reward, d(9, 23), d(9, 25)),
                PlanSegment(regular, d(9, 26), d(9, 28)),
            ),
            LeavePlanner.segmentsOf(d(9, 22), trip),
        )
    }

    @Test
    fun `split runs of the same kind become separate segments`() {
        val dayKinds = days(reward, 2) + days(regular, 1) + days(reward, 1)
        assertEquals(
            listOf(
                PlanSegment(reward, d(9, 23), d(9, 24)),
                PlanSegment(regular, d(9, 25), d(9, 25)),
                PlanSegment(reward, d(9, 26), d(9, 26)),
            ),
            LeavePlanner.segmentsOf(d(9, 23), dayKinds),
        )
        assertEquals(mapOf(reward to 3, regular to 1), LeavePlanner.counts(dayKinds))
    }

    @Test
    fun `unassigned days are skipped and split a run`() {
        val dayKinds = listOf(reward, null, reward, null)
        assertEquals(
            listOf(PlanSegment(reward, d(9, 22), d(9, 22)), PlanSegment(reward, d(9, 24), d(9, 24))),
            LeavePlanner.segmentsOf(d(9, 22), dayKinds),
        )
        assertEquals(2, LeavePlanner.unassigned(dayKinds))
        assertEquals(mapOf(reward to 2), LeavePlanner.counts(dayKinds))
    }

    @Test
    fun `segments cross month boundary`() {
        val segments = LeavePlanner.segmentsOf(d(9, 29), days(reward, 2) + days(regular, 3))
        assertEquals(PlanSegment(reward, d(9, 29), d(9, 30)), segments[0])
        assertEquals(PlanSegment(regular, d(10, 1), d(10, 3)), segments[1])
    }

    // ---- autoFill ----

    @Test
    fun `autoFill on an empty plan fills from top with enough remaining and keeps overnight empty`() {
        val result = LeavePlanner.autoFill(days(null, 7), listOf(overnight to 0, reward to 3, regular to 20))
        assertEquals(days(reward, 3) + days(regular, 4), result)
    }

    @Test
    fun `autoFill puts days beyond remaining on the first leave row next to it`() {
        val result = LeavePlanner.autoFill(days(null, 7), listOf(overnight to 0, reward to 2, regular to 1))
        assertEquals(days(reward, 6) + days(regular, 1), result)
    }

    @Test
    fun `autoFill treats negative remaining as zero`() {
        assertEquals(days(regular, 3), LeavePlanner.autoFill(days(null, 3), listOf(reward to -2, regular to 5)))
    }

    @Test
    fun `autoFill with only overnight row puts everything there`() {
        assertEquals(days(overnight, 2), LeavePlanner.autoFill(days(null, 2), listOf(overnight to 0)))
    }

    @Test
    fun `autoFill keeps painted days and fills only the empty ones using what is left`() {
        // 포상 잔여 3 중 이미 2일을 칠했다: 빈 날 4일에 포상 1, 연가 3.
        val painted = listOf(overnight, null, reward, null, reward, null, null)
        val result = LeavePlanner.autoFill(painted, listOf(overnight to 0, reward to 3, regular to 20))
        assertEquals(listOf(overnight, reward, reward, regular, reward, regular, regular), result)
    }

    @Test
    fun `autoFill leaves a fully painted plan alone`() {
        assertEquals(trip, LeavePlanner.autoFill(trip, listOf(overnight to 0, reward to 0, regular to 0)))
    }

    // ---- dayKindsOf ----

    @Test
    fun `existing records inside the range prefill each day`() {
        val usages = listOf(
            LeaveUsage("u1", "reward", d(9, 23), d(9, 25)),
            LeaveUsage("u2", "regular", d(9, 26), d(9, 28)),
        )
        val overnights = listOf(OvernightRecord("o1", d(9, 22)))
        assertEquals(trip, LeavePlanner.dayKindsOf(d(9, 22), d(9, 28), usages, overnights))
    }

    @Test
    fun `prefill keeps a reward regular reward arrangement`() {
        val usages = listOf(
            LeaveUsage("u1", "reward", d(9, 23), d(9, 24)),
            LeaveUsage("u2", "regular", d(9, 25), d(9, 25)),
            LeaveUsage("u3", "reward", d(9, 26), d(9, 26)),
        )
        val dayKinds = LeavePlanner.dayKindsOf(d(9, 23), d(9, 26), usages, emptyList())
        assertEquals(listOf(reward, reward, regular, reward), dayKinds)
        // 다시 저장해도 기록이 그대로라 아무것도 바뀌지 않는다.
        val changes = LeavePlanner.apply(d(9, 23), d(9, 26), dayKinds, usages, emptyList())
        assertEquals(LeavePlanner.Changes(emptyList(), emptyList(), emptyList(), emptyList()), changes)
    }

    @Test
    fun `prefill is clipped to the range and uncovered days are empty`() {
        val usages = listOf(LeaveUsage("u1", "reward", d(9, 20), d(9, 24)))
        assertEquals(
            days(reward, 2) + days(null, 4),
            LeavePlanner.dayKindsOf(d(9, 23), d(9, 28), usages, emptyList()),
        )
    }

    // ---- apply ----

    @Test
    fun `new plan on empty range adds contiguous records`() {
        val changes = LeavePlanner.apply(d(9, 22), d(9, 28), trip, emptyList(), emptyList())
        assertTrue(changes.deleteUsages.isEmpty())
        assertTrue(changes.deleteOvernights.isEmpty())
        assertEquals(listOf(OvernightDraft(d(9, 22), d(9, 22), null)), changes.addOvernights)
        assertEquals(
            listOf(
                LeaveUsageDraft("reward", d(9, 23), d(9, 25), null),
                LeaveUsageDraft("regular", d(9, 26), d(9, 28), null),
            ),
            changes.addUsages,
        )
    }

    @Test
    fun `painting a split arrangement saves one record per run`() {
        val changes = LeavePlanner.apply(
            d(9, 23), d(9, 26),
            days(reward, 2) + days(regular, 1) + days(reward, 1),
            emptyList(), emptyList(),
        )
        assertEquals(
            listOf(
                LeaveUsageDraft("reward", d(9, 23), d(9, 24), null),
                LeaveUsageDraft("regular", d(9, 25), d(9, 25), null),
                LeaveUsageDraft("reward", d(9, 26), d(9, 26), null),
            ),
            changes.addUsages,
        )
    }

    @Test
    fun `repainting the middle of a record splits it and keeps untouched records`() {
        val o1 = OvernightRecord("o1", d(9, 22), memo = "m")
        val u1 = LeaveUsage("u1", "reward", d(9, 23), d(9, 25))
        val u2 = LeaveUsage("u2", "regular", d(9, 26), d(9, 28))
        // 포상 가운데 9/24를 연가로 칠한다.
        val painted = LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 24), brush = regular)!!
        val changes = LeavePlanner.apply(d(9, 22), d(9, 28), painted.dayKinds, listOf(u1, u2), listOf(o1))
        assertTrue(changes.deleteOvernights.isEmpty())
        assertTrue(changes.addOvernights.isEmpty())
        assertEquals(listOf(u1), changes.deleteUsages)
        assertEquals(
            listOf(
                LeaveUsageDraft("reward", d(9, 23), d(9, 23), null),
                LeaveUsageDraft("regular", d(9, 24), d(9, 24), null),
                LeaveUsageDraft("reward", d(9, 25), d(9, 25), null),
            ),
            changes.addUsages,
        )
    }

    @Test
    fun `replanning an existing trip replaces changed records and keeps identical ones`() {
        val o1 = OvernightRecord("o1", d(9, 22), memo = "m")
        val u1 = LeaveUsage("u1", "reward", d(9, 23), d(9, 25))
        val u2 = LeaveUsage("u2", "regular", d(9, 26), d(9, 28))
        val changes = LeavePlanner.apply(
            d(9, 22), d(9, 28),
            days(overnight, 1) + days(reward, 2) + days(regular, 4),
            listOf(u1, u2), listOf(o1),
        )
        assertTrue(changes.deleteOvernights.isEmpty())
        assertTrue(changes.addOvernights.isEmpty())
        assertEquals(listOf(u1, u2), changes.deleteUsages)
        assertEquals(
            listOf(
                LeaveUsageDraft("reward", d(9, 23), d(9, 24), null),
                LeaveUsageDraft("regular", d(9, 25), d(9, 28), null),
            ),
            changes.addUsages,
        )
    }

    @Test
    fun `records longer than the range are trimmed and the outside pieces kept with label and memo`() {
        val usage = LeaveUsage("u1", "regular", d(9, 20), d(9, 30), label = "추석")
        val record = OvernightRecord("o1", d(9, 21), memo = "집", endDate = d(9, 23))
        val changes = LeavePlanner.apply(d(9, 23), d(9, 27), days(reward, 5), listOf(usage), listOf(record))
        assertEquals(listOf(usage), changes.deleteUsages)
        assertEquals(listOf(record), changes.deleteOvernights)
        assertEquals(
            listOf(
                LeaveUsageDraft("regular", d(9, 20), d(9, 22), "추석"),
                LeaveUsageDraft("regular", d(9, 28), d(9, 30), "추석"),
                LeaveUsageDraft("reward", d(9, 23), d(9, 27), null),
            ),
            changes.addUsages,
        )
        assertEquals(listOf(OvernightDraft(d(9, 21), d(9, 22), "집")), changes.addOvernights)
    }

    @Test
    fun `records outside the range are not touched`() {
        val before = LeaveUsage("u1", "reward", d(9, 18), d(9, 21))
        val after = OvernightRecord("o1", d(9, 29))
        val changes = LeavePlanner.apply(d(9, 22), d(9, 28), days(regular, 7), listOf(before), listOf(after))
        assertTrue(changes.deleteUsages.isEmpty())
        assertTrue(changes.deleteOvernights.isEmpty())
        assertTrue(changes.addOvernights.isEmpty())
        assertEquals(listOf(LeaveUsageDraft("regular", d(9, 22), d(9, 28), null)), changes.addUsages)
    }

    @Test
    fun `all empty days delete every record in the range`() {
        val u1 = LeaveUsage("u1", "reward", d(9, 23), d(9, 25))
        val o1 = OvernightRecord("o1", d(9, 22))
        val changes = LeavePlanner.apply(d(9, 22), d(9, 25), days(null, 4), listOf(u1), listOf(o1))
        assertEquals(listOf(u1), changes.deleteUsages)
        assertEquals(listOf(o1), changes.deleteOvernights)
        assertTrue(changes.addUsages.isEmpty())
        assertTrue(changes.addOvernights.isEmpty())
    }

    // ---- adjust without brush ----

    @Test
    fun `tapping before start extends with the first day's kind`() {
        assertEquals(
            LeavePlanner.Adjusted(d(9, 20), d(9, 28), days(overnight, 3) + days(reward, 3) + days(regular, 3)),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 20)),
        )
    }

    @Test
    fun `tapping after end extends with the last day's kind across the month`() {
        val result = LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(10, 2))!!
        assertEquals(d(9, 22), result.start)
        assertEquals(d(10, 2), result.end)
        assertEquals(days(overnight, 1) + days(reward, 3) + days(regular, 7), result.dayKinds)
    }

    @Test
    fun `extending next to an empty day adds empty days`() {
        val result = LeavePlanner.adjust(d(9, 22), d(9, 23), listOf(reward, null), d(9, 25))!!
        assertEquals(listOf(reward, null, null, null), result.dayKinds)
    }

    @Test
    fun `tapping the start or end day drops it`() {
        assertEquals(
            LeavePlanner.Adjusted(d(9, 23), d(9, 28), days(reward, 3) + days(regular, 3)),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 22)),
        )
        assertEquals(
            LeavePlanner.Adjusted(d(9, 22), d(9, 27), days(overnight, 1) + days(reward, 3) + days(regular, 2)),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 28)),
        )
    }

    @Test
    fun `tapping inside moves the nearer edge and the exact middle moves the end`() {
        // 9/24: 시작에서 2일, 끝에서 4일 → 시작을 옮긴다.
        assertEquals(
            LeavePlanner.Adjusted(d(9, 24), d(9, 28), days(reward, 2) + days(regular, 3)),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 24)),
        )
        // 9/26: 끝을 옮긴다.
        assertEquals(
            LeavePlanner.Adjusted(d(9, 22), d(9, 26), days(overnight, 1) + days(reward, 3) + days(regular, 1)),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 26)),
        )
        // 9/25는 양 끝에서 3일씩 → 종료일을 옮긴다.
        assertEquals(
            LeavePlanner.Adjusted(d(9, 22), d(9, 25), days(overnight, 1) + days(reward, 3)),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 25)),
        )
    }

    @Test
    fun `shrinking keeps the arrangement of the remaining days`() {
        val split = days(reward, 2) + days(regular, 1) + days(reward, 2)
        assertEquals(
            LeavePlanner.Adjusted(d(9, 22), d(9, 25), days(reward, 2) + days(regular, 1) + days(reward, 1)),
            LeavePlanner.adjust(d(9, 22), d(9, 26), split, d(9, 26)),
        )
    }

    @Test
    fun `tapping the only day of a one day range clears it`() {
        assertNull(LeavePlanner.adjust(d(9, 22), d(9, 22), listOf(reward), d(9, 22)))
    }

    // ---- adjust with brush (painting) ----

    @Test
    fun `painting a day inside sets that day's kind`() {
        val result = LeavePlanner.adjust(d(9, 23), d(9, 26), days(reward, 4), d(9, 25), brush = regular)
        assertEquals(LeavePlanner.Adjusted(d(9, 23), d(9, 26), listOf(reward, reward, regular, reward)), result)
    }

    @Test
    fun `painting an empty day fills it`() {
        val result = LeavePlanner.adjust(d(9, 22), d(9, 24), listOf(reward, null, reward), d(9, 23), brush = overnight)
        assertEquals(listOf(reward, overnight, reward), result!!.dayKinds)
    }

    @Test
    fun `painting an interior day already of the brush kind empties it`() {
        val result = LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 24), brush = reward)
        assertEquals(LeavePlanner.Adjusted(d(9, 22), d(9, 28), listOf(overnight, reward, null, reward, regular, regular, regular)), result)
    }

    @Test
    fun `painting the start or end day already of the brush kind drops it from the range`() {
        assertEquals(
            LeavePlanner.Adjusted(d(9, 23), d(9, 28), days(reward, 3) + days(regular, 3)),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 22), brush = overnight),
        )
        assertEquals(
            LeavePlanner.Adjusted(d(9, 22), d(9, 27), days(overnight, 1) + days(reward, 3) + days(regular, 2)),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 28), brush = regular),
        )
        assertNull(LeavePlanner.adjust(d(9, 22), d(9, 22), listOf(reward), d(9, 22), brush = reward))
    }

    @Test
    fun `painting an edge of another kind repaints it without shrinking`() {
        assertEquals(
            LeavePlanner.Adjusted(d(9, 22), d(9, 28), days(reward, 4) + days(regular, 3)),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 22), brush = reward),
        )
    }

    @Test
    fun `painting outside extends the range with the brush kind`() {
        assertEquals(
            LeavePlanner.Adjusted(d(9, 20), d(9, 28), days(reward, 2) + trip),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 20), brush = reward),
        )
        assertEquals(
            LeavePlanner.Adjusted(d(9, 22), d(9, 30), trip + days(reward, 2)),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 30), brush = reward),
        )
    }

    // ---- applyEdit ----

    @Test
    fun `shrinking an existing trip deletes the dropped days and leaves unrelated records`() {
        val o1 = OvernightRecord("o1", d(9, 22))
        val u1 = LeaveUsage("u1", "reward", d(9, 23), d(9, 25))
        val u2 = LeaveUsage("u2", "regular", d(9, 26), d(9, 28))
        val unrelatedBefore = LeaveUsage("u3", "regular", d(9, 10), d(9, 12))
        val unrelatedAfter = OvernightRecord("o2", d(10, 5))
        val adjusted = LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 26))!!
        val changes = LeavePlanner.applyEdit(
            d(9, 22), d(9, 28),
            adjusted.start, adjusted.end, adjusted.dayKinds,
            listOf(unrelatedBefore, u1, u2), listOf(o1, unrelatedAfter),
        )
        // 외박·포상은 그대로, 연가 9/26~28은 9/26 하루로 바뀐다.
        assertEquals(listOf(u2), changes.deleteUsages)
        assertTrue(changes.deleteOvernights.isEmpty())
        assertEquals(listOf(LeaveUsageDraft("regular", d(9, 26), d(9, 26), null)), changes.addUsages)
        assertTrue(changes.addOvernights.isEmpty())
    }

    @Test
    fun `shrinking an existing trip from the front deletes the dropped overnight`() {
        val o1 = OvernightRecord("o1", d(9, 22))
        val u1 = LeaveUsage("u1", "reward", d(9, 23), d(9, 25))
        val changes = LeavePlanner.applyEdit(d(9, 22), d(9, 25), d(9, 23), d(9, 25), days(reward, 3), listOf(u1), listOf(o1))
        assertEquals(listOf(o1), changes.deleteOvernights)
        assertTrue(changes.deleteUsages.isEmpty())
        assertTrue(changes.addUsages.isEmpty())
        assertTrue(changes.addOvernights.isEmpty())
    }

    @Test
    fun `plain apply over the new range alone would keep the dropped days`() {
        val u2 = LeaveUsage("u2", "regular", d(9, 26), d(9, 28))
        val changes = LeavePlanner.apply(d(9, 26), d(9, 26), days(regular, 1), listOf(u2), emptyList())
        // 범위 밖으로 삐져나온 9/27~28을 잘라 다시 넣는다 - 그래서 수정 저장에는 applyEdit가 필요하다.
        assertEquals(listOf(LeaveUsageDraft("regular", d(9, 27), d(9, 28), null)), changes.addUsages.filter { it.startDate > d(9, 26) })
    }

    @Test
    fun `applyEdit with a disjoint original range only touches the new range`() {
        val far = LeaveUsage("u1", "reward", d(9, 1), d(9, 3))
        val between = LeaveUsage("u2", "reward", d(9, 10), d(9, 12))
        val changes = LeavePlanner.applyEdit(d(9, 1), d(9, 3), d(9, 20), d(9, 21), days(regular, 2), listOf(far, between), emptyList())
        assertTrue(changes.deleteUsages.isEmpty())
        assertEquals(listOf(LeaveUsageDraft("regular", d(9, 20), d(9, 21), null)), changes.addUsages)
    }

    // ---- tripAround ----

    @Test
    fun `tripAround returns the touching records around the date`() {
        val usages = listOf(
            LeaveUsage("u1", "reward", d(9, 23), d(9, 25)),
            LeaveUsage("u2", "regular", d(9, 26), d(9, 28)),
            LeaveUsage("u3", "regular", d(10, 10), d(10, 11)),
        )
        val overnights = listOf(OvernightRecord("o1", d(9, 22)))
        val found = LeavePlanner.tripAround(d(9, 24), usages, overnights)!!
        assertEquals(d(9, 22), found.start)
        assertEquals(d(9, 28), found.end)
        assertEquals(7, found.totalDays)
        assertEquals(trip, found.dayKinds)
        assertEquals(3, found.segments.size)
    }

    @Test
    fun `tripAround is null on a day without records`() {
        val usages = listOf(LeaveUsage("u1", "reward", d(9, 23), d(9, 25)))
        assertNull(LeavePlanner.tripAround(d(9, 26), usages, emptyList()))
    }
}
