package com.untamedflame.schedule.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms alarms and re-posts the notification after a reboot, app update, or clock/
 * timezone change (all of which clear pending alarms).
 */
class BootReceiver : BroadcastReceiver() {

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
}
