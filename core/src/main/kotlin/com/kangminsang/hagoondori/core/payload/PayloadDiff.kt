package com.kangminsang.hagoondori.core.payload

import com.kangminsang.hagoondori.core.model.DutyType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** 페이로드 `M` 줄(생성 시각과 범위)의 내용. */
data class PayloadMeta(val generatedAt: Instant, val rangeStart: LocalDate, val rangeEnd: LocalDate)

/**
 * 마지막으로 장치에 보낸 페이로드와 지금 데이터로 만든 페이로드를 비교해, 장치 화면에 반영될
 * 변경만 사람이 읽는 문장으로 만든다(예: "근무 추가: 11-17 당직").
 *
 * 줄 형식은 [PayloadEncoder]가 정한 것을 따른다. `V/M/Z` 줄은 메타 정보라 비교하지 않는다.
 */
object PayloadDiff {

    private val IGNORED_PREFIXES = listOf("V|", "M|", "Z|")
    private const val OVERNIGHT_LABEL = "외박"

    /** 페이로드의 M 줄을 읽는다. 없거나 형식이 틀리면 null. */
    fun parseMeta(payload: String): PayloadMeta? {
        val line = payload.lineSequence().firstOrNull { it.startsWith("M|") } ?: return null
        val fields = line.split("|")
        if (fields.size != 4) return null
        return runCatching {
            PayloadMeta(
                generatedAt = Instant.parse(fields[1]),
                rangeStart = LocalDate.parse(fields[2]),
                rangeEnd = LocalDate.parse(fields[3]),
            )
        }.getOrNull()
    }

    /**
     * 마지막으로 장치에 보낸 [baseline]과 지금 데이터로 만든 [current]를 줄 단위로 비교해,
     * 장치 화면에 반영될 순수 변경만 사람이 읽는 문장 목록으로 돌려준다. V/M/Z 줄은 비교하지 않는다.
     * 같은 줄이 추가됐다 지워졌으면(양쪽에 모두 있으면) 변경이 아니다.
     */
    fun describe(baseline: String, current: String): List<String> {
        val before = bodyLines(baseline)
        val after = bodyLines(current).toMutableList()

        // 다중집합 차집합: 양쪽에 있는 줄은 하나씩 짝지어 상쇄한다. 남은 after가 추가된 줄이다.
        val removed = mutableListOf<String>()
        for (line in before) {
            if (!after.remove(line)) removed += line
        }
        val added = after

        val removedLines = removed.map(::parseLine)
        val addedLines = added.map(::parseLine)

        val changes = profileChanges(
            removedLines.filterIsInstance<ProfileLine>(),
            addedLines.filterIsInstance<ProfileLine>(),
        ) +
            overnightChanges(
                removedLines.filterIsInstance<OvernightLine>().toMutableList(),
                addedLines.filterIsInstance<OvernightLine>().toMutableList(),
            ) +
            holidayChanges(
                removedLines.filterIsInstance<HolidayLine>().toMutableList(),
                addedLines.filterIsInstance<HolidayLine>().toMutableList(),
            ) +
            eventChanges(
                removedLines.filterIsInstance<EventLine>().toMutableList(),
                addedLines.filterIsInstance<EventLine>().toMutableList(),
            ) +
            leaveChanges(
                removedLines.filterIsInstance<LeaveLine>().toMutableList(),
                addedLines.filterIsInstance<LeaveLine>().toMutableList(),
            ) +
            combatChanges(
                removedLines.filterIsInstance<CombatLine>().toMutableList(),
                addedLines.filterIsInstance<CombatLine>().toMutableList(),
            ) +
            outingChanges(
                removedLines.filterIsInstance<OutingLine>(),
                addedLines.filterIsInstance<OutingLine>(),
            ) +
            dutyChanges(
                removedLines.filterIsInstance<DutyLine>(),
                addedLines.filterIsInstance<DutyLine>(),
            )

        // 알 수 없는 종류(또는 형식이 깨진 줄)는 날짜가 없으므로 맨 뒤에 입력 순서대로 붙인다.
        val unknown = removedLines.filterIsInstance<OtherLine>().map { "기타 삭제: ${it.raw}" } +
            addedLines.filterIsInstance<OtherLine>().map { "기타 변경: ${it.raw}" }

        return changes.sortedBy { it.date }.map { it.text } + unknown
    }

    // -----------------------------------------------------------------
    // 줄 파싱
    // -----------------------------------------------------------------

    private fun bodyLines(payload: String): List<String> =
        payload.split('\n')
            .map { it.trimEnd('\r') }
            .filter { line -> line.isNotBlank() && IGNORED_PREFIXES.none { line.startsWith(it) } }

    private sealed interface ParsedLine

    private data class ProfileLine(
        val enlistment: LocalDate,
        val discharge: LocalDate,
        val promotion: LocalDate?,
        val wake: String,
        val dinner: String,
    ) : ParsedLine

