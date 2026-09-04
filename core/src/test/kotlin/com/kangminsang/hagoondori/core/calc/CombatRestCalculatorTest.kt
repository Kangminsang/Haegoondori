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

    @Test
    fun `summarizeAcrossLeaveTypes finds the converting type by overflowBehavior, not by name`() {
        val leaveTypes = listOf(
            LeaveType("annual", "연가", cap = null, overflowBehavior = OverflowBehavior.NONE),
            reward, // CONVERT_TO_COMBAT_REST
            rejectReward, // REJECT - 기여하지 않아야 함
        )
        val leaveGrants = listOf(
            LeaveGrant("g0", "annual", 5, LocalDate(2026, 1, 1)),
            LeaveGrant("g1", "reward", 19, LocalDate(2026, 1, 1)), // 2일 초과
            LeaveGrant("g2", "reward2", 20, LocalDate(2026, 1, 1)), // REJECT라 기여 안 함
        )
        val restGrants = listOf(CombatRestGrant("c1", 3, LocalDate(2026, 2, 1)))

        val summary = CombatRestCalculator.summarizeAcrossLeaveTypes(leaveTypes, leaveGrants, restGrants, emptyList())
        assertEquals(2, summary.convertedFromLeave)
        assertEquals(3, summary.directGranted)
        assertEquals(5, summary.totalGranted)
    }

    @Test
    fun `summarizeAcrossLeaveTypes sums multiple converting types if more than one exists`() {
        val secondReward = LeaveType("reward3", "특별포상", cap = 5, overflowBehavior = OverflowBehavior.CONVERT_TO_COMBAT_REST)
        val leaveTypes = listOf(reward, secondReward)
        val leaveGrants = listOf(
            LeaveGrant("g1", "reward", 19, LocalDate(2026, 1, 1)), // 2일 초과
            LeaveGrant("g2", "reward3", 8, LocalDate(2026, 1, 1)), // 3일 초과
        )

        val summary = CombatRestCalculator.summarizeAcrossLeaveTypes(leaveTypes, leaveGrants, emptyList(), emptyList())
        assertEquals(5, summary.convertedFromLeave)
    }

    @Test
    fun `summarizeAcrossLeaveTypes with no converting type yields only direct grants`() {
        val leaveTypes = listOf(LeaveType("annual", "연가", cap = null, overflowBehavior = OverflowBehavior.NONE))
        val restGrants = listOf(CombatRestGrant("c1", 4, LocalDate(2026, 2, 1)))

        val summary = CombatRestCalculator.summarizeAcrossLeaveTypes(leaveTypes, emptyList(), restGrants, emptyList())
        assertEquals(0, summary.convertedFromLeave)
        assertEquals(4, summary.totalGranted)
    }
}
