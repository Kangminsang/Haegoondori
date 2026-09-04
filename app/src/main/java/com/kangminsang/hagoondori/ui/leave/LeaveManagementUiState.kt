package com.kangminsang.hagoondori.ui.leave

import com.kangminsang.hagoondori.core.calc.CombatRestSummary
import com.kangminsang.hagoondori.core.calc.OvernightSchedule
import com.kangminsang.hagoondori.core.model.CombatRestGrant
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightForfeiture
import com.kangminsang.hagoondori.core.model.OvernightRecord
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.UserProfile
import com.kangminsang.hagoondori.ui.dashboard.LeaveTypeSummary
import kotlinx.datetime.LocalDate

/**
 * 차수 소멸 처리(F13) 입력 UI가 고를 수 있는 예정 차수 하나. 사용자는 차수 번호를
 * 직접 입력하지 않고 이 목록에서 골라야 한다(스펙 4.10절 요구사항).
 */
data class UpcomingSlotOption(val slotIndex: Int, val scheduledDate: LocalDate)

/** ③ 출타 관리(스펙 5.1/5.2절) 화면 상태 - 휴가/전투휴무/외박/외출 4개 탭이 공유한다. */
data class LeaveManagementUiState(
    val profile: UserProfile? = null,

    val leaveTypes: List<LeaveType> = emptyList(),
    val leaveGrants: List<LeaveGrant> = emptyList(),
    val leaveUsages: List<LeaveUsage> = emptyList(),
    val leaveSummaries: List<LeaveTypeSummary> = emptyList(),

    val combatRestGrants: List<CombatRestGrant> = emptyList(),
    val combatRestUsages: List<CombatRestUsage> = emptyList(),
    val combatRestSummary: CombatRestSummary = CombatRestSummary(convertedFromLeave = 0, directGranted = 0, totalUsed = 0),

    val overnightRecords: List<OvernightRecord> = emptyList(),
    val overnightForfeitures: List<OvernightForfeiture> = emptyList(),
    val overnightSchedule: OvernightSchedule? = null,
    val upcomingSlotOptions: List<UpcomingSlotOption> = emptyList(),

    val passRecords: List<PassRecord> = emptyList(),
    val holidays: List<Holiday> = emptyList(),
)
