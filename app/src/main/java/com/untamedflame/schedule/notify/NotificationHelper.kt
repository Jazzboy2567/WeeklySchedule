package com.untamedflame.schedule.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.untamedflame.schedule.R
import com.untamedflame.schedule.core.BlockResolver
import com.untamedflame.schedule.data.ScheduleBlock
import com.untamedflame.schedule.ui.MainActivity
import java.time.LocalDateTime

/**
 * Builds and posts the single persistent "what should I be doing now" notification.
 * The notification is ongoing (setOngoing) so it cannot be swiped away.
 */
object NotificationHelper {

    const val CHANNEL_ID = "current_block"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_desc)
                setShowBadge(false)
            }
            val mgr = context.getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(channel)
        }
    }

    /** Recompute the current block and (re)post or clear the ongoing notification. */
    fun refresh(context: Context, blocks: List<ScheduleBlock>, now: LocalDateTime = LocalDateTime.now()) {
        ensureChannel(context)
        val nm = NotificationManagerCompat.from(context)

        val current = BlockResolver.currentBlock(blocks, now)
        if (current == null) {
            // Nothing scheduled right now: remove the notification until the next block.
            nm.cancel(NOTIFICATION_ID)
            return
        }

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_schedule)
            .setContentTitle(current.title)
            .setContentText("${current.startLabel} – ${current.endLabel}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${current.title}\n${current.startLabel} – ${current.endLabel}")
            )
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .build()

        try {
            nm.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted yet; the UI prompts for it.
        }
    }
}
