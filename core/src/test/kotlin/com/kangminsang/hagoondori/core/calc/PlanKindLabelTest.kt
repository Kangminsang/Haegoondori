package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightRecord
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlanKindLabelTest {

    private val types = listOf(
        LeaveType("regular", "정기휴가", cap = null),
        LeaveType("reward", "포상휴가", cap = 17),
    )
    private val regular = PlanKind.Leave("regular")
    private val reward = PlanKind.Leave("reward")
    private fun d(m: Int, day: Int) = LocalDate(2026, m, day)

    @Test
    fun `day letter uses the device label's first character`() {
        assertEquals("연", PlanKindLabel.dayLetter(regular, types))
        assertEquals("포", PlanKindLabel.dayLetter(reward, types))
        assertEquals("외", PlanKindLabel.dayLetter(PlanKind.Overnight, types))
        assertEquals("휴", PlanKindLabel.dayLetter(PlanKind.Leave("deleted"), types))
    }

    @Test
    fun `short names`() {
        assertEquals("연가", PlanKindLabel.shortName(regular, types))
        assertEquals("외박", PlanKindLabel.shortName(PlanKind.Overnight, types))
    }

    @Test
    fun `overlapping days take the earliest-starting record, leave first on a tie`() {
        val usages = listOf(LeaveUsage("u1", "reward", d(9, 23), d(9, 25)))
        val overnights = listOf(
            OvernightRecord("o1", d(9, 22), endDate = d(9, 23)),
            OvernightRecord("o2", d(9, 25)),
        )
        val kinds = LeavePlanner.dayKindsOf(d(9, 21), d(9, 26), usages, overnights)
        assertEquals(listOf(null, PlanKind.Overnight, PlanKind.Overnight, reward, reward, null), kinds)

        val tie = LeavePlanner.dayKindsOf(d(9, 25), d(9, 25), listOf(LeaveUsage("u2", "reward", d(9, 25), d(9, 25))), overnights)
        assertEquals(listOf(reward), tie)
    }
}
