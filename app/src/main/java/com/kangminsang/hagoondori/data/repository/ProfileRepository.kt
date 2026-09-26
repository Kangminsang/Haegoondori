package com.kangminsang.hagoondori.data.repository

import com.kangminsang.hagoondori.core.model.UserProfile
import com.kangminsang.hagoondori.data.local.dao.UserProfileDao
import com.kangminsang.hagoondori.data.mapper.toCore
import com.kangminsang.hagoondori.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 복무 기준 정보 및 규정 설정(스펙 4.3절, F1). 앱 전체에서 단 1개만 존재한다. */
@Singleton
class ProfileRepository @Inject constructor(
    private val dao: UserProfileDao,
    private val syncStateRepository: SyncStateRepository,
) {
    fun observe(): Flow<UserProfile?> = dao.observe().map { it?.toCore() }

    suspend fun get(): UserProfile? = dao.get()?.toCore()

    suspend fun save(profile: UserProfile) {
        dao.upsert(profile.toEntity())
        syncStateRepository.markChanged("복무 정보 저장")
    }
}
