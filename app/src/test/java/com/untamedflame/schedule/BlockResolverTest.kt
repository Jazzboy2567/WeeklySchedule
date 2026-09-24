package com.untamedflame.schedule

import com.untamedflame.schedule.core.BlockResolver
import com.untamedflame.schedule.data.ScheduleBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BlockResolverTest {

    // Monday 2024-01-01 was a Monday (dayOfWeek = 1).
    private val monday9 = LocalDateTime.of(2024, 1, 1, 9, 30)

    private fun block(day: Int, start: Int, end: Int, title: String = "x") =
        ScheduleBlock(id = day.toLong(), dayOfWeek = day, startMinutes = start, endMinutes = end, title = title)

    @Test
    fun currentBlock_insideWindow_isFound() {
        val blocks = listOf(block(1, 9 * 60, 10 * 60, "Work"))
        assertEquals("Work", BlockResolver.currentBlock(blocks, monday9)?.title)
    }

    @Test
    fun currentBlock_inGap_isNull() {
        val blocks = listOf(block(1, 10 * 60, 11 * 60, "Work"))
        assertNull(BlockResolver.currentBlock(blocks, monday9))
    }

    @Test
    fun currentBlock_endIsExclusive() {
        val blocks = listOf(block(1, 8 * 60, 9 * 60, "Earlier"))
        // 9:30 is after an 8:00-9:00 block, so nothing is current.
        assertNull(BlockResolver.currentBlock(blocks, monday9))
    }

    @Test
    fun nextBoundary_isEndOfCurrentBlock() {
        val blocks = listOf(block(1, 9 * 60, 10 * 60, "Work"))
        val expected = LocalDateTime.of(2024, 1, 1, 10, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, BlockResolver.nextBoundaryMillis(blocks, monday9))
    }

    @Test
    fun nextBoundary_noBlocks_isNull() {
        assertNull(BlockResolver.nextBoundaryMillis(emptyList(), monday9))
    }

    @Test
    fun nextReminder_repeatsOnInterval_whileBlockActive() {
        // Block 9:00-11:00 on Monday, 30-min repeats. At 9:30 the next tick is 10:00.
        val blocks = listOf(block(1, 9 * 60, 11 * 60, "Work"))
        val expected = LocalDateTime.of(2024, 1, 1, 10, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, BlockResolver.nextReminderMillis(blocks, monday9, 30))
    }

    @Test
    fun nextReminder_zeroInterval_onlyFiresAtStart() {
        // 9:00-11:00 block; at 9:30 with interval 0 there is no more tick today, so the
        // next reminder is next Monday's 9:00 start.
        val blocks = listOf(block(1, 9 * 60, 11 * 60, "Work"))
        val nextWeekStart = LocalDateTime.of(2024, 1, 8, 9, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(nextWeekStart, BlockResolver.nextReminderMillis(blocks, monday9, 0))
    }
}
