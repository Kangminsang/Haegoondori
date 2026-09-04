package com.kangminsang.hagoondori.preview

import com.kangminsang.hagoondori.core.calc.DDayCalculator
import com.kangminsang.hagoondori.core.export.CalendarSnapshot
import kotlinx.datetime.LocalDate

/**
 * 장치가 **스스로** 계산해서 보여주는 값들(전역/진급 D-day, 진행률, 일정별 D-day,
 * 휴가 D-day 등, 스펙 7.1절)을 미리보기 목적으로 이 앱 안에서 재현한다.
 *
 * **이 파일은 의도적으로 core가 아니라 app(미리보기 전용)에 있다.** core는 "장치가
 * 무엇을 스스로 계산하는지"를 알 필요가 없다(6.4절) - 이 재현은 실제 장치 펌웨어의
 * 동작을 흉내 내기 위한 것일 뿐이며, 장치 쪽 구현이 바뀌면 이 파일만 따라 바뀌면 된다.
 */
object PreviewDisplayCalc {

    data class EventDDay(val title: String, val dDay: Int, val isImportant: Boolean)
    data class LeaveDDay(val label: String, val dDay: Int, val isOngoing: Boolean)

    fun dischargeDDay(snapshot: CalendarSnapshot, today: LocalDate): Int =
        DDayCalculator.dDay(today, snapshot.profile.dischargeDate)

    fun serviceProgressPercent(snapshot: CalendarSnapshot, today: LocalDate): Double =
        DDayCalculator.serviceProgress(snapshot.profile.enlistmentDate, snapshot.profile.dischargeDate, today)

    fun promotionDDay(snapshot: CalendarSnapshot, today: LocalDate): Int? =
        snapshot.profile.promotionDate?.let { DDayCalculator.dDay(today, it) }

    /** 진급 마커의 진행률 바 위 가로 위치(%). 진급 예정일이 없으면 null. */
    fun promotionMarkerPercent(snapshot: CalendarSnapshot, today: LocalDate): Double? =
        snapshot.profile.promotionDate?.let {
            DDayCalculator.promotionProgress(snapshot.profile.enlistmentDate, snapshot.profile.dischargeDate, it)
        }

    /** 다가오는 일정들의 D-day 목록. 이미 지난 일정은 제외하고 가까운 순으로 정렬한다. */
    fun upcomingEventDDays(snapshot: CalendarSnapshot, today: LocalDate, limit: Int = 4): List<EventDDay> =
        snapshot.events
            .filter { (it.endDate ?: it.startDate) >= today }
            .map { EventDDay(it.title, DDayCalculator.dDay(today, it.startDate), it.isImportant) }
            .sortedBy { it.dDay }
            .take(limit)

    /**
     * 휴가 D-day(7.1절 표) - 오늘이 휴가 기간 중이면 "며칠째"(종료까지 D-day),
     * 아니면 다음 휴가 시작일까지 D-day. 진행 중/예정 휴가가 전혀 없으면 null.
     */
    fun leaveDDay(snapshot: CalendarSnapshot, today: LocalDate): LeaveDDay? {
        val ongoing = snapshot.leaveUsages.firstOrNull { today in it.startDate..it.endDate }
        if (ongoing != null) {
            return LeaveDDay(ongoing.label ?: "휴가", DDayCalculator.dDay(today, ongoing.endDate), isOngoing = true)
        }
        val next = snapshot.leaveUsages.filter { it.startDate > today }.minByOrNull { it.startDate } ?: return null
        return LeaveDDay(next.label ?: "휴가", DDayCalculator.dDay(today, next.startDate), isOngoing = false)
    }

    fun nextOvernightDDay(snapshot: CalendarSnapshot, today: LocalDate): Int? =
        snapshot.nextOvernightDate?.let { DDayCalculator.dDay(today, it) }
}
