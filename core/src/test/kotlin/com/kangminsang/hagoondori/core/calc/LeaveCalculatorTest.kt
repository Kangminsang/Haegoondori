package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OverflowBehavior
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LeaveCalculatorTest {

    private val annual = LeaveType(id = "annual", name = "연가", cap = null, overflowBehavior = OverflowBehavior.NONE)
    private val reward = LeaveType(id = "reward", name = "포상휴가", cap = 17, overflowBehavior = OverflowBehavior.CONVERT_TO_COMBAT_REST)

    @Test
    fun `usageDays counts a same-day request as one day`() {
        val usage = LeaveUsage("u1", "annual", LocalDate(2026, 9, 4), LocalDate(2026, 9, 4))
        assertEquals(1, LeaveCalculator.usageDays(usage))
    }

    @Test
    fun `usageDays for two nights three days trip is three days regardless of weekend`() {
        // 8/30~9/2 휴가: 주말 포함 여부와 무관하게 4일 차감 (스펙 3.2.1절 예시)
        val usage = LeaveUsage("u2", "annual", LocalDate(2026, 8, 30), LocalDate(2026, 9, 2))
        assertEquals(4, LeaveCalculator.usageDays(usage))
    }

    @Test
    fun `totalGranted sums multiple grants of the same type only`() {
        val grants = listOf(
            LeaveGrant("g1", "annual", 5, LocalDate(2026, 1, 1)),
            LeaveGrant("g2", "annual", 3, LocalDate(2026, 3, 1)),
            LeaveGrant("g3", "reward", 10, LocalDate(2026, 2, 1)),
        )
        assertEquals(8, LeaveCalculator.totalGranted("annual", grants))
    }

    @Test
    fun `cap not reached - cappedGranted equals total and overflow is zero`() {
        val grants = listOf(LeaveGrant("g1", "reward", 15, LocalDate(2026, 1, 1)))
        assertEquals(15, LeaveCalculator.cappedGranted(reward, grants))
        assertEquals(0, LeaveCalculator.overflowAmount(reward, grants))
        assertEquals(2, LeaveCalculator.remainingCapCapacity(reward, grants))
    }

    @Test
    fun `cap reached exactly - no overflow`() {
        val grants = listOf(LeaveGrant("g1", "reward", 17, LocalDate(2026, 1, 1)))
        assertEquals(17, LeaveCalculator.cappedGranted(reward, grants))
        assertEquals(0, LeaveCalculator.overflowAmount(reward, grants))
        assertEquals(0, LeaveCalculator.remainingCapCapacity(reward, grants))
    }

    @Test
    fun `cap exceeded - spec example 19 granted 17 cap gives 17 recognized and 2 overflow`() {
        val grants = listOf(
            LeaveGrant("g1", "reward", 12, LocalDate(2026, 1, 1)),
            LeaveGrant("g2", "reward", 7, LocalDate(2026, 5, 1)),
        )
        assertEquals(17, LeaveCalculator.cappedGranted(reward, grants))
        assertEquals(2, LeaveCalculator.overflowAmount(reward, grants))
        assertEquals(0, LeaveCalculator.remainingCapCapacity(reward, grants))
    }

    @Test
    fun `REJECT overflow behavior still reports overflowAmount - conversion decision is the caller's job`() {
        val rejectType = LeaveType("special", "특별휴가", cap = 5, overflowBehavior = OverflowBehavior.REJECT)
        val grants = listOf(LeaveGrant("g1", "special", 8, LocalDate(2026, 1, 1)))
        // overflowAmount 자체는 상한 초과량만 계산한다; 이를 전투휴무로 전환할지 여부는
        // CombatRestCalculator가 overflowBehavior를 보고 판단한다.
        assertEquals(3, LeaveCalculator.overflowAmount(rejectType, grants))
    }

    @Test
    fun `no cap means unlimited - remainingCapCapacity is null`() {
        val grants = listOf(LeaveGrant("g1", "annual", 100, LocalDate(2026, 1, 1)))
        assertEquals(0, LeaveCalculator.overflowAmount(annual, grants))
        assertNull(LeaveCalculator.remainingCapCapacity(annual, grants))
    }

    @Test
    fun `remaining subtracts total used from capped granted`() {
        val grants = listOf(LeaveGrant("g1", "annual", 10, LocalDate(2026, 1, 1)))
        val usages = listOf(
            LeaveUsage("u1", "annual", LocalDate(2026, 3, 1), LocalDate(2026, 3, 2)), // 2일
            LeaveUsage("u2", "annual", LocalDate(2026, 5, 5), LocalDate(2026, 5, 5)), // 1일
        )
        assertEquals(7, LeaveCalculator.remaining(annual, grants, usages))
    }
}
