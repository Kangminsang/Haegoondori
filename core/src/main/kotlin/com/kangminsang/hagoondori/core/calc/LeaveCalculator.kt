package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import kotlinx.datetime.LocalDate
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
        type.fixedDays?.let { return it }
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
        if (type.fixedDays != null) return null // 부여를 기록하지 않는 종류라 "받기 전 경고"가 의미 없다
        val cap = type.cap ?: return null
        val total = totalGranted(type.id, grants)
        return maxOf(0, cap - total)
    }

    /**
     * 부여 하나의 현재 상태. [recognizedDays]는 상한을 적용해 인정된 일수, [usedDays]는 사용 기록이
     * 이 부여에서 차감된 일수, [expiredDays]는 유효 기간이 지나 소멸한 일수, [remainingDays]는
     * 아직 쓸 수 있는 일수다.
     */
    data class GrantStatus(
        val grant: LeaveGrant,
        val recognizedDays: Int,
        val usedDays: Int,
        val expiredDays: Int,
        val remainingDays: Int,
    )

    /** 한 휴가 종류의 요약. [grants]는 부여가 있는 종류에서만 채워진다(정기휴가처럼 총량 고정이면 비어 있음). */
    data class Summary(
        val granted: Int,
        val used: Int,
        val expired: Int,
        val remaining: Int,
        val grants: List<GrantStatus>,
    )

    /**
     * 유효 기간을 반영한 요약. 사용 기록은 날짜순으로, 사용 시작일에 유효한 부여 중 **유효 기간이
     * 가장 빨리 끝나는 것부터** 차감한다(기한 없는 부여는 마지막). 상한이 있으면 부여를 받은
     * 날짜순으로 상한까지만 인정한다. 유효 기간이 없는 부여만 있으면 결과는 기존
     * [remaining]과 같다.
     */
    fun summarize(
        type: LeaveType,
        grants: List<LeaveGrant>,
        usages: List<LeaveUsage>,
        today: LocalDate,
    ): Summary {
        val used = totalUsed(type.id, usages)
        type.fixedDays?.let { return Summary(it, used, 0, it - used, emptyList()) }

        val ordered = grants.filter { it.leaveTypeId == type.id }.sortedWith(compareBy({ it.grantedDate }, { it.id }))
        var capLeft = type.cap ?: Int.MAX_VALUE
        val recognized = ordered.associateWith { grant ->
            val days = minOf(grant.days, capLeft)
            capLeft -= days
            days
        }
        val left = recognized.toMutableMap()
        var uncovered = 0
        usages.filter { it.leaveTypeId == type.id }.sortedBy { it.startDate }.forEach { usage ->
            var need = usageDays(usage)
            val candidates = ordered
                .filter { (it.expiryDate == null || it.expiryDate >= usage.startDate) && (left[it] ?: 0) > 0 }
                .sortedWith(compareBy({ it.expiryDate == null }, { it.expiryDate }, { it.grantedDate }))
            for (grant in candidates) {
                if (need == 0) break
                val take = minOf(need, left.getValue(grant))
                left[grant] = left.getValue(grant) - take
                need -= take
            }
            uncovered += need
        }

        val statuses = ordered.map { grant ->
            val rec = recognized.getValue(grant)
            val leftover = left.getValue(grant)
            val expired = grant.expiryDate != null && grant.expiryDate < today
            GrantStatus(
                grant = grant,
                recognizedDays = rec,
                usedDays = rec - leftover,
                expiredDays = if (expired) leftover else 0,
                remainingDays = if (expired) 0 else leftover,
            )
        }
        return Summary(
            granted = recognized.values.sum(),
            used = used,
            expired = statuses.sumOf { it.expiredDays },
            remaining = statuses.sumOf { it.remainingDays } - uncovered,
            grants = statuses,
        )
    }
}
