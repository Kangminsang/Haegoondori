package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.ConductPoint
import com.kangminsang.hagoondori.core.model.ConductConversionRecord
import com.kangminsang.hagoondori.core.model.ConductConversionType
import com.kangminsang.hagoondori.core.model.ConductPointKind
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.PassType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.math.abs

/** 양호·과실 점수 합계(0.01점 단위). [net] = 양호 − 과실. */
data class ConductPointSummary(val merit: Int, val demerit: Int) {
    val net: Int get() = merit - demerit
}

/** 점수 입력 검증 결과. [error]가 null이면 통과, [centiPoints]·[reason]은 정리된 값이다. */
data class ConductPointInput(val centiPoints: Int?, val reason: String, val error: String?)

/** 초과근무 시간 입력 검증 결과. [error]가 null이면 통과, [tenthHours]는 0.1시간 단위 정수다. */
data class OvertimeHoursInput(val tenthHours: Int?, val error: String?)

/** 초과근무 시각 입력 검증 결과. [error]가 null이면 통과, [crossesMidnight]는 끝 시각이 다음 날인지다. */
data class OvertimeTimesInput(val tenthHours: Int?, val crossesMidnight: Boolean, val error: String?)

/** 위원회 결과 일수·횟수 입력 검증 결과. [error]가 null이면 통과. */
data class ConversionCountInput(val count: Int?, val error: String?)

/**
 * 점수 전환 현황. 전환은 군기지도위원회가 정한 뒤 그 결과를 [ConductConversionRecord]로 기록할 때 반영된다(휴가 부여·외박 차수 소멸은 따로 직접 기록한다). 양호와 과실은 서로 상쇄하고 유효기간 없이 이월되며, 포상휴가·외박제한으로 전환한 만큼은 사라진다.
 * 그래서 남은 점수는 하나의 [balance](0.01점 단위)로 본다: 양호 합 − 과실 합 − 포상휴가 전환분 + 외박제한 전환분.
 * 양수면 남은 양호, 음수면 남은 과실이다.
 */
data class ConductConversion(
    val merit: Int,
    val demerit: Int,
    val rewardLeaveDaysApplied: Int = 0,
    val overnightRestrictionsApplied: Int = 0,
) {
    val balance: Int
        get() = merit - demerit -
            rewardLeaveDaysApplied * ConductPointCalculator.CENTI_POINTS_PER_REWARD_LEAVE_DAY +
            overnightRestrictionsApplied * ConductPointCalculator.CENTI_POINTS_PER_OVERNIGHT_RESTRICTION

    /** 남은 양호로 위원회 심의를 받을 수 있는 포상휴가 일수(결과를 기록하면 줄어든다). */
    val rewardLeaveDaysPending: Int
        get() = if (balance > 0) balance / ConductPointCalculator.CENTI_POINTS_PER_REWARD_LEAVE_DAY else 0

    /** 남은 과실로 위원회 심의 대상이 되는 외박제한 횟수(결과를 기록하면 줄어든다). */
    val overnightRestrictionsPending: Int
        get() = if (balance < 0) -balance / ConductPointCalculator.CENTI_POINTS_PER_OVERNIGHT_RESTRICTION else 0

    /** 남은 양호에서 포상휴가 1일을 더 받기까지 필요한 양호. 남은 과실이 있으면 그것을 먼저 상쇄해야 하므로 그만큼 더한다. */
    val meritTowardNextRewardDay: Int
        get() = if (balance >= 0) {
            ConductPointCalculator.CENTI_POINTS_PER_REWARD_LEAVE_DAY - balance % ConductPointCalculator.CENTI_POINTS_PER_REWARD_LEAVE_DAY
        } else {
            -balance + ConductPointCalculator.CENTI_POINTS_PER_REWARD_LEAVE_DAY
        }

    /** 외박제한 1회가 더 걸리기까지 남은 과실. 남은 양호가 있으면 그것이 먼저 상쇄되므로 그만큼 더한다. */
    val demeritTowardNextRestriction: Int
        get() = if (balance <= 0) {
            ConductPointCalculator.CENTI_POINTS_PER_OVERNIGHT_RESTRICTION - (-balance) % ConductPointCalculator.CENTI_POINTS_PER_OVERNIGHT_RESTRICTION
        } else {
            balance + ConductPointCalculator.CENTI_POINTS_PER_OVERNIGHT_RESTRICTION
        }
}

object ConductPointCalculator {
    /** 양호 10점마다 포상휴가 1일 (0.01점 단위). */
    const val CENTI_POINTS_PER_REWARD_LEAVE_DAY = 1000

    /** 과실 20점마다 외박제한 1회 (0.01점 단위). */
    const val CENTI_POINTS_PER_OVERNIGHT_RESTRICTION = 2000

    const val MIN_OVERTIME_TENTH_HOURS = 1
    const val MAX_OVERTIME_TENTH_HOURS = 240

    private val HOURS_PATTERN = Regex("""\d{1,2}(\.\d?)?|\.\d""")

    /** 이전 사유 추천에 보여줄 기본 최대 개수. */
    const val DEFAULT_RECENT_REASON_LIMIT = 8

