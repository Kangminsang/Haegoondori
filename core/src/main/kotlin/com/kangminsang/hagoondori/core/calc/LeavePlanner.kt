package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightRecord
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** 휴가 계획에서 날짜에 칠하는 종류: 외박 또는 휴가 종류 하나. */
sealed interface PlanKind {
    data object Overnight : PlanKind
    data class Leave(val typeId: String) : PlanKind
}

/** 날짜가 정해진 계획 구간(같은 종류가 이어진 날들). [end]는 포함(inclusive)이다. */
data class PlanSegment(val kind: PlanKind, val start: LocalDate, val end: LocalDate) {
    val days: Int get() = start.daysUntil(end) + 1
}

/** 새로 저장할 휴가 사용 기록. id는 저장할 때 Repository가 붙인다. */
data class LeaveUsageDraft(val leaveTypeId: String, val startDate: LocalDate, val endDate: LocalDate, val label: String?)

/** 새로 저장할 외박 기록. id는 저장할 때 Repository가 붙인다. */
data class OvernightDraft(val date: LocalDate, val endDate: LocalDate, val memo: String?)

/**
 * 이미 저장된 출타 하나(맞닿은 휴가·외박 기록 묶음). [dayKinds]는 [start]부터 하루씩의 종류다.
 */
data class PlannedTrip(val start: LocalDate, val end: LocalDate, val dayKinds: List<PlanKind?>) {
    val totalDays: Int get() = start.daysUntil(end) + 1

    /** 날짜순으로 이어진 같은 종류 구간. */
    val segments: List<PlanSegment> get() = LeavePlanner.segmentsOf(start, dayKinds)
}

/**
 * 달력의 "휴가 계획": 전체 기간을 고르고, 그 안의 날마다 외박·휴가 종류를 칠한다.
 *
 * 계획은 [start]~[end]의 날마다 하나씩인 종류 목록(`List<PlanKind?>`, null = 아직 배분하지 않은 날)이다.
 * 한 출타는 DB에 따로 묶음이 없고, 지금처럼 맞닿은 [LeaveUsage]/[OvernightRecord] 여러 개로 저장된다
 * ([OutingPeriodCalculator]가 맞닿은 기록을 한 기간으로 합친다). 같은 종류가 이어진 날들([segmentsOf])이
 * 기록 하나가 되므로, 포상-연가-포상처럼 떨어진 같은 종류는 기록 두 개가 된다. 이 객체는 순수 계산만 한다.
 * 저장은 앱이 [Changes]를 Repository에 반영해서 한다.
 */
object LeavePlanner {

    /** 기간의 일수(양 끝 포함). */
    fun dayCount(start: LocalDate, end: LocalDate): Int = start.daysUntil(end) + 1

    /** 종류별로 칠한 일수. 배분하지 않은 날은 세지 않는다. */
    fun counts(dayKinds: List<PlanKind?>): Map<PlanKind, Int> =
        dayKinds.filterNotNull().groupingBy { it }.eachCount()

    /** 아직 배분하지 않은 날 수. */
    fun unassigned(dayKinds: List<PlanKind?>): Int = dayKinds.count { it == null }

    /** [start]부터 하루씩인 [dayKinds]를 같은 종류가 이어진 구간으로 묶는다. 배분하지 않은 날은 건너뛴다. */
    fun segmentsOf(start: LocalDate, dayKinds: List<PlanKind?>): List<PlanSegment> {
        val result = mutableListOf<PlanSegment>()
        dayKinds.forEachIndexed { i, kind ->
            if (kind == null) return@forEachIndexed
            val day = start.plus(i, DateTimeUnit.DAY)
            val last = result.lastOrNull()
            if (last != null && last.kind == kind && last.end.plus(1, DateTimeUnit.DAY) == day) {
                result[result.lastIndex] = last.copy(end = day)
            } else {
                result += PlanSegment(kind, day, day)
            }
        }
        return result
    }

