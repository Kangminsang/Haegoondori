package com.kangminsang.hagoondori.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5,
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

        /**
         * 휴가 종류 세 가지(정기/포상/위로)를 DB를 열 때마다 맞춰 둔다. 받을 수 있는 휴가가
         * 사실상 이 셋뿐이라 종류를 직접 추가하는 화면을 두지 않는다. 정기휴가는 27일로
         * 총량이 고정돼 부여 기록이 필요 없고(fixedDays), 포상휴가(상한 17일, 부여를 기록하며
         * 상한 초과분은 전투휴무로 전환)와 위로휴가는 부여 기록의 합이 총량이다. 같은 이름의
         * 종류가 이미 있으면 새로 만들지 않고 고정 일수만 맞춘다. 휴가 종류는 장치 페이로드에 들어가지 않아 동기화 대기 건수는
         * 올리지 않는다. 포상 초과분의 전투휴무 전환은 스펙 3.2.2절.
         */
        val SEED_DEFAULT_LEAVE_TYPES = object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                listOf(
                    LeaveSeed("leave-consolation", "위로휴가", cap = null, fixedDays = null),
                    LeaveSeed("leave-regular", "정기휴가", cap = null, fixedDays = 27),
                    LeaveSeed("leave-reward", "포상휴가", cap = 17, fixedDays = null),
                ).forEachIndexed { index, seed ->
                    val overflow = if (seed.cap != null) "CONVERT_TO_COMBAT_REST" else "NONE"
                    db.execSQL(
                        "INSERT INTO leave_type (id, name, cap, overflowBehavior, fixedDays, sortOrder) " +
                            "SELECT ?, ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM leave_type WHERE name = ?)",
                        arrayOf<Any?>(seed.id, seed.name, seed.cap, overflow, seed.fixedDays, index, seed.name),
                    )
                    db.execSQL(
                        "UPDATE leave_type SET fixedDays = ? WHERE name = ?",
                        arrayOf<Any?>(seed.fixedDays, seed.name),
                    )
                }
            }
        }

        private data class LeaveSeed(val id: String, val name: String, val cap: Int?, val fixedDays: Int?)

        /** 휴가 종류를 사용자가 정렬할 수 있게 컬럼을 추가한다. 기존 표시 순서(이름순)를 그대로 순번으로 옮긴다. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE leave_type ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE leave_type SET sortOrder = " +
                        "(SELECT COUNT(*) FROM leave_type AS other WHERE other.name < leave_type.name)",
                )
            }
        }

        /** 부여에 유효 기간(`expiryDate`)을 둘 수 있게 컬럼을 추가한다. 기존 부여는 기한 없음. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE leave_grant ADD COLUMN expiryDate INTEGER")
            }
        }

        /** 고정 총량 휴가(정기휴가)를 위해 컬럼을 추가한다. 값은 [SEED_DEFAULT_LEAVE_TYPES]가 채운다. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE leave_type ADD COLUMN fixedDays INTEGER")
            }
        }

        /** 외박을 기간으로 기록하기 위해 `endDate`를 추가한다. 기존 기록은 하루짜리(시작일=종료일)로 유지된다. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE overnight_record ADD COLUMN endDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE overnight_record SET endDate = date")
            }
        }
    }
}
