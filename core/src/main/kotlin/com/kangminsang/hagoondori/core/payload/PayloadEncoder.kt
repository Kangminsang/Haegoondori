package com.kangminsang.hagoondori.core.payload

import com.kangminsang.hagoondori.core.export.CalendarSnapshot
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.UserProfile
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * [CalendarSnapshot]을 장치 전송 페이로드(`calendar.txt`)로 인코딩한다 (스펙 7.2/7.3절).
 *
 * 형식: 파이프(`|`) 구분 줄 단위 텍스트, UTF-8, **LF** 개행(CRLF 아님). 각 줄의 첫
 * 필드가 레코드 종류(`V/M/P/O/H/E/L/C/D/Z`)를 나타낸다.
 *
 * 파일을 실제로 어디에 어떻게 쓰는지(USB, SAF, 로컬 파일 등)는 이 클래스의 관심사가
 * 아니다 — 여기서는 순수하게 문자열만 만든다. 파일명(`calendar.txt`)을 정하고 실제
 * 쓰기를 수행하는 것은 app 계층의 `ExportAdapter` 구현체다.
 */
object PayloadEncoder {

    private const val PAYLOAD_VERSION = 1
    private val KST = TimeZone.of("Asia/Seoul")

    /**
     * [snapshot]을 페이로드 전체 텍스트로 인코딩한다.
     *
     * 마지막 `Z` 줄은 그 이전까지의 UTF-8 바이트 수와, 같은 범위에 대한 CRC32(대문자
     * 8자리 16진수)를 담는다(7.3절 무결성 보장). 장치는 이 `Z` 줄이 없거나 값이
     * 일치하지 않으면 불완전/손상 전송으로 보고 기존 데이터를 유지한다.
     */
    fun encode(snapshot: CalendarSnapshot): String {
        val lines = mutableListOf<String>()

        lines += "V|$PAYLOAD_VERSION"
        lines += encodeMetaLine(snapshot)
        lines += encodeProfileLine(snapshot.profile)
        snapshot.nextOvernightDate?.let { lines += "O|$it" }

        DateRangeFilter.filterWithin(snapshot.holidays, snapshot.rangeStart, snapshot.rangeEnd) { it.date }
            .sortedBy { it.date }
            .forEach { lines += encodeHolidayLine(it) }

        DateRangeFilter.filterOverlapping(
            snapshot.events, snapshot.rangeStart, snapshot.rangeEnd,
            start = { it.startDate }, end = { it.endDate ?: it.startDate },
        )
            .sortedBy { it.startDate }
            .forEach { lines += encodeEventLine(it) }

        DateRangeFilter.filterOverlapping(
            snapshot.leaveUsages, snapshot.rangeStart, snapshot.rangeEnd,
            start = { it.startDate }, end = { it.endDate },
        )
            .sortedBy { it.startDate }
            .forEach { lines += encodeLeaveLine(it) }

        DateRangeFilter.filterOverlapping(
            snapshot.combatRestUsages, snapshot.rangeStart, snapshot.rangeEnd,
            start = { it.startDate }, end = { it.endDate },
        )
            .sortedBy { it.startDate }
            .forEach { lines += encodeCombatRestLine(it) }

        DateRangeFilter.filterWithin(snapshot.dutyAssignments, snapshot.rangeStart, snapshot.rangeEnd) { it.date }
            .sortedWith(compareBy({ it.date }, { it.type.name }))
            .forEach { lines += encodeDutyLine(it) }

        // Z 줄 이전까지의 바이트 수 + CRC32는 "데이터 줄들을 LF로 join하고 마지막에
        // LF를 하나 더 붙인" 범위를 기준으로 계산한다 - 그래야 Z 줄이 그 바로 다음
        // 줄로 자연스럽게 이어진다.
        val body = lines.joinToString(separator = "\n") + "\n"
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val crc = Crc32.compute(bodyBytes)
        val zLine = "Z|${bodyBytes.size}|$crc\n"

        return body + zLine
    }

    private fun encodeMetaLine(snapshot: CalendarSnapshot): String =
        "M|${formatGeneratedAt(snapshot.generatedAt)}|${snapshot.rangeStart}|${snapshot.rangeEnd}"

    private fun encodeProfileLine(profile: UserProfile): String {
        val promotion = profile.promotionDate?.toString().orEmpty()
        return "P|${profile.enlistmentDate}|${profile.dischargeDate}|$promotion|" +
            "${formatTime(profile.wakeUpTime)}|${formatTime(profile.dinnerTime)}"
    }

    private fun encodeHolidayLine(holiday: Holiday): String =
        "H|${holiday.date}|${sanitizeField(holiday.name, fieldName = "공휴일 이름")}"

    private fun encodeEventLine(event: Event): String {
        // 하루짜리 일정도 종료일을 시작일과 같게 채운다 (장치 파싱 단순화, 7.2절)
        val end = event.endDate ?: event.startDate
        val important = if (event.isImportant) 1 else 0
        return "E|${event.startDate}|$end|${sanitizeField(event.title, fieldName = "일정 제목")}|$important"
    }

    private fun encodeLeaveLine(usage: LeaveUsage): String {
        val label = sanitizeField(usage.label.orEmpty(), fieldName = "휴가 라벨")
        return "L|${usage.startDate}|${usage.endDate}|$label"
    }

    private fun encodeCombatRestLine(usage: CombatRestUsage): String =
        "C|${usage.startDate}|${usage.endDate}"

    private fun encodeDutyLine(duty: DutyAssignment): String =
        "D|${duty.date}|${duty.type.name}"

    private fun formatTime(time: LocalTime): String =
        "%02d:%02d".format(time.hour, time.minute)

    /**
     * 생성 시각을 ISO 8601로 포맷한다. 이 앱은 한국 해군 복무자 전용이므로 항상
     * Asia/Seoul(UTC+9, 서머타임 없음) 기준 오프셋을 명시한다 — 예: `2026-09-04T21:14:00+09:00`.
     * `LocalDateTime.toString()`을 그대로 쓰지 않는 이유는, 나노초가 포함된 Instant를
     * 넘기면 스펙 예시에 없는 소수점 이하 자리가 붙어버리기 때문이다.
     */
    private fun formatGeneratedAt(instant: Instant): String {
        val local = instant.toLocalDateTime(KST)
        return "%04d-%02d-%02dT%02d:%02d:%02d+09:00".format(
            local.year, local.monthNumber, local.dayOfMonth, local.hour, local.minute, local.second,
        )
    }

    /**
     * `|`와 개행은 페이로드 구분자/줄 경계와 충돌하므로 항상 금지한다. 이미 입력
     * 단계(예: [com.kangminsang.hagoondori.core.model.Event]의 생성자, app의 입력
     * 폼)에서 걸러졌어야 하지만, 인코더 자체도 방어적으로 재검증해 계약 위반을
     * 조기에 발견한다.
     */
    private fun sanitizeField(value: String, fieldName: String): String {
        require('|' !in value) { "$fieldName 에 '|' 문자를 쓸 수 없습니다: \"$value\"" }
        require('\n' !in value && '\r' !in value) { "$fieldName 에 개행 문자를 쓸 수 없습니다: \"$value\"" }
        return value
    }
}
