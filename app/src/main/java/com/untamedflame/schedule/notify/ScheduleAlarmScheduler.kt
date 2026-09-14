package com.untamedflame.schedule.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.untamedflame.schedule.core.BlockResolver
import com.untamedflame.schedule.data.ScheduleDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Schedules a single AlarmManager alarm at the next block boundary. When it fires,
 * [BlockAlarmReceiver] refreshes the notification and re-arms the next alarm.
 */
object ScheduleAlarmScheduler {

    private const val REQUEST_CODE = 42

    private fun alarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, BlockAlarmReceiver::class.java).apply {
            action = BlockAlarmReceiver.ACTION_BOUNDARY
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Refresh the notification now and arm the next boundary alarm. Safe to call often. */
    suspend fun syncNow(context: Context) {
        val blocks = withContext(Dispatchers.IO) {
            ScheduleDatabase.get(context).scheduleDao().getAll()
        }
        NotificationHelper.refresh(context, blocks)

        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = alarmIntent(context)
        am.cancel(pending)

        val nextMillis = BlockResolver.nextBoundaryMillis(blocks, LocalDateTime.now()) ?: return

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMillis, pending)
        } else {
            // Fall back to an inexact alarm; the user can grant exact alarms in Settings.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMillis, pending)
        }
    }
}
