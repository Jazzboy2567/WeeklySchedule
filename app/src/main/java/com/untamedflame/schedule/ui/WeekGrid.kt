package com.untamedflame.schedule.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.untamedflame.schedule.data.ScheduleBlock
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val HourHeight = 56.dp
private val GutterWidth = 52.dp
private val HeaderHeight = 44.dp
private const val HOURS = 24
private const val SNAP = 15          // minutes
private const val MIN_DURATION = 30  // minutes

private data class Selection(val days: Set<Int>, val start: Int, val end: Int, val colMin: Int, val colMax: Int)

@Composable
fun WeekGrid(
    blocks: List<ScheduleBlock>,
    onCreate: (days: Set<Int>, start: Int, end: Int) -> Unit,
    onBlockClick: (ScheduleBlock) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    val totalHeight = HourHeight * HOURS

    // Scroll to ~6 AM on first show so mornings are visible.
    LaunchedEffect(Unit) {
        scroll.scrollTo(with(density) { (HourHeight.toPx() * 6).roundToInt() })
    }

    Column(modifier.fillMaxSize()) {
        // Day header (fixed).
        Row(Modifier.fillMaxWidth().height(HeaderHeight)) {
            Spacer(Modifier.width(GutterWidth))
            DAY_LABELS.forEach { label ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        HorizontalDivider()

        Box(Modifier.weight(1f).verticalScroll(scroll)) {
            Row(Modifier.height(totalHeight)) {
                TimeGutter()
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                    val colWidth = maxWidth / 7
                    val colWidthPx = with(density) { colWidth.toPx() }
                    val hourPx = with(density) { HourHeight.toPx() }

                    var selStart by remember { mutableStateOf<Offset?>(null) }
                    var selCur by remember { mutableStateOf<Offset?>(null) }

                    fun selectionFrom(a: Offset, b: Offset): Selection {
                        val colA = (a.x / colWidthPx).toInt().coerceIn(0, 6)
                        val colB = (b.x / colWidthPx).toInt().coerceIn(0, 6)
                        val cMin = min(colA, colB)
                        val cMax = max(colA, colB)
                        val mA = snap(a.y / hourPx * 60f)
                        val mB = snap(b.y / hourPx * 60f)
                        var start = min(mA, mB)
                        var end = max(mA, mB)
                        if (end - start < MIN_DURATION) end = start + MIN_DURATION
                        start = start.coerceIn(0, 1440 - MIN_DURATION)
                        end = end.coerceIn(start + MIN_DURATION, 1440)
                        val days = (cMin..cMax).map { colToDay(it) }.toSet()
                        return Selection(days, start, end, cMin, cMax)
                    }

                    // Grid lines.
                    val lineColor = MaterialTheme.colorScheme.outlineVariant
                    Canvas(Modifier.fillMaxSize()) {
                        for (h in 0..HOURS) {
                            val y = hourPx * h
                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1f)
                        }
                        for (c in 0..7) {
                            val x = colWidthPx * c
                            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 1f)
                        }
                    }

                    // Interaction layer: long-press + drag to create, tap to create.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(colWidthPx, hourPx, blocks) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { o -> selStart = o; selCur = o },
                                    onDrag = { ch, _ -> ch.consume(); selCur = ch.position },
                                    onDragEnd = {
                                        val s = selStart
                                        val c = selCur
                                        if (s != null && c != null) {
                                            val sel = selectionFrom(s, c)
                                            onCreate(sel.days, sel.start, sel.end)
                                        }
                                        selStart = null; selCur = null
                                    },
                                    onDragCancel = { selStart = null; selCur = null }
                                )
                            }
                            .pointerInput(colWidthPx, hourPx) {
                                detectTapGestures { o ->
                                    val col = (o.x / colWidthPx).toInt().coerceIn(0, 6)
                                    val start = snap(o.y / hourPx * 60f).coerceIn(0, 1440 - 60)
                                    onCreate(setOf(colToDay(col)), start, start + 60)
                                }
                            }
                    )

                    // Existing blocks.
                    blocks.forEach { block ->
                        val col = dayToCol(block.dayOfWeek)
                        val top = HourHeight * (block.startMinutes / 60f)
                        val h = HourHeight * ((block.endMinutes - block.startMinutes) / 60f)
                        val color = groupColor(block.groupId)
                        Box(
                            Modifier
                                .offset(x = colWidth * col, y = top)
                                .width(colWidth)
                                .height(h)
                                .padding(horizontal = 1.dp, vertical = 1.dp)
                                .background(color, RoundedCornerShape(6.dp))
                                .clickable { onBlockClick(block) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Column {
                                Text(
                                    block.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (h > HourHeight * 0.5f) {
                                    Text(
                                        ScheduleBlock.formatMinutes(block.startMinutes),
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 9.sp,
                                        lineHeight = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Live selection highlight while dragging.
                    val s = selStart
                    val c = selCur
                    if (s != null && c != null) {
                        val sel = selectionFrom(s, c)
                        val top = HourHeight * (sel.start / 60f)
                        val h = HourHeight * ((sel.end - sel.start) / 60f)
                        val left = colWidth * sel.colMin
                        val wCols = (sel.colMax - sel.colMin + 1)
                        Box(
                            Modifier
                                .offset(x = left, y = top)
                                .width(colWidth * wCols)
                                .height(h)
                                .padding(1.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                                    RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeGutter() {
    Column(Modifier.width(GutterWidth).fillMaxHeight()) {
        for (h in 0 until HOURS) {
            Box(Modifier.fillMaxWidth().height(HourHeight), contentAlignment = Alignment.TopEnd) {
                Text(
                    hourLabel(h),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp),
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun snap(minutes: Float): Int =
    ((minutes / SNAP).roundToInt() * SNAP).coerceIn(0, 1440)

private fun hourLabel(hour24: Int): String {
    val period = if (hour24 < 12) "AM" else "PM"
    val h = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return "$h $period"
}
