package com.untamedflame.schedule.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires at each schedule boundary. Refreshes the ongoing notification to reflect the
 * block that is now active and arms the next boundary alarm.
 */
class BlockAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ScheduleAlarmScheduler.syncNow(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_BOUNDARY = "com.untamedflame.schedule.ACTION_BOUNDARY"
    }
}
