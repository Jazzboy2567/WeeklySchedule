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

class ScheduleViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = ScheduleDatabase.get(app).scheduleDao()

    val blocks: StateFlow<List<ScheduleBlock>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addOrUpdate(block: ScheduleBlock) = viewModelScope.launch {
        if (block.id == 0L) dao.insert(block) else dao.update(block)
        ScheduleAlarmScheduler.syncNow(getApplication())
    }

    fun delete(block: ScheduleBlock) = viewModelScope.launch {
        dao.delete(block)
        ScheduleAlarmScheduler.syncNow(getApplication())
    }

    /** Called from the UI after permissions are (re)granted to force a refresh. */
    fun resync() = viewModelScope.launch {
        ScheduleAlarmScheduler.syncNow(getApplication())
    }
}
