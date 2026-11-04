package com.kangminsang.hagoondori.export

import com.kangminsang.hagoondori.core.calc.OvernightScheduleCalculator
import com.kangminsang.hagoondori.core.export.CalendarSnapshot
import com.kangminsang.hagoondori.core.export.TransmissionRange
import com.kangminsang.hagoondori.data.repository.CombatRestRepository
import com.kangminsang.hagoondori.data.repository.DutyRepository
import com.kangminsang.hagoondori.data.repository.EventRepository
import com.kangminsang.hagoondori.data.repository.HolidayRepository
import com.kangminsang.hagoondori.data.repository.LeaveRepository
import com.kangminsang.hagoondori.data.repository.OvernightRepository
import com.kangminsang.hagoondori.data.repository.PassRepository
import com.kangminsang.hagoondori.data.repository.ProfileRepository
import com.kangminsang.hagoondori.util.AppClock
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 여러 Repository의 현재 데이터를 모아 [CalendarSnapshot] 하나로 조립한다 - app
 * 내부 구조(Room 엔티티 등)를 그대로 전송 계층에 넘기지 않기 위한 6.2절의 경계선이
 * 바로 여기서 그어진다.
 */
@Singleton
class SnapshotBuilder @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val eventRepository: EventRepository,
    private val holidayRepository: HolidayRepository,
    private val leaveRepository: LeaveRepository,
    private val combatRestRepository: CombatRestRepository,
    private val overnightRepository: OvernightRepository,
    private val dutyRepository: DutyRepository,
    private val passRepository: PassRepository,
) {
    /**
     * 복무 정보가 아직 없으면 스냅샷을 만들 수 없으므로 null을 반환한다.
     *
     * [generatedAt]과 범위를 주지 않으면 지금 시각과 오늘 기준 전송 범위를 쓴다. 변경 비교처럼
     * 지난 페이로드와 같은 조건으로 다시 만들어야 할 때만 값을 넘긴다.
     */
    suspend fun build(
        generatedAt: Instant = AppClock.now(),
        rangeStart: LocalDate? = null,
        rangeEnd: LocalDate? = null,
    ): CalendarSnapshot? {
        val profile = profileRepository.get() ?: return null

        val today = AppClock.today()
        val start = rangeStart ?: TransmissionRange.start(today)
        val end = rangeEnd ?: TransmissionRange.end(today)

        val nextOvernightDate = profile.firstOvernightDate?.let { first ->
            val records = overnightRepository.observeRecords().first()
            val forfeitures = overnightRepository.observeForfeitures().first()
            OvernightScheduleCalculator.buildSchedule(first, profile.overnightCycleWeeks, records, forfeitures).nextScheduledDate
        }

        return CalendarSnapshot(
            generatedAt = generatedAt,
            rangeStart = start,
            rangeEnd = end,
            profile = profile,
            nextOvernightDate = nextOvernightDate,
            holidays = holidayRepository.getAll(),
            events = eventRepository.observeAll().first(),
            leaveUsages = leaveRepository.observeAllUsages().first(),
            leaveTypes = leaveRepository.observeTypes().first(),
            overnightRecords = overnightRepository.observeRecords().first(),
            combatRestUsages = combatRestRepository.observeUsages().first(),
            passRecords = passRepository.observeAll().first(),
            dutyAssignments = dutyRepository.observeAll().first(),
        )
    }
}
