package com.kangminsang.hagoondori.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kangminsang.hagoondori.data.local.dao.CombatRestDao
import com.kangminsang.hagoondori.data.local.dao.DutyDao
import com.kangminsang.hagoondori.data.local.dao.EventDao
import com.kangminsang.hagoondori.data.local.dao.HolidayDao
import com.kangminsang.hagoondori.data.local.dao.LeaveDao
import com.kangminsang.hagoondori.data.local.dao.OvernightDao
import com.kangminsang.hagoondori.data.local.dao.PassDao
import com.kangminsang.hagoondori.data.local.dao.SyncStateDao
import com.kangminsang.hagoondori.data.local.dao.UserProfileDao
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
 * 이 앱의 유일한 로컬 데이터베이스 (스펙 8.2절: 서버 없음, 계정 없음, SQLite 기반).
 * 데이터 양이 매우 적으므로(1년치 수백 건) 성능보다 구조의 명확성을 우선한다 -
 * 그래서 엔티티당 별도 파생 캐시 테이블 없이, 원본 레코드만 저장한다.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        LeaveTypeEntity::class,
        LeaveGrantEntity::class,
        LeaveUsageEntity::class,
        CombatRestGrantEntity::class,
        CombatRestUsageEntity::class,
        OvernightRecordEntity::class,
        OvernightForfeitureEntity::class,
        PassRecordEntity::class,
        EventEntity::class,
        DutyAssignmentEntity::class,
        HolidayEntity::class,
        SyncStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class HagoondoriDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun leaveDao(): LeaveDao
    abstract fun combatRestDao(): CombatRestDao
    abstract fun overnightDao(): OvernightDao
    abstract fun passDao(): PassDao
    abstract fun eventDao(): EventDao
    abstract fun dutyDao(): DutyDao
    abstract fun holidayDao(): HolidayDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        const val DATABASE_NAME = "hagoondori.db"
    }
}
