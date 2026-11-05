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
