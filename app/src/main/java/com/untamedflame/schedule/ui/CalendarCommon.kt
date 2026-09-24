package com.untamedflame.schedule.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/** Column order shown in the grid: Sunday first, matching Google Calendar. */
val DAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

/** Grid column (0 = Sun .. 6 = Sat) -> stored dayOfWeek (1 = Mon .. 7 = Sun). */
private val COL_TO_DAY = intArrayOf(7, 1, 2, 3, 4, 5, 6)

fun colToDay(col: Int): Int = COL_TO_DAY[col.coerceIn(0, 6)]

fun dayToCol(day: Int): Int = COL_TO_DAY.indexOf(day).coerceAtLeast(0)

/** Full day names for the editor, indexed by stored dayOfWeek (1..7). */
fun dayFullName(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"; 4 -> "Thursday"
    5 -> "Friday"; 6 -> "Saturday"; else -> "Sunday"
}

/** What the editor is currently editing. groupId == null means a brand-new block. */
data class EditorTarget(
    val groupId: String?,
    val days: Set<Int>,
    val startMinutes: Int,
    val endMinutes: Int,
    val title: String
)

/**
 * An in-progress, unsaved block the user is placing/resizing on the grid.
 * [colMin]..[colMax] are grid columns (0 = Sun .. 6 = Sat), inclusive, so a draft can span days.
 */
data class DraftSel(
    val colMin: Int,
    val colMax: Int,
    val startMinutes: Int,
    val endMinutes: Int
) {
    val days: Set<Int> get() = (colMin..colMax).map { colToDay(it) }.toSet()
}

private val BLOCK_PALETTE = listOf(
    Color(0xFF3B5BDB), // indigo
    Color(0xFF2B8A3E), // green
    Color(0xFFD9480F), // orange
    Color(0xFF862E9C), // purple
    Color(0xFF0B7285), // teal
    Color(0xFFC2255C), // pink
    Color(0xFF5C940D), // lime
    Color(0xFF1864AB)  // blue
)

/** Stable color for a group so multi-day blocks share one hue. */
fun groupColor(seed: String): Color {
    if (seed.isEmpty()) return BLOCK_PALETTE[0]
    return BLOCK_PALETTE[abs(seed.hashCode()) % BLOCK_PALETTE.size]
}
