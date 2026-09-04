package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.CombatRestGrant
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.OverflowBehavior
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CombatRestCalculatorTest {

    private val reward = LeaveType("reward", "포상휴가", cap = 17, overflowBehavior = OverflowBehavior.CONVERT_TO_COMBAT_REST)
    private val rejectReward = LeaveType("reward2", "포상휴가(비전환)", cap = 17, overflowBehavior = OverflowBehavior.REJECT)

    @Test
    fun `only conversion source - no direct grants`() {
        val leaveGrants = listOf(LeaveGrant("g1", "reward", 19, LocalDate(2026, 1, 1)))
        val summary = CombatRestCalculator.summarize(reward, leaveGrants, emptyList(), emptyList())
        assertEquals(2, summary.convertedFromLeave)
        assertEquals(0, summary.directGranted)
        assertEquals(2, summary.totalGranted)
        assertEquals(2, summary.remaining)
    }

    @Test
    fun `only direct grant source - cap not reached so no conversion`() {
        val leaveGrants = listOf(LeaveGrant("g1", "reward", 10, LocalDate(2026, 1, 1)))
        val restGrants = listOf(CombatRestGrant("c1", 3, LocalDate(2026, 2, 1)))
        val summary = CombatRestCalculator.summarize(reward, leaveGrants, restGrants, emptyList())
        assertEquals(0, summary.convertedFromLeave)
        assertEquals(3, summary.directGranted)
        assertEquals(3, summary.totalGranted)
    }

    @Test
    fun `mixed sources - spec example 5 days as 2 converted plus 3 direct`() {
        val leaveGrants = listOf(LeaveGrant("g1", "reward", 19, LocalDate(2026, 1, 1)))
        val restGrants = listOf(CombatRestGrant("c1", 3, LocalDate(2026, 2, 1)))
        val summary = CombatRestCalculator.summarize(reward, leaveGrants, restGrants, emptyList())
        assertEquals(2, summary.convertedFromLeave)
        assertEquals(3, summary.directGranted)
        assertEquals(5, summary.totalGranted)
    }

    @Test
    fun `overflowBehavior other than CONVERT_TO_COMBAT_REST yields zero conversion`() {
        val leaveGrants = listOf(LeaveGrant("g1", "reward2", 20, LocalDate(2026, 1, 1)))
        val summary = CombatRestCalculator.summarize(rejectReward, leaveGrants, emptyList(), emptyList())
        assertEquals(0, summary.convertedFromLeave)
        assertEquals(0, summary.totalGranted)
    }

    @Test
    fun `usage reduces remaining and totalUsed counts usage periods inclusive`() {
        val leaveGrants = listOf(LeaveGrant("g1", "reward", 19, LocalDate(2026, 1, 1)))
        val restGrants = listOf(CombatRestGrant("c1", 3, LocalDate(2026, 2, 1)))
        val usages = listOf(CombatRestUsage("u1", LocalDate(2026, 9, 20), LocalDate(2026, 9, 20)))
        val summary = CombatRestCalculator.summarize(reward, leaveGrants, restGrants, usages)
        assertEquals(5, summary.totalGranted)
        assertEquals(1, summary.totalUsed)
        assertEquals(4, summary.remaining)
    }
}