    /**
     * 기간 안의 기존 기록을 날마다 펼친 종류(수정할 때 미리 채우기). 기록에 있던 배치를 그대로 둔다.
     * 하루를 여러 기록이 덮으면 먼저 시작한 기록 하나만 쓴다. 기록이 없는 날은 null.
     */
    fun dayKindsOf(
        start: LocalDate,
        end: LocalDate,
        usages: List<LeaveUsage>,
        overnights: List<OvernightRecord>,
    ): List<PlanKind?> {
        val records = recordSegments(usages, overnights)
            .filter { it.start <= end && it.end >= start }
            .sortedWith(compareBy({ it.start }, { it.end }))
        return List(dayCount(start, end)) { i ->
            val day = start.plus(i, DateTimeUnit.DAY)
            records.firstOrNull { day >= it.start && day <= it.end }?.kind
        }
    }

    /**
     * 자동 배분. [rows]는 (종류, 이 계획에 쓸 수 있는 잔여 일수)이고 화면의 행 순서다. 배분하지 않은 날만 채우고,
     * 이미 칠한 날은 그대로 둔다. 각 휴가 행은 잔여에서 이미 칠한 일수를 뺀 만큼, 위에서부터 채운다. 외박은 채우지
     * 않는다. 잔여를 다 써도 남는 날은 첫 휴가 행에 몰아 둔다(초과 경고는 화면이 띄운다). 휴가 행이 없으면 첫 행에 몬다.
     * 채우는 날은 날짜순으로 행 순서대로 들어간다 - 아무것도 칠하지 않았다면 기간 전체가 행 순서대로 이어진다.
     */
    fun autoFill(dayKinds: List<PlanKind?>, rows: List<Pair<PlanKind, Int>>): List<PlanKind?> {
        val empty = dayKinds.indices.filter { dayKinds[it] == null }
        if (empty.isEmpty() || rows.isEmpty()) return dayKinds
        val painted = counts(dayKinds)
        var left = empty.size
        val fill = rows.map { (kind, remaining) ->
            if (kind is PlanKind.Overnight) 0
            else minOf((remaining - (painted[kind] ?: 0)).coerceAtLeast(0), left).also { left -= it }
        }.toMutableList()
        if (left > 0) {
            val target = rows.indexOfFirst { it.first is PlanKind.Leave }.takeIf { it >= 0 } ?: 0
            fill[target] += left
        }
        val order = rows.flatMapIndexed { i, (kind, _) -> List(fill[i]) { kind } }
        val result = dayKinds.toMutableList()
        empty.forEachIndexed { n, index -> result[index] = order[n] }
        return result
    }

    /** [adjust] 결과: 바뀐 기간과 그 기간의 날마다 종류. */
    data class Adjusted(val start: LocalDate, val end: LocalDate, val dayKinds: List<PlanKind?>)

