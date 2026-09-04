package com.kangminsang.hagoondori.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kangminsang.hagoondori.data.local.entity.LeaveGrantEntity
import com.kangminsang.hagoondori.data.local.entity.LeaveTypeEntity
import com.kangminsang.hagoondori.data.local.entity.LeaveUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leave_type ORDER BY name")
    fun observeTypes(): Flow<List<LeaveTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertType(entity: LeaveTypeEntity)

    @Delete
    suspend fun deleteType(entity: LeaveTypeEntity)

    @Query("SELECT * FROM leave_grant ORDER BY grantedDate")
    fun observeAllGrants(): Flow<List<LeaveGrantEntity>>

    @Query("SELECT * FROM leave_grant WHERE leaveTypeId = :leaveTypeId ORDER BY grantedDate")
    fun observeGrants(leaveTypeId: String): Flow<List<LeaveGrantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGrant(entity: LeaveGrantEntity)

    @Delete
    suspend fun deleteGrant(entity: LeaveGrantEntity)

    @Query("SELECT * FROM leave_usage ORDER BY startDate")
    fun observeAllUsages(): Flow<List<LeaveUsageEntity>>

    @Query("SELECT * FROM leave_usage WHERE leaveTypeId = :leaveTypeId ORDER BY startDate")
    fun observeUsages(leaveTypeId: String): Flow<List<LeaveUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsage(entity: LeaveUsageEntity)

    @Delete
    suspend fun deleteUsage(entity: LeaveUsageEntity)
}
