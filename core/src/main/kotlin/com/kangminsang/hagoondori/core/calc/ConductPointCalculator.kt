package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.ConductPoint
import com.kangminsang.hagoondori.core.model.ConductPointKind
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.OverflowBehavior
import com.kangminsang.hagoondori.core.model.OvernightForfeiture
import com.kangminsang.hagoondori.core.model.PassType
import kotlinx.datetime.LocalDate
import kotlin.math.abs

/** 양호·과실 점수 합계(0.01점 단위). [net] = 양호 − 과실. */
data class ConductPointSummary(val merit: Int, val demerit: Int) {
    val net: Int get() = merit - demerit
}

/** 점수 입력 검증 결과. [error]가 null이면 통과, [centiPoints]·[reason]은 정리된 값이다. */
data class ConductPointInput(val centiPoints: Int?, val reason: String, val error: String?)

/** 초과근무 시간 입력 검증 결과. [error]가 null이면 통과, [tenthHours]는 0.1시간 단위 정수다. */
data class OvertimeHoursInput(val tenthHours: Int?, val error: String?)

/**
 * 점수 전환 현황(누적). [rewardLeaveDaysEarned]/[overnightRestrictionsEarned]는 점수로 받을 수 있는 총량,
 * [meritTowardNext]/[demeritTowardNext]는 다음 전환까지 남은 점수(0.01점 단위)다.
 * 적용분 이후로 더 기록할 수 있는 양이 pending, 점수보다 많이 기록한 양이 overApplied다.
 */
data class ConductConversion(
    val rewardLeaveDaysEarned: Int,
    val meritTowardNext: Int,
    val overnightRestrictionsEarned: Int,
    val demeritTowardNext: Int,
    val rewardLeaveDaysApplied: Int = 0,
    val overnightRestrictionsApplied: Int = 0,
) {
    val rewardLeaveDaysPending: Int get() = maxOf(0, rewardLeaveDaysEarned - rewardLeaveDaysApplied)
    val overnightRestrictionsPending: Int get() = maxOf(0, overnightRestrictionsEarned - overnightRestrictionsApplied)
    val rewardLeaveOverApplied: Boolean get() = rewardLeaveDaysApplied > rewardLeaveDaysEarned
    val overnightRestrictionOverApplied: Boolean get() = overnightRestrictionsApplied > overnightRestrictionsEarned
}

object ConductPointCalculator {
    /** 양호 10점마다 포상휴가 1일 (0.01점 단위). */
    const val CENTI_POINTS_PER_REWARD_LEAVE_DAY = 1000

    /** 과실 20점마다 외박제한 1회 (0.01점 단위). */
    const val CENTI_POINTS_PER_OVERNIGHT_RESTRICTION = 2000

    /** 양호 점수로 포상휴가를 부여할 때 [LeaveGrant.reason]에 적는 문구. 적용분을 세는 기준이다. */
    const val REWARD_CONVERSION_REASON = "양호 점수 전환"

    /** 과실 점수로 외박 차수를 제한할 때 [OvernightForfeiture.reason]에 적는 문구. 적용분을 세는 기준이다. */
    const val OVERNIGHT_RESTRICTION_REASON = "과실 점수 외박제한"

    const val MIN_OVERTIME_TENTH_HOURS = 1
    const val MAX_OVERTIME_TENTH_HOURS = 240

    private val HOURS_PATTERN = Regex("""\d{1,2}(\.\d?)?|\.\d""")

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

    /** 날짜가 휴일(주말·공휴일)이면 true. 초과근무 평일/휴일 자동 판정에 쓴다. */
    fun isHolidayOvertime(date: LocalDate, holidays: List<Holiday>): Boolean =
        PassCalculator.classifyType(date, holidays) == PassType.HOLIDAY

    /** 초과근무 사유를 비웠을 때 쓰는 문구: "평일 초과근무 1.5시간". */
    fun overtimeReason(tenthHours: Int, onHoliday: Boolean): String =
        (if (onHoliday) "휴일" else "평일") + " 초과근무 ${formatHours(tenthHours)}시간"

    /** 포상휴가 종류를 찾는다: id "leave-reward" → 이름 "포상휴가" → 초과분을 전투휴무로 전환하는 종류 순. 없으면 null. */
    fun findRewardLeaveType(types: List<LeaveType>): LeaveType? =
        types.firstOrNull { it.id == "leave-reward" }
            ?: types.firstOrNull { it.name == "포상휴가" }
            ?: types.firstOrNull { it.overflowBehavior == OverflowBehavior.CONVERT_TO_COMBAT_REST }

    /** 점수 합계에서 전환 현황을 계산한다. 기록 전체를 누적하며 초기화하지 않는다. 적용분은 부여·제한 기록의 사유로 센다. */
    fun conversion(
        summary: ConductPointSummary,
        grants: List<LeaveGrant> = emptyList(),
        forfeitures: List<OvernightForfeiture> = emptyList(),
    ): ConductConversion = ConductConversion(
        rewardLeaveDaysEarned = summary.merit / CENTI_POINTS_PER_REWARD_LEAVE_DAY,
        meritTowardNext = CENTI_POINTS_PER_REWARD_LEAVE_DAY - summary.merit % CENTI_POINTS_PER_REWARD_LEAVE_DAY,
        overnightRestrictionsEarned = summary.demerit / CENTI_POINTS_PER_OVERNIGHT_RESTRICTION,
        demeritTowardNext = CENTI_POINTS_PER_OVERNIGHT_RESTRICTION - summary.demerit % CENTI_POINTS_PER_OVERNIGHT_RESTRICTION,
        rewardLeaveDaysApplied = grants.filter { it.reason == REWARD_CONVERSION_REASON }.sumOf { it.days },
        overnightRestrictionsApplied = forfeitures.count { it.reason == OVERNIGHT_RESTRICTION_REASON },
    )
}