    /**
     * 기간을 다 고른 뒤 달력 칸을 눌렀을 때 기간과 배치를 함께 고친다. [dayKinds]는 [start]부터 하루씩의 종류다.
     *
     * [brush](패널에서 고른 종류)가 있으면:
     * - 기간 안의 날 → 그날을 [brush]로 칠한다. 이미 [brush]면 시작일·종료일은 기간에서 빼고, 안쪽 날은 비운다(null).
     * - 기간 바깥 → 그날까지 늘리고, 늘어난 날은 [brush]로 칠한다.
     *
     * [brush]가 없으면:
     * - 시작일보다 앞·종료일보다 뒤 → 그날까지 늘린다. 늘어난 날은 맞닿은 끝 날과 같은 종류다(그 날이 비었으면 빈 날).
     * - 시작일·종료일 → 그 끝에서 하루를 뺀다.
     * - 기간 안쪽 → 더 가까운 끝을 그날로 옮긴다(거리가 같으면 종료일을 옮긴다).
     *
     * 기간이 비게 되면(하루짜리 기간의 그날을 빼면) null.
     */
    fun adjust(
        start: LocalDate,
        end: LocalDate,
        dayKinds: List<PlanKind?>,
        tapped: LocalDate,
        brush: PlanKind? = null,
    ): Adjusted? {
        require(end >= start) { "end는 start보다 앞설 수 없다" }
        val total = dayCount(start, end)
        require(dayKinds.size == total) { "dayKinds는 기간의 일수와 같아야 한다" }
        return when {
            tapped < start -> {
                val added = tapped.daysUntil(start)
                Adjusted(tapped, end, List(added) { brush ?: dayKinds.first() } + dayKinds)
            }
            tapped > end -> {
                val added = end.daysUntil(tapped)
                Adjusted(start, tapped, dayKinds + List(added) { brush ?: dayKinds.last() })
            }
            brush != null -> {
                val index = start.daysUntil(tapped)
                when {
                    dayKinds[index] != brush -> Adjusted(start, end, dayKinds.toMutableList().also { it[index] = brush })
                    total == 1 -> null
                    index == 0 -> Adjusted(start.plus(1, DateTimeUnit.DAY), end, dayKinds.drop(1))
                    index == total - 1 -> Adjusted(start, end.minus(1, DateTimeUnit.DAY), dayKinds.dropLast(1))
                    else -> Adjusted(start, end, dayKinds.toMutableList().also { it[index] = null })
                }
            }
            total == 1 -> null
            else -> {
                val fromStart = start.daysUntil(tapped)
                val fromEnd = tapped.daysUntil(end)
                // 끝을 누르면 하루, 안쪽을 누르면 가까운 끝에서 그날까지 뺀다.
                val trimFront = tapped == start || (tapped != end && fromStart < fromEnd)
                if (trimFront) {
                    val removed = maxOf(fromStart, 1)
                    Adjusted(start.plus(removed, DateTimeUnit.DAY), end, dayKinds.drop(removed))
                } else {
                    val removed = maxOf(fromEnd, 1)
                    Adjusted(start, end.minus(removed, DateTimeUnit.DAY), dayKinds.dropLast(removed))
                }
            }
        }
    }

    /**
     * 이미 있던 출타([originalStart]~[originalEnd])를 [start]~[end]로 고쳐 저장하는 변경. 두 기간을 합친 범위의
     * 기록을 정리하므로, 줄여서 빠진 날의 기록도 지워진다. 원래 기간 밖의 기록은 건드리지 않는다.
     * 날짜는 [dayKinds]의 구간([segmentsOf])대로 들어간다.
     *
     * 원래 기간이 없거나, 새 기간과 겹치지도 맞닿지도 않으면(사이의 무관한 기록을 지우지 않도록) [apply]와 같다.
     */
    fun applyEdit(
        originalStart: LocalDate?,
        originalEnd: LocalDate?,
        start: LocalDate,
        end: LocalDate,
        dayKinds: List<PlanKind?>,
        usages: List<LeaveUsage>,
        overnights: List<OvernightRecord>,
    ): Changes {
        require(end >= start) { "end는 start보다 앞설 수 없다" }
        if (originalStart == null || originalEnd == null) return apply(start, end, dayKinds, usages, overnights)
        val touches = originalStart <= end.plus(1, DateTimeUnit.DAY) && originalEnd >= start.minus(1, DateTimeUnit.DAY)
        if (!touches) return apply(start, end, dayKinds, usages, overnights)
        return replace(minOf(originalStart, start), maxOf(originalEnd, end), segmentsWithin(start, end, dayKinds), usages, overnights)
    }

    /** [apply] 결과. 지울 기록과 새로 넣을 기록(초안). */
    data class Changes(
        val deleteUsages: List<LeaveUsage>,
        val deleteOvernights: List<OvernightRecord>,
        val addUsages: List<LeaveUsageDraft>,
        val addOvernights: List<OvernightDraft>,
    )

    /**
     * [start]~[end] 기간을 [dayKinds]로 바꾸는 변경을 계산한다.
     * - 기간과 겹치는 기존 기록은 지운다. 기간 밖으로 삐져나온 앞·뒤 조각은 잘라서 다시 넣는다(label·memo 유지).
     * - [dayKinds]의 구간([segmentsOf])마다 새 기록을 넣는다. 떨어진 같은 종류는 따로 들어간다.
     * - 새 구간과 종류·날짜가 똑같은 기존 기록은 지우지도 새로 넣지도 않고 그대로 둔다(label·memo·id 유지).
     *
     * [dayKinds]가 모두 null이면 기간 안의 기록을 모두 지우는 셈이다(출타 삭제).
     */
    fun apply(
        start: LocalDate,
        end: LocalDate,
        dayKinds: List<PlanKind?>,
        usages: List<LeaveUsage>,
        overnights: List<OvernightRecord>,
    ): Changes {
        require(end >= start) { "end는 start보다 앞설 수 없다" }
        return replace(start, end, segmentsWithin(start, end, dayKinds), usages, overnights)
    }

