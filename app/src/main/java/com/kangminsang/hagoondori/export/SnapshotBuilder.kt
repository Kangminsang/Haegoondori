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
    /** 복무 정보가 아직 없으면 스냅샷을 만들 수 없으므로 null을 반환한다. */
    suspend fun build(): CalendarSnapshot? {
        val profile = profileRepository.get() ?: return null

        val today = AppClock.today()
        val rangeStart = TransmissionRange.start(today)
        val rangeEnd = TransmissionRange.end(today)

        val nextOvernightDate = profile.firstOvernightDate?.let { first ->
            val records = overnightRepository.observeRecords().first()
            val forfeitures = overnightRepository.observeForfeitures().first()
            OvernightScheduleCalculator.buildSchedule(first, profile.overnightCycleWeeks, records, forfeitures).nextScheduledDate
        }

        return CalendarSnapshot(
            generatedAt = AppClock.now(),
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
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
