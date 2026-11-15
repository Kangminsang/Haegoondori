package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.payload.LeaveDeviceLabel

/**
 * 출타 종류([PlanKind])의 화면 표기. 달력 칸 글자(보기·휴가 계획 모드 공통), 범례, 출타 요약이 모두 여기서
 * 이름을 얻어야 같은 종류가 화면마다 다른 글자로 보이지 않는다.
 */
object PlanKindLabel {
    private const val UNKNOWN_LEAVE = "휴가"

    /** 장치 라벨처럼 두 글자로 줄인 이름: 외박, 포상, 연가(정기휴가). 지워진 종류는 "휴가". */
    fun shortName(kind: PlanKind, types: List<LeaveType>): String = when (kind) {
        PlanKind.Overnight -> OutingPeriodCalculator.OVERNIGHT_LABEL
        is PlanKind.Leave -> typeName(kind, types)?.let(LeaveDeviceLabel::forTypeName) ?: UNKNOWN_LEAVE
    }

    /** 달력 칸의 한 글자: 외박은 "외", 휴가는 [shortName]의 첫 글자(포상 → "포", 정기휴가 → "연"). */
    fun dayLetter(kind: PlanKind, types: List<LeaveType>): String = when (kind) {
        PlanKind.Overnight -> "외"
        is PlanKind.Leave -> shortName(kind, types).take(1)
    }

    /** 범례에 쓰는 원래 이름: 외박, 포상휴가, 정기휴가. */
    fun fullName(kind: PlanKind, types: List<LeaveType>): String = when (kind) {
        PlanKind.Overnight -> OutingPeriodCalculator.OVERNIGHT_LABEL
        is PlanKind.Leave -> typeName(kind, types) ?: UNKNOWN_LEAVE
    }

    data class LegendEntry(val letter: String, val name: String)

    /**
     * [kinds]에 나온 종류마다 (글자, 이름) 하나씩. 휴가는 [types] 순서(지워진 종류는 그 뒤), 외박은 맨 끝이다.
     * 두 종류의 첫 글자가 같아도 그대로 둔다 - 범례의 이름으로 구분한다.
     */
    fun legend(kinds: Collection<PlanKind>, types: List<LeaveType>): List<LegendEntry> {
        val present = kinds.toSet()
        val order = types.map { PlanKind.Leave(it.id) }
        val known = order.filter { it in present }
        val unknown = present.filterIsInstance<PlanKind.Leave>().filter { it !in order }.sortedBy { it.typeId }
        val overnight = listOfNotNull(PlanKind.Overnight.takeIf { it in present })
        return (known + unknown + overnight).map { LegendEntry(dayLetter(it, types), fullName(it, types)) }
    }

    private fun typeName(kind: PlanKind.Leave, types: List<LeaveType>): String? =
        types.firstOrNull { it.id == kind.typeId }?.name
}