    private data class OvernightLine(val date: LocalDate) : ParsedLine
    private data class HolidayLine(val date: LocalDate, val name: String) : ParsedLine
    private data class EventLine(val start: LocalDate, val end: LocalDate, val title: String, val important: Boolean) : ParsedLine
    private data class LeaveLine(val start: LocalDate, val end: LocalDate, val label: String) : ParsedLine
    private data class CombatLine(val start: LocalDate, val end: LocalDate) : ParsedLine
    private data class OutingLine(val date: LocalDate) : ParsedLine
    private data class DutyLine(val date: LocalDate, val type: String) : ParsedLine

    /** 알 수 없는 종류이거나 형식이 깨진 줄. */
    private data class OtherLine(val raw: String) : ParsedLine

    private fun parseLine(line: String): ParsedLine {
        val f = line.split("|")
        val parsed = runCatching {
            when (f[0]) {
                "P" -> f.takeIf { it.size == 6 }?.let {
                    ProfileLine(
                        enlistment = LocalDate.parse(it[1]),
                        discharge = LocalDate.parse(it[2]),
                        promotion = if (it[3].isEmpty()) null else LocalDate.parse(it[3]),
                        wake = it[4],
                        dinner = it[5],
                    )
                }
                "O" -> f.takeIf { it.size == 2 }?.let { OvernightLine(LocalDate.parse(it[1])) }
                "H" -> f.takeIf { it.size == 3 }?.let { HolidayLine(LocalDate.parse(it[1]), it[2]) }
                "E" -> f.takeIf { it.size == 5 }?.let {
                    parseFlag(it[4])?.let { flag ->
                        EventLine(LocalDate.parse(it[1]), LocalDate.parse(it[2]), it[3], flag)
                    }
                }
                "L" -> f.takeIf { it.size == 4 }?.let { LeaveLine(LocalDate.parse(it[1]), LocalDate.parse(it[2]), it[3]) }
                "C" -> f.takeIf { it.size == 3 }?.let { CombatLine(LocalDate.parse(it[1]), LocalDate.parse(it[2])) }
                "G" -> f.takeIf { it.size == 2 }?.let { OutingLine(LocalDate.parse(it[1])) }
                "D" -> f.takeIf { it.size == 3 }?.let { DutyLine(LocalDate.parse(it[1]), it[2]) }
                else -> null
            }
        }.getOrNull()
        return parsed ?: OtherLine(line)
    }

    private fun parseFlag(value: String): Boolean? = when (value) {
        "1" -> true
        "0" -> false
        else -> null
    }

    // -----------------------------------------------------------------
    // 종류별 변경 문장
    // -----------------------------------------------------------------

    /** 출력 문장과, 정렬에 쓸 날짜(없으면 null). */
    private class Change(val date: LocalDate?, val text: String)

    /**
     * 같은 키를 가진 제거/추가 줄을 짝짓는다. 키마다 한 번씩 차례로 시도하며, 짝지은 줄은
     * [removed]와 [added]에서 빠진다. 짝지은 쌍을 돌려준다.
     */
    private fun <T> pairs(removed: MutableList<T>, added: MutableList<T>, vararg keys: (T) -> Any?): List<Pair<T, T>> {
        val out = mutableListOf<Pair<T, T>>()
        for (key in keys) {
            val iter = removed.iterator()
            while (iter.hasNext()) {
                val old = iter.next()
                val index = added.indexOfFirst { key(it) == key(old) }
                if (index >= 0) {
                    out += old to added.removeAt(index)
                    iter.remove()
                }
            }
        }
        return out
    }

    private fun profileChanges(removed: List<ProfileLine>, added: List<ProfileLine>): List<Change> {
        val old = removed.firstOrNull()
        val new = added.firstOrNull()
        if (old == null && new == null) return emptyList()
        if (old == null || new == null) return listOf(Change(null, "복무 정보 변경"))

        val texts = mutableListOf<String>()
        if (old.enlistment != new.enlistment) texts += "입대일 변경: ${md(old.enlistment)} → ${md(new.enlistment)}"
        if (old.discharge != new.discharge) texts += "전역일 변경: ${md(old.discharge)} → ${md(new.discharge)}"
        if (old.promotion != new.promotion) {
            texts += "진급일 변경: ${promotionText(old.promotion)} → ${promotionText(new.promotion)}"
        }
        if (old.wake != new.wake) texts += "기상 시간 변경: ${old.wake} → ${new.wake}"
        if (old.dinner != new.dinner) texts += "저녁 시간 변경: ${old.dinner} → ${new.dinner}"
        return texts.map { Change(null, it) }
    }

    private fun overnightChanges(removed: MutableList<OvernightLine>, added: MutableList<OvernightLine>): List<Change> {
        val out = mutableListOf<Change>()
        // 다음 외박 예정일은 한 줄뿐이므로 양쪽에 하나씩 있으면 그냥 짝짓는다.
        pairs(removed, added, { true }).forEach { (old, new) ->
            out += Change(new.date, "다음 외박 예정일 변경: ${md(old.date)} → ${md(new.date)}")
        }
        removed.forEach { out += Change(it.date, "다음 외박 예정일 없어짐 (${md(it.date)})") }
        added.forEach { out += Change(it.date, "다음 외박 예정일 추가: ${md(it.date)}") }
        return sorted(out)
    }

