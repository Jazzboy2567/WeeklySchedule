package com.untamedflame.schedule.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One entry in the weekly schedule.
 *
 * @param dayOfWeek 1 = Monday ... 7 = Sunday (matches java.time.DayOfWeek.value).
 * @param startMinutes minutes from midnight the block begins (0..1439).
 * @param endMinutes   minutes from midnight the block ends (1..1440, exclusive end).
 * @param title        what the user should be doing during this block.
 * @param groupId      links blocks created together across multiple days, so editing or
 *                     deleting one affects the whole set. Empty for legacy single blocks.
 */
@Entity(tableName = "schedule_blocks")
data class ScheduleBlock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val title: String,
    val groupId: String = ""
) {
    val startLabel: String get() = formatMinutes(startMinutes)
    val endLabel: String get() = formatMinutes(endMinutes)

    companion object {
        fun formatMinutes(m: Int): String {
            val clamped = m.coerceIn(0, 1440)
            val h = (clamped / 60) % 24
            val min = clamped % 60
            val period = if (h < 12) "AM" else "PM"
            val h12 = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            return "%d:%02d %s".format(h12, min, period)
        }
    }
}