    private fun segmentsWithin(start: LocalDate, end: LocalDate, dayKinds: List<PlanKind?>): List<PlanSegment> {
        require(dayKinds.size == dayCount(start, end)) { "dayKinds는 기간의 일수와 같아야 한다" }
        return segmentsOf(start, dayKinds)
    }

    /** [start]~[end]와 겹치는 기록을 [segments](이 범위 안)로 바꾸는 변경. [apply] 설명 참고. */
    private fun replace(
        start: LocalDate,
        end: LocalDate,
        segments: List<PlanSegment>,
        usages: List<LeaveUsage>,
        overnights: List<OvernightRecord>,
    ): Changes {
        val newSegments = segments.toMutableList()
        val dayBefore = start.minus(1, DateTimeUnit.DAY)
        val dayAfter = end.plus(1, DateTimeUnit.DAY)

        val deleteUsages = mutableListOf<LeaveUsage>()
        val addUsages = mutableListOf<LeaveUsageDraft>()
        usages.filter { it.startDate <= end && it.endDate >= start }.forEach { usage ->
            val same = PlanSegment(PlanKind.Leave(usage.leaveTypeId), usage.startDate, usage.endDate)
            if (newSegments.remove(same)) return@forEach
            deleteUsages += usage
            if (usage.startDate < start) addUsages += LeaveUsageDraft(usage.leaveTypeId, usage.startDate, dayBefore, usage.label)
            if (usage.endDate > end) addUsages += LeaveUsageDraft(usage.leaveTypeId, dayAfter, usage.endDate, usage.label)
        }

        val deleteOvernights = mutableListOf<OvernightRecord>()
        val addOvernights = mutableListOf<OvernightDraft>()
        overnights.filter { it.date <= end && it.endDate >= start }.forEach { record ->
            if (newSegments.remove(PlanSegment(PlanKind.Overnight, record.date, record.endDate))) return@forEach
            deleteOvernights += record
            if (record.date < start) addOvernights += OvernightDraft(record.date, dayBefore, record.memo)
            if (record.endDate > end) addOvernights += OvernightDraft(dayAfter, record.endDate, record.memo)
        }

        newSegments.forEach { segment ->
            when (val kind = segment.kind) {
                PlanKind.Overnight -> addOvernights += OvernightDraft(segment.start, segment.end, null)
                is PlanKind.Leave -> addUsages += LeaveUsageDraft(kind.typeId, segment.start, segment.end, null)
            }
        }
        return Changes(deleteUsages, deleteOvernights, addUsages, addOvernights)
    }

    /**
     * [date]를 포함하는 출타(맞닿은 실제 기록 묶음). 계산상의 다음 외박 예정일은 넣지 않는다.
     * 그날 휴가·외박 기록이 없으면 null.
     */
    fun tripAround(
        date: LocalDate,
        usages: List<LeaveUsage>,
        overnights: List<OvernightRecord>,
    ): PlannedTrip? {
        val range = OutingPeriodCalculator.recordedRanges(usages, overnights).firstOrNull { date in it } ?: return null
        val dayKinds = dayKindsOf(range.start, range.endInclusive, usages, overnights)
        if (dayKinds.all { it == null }) return null
        return PlannedTrip(range.start, range.endInclusive, dayKinds)
    }

    private fun recordSegments(usages: List<LeaveUsage>, overnights: List<OvernightRecord>): List<PlanSegment> =
        usages.map { PlanSegment(PlanKind.Leave(it.leaveTypeId), it.startDate, it.endDate) } +
            overnights.map { PlanSegment(PlanKind.Overnight, it.date, it.endDate) }
}
