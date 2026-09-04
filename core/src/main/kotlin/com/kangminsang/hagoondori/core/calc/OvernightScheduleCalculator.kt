package com.kangminsang.hagoondori.core.calc

import com.kangminsang.hagoondori.core.model.OvernightForfeiture
import com.kangminsang.hagoondori.core.model.OvernightRecord
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * 실제 외박 기록 한 건이 예정 격자의 몇 번째 차수([slotIndex])에 대응하는지, 그리고
 * 그 차수의 예정일과 비교해 며칠 지연/선행했는지([delayDays], 양수=지연, 음수=선행).
 */
data class SlotMatch(
    val slotIndex: Int,
    val record: OvernightRecord,
    val scheduledDate: LocalDate,
    val delayDays: Int,
)

/**
 * 외박 차수 현황 전체 (스펙 3.4.5절이 요구하는 정보).
 *
 * @property matches 기록마다 대응된 차수/예정일/지연·선행 일수
 * @property nextSlotIndex 다음에 나가야 할 차수 번호
 * @property nextScheduledDate 다음 차수의 예정일 (격자상 고정값 — 지연/선행과 무관)
 * @property forfeitedSlots 소멸된 차수 번호 집합 (이력 표시용)
 */
data class OvernightSchedule(
    val matches: List<SlotMatch>,
    val nextSlotIndex: Int,
    val nextScheduledDate: LocalDate,
    val forfeitedSlots: Set<Int>,
)

/**
 * 주기형 자원(6주 외박) 계산 (스펙 3.4절) — **이 앱의 핵심 알고리즘**.
 *
 * 예정일 격자는 [firstOvernightDate] 기준으로 고정되어 있고 절대 변하지 않는다.
 * 실제 외박일만 그 격자 위에서 흔들린다(지연/선행). "당겨서 보상"과 "미뤄서 맞춤"은
 * 별도 로직이 아니라 격자가 고정되어 있기 때문에 자연히 발생하는 결과다 — 보정 코드가
 * 필요 없다(3.4.2절).
 *
 * 징계(외박제한)로 소멸된 차수는 격자 자체를 바꾸지 않고, 그 차수만 매칭 대상에서
 * 제외된다(3.4.3절) — 그래야 "소멸된 차수를 아직 안 나갔다"는 잘못된 지연 경고가
 * 뜨지 않는다.
 */
object OvernightScheduleCalculator {

    /** n번째 차수의 예정일 = firstOvernightDate + cycleWeeks*7*n일 (3.4.2절 공식). 격자 자체는 불변이다. */
    fun scheduledDate(firstOvernightDate: LocalDate, cycleWeeks: Int, slotIndex: Int): LocalDate {
        require(cycleWeeks > 0) { "cycleWeeks는 1 이상이어야 한다: $cycleWeeks" }
        require(slotIndex >= 0) { "slotIndex는 0 이상이어야 한다: $slotIndex" }
        return firstOvernightDate.plus(cycleWeeks * 7 * slotIndex, DateTimeUnit.DAY)
    }

    /**
     * 0, 1, 2, ... 전체 차수에서 소멸된 차수([forfeitedSlotIndices])를 제외한 무한
     * 시퀀스. 두 차수 이상 연속 소멸도 그대로 처리된다(3.4.3절 — 별도 모델 변경 불필요).
     */
    fun effectiveSlots(forfeitedSlotIndices: Set<Int>): Sequence<Int> =
        generateSequence(0) { it + 1 }.filter { it !in forfeitedSlotIndices }

    /**
     * 차수 매칭 알고리즘 (3.4.4절):
     * 1. 전체 차수 = [0, 1, 2, ...]
     * 2. 유효 차수 = 전체 차수 − 소멸된 차수
     * 3. 외박 기록을 날짜 오름차순 정렬
     * 4. i번째 기록 ↔ 유효 차수의 i번째 항목에 대응
     * 5. 다음 예정 차수 = 유효 차수 중 (기록 개수)번째 항목
     *
     * [records]는 입력 순서와 무관하게 내부에서 날짜순으로 정렬한다 — 각 기록에 차수
     * 번호를 저장할 필요가 없다.
     */
    fun buildSchedule(
        firstOvernightDate: LocalDate,
        cycleWeeks: Int,
        records: List<OvernightRecord>,
        forfeitures: List<OvernightForfeiture>,
    ): OvernightSchedule {
        val forfeitedSlots = forfeitures.map { it.slotIndex }.toSet()
        val sortedRecords = records.sortedBy { it.date }
        val effective = effectiveSlots(forfeitedSlots).take(sortedRecords.size + 1).toList()

        val matches = sortedRecords.mapIndexed { i, record ->
            val slot = effective[i]
            val scheduled = scheduledDate(firstOvernightDate, cycleWeeks, slot)
            SlotMatch(
                slotIndex = slot,
                record = record,
                scheduledDate = scheduled,
                delayDays = scheduled.daysUntil(record.date),
            )
        }

        val nextSlotIndex = effective[sortedRecords.size]
        val nextScheduledDate = scheduledDate(firstOvernightDate, cycleWeeks, nextSlotIndex)

        return OvernightSchedule(
            matches = matches,
            nextSlotIndex = nextSlotIndex,
            nextScheduledDate = nextScheduledDate,
            forfeitedSlots = forfeitedSlots,
        )
    }
}