    private fun holidayChanges(removed: MutableList<HolidayLine>, added: MutableList<HolidayLine>): List<Change> {
        val out = mutableListOf<Change>()
        pairs(removed, added, { it.date }).forEach { (old, new) ->
            out += Change(new.date, "공휴일 변경: ${md(old.date)} ${old.name} → ${new.name}")
        }
        removed.forEach { out += Change(it.date, "공휴일 삭제: ${md(it.date)} ${it.name}") }
        added.forEach { out += Change(it.date, "공휴일 추가: ${md(it.date)} ${it.name}") }
        return sorted(out)
    }

    private fun eventChanges(removed: MutableList<EventLine>, added: MutableList<EventLine>): List<Change> {
        val out = mutableListOf<Change>()
        pairs(removed, added, { it.title }, { it.start }).forEach { (old, new) ->
            val parts = mutableListOf<String>()
            if (old.title != new.title) parts += "${old.title} → ${new.title}"
            val oldPeriod = period(old.start, old.end)
            val newPeriod = period(new.start, new.end)
            if (oldPeriod != newPeriod) parts += "$oldPeriod → $newPeriod"
            if (old.important != new.important) parts += if (new.important) "중요 표시 켬" else "중요 표시 끔"
            out += Change(new.start, "일정 수정: ${new.title} (${parts.joinToString(", ")})")
        }
        removed.forEach { out += Change(it.start, "일정 삭제: ${it.title} (${period(it.start, it.end)})") }
        added.forEach { out += Change(it.start, "일정 추가: ${it.title} (${period(it.start, it.end)})") }
        return sorted(out)
    }

    private fun leaveChanges(removed: MutableList<LeaveLine>, added: MutableList<LeaveLine>): List<Change> {
        val out = mutableListOf<Change>()
        pairs(removed, added, { it.label to it.start }, { it.label to it.end }).forEach { (old, new) ->
            val (noun, prefix) = leaveNoun(new.label)
            out += Change(
                new.start,
                "$noun 수정: $prefix${period(old.start, old.end)} → ${period(new.start, new.end)}",
            )
        }
        removed.forEach {
            val (noun, prefix) = leaveNoun(it.label)
            out += Change(it.start, "$noun 삭제: $prefix${period(it.start, it.end)}")
        }
        added.forEach {
            val (noun, prefix) = leaveNoun(it.label)
            out += Change(it.start, "$noun 추가: $prefix${period(it.start, it.end)}")
        }
        return sorted(out)
    }

    /** 외박은 이름 그대로, 휴가는 "휴가 <라벨>"로 쓴다. 반환값은 (명사, 라벨 접두어). */
    private fun leaveNoun(label: String): Pair<String, String> = when {
        label == OVERNIGHT_LABEL -> "외박" to ""
        label.isEmpty() -> "휴가" to ""
        else -> "휴가" to "$label "
    }

    private fun combatChanges(removed: MutableList<CombatLine>, added: MutableList<CombatLine>): List<Change> {
        val out = mutableListOf<Change>()
        pairs(removed, added, { it.start }, { it.end }).forEach { (old, new) ->
            out += Change(new.start, "전투휴무 수정: ${period(old.start, old.end)} → ${period(new.start, new.end)}")
        }
        removed.forEach { out += Change(it.start, "전투휴무 삭제: ${period(it.start, it.end)}") }
        added.forEach { out += Change(it.start, "전투휴무 추가: ${period(it.start, it.end)}") }
        return sorted(out)
    }

    private fun outingChanges(removed: List<OutingLine>, added: List<OutingLine>): List<Change> {
        val out = mutableListOf<Change>()
        removed.forEach { out += Change(it.date, "외출 삭제: ${md(it.date)}") }
        added.forEach { out += Change(it.date, "외출 추가: ${md(it.date)}") }
        return sorted(out)
    }

    private fun dutyChanges(removed: List<DutyLine>, added: List<DutyLine>): List<Change> {
        val out = mutableListOf<Change>()
        removed.forEach { out += Change(it.date, "근무 삭제: ${md(it.date)} ${dutyLabel(it.type)}") }
        added.forEach { out += Change(it.date, "근무 추가: ${md(it.date)} ${dutyLabel(it.type)}") }
        return sorted(out)
    }

    private fun dutyLabel(type: String): String = when (type) {
        DutyType.DUTY.name -> "당직"
        DutyType.OFF_DUTY.name -> "비번"
        DutyType.MESS.name -> "츄라이"
        else -> type
    }

    private fun sorted(changes: List<Change>): List<Change> = changes.sortedBy { it.date }

    private fun promotionText(date: LocalDate?): String = date?.let(::md) ?: "없음"

    /** 화면 표시용 `MM-dd`. */
    private fun md(date: LocalDate): String = "%02d-%02d".format(date.monthNumber, date.dayOfMonth)

    /** 하루짜리면 `MM-dd`, 기간이면 `MM-dd~MM-dd`. */
    private fun period(start: LocalDate, end: LocalDate): String =
        if (start == end) md(start) else "${md(start)}~${md(end)}"
}
