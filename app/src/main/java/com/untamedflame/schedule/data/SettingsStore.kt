package com.untamedflame.schedule.data

import android.content.Context

enum class NotificationMode { LOCKED, REMINDER }

/**
 * Simple settings persisted in SharedPreferences.
 *
 * - [notificationMode] LOCKED  = ongoing notification that can't be swiped away, shown for the
 *   whole duration of the active block.
 * - [notificationMode] REMINDER = a normal, dismissable notification that alerts when a block
 *   starts and then repeats every [reminderIntervalMinutes] until the block ends
 *   (0 = only at the start).
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var notificationMode: NotificationMode
        get() = when (prefs.getString(KEY_MODE, MODE_LOCKED)) {
            MODE_REMINDER -> NotificationMode.REMINDER
            else -> NotificationMode.LOCKED
        }
        set(value) = prefs.edit()
            .putString(KEY_MODE, if (value == NotificationMode.REMINDER) MODE_REMINDER else MODE_LOCKED)
            .apply()

    /** Minutes between repeat reminders in REMINDER mode. 0 means only at the block's start. */
    var reminderIntervalMinutes: Int
        get() = prefs.getInt(KEY_INTERVAL, 60)
        set(value) = prefs.edit().putInt(KEY_INTERVAL, value.coerceIn(0, 24 * 60)).apply()

    /** true = dark theme, false = light. Defaults to light. */
    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK, value).apply()

    companion object {
        private const val KEY_MODE = "notification_mode"
        private const val KEY_INTERVAL = "reminder_interval"
        private const val KEY_DARK = "dark_theme"
        private const val MODE_LOCKED = "locked"
        private const val MODE_REMINDER = "reminder"
    }
}
