package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightRecord
import com.kangminsang.hagoondori.core.payload.LeaveDeviceLabel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * 이어 붙여 나가는 출타 한 덩어리. 부대에서는 외박에 포상·연가를 붙여 쓰므로 맞닿은
 * 구간을 하나의 기간으로 합쳐 보여준다 (장치 화면 설계서 v2, 7.2절과 같은 규칙).
 *
 * @property parts 종류별 일수. 기간을 넓히는 순서(시작일순)로 나열된다.
 */
data class OutingPeriod(
    val start: LocalDate,
    val end: LocalDate,
    val parts: List<Part>,
) {
    data class Part(val label: String, val days: Int)

    val totalDays: Int get() = start.daysUntil(end) + 1
}

object OutingPeriodCalculator {
    const val OVERNIGHT_LABEL = "외박"

    private data class Segment(val start: LocalDate, val end: LocalDate, val label: String)

    /**
     * 휴가·외박 구간을 합쳐, 종료일이 [today] 이후인 첫 기간(진행 중 포함)을 돌려준다.
     * 없으면 null. [nextOvernightDate]는 아직 기록되지 않은 다음 외박 예정일(하루짜리)이다.
     */
    fun next(
        today: LocalDate,
        leaveUsages: List<LeaveUsage>,
        leaveTypes: List<LeaveType>,
        overnightRecords: List<OvernightRecord>,
        nextOvernightDate: LocalDate?,
    ): OutingPeriod? {
        val typeNames = leaveTypes.associate { it.id to it.name }
        val segments = buildList {
            leaveUsages.forEach { usage ->
                val label = typeNames[usage.leaveTypeId]?.let(LeaveDeviceLabel::forTypeName)
                    ?: usage.label?.takeIf { it.isNotBlank() }
                    ?: "휴가"
                add(Segment(usage.startDate, usage.endDate, label))
            }
            overnightRecords.forEach { add(Segment(it.date, it.endDate, OVERNIGHT_LABEL)) }
            nextOvernightDate?.let { add(Segment(it, it, OVERNIGHT_LABEL)) }
        }
        return merge(segments).firstOrNull { it.end >= today }
    }

    private fun merge(segments: List<Segment>): List<OutingPeriod> {
        val sorted = segments.sortedWith(compareBy({ it.start }, { it.end }))
        val periods = mutableListOf<OutingPeriod>()
        var chunk = mutableListOf<Segment>()
        var chunkEnd: LocalDate? = null

        fun flush() {
            if (chunk.isNotEmpty()) periods += toPeriod(chunk)
            chunk = mutableListOf()
        }

        for (segment in sorted) {
            // 맞닿거나(끝 다음 날 시작) 겹치면 같은 덩어리
            if (chunkEnd != null && segment.start > chunkEnd.plus(1, DateTimeUnit.DAY)) flush()
            chunk += segment
            if (chunkEnd == null || segment.end > chunkEnd) chunkEnd = segment.end
            if (chunk.size == 1) chunkEnd = segment.end
        }
        flush()
        return periods
    }

    /** 각 날은 그날을 덮는 첫 구간(시작일순) 하나에만 센다. 다른 구간에 완전히 덮인 구간은 이름에서도 뺀다. */
    private fun toPeriod(chunk: List<Segment>): OutingPeriod {
        val counts = linkedMapOf<String, Int>()
        var coveredUntil: LocalDate? = null
        for (segment in chunk) {
            val newFrom = coveredUntil?.plus(1, DateTimeUnit.DAY)?.let { maxOf(it, segment.start) } ?: segment.start
            if (segment.end < newFrom) continue
            counts.merge(segment.label, newFrom.daysUntil(segment.end) + 1, Int::plus)
            coveredUntil = segment.end
        }
        return OutingPeriod(
            start = chunk.first().start,
            end = chunk.maxOf { it.end },
            parts = counts.map { (label, days) -> OutingPeriod.Part(label, days) },
        )
    }
}
