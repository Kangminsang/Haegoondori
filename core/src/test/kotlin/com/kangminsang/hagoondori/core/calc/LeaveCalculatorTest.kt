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
    fun `fixedDays type ignores grants - total is the fixed amount and remaining subtracts usage`() {
        val regular = LeaveType("regular", "정기휴가", cap = null, fixedDays = 27)
        val usages = listOf(LeaveUsage("u", "regular", LocalDate(2026, 9, 1), LocalDate(2026, 9, 10)))
        assertEquals(27, LeaveCalculator.cappedGranted(regular, emptyList()))
        assertEquals(17, LeaveCalculator.remaining(regular, emptyList(), usages))
        assertNull(LeaveCalculator.remainingCapCapacity(regular, emptyList()))
    }

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

    // ---- 유효 기간 (summarize) ----

    private val today = LocalDate(2026, 9, 19)
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate(y, m, day)
    private fun grant(id: String, days: Int, granted: LocalDate, expiry: LocalDate? = null) =
        LeaveGrant(id, "consolation", days, granted, null, expiry)
    private val consolation = LeaveType("consolation", "위로휴가", cap = null)
    private fun use(id: String, start: LocalDate, end: LocalDate = start) = LeaveUsage(id, "consolation", start, end)

    @Test
    fun `summarize without expiry equals the old remaining`() {
        val grants = listOf(grant("g1", 3, d(1, 1)), grant("g2", 2, d(2, 1)))
        val usages = listOf(use("u", d(3, 1), d(3, 2)))
        val s = LeaveCalculator.summarize(consolation, grants, usages, today)
        assertEquals(5, s.granted)
        assertEquals(2, s.used)
        assertEquals(0, s.expired)
        assertEquals(LeaveCalculator.remaining(consolation, grants, usages), s.remaining)
    }

    @Test
    fun `unused days of an expired grant are lost`() {
        val grants = listOf(grant("g1", 3, d(1, 1), expiry = d(6, 30)), grant("g2", 2, d(2, 1)))
        val s = LeaveCalculator.summarize(consolation, grants, emptyList(), today)
        assertEquals(3, s.expired)
        assertEquals(2, s.remaining)
        assertEquals(3, s.grants.first { it.grant.id == "g1" }.expiredDays)
    }

    @Test
    fun `usage consumes the earliest expiring grant first`() {
        val grants = listOf(grant("late", 2, d(1, 1), expiry = d(12, 31)), grant("soon", 2, d(1, 2), expiry = d(10, 31)))
        val s = LeaveCalculator.summarize(consolation, grants, listOf(use("u", d(8, 1), d(8, 2))), today)
        assertEquals(0, s.grants.first { it.grant.id == "soon" }.remainingDays)
        assertEquals(2, s.grants.first { it.grant.id == "late" }.remainingDays)
    }

    @Test
    fun `a grant already expired on the usage date is not used`() {
        val grants = listOf(grant("old", 2, d(1, 1), expiry = d(3, 31)), grant("new", 2, d(2, 1)))
        val s = LeaveCalculator.summarize(consolation, grants, listOf(use("u", d(6, 1))), today)
        assertEquals(1, s.grants.first { it.grant.id == "new" }.usedDays)
        assertEquals(0, s.grants.first { it.grant.id == "old" }.usedDays)
        assertEquals(2, s.expired)
    }

    @Test
    fun `expiring today is still valid - only the day after expires`() {
        val grants = listOf(grant("g", 2, d(1, 1), expiry = today))
        val s = LeaveCalculator.summarize(consolation, grants, emptyList(), today)
        assertEquals(0, s.expired)
        assertEquals(2, s.remaining)
    }

    @Test
    fun `cap recognizes grants in date order and ignores the overflow`() {
        val capped = LeaveType("consolation", "포상휴가", cap = 4, overflowBehavior = OverflowBehavior.CONVERT_TO_COMBAT_REST)
        val grants = listOf(grant("a", 3, d(1, 1)), grant("b", 3, d(2, 1)))
        val s = LeaveCalculator.summarize(capped, grants, emptyList(), today)
        assertEquals(4, s.granted)
        assertEquals(listOf(3, 1), s.grants.map { it.recognizedDays })
    }

    @Test
    fun `future leave is planned, not used`() {
        val grants = listOf(grant("g1", 10, d(1, 1)))
        val s = LeaveCalculator.summarize(consolation, grants, listOf(use("u", d(9, 26), d(9, 28))), today)
        assertEquals(0, s.used)
        assertEquals(10, s.remaining)
        assertEquals(3, s.planned)
        assertEquals(7, s.remainingAfterPlanned)
    }

    @Test
    fun `ongoing leave counts elapsed days as used and the rest as planned`() {
        val grants = listOf(grant("g1", 10, d(1, 1)))
        // 9/18~9/21, 오늘 9/19 -> 18,19일은 사용, 20,21일은 계획
        val s = LeaveCalculator.summarize(consolation, grants, listOf(use("u", d(9, 18), d(9, 21))), today)
        assertEquals(2, s.used)
        assertEquals(8, s.remaining)
        assertEquals(2, s.planned)
        assertEquals(6, s.remainingAfterPlanned)
    }

    @Test
    fun `fixed days leave type also separates planned days`() {
        val regular = LeaveType("consolation", "정기휴가", cap = null, fixedDays = 27)
        val s = LeaveCalculator.summarize(regular, emptyList(), listOf(use("u", d(9, 26), d(9, 28))), today)
        assertEquals(27, s.remaining)
        assertEquals(3, s.planned)
        assertEquals(24, s.remainingAfterPlanned)
    }
}
