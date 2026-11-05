package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightRecord
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** 휴가 계획에서 일수를 나눠 받는 종류: 외박 또는 휴가 종류 하나. */
sealed interface PlanKind {
    data object Overnight : PlanKind
    data class Leave(val typeId: String) : PlanKind
}

/** 계획 한 행: [kind]에 [days]일을 쓴다. */
data class PlanPart(val kind: PlanKind, val days: Int)

/** 날짜가 정해진 계획 구간. [end]는 포함(inclusive)이다. */
data class PlanSegment(val kind: PlanKind, val start: LocalDate, val end: LocalDate) {
    val days: Int get() = start.daysUntil(end) + 1
}

/** 새로 저장할 휴가 사용 기록. id는 저장할 때 Repository가 붙인다. */
data class LeaveUsageDraft(val leaveTypeId: String, val startDate: LocalDate, val endDate: LocalDate, val label: String?)

/** 새로 저장할 외박 기록. id는 저장할 때 Repository가 붙인다. */
data class OvernightDraft(val date: LocalDate, val endDate: LocalDate, val memo: String?)

/**
 * 이미 저장된 출타 하나(맞닿은 휴가·외박 기록 묶음). [parts]는 날짜순으로 이어진 종류별 일수다.
 */
data class PlannedTrip(val start: LocalDate, val end: LocalDate, val parts: List<PlanPart>) {
    val totalDays: Int get() = start.daysUntil(end) + 1
}

/**
 * 달력의 "휴가 계획": 전체 기간을 먼저 고르고, 그 일수를 외박과 휴가 종류별로 나눈다.
 *
 * 한 출타는 DB에 따로 묶음이 없고, 지금처럼 맞닿은 [LeaveUsage]/[OvernightRecord] 여러 개로 저장된다
 * ([OutingPeriodCalculator]가 맞닿은 기록을 한 기간으로 합친다). 이 객체는 그 기록들을 기간 단위로
 * 나누고 바꾸는 순수 계산만 한다. 저장은 앱이 [Changes]를 Repository에 반영해서 한다.
 */
object LeavePlanner {

    /** 기간의 일수(양 끝 포함). */
    fun dayCount(start: LocalDate, end: LocalDate): Int = start.daysUntil(end) + 1

    /** 일수가 0 이하인 행은 건너뛰고, [parts] 순서대로 [start]부터 날짜를 이어 붙인다. */
    fun layout(start: LocalDate, parts: List<PlanPart>): List<PlanSegment> {
        var cursor = start
        return parts.filter { it.days > 0 }.map { part ->
            val end = cursor.plus(part.days - 1, DateTimeUnit.DAY)
            PlanSegment(part.kind, cursor, end).also { cursor = end.plus(1, DateTimeUnit.DAY) }
        }
    }

    /**
     * 자동 배분. [rows]는 (종류, 잔여 일수)이고 화면의 행 순서다. 외박은 0일로 두고, 휴가 행을 위에서부터
     * 잔여만큼 채운다. 잔여를 다 써도 남는 일수는 첫 휴가 행에 몰아 둔다(초과 경고는 화면이 띄운다).
     * 결과의 합은 항상 [totalDays]다. 휴가 행이 없으면 첫 행에 몬다.
     */
    fun autoFill(totalDays: Int, rows: List<Pair<PlanKind, Int>>): List<PlanPart> {
        var left = totalDays
        val days = rows.map { (kind, remaining) ->
            if (kind is PlanKind.Overnight) 0
            else minOf(remaining.coerceAtLeast(0), left).also { left -= it }
        }.toMutableList()
        if (left > 0 && rows.isNotEmpty()) {
            val target = rows.indexOfFirst { it.first is PlanKind.Leave }.takeIf { it >= 0 } ?: 0
            days[target] += left
        }
        return rows.mapIndexed { i, (kind, _) -> PlanPart(kind, days[i]) }
    }

    /**
     * 기간 안에 이미 있는 기록을 날짜순으로 잘라, 이어진 같은 종류끼리 묶은 일수(수정할 때 패널 미리 채우기).
     * 하루를 여러 기록이 덮으면 먼저 시작한 기록 하나만 센다. 기록이 없는 날은 세지 않는다.
     */
    fun existingParts(
        start: LocalDate,
        end: LocalDate,
        usages: List<LeaveUsage>,
        overnights: List<OvernightRecord>,
    ): List<PlanPart> {
        val records = recordSegments(usages, overnights)
            .filter { it.start <= end && it.end >= start }
            .sortedWith(compareBy({ it.start }, { it.end }))
        val result = mutableListOf<PlanPart>()
        var day = start
        while (day <= end) {
            records.firstOrNull { day >= it.start && day <= it.end }?.let { covering ->
                val last = result.lastOrNull()
                if (last != null && last.kind == covering.kind) result[result.lastIndex] = last.copy(days = last.days + 1)
                else result += PlanPart(covering.kind, 1)
            }
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return result
    }

    /** [apply] 결과. 지울 기록과 새로 넣을 기록(초안). */
    data class Changes(
        val deleteUsages: List<LeaveUsage>,
        val deleteOvernights: List<OvernightRecord>,
        val addUsages: List<LeaveUsageDraft>,
        val addOvernights: List<OvernightDraft>,
    )

    /**
     * [start]~[end] 기간을 [parts]로 바꾸는 변경을 계산한다.
     * - 기간과 겹치는 기존 기록은 지운다. 기간 밖으로 삐져나온 앞·뒤 조각은 잘라서 다시 넣는다(label·memo 유지).
     * - [parts]를 [layout]한 새 구간을 넣는다.
     * - 새 구간과 종류·날짜가 똑같은 기존 기록은 지우지도 새로 넣지도 않고 그대로 둔다(label·memo·id 유지).
     *
     * [parts]가 비어 있으면 기간 안의 기록을 모두 지우는 셈이다(출타 삭제).
     */
    fun apply(
        start: LocalDate,
        end: LocalDate,
        parts: List<PlanPart>,
        usages: List<LeaveUsage>,
        overnights: List<OvernightRecord>,
    ): Changes {
        require(end >= start) { "end는 start보다 앞설 수 없다" }
        val newSegments = layout(start, parts).toMutableList()
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
        val parts = existingParts(range.start, range.endInclusive, usages, overnights)
        if (parts.isEmpty()) return null
        return PlannedTrip(range.start, range.endInclusive, parts)
    }

    private fun recordSegments(usages: List<LeaveUsage>, overnights: List<OvernightRecord>): List<PlanSegment> =
        usages.map { PlanSegment(PlanKind.Leave(it.leaveTypeId), it.startDate, it.endDate) } +
            overnights.map { PlanSegment(PlanKind.Overnight, it.date, it.endDate) }
}
