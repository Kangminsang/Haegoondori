package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.CombatRestGrant
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.OverflowBehavior
import kotlinx.datetime.daysUntil

/**
 * 전투휴무 잔여를 두 원천으로 나눠 보여주기 위한 요약(3.3.3절):
 * "전투휴무 5일 (전환 2일 + 부여 3일)"처럼, 포상 기록을 수정했을 때 숫자가 왜
 * 바뀌는지 사용자가 이해할 수 있어야 한다.
 */
data class CombatRestSummary(
    /** 포상휴가 상한 초과분에서 자동 전환된 양 (계산값, 저장하지 않음) */
    val convertedFromLeave: Int,
    /** [CombatRestGrant]로 직접 부여받은 양 (저장값) */
    val directGranted: Int,
    val totalUsed: Int,
) {
    val totalGranted: Int get() = convertedFromLeave + directGranted
    val remaining: Int get() = totalGranted - totalUsed
}

/**
 * 혼합 소모형 자원(전투휴무) 계산 (스펙 3.3절).
 *
 * 총 부여량 = max(0, 포상 부여 누적 − 포상 상한) [자동 전환분, 계산만 함]
 *           + [CombatRestGrant] 합계 [직접 부여분, 저장값]
 */
object CombatRestCalculator {

    /**
     * 포상휴가 초과분 중 전투휴무로 전환되는 양. [rewardLeaveType]의
     * [LeaveType.overflowBehavior]가 [OverflowBehavior.CONVERT_TO_COMBAT_REST]가 아니면
     * (REJECT거나 NONE이면) 전환이 일어나지 않으므로 0을 반환한다.
     */
    fun convertedFromLeaveOverflow(rewardLeaveType: LeaveType, leaveGrants: List<LeaveGrant>): Int {
        if (rewardLeaveType.overflowBehavior != OverflowBehavior.CONVERT_TO_COMBAT_REST) return 0
        return LeaveCalculator.overflowAmount(rewardLeaveType, leaveGrants)
    }

    fun totalUsed(usages: List<CombatRestUsage>): Int =
        usages.sumOf { it.startDate.daysUntil(it.endDate) + 1 }

    fun summarize(
        rewardLeaveType: LeaveType,
        leaveGrants: List<LeaveGrant>,
        restGrants: List<CombatRestGrant>,
        restUsages: List<CombatRestUsage>,
    ): CombatRestSummary = CombatRestSummary(
        convertedFromLeave = convertedFromLeaveOverflow(rewardLeaveType, leaveGrants),
        directGranted = restGrants.sumOf { it.days },
        totalUsed = totalUsed(restUsages),
    )

    /**
     * [summarize]의 다중 휴가종류 버전. 휴가 종류는 사용자가 F1/F7에서 자유롭게
     * 정의하므로("포상휴가"라는 이름이 고정되어 있지 않으므로), 어떤 것이 전투휴무로
     * 전환되는 종류인지는 이름이 아니라 [LeaveType.overflowBehavior]로 판별한다.
     * 그런 종류가 여러 개여도(드물지만 가능) 전환분을 전부 합산한다. 하나도 없으면
     * 전환분은 0이고, 직접 부여분만 남는다.
     */
    fun summarizeAcrossLeaveTypes(
        leaveTypes: List<LeaveType>,
        leaveGrants: List<LeaveGrant>,
        restGrants: List<CombatRestGrant>,
        restUsages: List<CombatRestUsage>,
    ): CombatRestSummary {
        val converted = leaveTypes
            .filter { it.overflowBehavior == OverflowBehavior.CONVERT_TO_COMBAT_REST }
            .sumOf { convertedFromLeaveOverflow(it, leaveGrants) }
        return CombatRestSummary(
            convertedFromLeave = converted,
            directGranted = restGrants.sumOf { it.days },
            totalUsed = totalUsed(restUsages),
        )
    }
}
