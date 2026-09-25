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
import com.untamedflame.schedule.data.NotificationMode
import com.untamedflame.schedule.data.ScheduleBlock
import com.untamedflame.schedule.ui.MainActivity
import java.time.LocalDateTime

/**
 * Builds and posts the "what should I be doing now" notification.
 *
 * LOCKED mode   -> an ongoing (un-swipeable), silent notification shown for the whole block.
 * REMINDER mode -> a normal, dismissable notification that alerts at the block start and again
 *                  on each repeat tick while the block is active.
 */
object NotificationHelper {

    // Bumped from the original "current_block" (IMPORTANCE_LOW). A channel's importance can't be
    // raised after creation, so a new id is needed to make the locked notification rank higher.
    const val CHANNEL_LOCKED = "current_block_v2"
    const val CHANNEL_REMINDER = "reminders"
    const val NOTIFICATION_ID = 1001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            // Remove the old low-importance channel so it stops showing in app settings.
            mgr.deleteNotificationChannel("current_block")

            // DEFAULT importance keeps it in the main (top) section rather than the collapsed
            // "silent" group, but with sound/vibration disabled it stays quiet.
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_LOCKED,
                    context.getString(R.string.channel_locked_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.channel_locked_desc)
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                    enableLights(false)
                }
            )
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDER,
                    context.getString(R.string.channel_reminder_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.channel_reminder_desc)
                }
            )
        }
    }

    /** Recompute the current block and post/clear the notification for the given [mode]. */
    fun refresh(
        context: Context,
        blocks: List<ScheduleBlock>,
        mode: NotificationMode,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        ensureChannels(context)
        val nm = NotificationManagerCompat.from(context)
        val current = BlockResolver.currentBlock(blocks, now)

        if (current == null) {
            // In LOCKED mode we actively clear between blocks; REMINDER notifications auto-cancel.
            if (mode == NotificationMode.LOCKED) nm.cancel(NOTIFICATION_ID)
            return
        }

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Single line of text: the task name is the title, the time window is the description.
        val builder = NotificationCompat.Builder(
            context,
            if (mode == NotificationMode.LOCKED) CHANNEL_LOCKED else CHANNEL_REMINDER
        )
            .setSmallIcon(R.drawable.ic_schedule)
            .setContentTitle(current.title)
            .setContentText("${current.startLabel} – ${current.endLabel}")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openIntent)
            .setWhen(System.currentTimeMillis()) // keep it recent so it ranks near the top

        if (mode == NotificationMode.LOCKED) {
            builder.setOngoing(true)
                .setOnlyAlertOnce(true) // don't buzz on every minute's update
                .setPriority(NotificationCompat.PRIORITY_HIGH)
        } else {
            builder.setOngoing(false)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
        }

        try {
            nm.notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted yet; the UI prompts for it.
        }
    }
}
