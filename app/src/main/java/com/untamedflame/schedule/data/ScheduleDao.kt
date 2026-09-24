package com.untamedflame.schedule.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedule_blocks ORDER BY dayOfWeek, startMinutes")
    fun observeAll(): Flow<List<ScheduleBlock>>

    @Query("SELECT * FROM schedule_blocks ORDER BY dayOfWeek, startMinutes")
    suspend fun getAll(): List<ScheduleBlock>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: ScheduleBlock): Long

    @Update
    suspend fun update(block: ScheduleBlock)

    @Delete
    suspend fun delete(block: ScheduleBlock)

    @Query("DELETE FROM schedule_blocks WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: String)
}
