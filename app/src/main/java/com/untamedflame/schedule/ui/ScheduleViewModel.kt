package com.untamedflame.schedule.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.untamedflame.schedule.data.ScheduleBlock
import com.untamedflame.schedule.data.ScheduleDatabase
import com.untamedflame.schedule.notify.ScheduleAlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ScheduleViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = ScheduleDatabase.get(app).scheduleDao()

    val blocks: StateFlow<List<ScheduleBlock>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Create or replace a group of identical blocks, one per selected day. Passing an existing
     * [groupId] replaces that whole group (used when editing); null creates a new group.
     */
    fun saveGroup(
        groupId: String?,
        days: Set<Int>,
        startMinutes: Int,
        endMinutes: Int,
        title: String
    ) = viewModelScope.launch {
        val gid = groupId ?: UUID.randomUUID().toString()
        dao.deleteByGroup(gid)
        for (day in days) {
            dao.insert(
                ScheduleBlock(
                    dayOfWeek = day,
                    startMinutes = startMinutes,
                    endMinutes = endMinutes,
                    title = title,
                    groupId = gid
                )
            )
        }
        ScheduleAlarmScheduler.syncNow(getApplication())
    }

    fun deleteGroup(groupId: String) = viewModelScope.launch {
        dao.deleteByGroup(groupId)
        ScheduleAlarmScheduler.syncNow(getApplication())
    }

    /** Called from the UI after permissions are (re)granted to force a refresh. */
    fun resync() = viewModelScope.launch {
        ScheduleAlarmScheduler.syncNow(getApplication())
    }
}
