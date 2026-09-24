package com.untamedflame.schedule.core

import com.untamedflame.schedule.data.ScheduleBlock
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Pure logic for interpreting a weekly schedule against a point in time.
 * No Android dependencies, so it is easy to unit test.
 */
object BlockResolver {

    private fun minutesOfWeek(now: LocalDateTime): Int {
        // Monday 00:00 == 0.
        val dayIndex = now.dayOfWeek.value - 1 // 0..6
        return dayIndex * 24 * 60 + now.hour * 60 + now.minute
    }

    /** The block that contains [now], or null if the user is in a free gap. */
    fun currentBlock(blocks: List<ScheduleBlock>, now: LocalDateTime): ScheduleBlock? {
        val nowMin = minutesOfWeek(now)
        return blocks.firstOrNull { b ->
            val start = (b.dayOfWeek - 1) * 24 * 60 + b.startMinutes
            val end = (b.dayOfWeek - 1) * 24 * 60 + b.endMinutes
            nowMin in start until end
        }
    }

    /**
     * The next boundary (any block start or end) strictly after [now], as epoch millis.
     * Returns null when there are no blocks at all.
     */
    fun nextBoundaryMillis(
        blocks: List<ScheduleBlock>,
        now: LocalDateTime,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long? {
        if (blocks.isEmpty()) return null
        var best: LocalDateTime? = null
        for (b in blocks) {
            for (minute in intArrayOf(b.startMinutes, b.endMinutes)) {
                val candidate = nextOccurrence(b.dayOfWeek, minute, now)
                if (best == null || candidate.isBefore(best)) best = candidate
            }
        }
        return best?.atZone(zone)?.toInstant()?.toEpochMilli()
    }

    /**
     * REMINDER mode: the next time a reminder should fire strictly after [now], as epoch millis.
     * Reminders fire at each block's start and then every [intervalMinutes] until the block ends
     * (intervalMinutes <= 0 means only at the start). Returns null when there are no blocks.
     */
    fun nextReminderMillis(
        blocks: List<ScheduleBlock>,
        now: LocalDateTime,
        intervalMinutes: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long? {
        if (blocks.isEmpty()) return null
        val nowMs = now.atZone(zone).toInstant().toEpochMilli()
        var best: Long? = null
        for (b in blocks) {
            val durationMs = (b.endMinutes - b.startMinutes).toLong() * 60_000L
            // Consider this week's and next week's occurrence so an in-progress block's
            // remaining interval ticks are covered as well as the next upcoming start.
            for (startDt in occurrencesAround(b.dayOfWeek, b.startMinutes, now)) {
                val startMs = startDt.atZone(zone).toInstant().toEpochMilli()
                val endMs = startMs + durationMs
                var t = startMs
                val step = if (intervalMinutes > 0) intervalMinutes.toLong() * 60_000L else durationMs + 1
                while (t < endMs) {
                    if (t > nowMs && (best == null || t < best!!)) best = t
                    if (intervalMinutes <= 0) break
                    t += step
                }
            }
        }
        return best
    }

    /** Start date-times for [dayOfWeek] in the current week and the following week. */
    private fun occurrencesAround(dayOfWeek: Int, minuteOfDay: Int, now: LocalDateTime): List<LocalDateTime> {
        val monday = now.toLocalDate().minusDays((now.dayOfWeek.value - 1).toLong())
        val thisWeek = monday.plusDays((dayOfWeek - 1).toLong()).atStartOfDay().plusMinutes(minuteOfDay.toLong())
        return listOf(thisWeek, thisWeek.plusWeeks(1))
    }

    /**
     * The next date-time on [dayOfWeek] (1=Mon..7=Sun) at [minuteOfDay] minutes past
     * midnight that is strictly after [now]. Basing this on midnight + minutes keeps
     * the "ends at 1440 (== next midnight)" case correct.
     */
    private fun nextOccurrence(dayOfWeek: Int, minuteOfDay: Int, now: LocalDateTime): LocalDateTime {
        var date = now.toLocalDate()
        var guard = 0
        while (date.dayOfWeek.value != dayOfWeek && guard < 8) {
            date = date.plusDays(1)
            guard++
        }
        var candidate = date.atStartOfDay().plusMinutes(minuteOfDay.toLong())
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusWeeks(1)
        }
        return candidate
    }
}
