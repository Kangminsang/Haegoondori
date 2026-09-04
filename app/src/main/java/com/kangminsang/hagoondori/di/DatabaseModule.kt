package com.kangminsang.hagoondori.di

import android.content.Context
import androidx.room.Room
import com.kangminsang.hagoondori.data.local.HagoondoriDatabase
import com.kangminsang.hagoondori.data.local.dao.CombatRestDao
import com.kangminsang.hagoondori.data.local.dao.DutyDao
import com.kangminsang.hagoondori.data.local.dao.EventDao
import com.kangminsang.hagoondori.data.local.dao.HolidayDao
import com.kangminsang.hagoondori.data.local.dao.LeaveDao
import com.kangminsang.hagoondori.data.local.dao.OvernightDao
import com.kangminsang.hagoondori.data.local.dao.PassDao
import com.kangminsang.hagoondori.data.local.dao.SyncStateDao
import com.kangminsang.hagoondori.data.local.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room 데이터베이스와 각 Dao를 앱 전역 싱글턴으로 제공한다(원칙 B: 서버/계정 없음,
 * 로컬 SQLite 하나가 유일한 저장소).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HagoondoriDatabase =
        Room.databaseBuilder(context, HagoondoriDatabase::class.java, HagoondoriDatabase.DATABASE_NAME)
            .build()

    @Provides
    fun provideUserProfileDao(db: HagoondoriDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideLeaveDao(db: HagoondoriDatabase): LeaveDao = db.leaveDao()

    @Provides
    fun provideCombatRestDao(db: HagoondoriDatabase): CombatRestDao = db.combatRestDao()

    @Provides
    fun provideOvernightDao(db: HagoondoriDatabase): OvernightDao = db.overnightDao()

    @Provides
    fun providePassDao(db: HagoondoriDatabase): PassDao = db.passDao()

    @Provides
    fun provideEventDao(db: HagoondoriDatabase): EventDao = db.eventDao()

    @Provides
    fun provideDutyDao(db: HagoondoriDatabase): DutyDao = db.dutyDao()

    @Provides
    fun provideHolidayDao(db: HagoondoriDatabase): HolidayDao = db.holidayDao()

    @Provides
    fun provideSyncStateDao(db: HagoondoriDatabase): SyncStateDao = db.syncStateDao()
}
