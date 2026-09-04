package com.kangminsang.hagoondori.data.mapper

import com.kangminsang.hagoondori.core.model.CombatRestGrant
import com.kangminsang.hagoondori.core.model.CombatRestUsage
import com.kangminsang.hagoondori.core.model.DutyAssignment
import com.kangminsang.hagoondori.core.model.Event
import com.kangminsang.hagoondori.core.model.Holiday
import com.kangminsang.hagoondori.core.model.LeaveGrant
import com.kangminsang.hagoondori.core.model.LeaveType
import com.kangminsang.hagoondori.core.model.LeaveUsage
import com.kangminsang.hagoondori.core.model.OvernightForfeiture
import com.kangminsang.hagoondori.core.model.OvernightRecord
import com.kangminsang.hagoondori.core.model.PassRecord
import com.kangminsang.hagoondori.core.model.SyncState
import com.kangminsang.hagoondori.core.model.UserProfile
import com.kangminsang.hagoondori.data.local.entity.CombatRestGrantEntity
import com.kangminsang.hagoondori.data.local.entity.CombatRestUsageEntity
import com.kangminsang.hagoondori.data.local.entity.DutyAssignmentEntity
import com.kangminsang.hagoondori.data.local.entity.EventEntity
import com.kangminsang.hagoondori.data.local.entity.HolidayEntity
import com.kangminsang.hagoondori.data.local.entity.LeaveGrantEntity
import com.kangminsang.hagoondori.data.local.entity.LeaveTypeEntity
import com.kangminsang.hagoondori.data.local.entity.LeaveUsageEntity
import com.kangminsang.hagoondori.data.local.entity.OvernightForfeitureEntity
import com.kangminsang.hagoondori.data.local.entity.OvernightRecordEntity
import com.kangminsang.hagoondori.data.local.entity.PassRecordEntity
import com.kangminsang.hagoondori.data.local.entity.SyncStateEntity
import com.kangminsang.hagoondori.data.local.entity.UserProfileEntity

/**
 * Room 엔티티 ↔ core 도메인 모델 변환. 얇은 필드 대 필드 매핑만 하며, 계산은
 * 절대 하지 않는다 - 계산은 core.calc의 몫이다.
 */

fun UserProfileEntity.toCore(): UserProfile = UserProfile(
    enlistmentDate = enlistmentDate,
    dischargeDate = dischargeDate,
    promotionDate = promotionDate,
    firstOvernightDate = firstOvernightDate,
    overnightCycleWeeks = overnightCycleWeeks,
    weekdayPassPerMonth = weekdayPassPerMonth,
    holidayPassPerMonth = holidayPassPerMonth,
    wakeUpTime = wakeUpTime,
    dinnerTime = dinnerTime,
    autoAddOffDuty = autoAddOffDuty,
)

fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
    enlistmentDate = enlistmentDate,
    dischargeDate = dischargeDate,
    promotionDate = promotionDate,
    firstOvernightDate = firstOvernightDate,
    overnightCycleWeeks = overnightCycleWeeks,
    weekdayPassPerMonth = weekdayPassPerMonth,
    holidayPassPerMonth = holidayPassPerMonth,
    wakeUpTime = wakeUpTime,
    dinnerTime = dinnerTime,
    autoAddOffDuty = autoAddOffDuty,
)

fun LeaveTypeEntity.toCore(): LeaveType = LeaveType(id, name, cap, overflowBehavior)
fun LeaveType.toEntity(): LeaveTypeEntity = LeaveTypeEntity(id, name, cap, overflowBehavior)

fun LeaveGrantEntity.toCore(): LeaveGrant = LeaveGrant(id, leaveTypeId, days, grantedDate, reason)
fun LeaveGrant.toEntity(): LeaveGrantEntity = LeaveGrantEntity(id, leaveTypeId, days, grantedDate, reason)

fun LeaveUsageEntity.toCore(): LeaveUsage = LeaveUsage(id, leaveTypeId, startDate, endDate, label)
fun LeaveUsage.toEntity(): LeaveUsageEntity = LeaveUsageEntity(id, leaveTypeId, startDate, endDate, label)

fun CombatRestGrantEntity.toCore(): CombatRestGrant = CombatRestGrant(id, days, grantedDate, reason)
fun CombatRestGrant.toEntity(): CombatRestGrantEntity = CombatRestGrantEntity(id, days, grantedDate, reason)

fun CombatRestUsageEntity.toCore(): CombatRestUsage = CombatRestUsage(id, startDate, endDate, memo)
fun CombatRestUsage.toEntity(): CombatRestUsageEntity = CombatRestUsageEntity(id, startDate, endDate, memo)

fun OvernightRecordEntity.toCore(): OvernightRecord = OvernightRecord(id, date, memo)
fun OvernightRecord.toEntity(): OvernightRecordEntity = OvernightRecordEntity(id, date, memo)

fun OvernightForfeitureEntity.toCore(): OvernightForfeiture = OvernightForfeiture(id, slotIndex, reason, recordedDate)
fun OvernightForfeiture.toEntity(): OvernightForfeitureEntity =
    OvernightForfeitureEntity(id, slotIndex, reason, recordedDate)

fun PassRecordEntity.toCore(): PassRecord = PassRecord(id, date, type, memo)
fun PassRecord.toEntity(): PassRecordEntity = PassRecordEntity(id, date, type, memo)

fun EventEntity.toCore(): Event = Event(id, title, startDate, endDate, isImportant, memo)
fun Event.toEntity(): EventEntity = EventEntity(id, title, startDate, endDate, isImportant, memo)

fun DutyAssignmentEntity.toCore(): DutyAssignment = DutyAssignment(id, date, type)
fun DutyAssignment.toEntity(): DutyAssignmentEntity = DutyAssignmentEntity(id, date, type)

fun HolidayEntity.toCore(): Holiday = Holiday(date, name, isSubstitute)
fun Holiday.toEntity(): HolidayEntity = HolidayEntity(date, name, isSubstitute)

fun SyncStateEntity.toCore(): SyncState = SyncState(lastSyncedAt, pendingChangeCount)
fun SyncState.toEntity(): SyncStateEntity = SyncStateEntity(
    lastSyncedAt = lastSyncedAt,
    pendingChangeCount = pendingChangeCount,
)