    /**
     * [kind]로 이전에 쓴 사유를 최근 사용 순으로 최대 [limit]개 돌려준다. 공백을 정리해 중복을 없애고,
     * 초과근무 기록도 사용자가 직접 적은 사유는 넣고, 비워 둬서 자동으로 채운 사유("평일 초과근무 …")는 뺀다. 날짜가 같으면 [records]에서 뒤에 있는(나중에 추가된) 것이 먼저다.
     */
    fun recentReasons(records: List<ConductPoint>, kind: ConductPointKind, limit: Int = DEFAULT_RECENT_REASON_LIMIT): List<String> =
        records.withIndex()
            .filter { it.value.kind == kind && !isAutoOvertimeReason(it.value) }
            .sortedWith(compareByDescending<IndexedValue<ConductPoint>> { it.value.date }.thenByDescending { it.index })
            .map { it.value.reason.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(limit)

    /** 위원회가 정한 일수·횟수 입력을 검증한다. 1~[pending]의 정수이고, [unit]은 "일"·"회" 같은 오류 문구용 단위다. */
    fun validateConversionCount(text: String, pending: Int, unit: String): ConversionCountInput {
        val count = text.trim().takeIf { it.isNotEmpty() && it.all(Char::isDigit) && it.length <= 4 }?.toInt()
        return if (count == null || count !in 1..pending) {
            ConversionCountInput(null, "1~${pending}${unit}로 입력해 주세요.")
        } else {
            ConversionCountInput(count, null)
        }
    }

    /** 소수점 아래 최대 자릿수(0.01점 단위). */
    const val DECIMALS = 2
    const val MIN_CENTI_POINTS = 1
    const val MAX_CENTI_POINTS = 99_999
    const val MAX_REASON_LENGTH = 50
    const val MAX_MEMO_LENGTH = 200

    private val POINTS_PATTERN = Regex("""\d{1,3}(\.\d{0,$DECIMALS})?|\.\d{1,$DECIMALS}""")

    fun summarize(records: List<ConductPoint>): ConductPointSummary = ConductPointSummary(
        merit = records.filter { it.kind == ConductPointKind.MERIT }.sumOf { it.centiPoints },
        demerit = records.filter { it.kind == ConductPointKind.DEMERIT }.sumOf { it.centiPoints },
    )

    /** [year]년 [month]월에 기록된 점수만 합산한다. */
    fun summarizeMonth(records: List<ConductPoint>, year: Int, month: Int): ConductPointSummary =
        summarize(records.filter { it.date.year == year && it.date.monthNumber == month })

    /** "0.25", "1.5", ".5", "3" 같은 입력을 0.01점 단위 정수로 바꾼다. 형식이 맞지 않으면 null. */
    fun parseCentiPoints(text: String): Int? {
        val t = text.trim()
        if (!POINTS_PATTERN.matches(t)) return null
        val whole = t.substringBefore('.').ifEmpty { "0" }.toInt()
        val fraction = t.substringAfter('.', "").padEnd(DECIMALS, '0').toInt()
        return whole * 100 + fraction
    }

    /** 0.01점 단위 정수를 사람이 읽는 점수로 바꾼다: 25 → "0.25", 150 → "1.5", 300 → "3". 부호는 붙이지 않는다. */
    fun format(centiPoints: Int): String {
        val a = abs(centiPoints)
        val whole = a / 100
        val fraction = (a % 100).toString().padStart(DECIMALS, '0').trimEnd('0')
        return if (fraction.isEmpty()) "$whole" else "$whole.$fraction"
    }

    /** 입력 문자열을 검증한다. 점수는 0.01~999.99(소수 둘째 자리까지), 사유는 공백 제거 후 비어 있지 않고 50자 이하. */
    fun validate(pointsText: String, reason: String, memo: String = ""): ConductPointInput {
        val trimmedReason = reason.trim()
        val centiPoints = parseCentiPoints(pointsText)
        val error = when {
            centiPoints == null || centiPoints !in MIN_CENTI_POINTS..MAX_CENTI_POINTS ->
                "점수는 0.01~999.99점(소수 둘째 자리까지)으로 입력해 주세요."
            trimmedReason.isEmpty() -> "사유를 입력해 주세요."
            trimmedReason.length > MAX_REASON_LENGTH -> "사유는 ${MAX_REASON_LENGTH}자 이하로 입력해 주세요."
            memo.trim().length > MAX_MEMO_LENGTH -> "메모는 ${MAX_MEMO_LENGTH}자 이하로 입력해 주세요."
            else -> null
        }
        return ConductPointInput(centiPoints, trimmedReason, error)
    }

    /** 초과근무 양호 점수(0.01점 단위): 평일 시간당 0.5점, 휴일 시간당 1점. 0.1시간 단위라 항상 정확히 나눠진다. */
    fun overtimeCentiPoints(tenthHours: Int, onHoliday: Boolean): Int = tenthHours * (if (onHoliday) 10 else 5)

    /** "2", "1.5", ".5" 같은 시간 입력을 0.1시간 단위 정수로 바꾼다. 형식이 맞지 않으면 null. */
    fun parseTenthHours(text: String): Int? {
        val t = text.trim()
        if (!HOURS_PATTERN.matches(t)) return null
        val whole = t.substringBefore('.').ifEmpty { "0" }.toInt()
        val tenth = t.substringAfter('.', "").ifEmpty { "0" }.toInt()
        return whole * 10 + tenth
    }

    /** 0.1시간 단위 정수를 읽기 쉬운 시간으로: 15 → "1.5", 30 → "3". */
    fun formatHours(tenthHours: Int): String {
        val whole = tenthHours / 10
        val tenth = tenthHours % 10
        return if (tenth == 0) "$whole" else "$whole.$tenth"
    }

    /** 초과근무 시간 입력을 검증한다. 0.1~24시간, 소수 첫째 자리까지. */
    fun validateOvertimeHours(text: String): OvertimeHoursInput {
        val tenth = parseTenthHours(text)
        return if (tenth == null || tenth !in MIN_OVERTIME_TENTH_HOURS..MAX_OVERTIME_TENTH_HOURS) {
            OvertimeHoursInput(null, "시간은 0.1~24시간(소수 첫째 자리까지)으로 입력해 주세요.")
        } else {
            OvertimeHoursInput(tenth, null)
        }
    }

    /** "08:05" 형태의 시각 표기. */
    fun formatTime(time: LocalTime): String = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

    /**
     * 시작~끝 시각의 근무 시간을 0.1시간(6분) 단위로 센다. 끝이 시작보다 이르면 자정을 넘긴 것으로 보고 다음 날로 계산한다.
     * 6분 미만의 나머지는 버린다(내림): 18:00~19:20(80분) = 1.3시간. 시작과 끝이 같으면 0을 돌려준다(검증에서 오류).
     */
    fun overtimeTenthHours(start: LocalTime, end: LocalTime): Int {
        val s = start.hour * 60 + start.minute
        val e = end.hour * 60 + end.minute
        if (s == e) return 0
        val minutes = if (e > s) e - s else e + 24 * 60 - s
        return minutes / 6
    }

    /** 시작·끝 시각 입력을 검증한다. 같은 시각이거나 6분 미만이면 오류. */
    fun validateOvertimeTimes(start: LocalTime?, end: LocalTime?): OvertimeTimesInput {
        if (start == null || end == null) return OvertimeTimesInput(null, false, "시작과 끝 시각을 입력해 주세요.")
        if (start == end) return OvertimeTimesInput(null, false, "시작과 끝 시각이 같아요.")
        val crosses = end < start
        val tenth = overtimeTenthHours(start, end)
        return if (tenth < MIN_OVERTIME_TENTH_HOURS) {
            OvertimeTimesInput(null, crosses, "6분 이상이어야 해요.")
        } else {
            OvertimeTimesInput(tenth, crosses, null)
        }
    }

    /** 날짜가 휴일(주말·공휴일)이면 true. 초과근무 평일/휴일 자동 판정에 쓴다. */
    fun isHolidayOvertime(date: LocalDate, holidays: List<Holiday>): Boolean =
        PassCalculator.classifyType(date, holidays) == PassType.HOLIDAY

    /** 초과근무 사유를 비웠을 때 쓰는 문구: "평일 초과근무 1.5시간". */
    fun overtimeReason(tenthHours: Int, onHoliday: Boolean): String =
        (if (onHoliday) "휴일" else "평일") + " 초과근무 ${formatHours(tenthHours)}시간"

    /** 초과근무 기록의 사유가 사유를 비웠을 때 자동으로 채운 문구인지. 그런 사유는 근무 정보와 겹치므로 따로 보여주지 않는다. */
    fun isAutoOvertimeReason(record: ConductPoint): Boolean {
        val o = record.overtime ?: return false
        val r = record.reason.trim()
        if (r == overtimeReason(o.tenthHours, o.onHoliday)) return true
        val start = o.startTime
        val end = o.endTime
        return start != null && end != null && r == overtimeReason(o.onHoliday, start, end)
    }

    /** 시각이 있는 초과근무의 사유 문구: "평일 초과근무 18:00~20:30". */
    fun overtimeReason(onHoliday: Boolean, start: LocalTime, end: LocalTime): String =
        (if (onHoliday) "휴일" else "평일") + " 초과근무 ${formatTime(start)}~${formatTime(end)}"

    /** 점수 합계와 지금까지의 위원회 결과 기록으로 전환 현황을 계산한다. */
    fun conversion(
        summary: ConductPointSummary,
        conversions: List<ConductConversionRecord> = emptyList(),
    ): ConductConversion = ConductConversion(
        merit = summary.merit,
        demerit = summary.demerit,
        rewardLeaveDaysApplied = conversions.filter { it.type == ConductConversionType.REWARD_LEAVE }.sumOf { it.count },
        overnightRestrictionsApplied = conversions.filter { it.type == ConductConversionType.OVERNIGHT_RESTRICTION }.sumOf { it.count },
    )
}
