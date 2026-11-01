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

    /**
     * 전역 전에 남은 외박 목록 (화면의 "전역까지 남은 외박" 카드용). 결과는 [RemainingOvernight.slotIndex] 오름차순이다.
     *
     * - 이미 등록한 외박 중 오늘 이후 날짜인 기록은 [RemainingOvernightStatus.RECORDED]로 포함한다.
     *   이 차수는 [OvernightSchedule.nextSlotIndex]보다 앞에 있어서, 빼 버리면 남은 개수가 하나 모자란다.
     * - 아직 나가지 않은 차수는 [OvernightSchedule.nextSlotIndex]부터 소멸 차수를 뺀 예정일로 채운다.
     *   예정일이 오늘보다 앞이면 [RemainingOvernightStatus.OVERDUE], 아니면 [RemainingOvernightStatus.SCHEDULED]다.
     * - 전역일 당일([dischargeDate])과 그 이후는 모두 뺀다. 전역일에는 외박을 나갈 수 없다고 본다.
     *   그래서 전역일 전날까지의 차수만 남는다.
     *
     * 예정 차수 시퀀스는 무한하므로 [takeWhile]로 전역일 전에서 끊는다.
     */
    fun remainingUntilDischarge(
        firstOvernightDate: LocalDate,
        cycleWeeks: Int,
        schedule: OvernightSchedule,
        dischargeDate: LocalDate,
        today: LocalDate,
    ): List<RemainingOvernight> {
        val recorded = schedule.matches
            .filter { it.record.date >= today && it.record.date < dischargeDate }
            .map { RemainingOvernight(it.slotIndex, it.record.date, RemainingOvernightStatus.RECORDED) }

        val scheduled = effectiveSlots(schedule.forfeitedSlots)
            .dropWhile { it < schedule.nextSlotIndex }
            .map { slot ->
                val date = scheduledDate(firstOvernightDate, cycleWeeks, slot)
                RemainingOvernight(
                    slotIndex = slot,
                    date = date,
                    status = if (date < today) RemainingOvernightStatus.OVERDUE else RemainingOvernightStatus.SCHEDULED,
                )
            }
            .takeWhile { it.date < dischargeDate }
            .toList()

        return (recorded + scheduled).sortedBy { it.slotIndex }
    }
}

/** 전역 전 외박 한 건의 상태. [RECORDED]=이미 등록됨, [SCHEDULED]=예정, [OVERDUE]=예정일이 지났는데 등록되지 않음. */
enum class RemainingOvernightStatus { RECORDED, SCHEDULED, OVERDUE }

/** 전역 전에 남은 외박 한 건. [date]는 등록된 날짜(RECORDED)이거나 예정일이다. */
data class RemainingOvernight(val slotIndex: Int, val date: LocalDate, val status: RemainingOvernightStatus)
