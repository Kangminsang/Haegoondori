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

    // ---- layout ----

    @Test
    fun `overnight 1 reward 3 regular 3 are laid out in row order`() {
        val segments = LeavePlanner.layout(d(9, 22), listOf(PlanPart(overnight, 1), PlanPart(reward, 3), PlanPart(regular, 3)))
        assertEquals(
            listOf(
                PlanSegment(overnight, d(9, 22), d(9, 22)),
                PlanSegment(reward, d(9, 23), d(9, 25)),
                PlanSegment(regular, d(9, 26), d(9, 28)),
            ),
            segments,
        )
    }

    @Test
    fun `zero day rows are skipped`() {
        val segments = LeavePlanner.layout(d(9, 22), listOf(PlanPart(overnight, 0), PlanPart(reward, 2), PlanPart(regular, 0)))
        assertEquals(listOf(PlanSegment(reward, d(9, 22), d(9, 23))), segments)
    }

    @Test
    fun `layout crosses month boundary`() {
        val segments = LeavePlanner.layout(d(9, 29), listOf(PlanPart(reward, 2), PlanPart(regular, 3)))
        assertEquals(PlanSegment(reward, d(9, 29), d(9, 30)), segments[0])
        assertEquals(PlanSegment(regular, d(10, 1), d(10, 3)), segments[1])
    }

    // ---- autoFill ----

    @Test
    fun `autoFill fills from top with enough remaining and keeps overnight at zero`() {
        val parts = LeavePlanner.autoFill(7, listOf(overnight to 0, reward to 3, regular to 20))
        assertEquals(listOf(PlanPart(overnight, 0), PlanPart(reward, 3), PlanPart(regular, 4)), parts)
    }

    @Test
    fun `autoFill puts days beyond remaining on the first leave row`() {
        val parts = LeavePlanner.autoFill(7, listOf(overnight to 0, reward to 2, regular to 1))
        assertEquals(listOf(PlanPart(overnight, 0), PlanPart(reward, 6), PlanPart(regular, 1)), parts)
        assertEquals(7, parts.sumOf { it.days })
    }

    @Test
    fun `autoFill treats negative remaining as zero`() {
        val parts = LeavePlanner.autoFill(3, listOf(reward to -2, regular to 5))
        assertEquals(listOf(PlanPart(reward, 0), PlanPart(regular, 3)), parts)
    }

    @Test
    fun `autoFill with only overnight row puts everything there`() {
        assertEquals(listOf(PlanPart(overnight, 2)), LeavePlanner.autoFill(2, listOf(overnight to 0)))
    }

    // ---- existingParts ----

    @Test
    fun `existing records inside the range prefill the panel`() {
        val usages = listOf(
            LeaveUsage("u1", "reward", d(9, 23), d(9, 25)),
            LeaveUsage("u2", "regular", d(9, 26), d(9, 28)),
        )
        val overnights = listOf(OvernightRecord("o1", d(9, 22)))
        assertEquals(
            listOf(PlanPart(overnight, 1), PlanPart(reward, 3), PlanPart(regular, 3)),
            LeavePlanner.existingParts(d(9, 22), d(9, 28), usages, overnights),
        )
    }

    @Test
    fun `existing parts are clipped to the range and uncovered days are not counted`() {
        val usages = listOf(LeaveUsage("u1", "reward", d(9, 20), d(9, 24)))
        assertEquals(
            listOf(PlanPart(reward, 2)),
            LeavePlanner.existingParts(d(9, 23), d(9, 28), usages, emptyList()),
        )
    }

    @Test
    fun `adjacent records of the same type merge into one part`() {
        val usages = listOf(
            LeaveUsage("u1", "reward", d(9, 22), d(9, 23)),
            LeaveUsage("u2", "reward", d(9, 24), d(9, 25)),
        )
        assertEquals(listOf(PlanPart(reward, 4)), LeavePlanner.existingParts(d(9, 22), d(9, 25), usages, emptyList()))
    }

    // ---- apply ----

    @Test
    fun `new plan on empty range adds contiguous records`() {
        val changes = LeavePlanner.apply(
            d(9, 22), d(9, 28),
            listOf(PlanPart(overnight, 1), PlanPart(reward, 3), PlanPart(regular, 3)),
            emptyList(), emptyList(),
        )
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
    fun `replanning an existing trip replaces changed records and keeps identical ones`() {
        val o1 = OvernightRecord("o1", d(9, 22), memo = "m")
        val u1 = LeaveUsage("u1", "reward", d(9, 23), d(9, 25))
        val u2 = LeaveUsage("u2", "regular", d(9, 26), d(9, 28))
        val changes = LeavePlanner.apply(
            d(9, 22), d(9, 28),
            listOf(PlanPart(overnight, 1), PlanPart(reward, 2), PlanPart(regular, 4)),
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
        val changes = LeavePlanner.apply(
            d(9, 23), d(9, 27),
            listOf(PlanPart(reward, 5)),
            listOf(usage), listOf(record),
        )
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
        val changes = LeavePlanner.apply(d(9, 22), d(9, 28), listOf(PlanPart(regular, 7)), listOf(before), listOf(after))
        assertTrue(changes.deleteUsages.isEmpty())
        assertTrue(changes.deleteOvernights.isEmpty())
        assertTrue(changes.addOvernights.isEmpty())
        assertEquals(listOf(LeaveUsageDraft("regular", d(9, 22), d(9, 28), null)), changes.addUsages)
    }

    @Test
    fun `empty parts delete every record in the range`() {
        val u1 = LeaveUsage("u1", "reward", d(9, 23), d(9, 25))
        val o1 = OvernightRecord("o1", d(9, 22))
        val changes = LeavePlanner.apply(d(9, 22), d(9, 25), emptyList(), listOf(u1), listOf(o1))
        assertEquals(listOf(u1), changes.deleteUsages)
        assertEquals(listOf(o1), changes.deleteOvernights)
        assertTrue(changes.addUsages.isEmpty())
        assertTrue(changes.addOvernights.isEmpty())
    }

    // ---- adjust ----

    /** 외박1 포상3 연가3, 9/22~9/28. */
    private val trip = listOf(PlanPart(overnight, 1), PlanPart(reward, 3), PlanPart(regular, 3))

    @Test
    fun `tapping before start extends the front part`() {
        val result = LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 20))
        assertEquals(
            LeavePlanner.Adjusted(d(9, 20), d(9, 28), listOf(PlanPart(overnight, 3), PlanPart(reward, 3), PlanPart(regular, 3))),
            result,
        )
    }

    @Test
    fun `tapping after end extends the back part across the month`() {
        val result = LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(10, 2))!!
        assertEquals(d(9, 22), result.start)
        assertEquals(d(10, 2), result.end)
        assertEquals(listOf(PlanPart(overnight, 1), PlanPart(reward, 3), PlanPart(regular, 7)), result.parts)
        assertEquals(LeavePlanner.dayCount(result.start, result.end), result.parts.sumOf { it.days })
    }

    @Test
    fun `extending skips zero rows at the edges`() {
        val parts = listOf(PlanPart(overnight, 0), PlanPart(reward, 2), PlanPart(regular, 0))
        assertEquals(
            listOf(PlanPart(overnight, 0), PlanPart(reward, 4), PlanPart(regular, 0)),
            LeavePlanner.adjust(d(9, 22), d(9, 23), parts, d(9, 20))!!.parts,
        )
        assertEquals(
            listOf(PlanPart(overnight, 0), PlanPart(reward, 3), PlanPart(regular, 0)),
            LeavePlanner.adjust(d(9, 22), d(9, 23), parts, d(9, 24))!!.parts,
        )
    }

    @Test
    fun `tapping the start day drops it and removes a part that reaches zero`() {
        val result = LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 22))
        assertEquals(
            LeavePlanner.Adjusted(d(9, 23), d(9, 28), listOf(PlanPart(overnight, 0), PlanPart(reward, 3), PlanPart(regular, 3))),
            result,
        )
        assertEquals(listOf(PlanSegment(reward, d(9, 23), d(9, 25)), PlanSegment(regular, d(9, 26), d(9, 28))), LeavePlanner.layout(result!!.start, result.parts))
    }

    @Test
    fun `tapping the end day drops it from the back part`() {
        val result = LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 28))
        assertEquals(
            LeavePlanner.Adjusted(d(9, 22), d(9, 27), listOf(PlanPart(overnight, 1), PlanPart(reward, 3), PlanPart(regular, 2))),
            result,
        )
    }

    @Test
    fun `tapping inside moves the nearer edge`() {
        // 9/24: 시작에서 2일, 끝에서 4일 → 시작을 옮긴다. 외박1을 다 빼고 포상에서 1 뺀다.
        assertEquals(
            LeavePlanner.Adjusted(d(9, 24), d(9, 28), listOf(PlanPart(overnight, 0), PlanPart(reward, 2), PlanPart(regular, 3))),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 24)),
        )
        // 9/26: 시작에서 4일, 끝에서 2일 → 끝을 옮긴다. 연가 3 중 2를 뺀다.
        assertEquals(
            LeavePlanner.Adjusted(d(9, 22), d(9, 26), listOf(PlanPart(overnight, 1), PlanPart(reward, 3), PlanPart(regular, 1))),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 26)),
        )
    }

    @Test
    fun `tapping the exact middle moves the end`() {
        // 9/25는 양 끝에서 3일씩 → 종료일을 옮긴다. 연가 3을 다 빼 0이 된다.
        assertEquals(
            LeavePlanner.Adjusted(d(9, 22), d(9, 25), listOf(PlanPart(overnight, 1), PlanPart(reward, 3), PlanPart(regular, 0))),
            LeavePlanner.adjust(d(9, 22), d(9, 28), trip, d(9, 25)),
        )
    }

    @Test
    fun `shrinking across a month boundary takes days from several parts`() {
        // 9/29~10/05: 외박1 포상1 연가5. 10/1은 시작에서 2일, 끝에서 4일 → 시작을 옮기며 외박·포상이 모두 빠진다.
        val parts = listOf(PlanPart(overnight, 1), PlanPart(reward, 1), PlanPart(regular, 5))
        assertEquals(
            LeavePlanner.Adjusted(d(10, 1), d(10, 5), listOf(PlanPart(overnight, 0), PlanPart(reward, 0), PlanPart(regular, 5))),
            LeavePlanner.adjust(d(9, 29), d(10, 5), parts, d(10, 1)),
        )
        // 10/5(종료일)을 누르면 하루 줄어 10/4까지.
        assertEquals(
            LeavePlanner.Adjusted(d(9, 29), d(10, 4), listOf(PlanPart(overnight, 1), PlanPart(reward, 1), PlanPart(regular, 4))),
            LeavePlanner.adjust(d(9, 29), d(10, 5), parts, d(10, 5)),
        )
    }

    @Test
    fun `shrinking from the end takes unassigned days first`() {
        // 5일 중 포상 3일만 배분: 뒤 2일은 비어 있다. 종료일을 누르면 빈 날만 빠진다.
        assertEquals(
            LeavePlanner.Adjusted(d(9, 22), d(9, 25), listOf(PlanPart(reward, 3))),
            LeavePlanner.adjust(d(9, 22), d(9, 26), listOf(PlanPart(reward, 3)), d(9, 26)),
        )
        // 포상 4일(빈 날 1일)에서 가운데 9/24를 누르면 2일이 빠진다: 빈 날 1 + 포상 1.
        assertEquals(
            LeavePlanner.Adjusted(d(9, 22), d(9, 24), listOf(PlanPart(reward, 3))),
            LeavePlanner.adjust(d(9, 22), d(9, 26), listOf(PlanPart(reward, 4)), d(9, 24)),
        )
    }

    @Test
    fun `tapping the only day of a one day range clears it`() {
        assertNull(LeavePlanner.adjust(d(9, 22), d(9, 22), listOf(PlanPart(reward, 1)), d(9, 22)))
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
            adjusted.start, adjusted.end, adjusted.parts,
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
        val changes = LeavePlanner.applyEdit(
            d(9, 22), d(9, 25), d(9, 23), d(9, 25),
            listOf(PlanPart(overnight, 0), PlanPart(reward, 3)),
            listOf(u1), listOf(o1),
        )
        assertEquals(listOf(o1), changes.deleteOvernights)
        assertTrue(changes.deleteUsages.isEmpty())
        assertTrue(changes.addUsages.isEmpty())
        assertTrue(changes.addOvernights.isEmpty())
    }

    @Test
    fun `plain apply over the new range alone would keep the dropped days`() {
        val u2 = LeaveUsage("u2", "regular", d(9, 26), d(9, 28))
        val changes = LeavePlanner.apply(d(9, 26), d(9, 26), listOf(PlanPart(regular, 1)), listOf(u2), emptyList())
        // 범위 밖으로 삐져나온 9/27~28을 잘라 다시 넣는다 - 그래서 수정 저장에는 applyEdit가 필요하다.
        assertEquals(listOf(LeaveUsageDraft("regular", d(9, 27), d(9, 28), null)), changes.addUsages.filter { it.startDate > d(9, 26) })
    }

    @Test
    fun `applyEdit with a disjoint original range only touches the new range`() {
        val far = LeaveUsage("u1", "reward", d(9, 1), d(9, 3))
        val between = LeaveUsage("u2", "reward", d(9, 10), d(9, 12))
        val changes = LeavePlanner.applyEdit(
            d(9, 1), d(9, 3), d(9, 20), d(9, 21),
            listOf(PlanPart(regular, 2)),
            listOf(far, between), emptyList(),
        )
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
        val trip = LeavePlanner.tripAround(d(9, 24), usages, overnights)!!
        assertEquals(d(9, 22), trip.start)
        assertEquals(d(9, 28), trip.end)
        assertEquals(7, trip.totalDays)
        assertEquals(listOf(PlanPart(overnight, 1), PlanPart(reward, 3), PlanPart(regular, 3)), trip.parts)
    }

    @Test
    fun `tripAround is null on a day without records`() {
        val usages = listOf(LeaveUsage("u1", "reward", d(9, 23), d(9, 25)))
        assertNull(LeavePlanner.tripAround(d(9, 26), usages, emptyList()))
    }
}
