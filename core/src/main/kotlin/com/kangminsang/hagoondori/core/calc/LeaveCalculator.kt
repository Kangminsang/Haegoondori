package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import kotlinx.datetime.daysUntil

/**
 * 소모형 자원(휴가) 계산 (스펙 3.2절).
 *
 * [LeaveGrant](부여)와 [LeaveUsage](사용)를 반드시 분리해서 다룬다 — 포상휴가 17일
 * 상한은 **부여 누적** 기준으로 판정되므로, 사용량만 세면 상한 도달 여부를 알 수 없다.
 */
object LeaveCalculator {

    /**
     * 사용 일수. 평일·주말·공휴일을 가리지 않고 균일하게 차감된다(3.2.1절).
     * `endDate`는 포함(inclusive)이므로 당일 신청은 1일, 2박3일은 3일이다.
     */
    fun usageDays(usage: LeaveUsage): Int =
        usage.startDate.daysUntil(usage.endDate) + 1

    /** 특정 휴가 종류의 부여 누적 합계 (상한과 무관하게, 실제로 부여받은 총량). */
    fun totalGranted(leaveTypeId: String, grants: List<LeaveGrant>): Int =
        grants.filter { it.leaveTypeId == leaveTypeId }.sumOf { it.days }

    /**
     * 상한을 적용한 "인정" 부여량. 예: 포상 부여 누적 19일, 상한 17일 → 17일 인정(3.2.2절 예시).
     * cap이 없으면(무제한) 부여 누적 그대로.
     */
    fun cappedGranted(type: LeaveType, grants: List<LeaveGrant>): Int {
        val total = totalGranted(type.id, grants)
        val cap = type.cap ?: return total
        return minOf(total, cap)
    }

    /**
     * 상한 초과분. 예: 부여 누적 19일, 상한 17일 → 2일(3.2.2절 예시).
     * 이 값이 [com.kangminsang.hagoondori.core.model.OverflowBehavior.CONVERT_TO_COMBAT_REST]인
     * 휴가 종류에서 전투휴무로 전환된다(3.3.2절, [CombatRestCalculator] 참고).
     */
    fun overflowAmount(type: LeaveType, grants: List<LeaveGrant>): Int {
        val total = totalGranted(type.id, grants)
        val cap = type.cap ?: return 0
        return maxOf(0, total - cap)
    }

    /** 특정 휴가 종류의 사용 일수 합계. */
    fun totalUsed(leaveTypeId: String, usages: List<LeaveUsage>): Int =
        usages.filter { it.leaveTypeId == leaveTypeId }.sumOf { usageDays(it) }

    /** 잔여 = 인정된 부여량 - 사용량. 음수가 되지는 않는다(과사용은 상위 계층 책임). */
    fun remaining(type: LeaveType, grants: List<LeaveGrant>, usages: List<LeaveUsage>): Int =
        cappedGranted(type, grants) - totalUsed(type.id, usages)

    /**
     * 상한까지 남은 "받을 수 있는" 여유량. 포상을 받기 전에 경고하기 위한 값이다(3.2.3절):
     * "포상 상한까지 2일. 이후 받는 포상은 전투휴무로 전환됩니다" 같은 문구에 쓰인다.
     * cap이 없으면 무제한이므로 null.
     */
    fun remainingCapCapacity(type: LeaveType, grants: List<LeaveGrant>): Int? {
        val cap = type.cap ?: return null
        val total = totalGranted(type.id, grants)
        return maxOf(0, cap - total)
    }
}
