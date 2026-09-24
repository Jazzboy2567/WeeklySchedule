package com.untamedflame.schedule.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
private val HandleHeight = 16.dp
private val IndentStep = 14.dp
private const val HOURS = 24
private const val SNAP = 15          // minutes
private const val MIN_DURATION = 30  // minutes

private data class Selection(val days: Set<Int>, val start: Int, val end: Int, val colMin: Int, val colMax: Int)
private data class Placed(val block: ScheduleBlock, val indent: Int)

@Composable
fun WeekGrid(
    blocks: List<ScheduleBlock>,
    draft: DraftSel?,
    onDraftChange: (DraftSel?) -> Unit,
    onCommitDraft: () -> Unit,
    onCreate: (days: Set<Int>, start: Int, end: Int) -> Unit,
    onBlockClick: (ScheduleBlock) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    val totalHeight = HourHeight * HOURS
    val currentDraft by rememberUpdatedState(draft)

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
                        val cMin = min((a.x / colWidthPx).toInt(), (b.x / colWidthPx).toInt()).coerceIn(0, 6)
                        val cMax = max((a.x / colWidthPx).toInt(), (b.x / colWidthPx).toInt()).coerceIn(0, 6)
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

                    // Interaction layer: tap = place a draft; long-press + drag = multi-day create.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(colWidthPx, hourPx) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { o -> selStart = o; selCur = o },
                                    onDrag = { ch, _ -> ch.consume(); selCur = ch.position },
                                    onDragEnd = {
                                        val s = selStart; val c = selCur
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
                                    onDraftChange(DraftSel(colToDay(col), start, start + 60))
                                }
                            }
                    )

                    // Existing blocks, laid out per day with overlaps (largest at back).
                    blocks.groupBy { it.dayOfWeek }.forEach { (_, dayBlocks) ->
                        placeDay(dayBlocks).forEach { placed ->
                            val block = placed.block
                            val col = dayToCol(block.dayOfWeek)
                            val indentDp = minOf(IndentStep * placed.indent, colWidth * 0.55f)
                            val top = HourHeight * (block.startMinutes / 60f)
                            val h = HourHeight * ((block.endMinutes - block.startMinutes) / 60f)
                            Box(
                                Modifier
                                    .offset(x = colWidth * col + indentDp, y = top)
                                    .width(colWidth - indentDp)
                                    .height(h)
                                    .padding(horizontal = 1.dp, vertical = 1.dp)
                                    .background(groupColor(block.groupId), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
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
                    }

                    // Live long-press selection highlight.
                    val s = selStart; val c = selCur
                    if (s != null && c != null) {
                        val sel = selectionFrom(s, c)
                        Box(
                            Modifier
                                .offset(x = colWidth * sel.colMin, y = HourHeight * (sel.start / 60f))
                                .width(colWidth * (sel.colMax - sel.colMin + 1))
                                .height(HourHeight * ((sel.end - sel.start) / 60f))
                                .padding(1.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                                    RoundedCornerShape(6.dp)
                                )
                        )
                    }

                    // The draft block: resizable + movable, tap to open the editor.
                    if (draft != null) {
                        DraftBlock(
                            draft = draft,
                            colWidth = colWidth,
                            colWidthPx = colWidthPx,
                            hourPx = hourPx,
                            currentDraft = { currentDraft },
                            onDraftChange = onDraftChange,
                            onCommit = onCommitDraft
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftBlock(
    draft: DraftSel,
    colWidth: androidx.compose.ui.unit.Dp,
    colWidthPx: Float,
    hourPx: Float,
    currentDraft: () -> DraftSel?,
    onDraftChange: (DraftSel?) -> Unit,
    onCommit: () -> Unit
) {
    val col = dayToCol(draft.dayOfWeek)
    val top = HourHeight * (draft.startMinutes / 60f)
    val h = HourHeight * ((draft.endMinutes - draft.startMinutes) / 60f)
    val accent = MaterialTheme.colorScheme.primary

    Box(
        Modifier
            .offset(x = colWidth * col, y = top)
            .width(colWidth)
            .height(h)
    ) {
        // Body: tap to open editor, drag to move.
        Box(
            Modifier
                .fillMaxSize()
                .padding(1.dp)
                .background(accent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .border(1.5.dp, accent, RoundedCornerShape(6.dp))
                .pointerInput(colWidthPx, hourPx) {
                    detectTapGestures { onCommit() }
                }
                .pointerInput(colWidthPx, hourPx) {
                    var bStart = 0; var bEnd = 0; var bCol = 0; var aX = 0f; var aY = 0f
                    detectDragGestures(
                        onDragStart = {
                            val d = currentDraft(); if (d != null) {
                                bStart = d.startMinutes; bEnd = d.endMinutes; bCol = dayToCol(d.dayOfWeek)
                            }
                            aX = 0f; aY = 0f
                        },
                        onDrag = { ch, off ->
                            ch.consume(); aX += off.x; aY += off.y
                            val dur = bEnd - bStart
                            val ns = snap(bStart + aY / hourPx * 60f).coerceIn(0, 1440 - dur)
                            val nc = ((bCol * colWidthPx + aX) / colWidthPx).roundToInt().coerceIn(0, 6)
                            onDraftChange(DraftSel(colToDay(nc), ns, ns + dur))
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${ScheduleBlock.formatMinutes(draft.startMinutes)} – ${ScheduleBlock.formatMinutes(draft.endMinutes)}",
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        // Top resize handle -> moves the start time.
        ResizeHandle(
            modifier = Modifier.align(Alignment.TopCenter),
            hourPx = hourPx,
            onBase = { currentDraft()?.startMinutes ?: 0 },
            onDelta = { base, deltaMin ->
                val d = currentDraft() ?: return@ResizeHandle
                val ns = snap((base + deltaMin).toFloat()).coerceIn(0, d.endMinutes - MIN_DURATION)
                onDraftChange(d.copy(startMinutes = ns))
            }
        )
        // Bottom resize handle -> moves the end time.
        ResizeHandle(
            modifier = Modifier.align(Alignment.BottomCenter),
            hourPx = hourPx,
            onBase = { currentDraft()?.endMinutes ?: 0 },
            onDelta = { base, deltaMin ->
                val d = currentDraft() ?: return@ResizeHandle
                val ne = snap((base + deltaMin).toFloat()).coerceIn(d.startMinutes + MIN_DURATION, 1440)
                onDraftChange(d.copy(endMinutes = ne))
            }
        )

        // Cancel the draft.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
                .background(accent, RoundedCornerShape(10.dp))
                .clickable { onDraftChange(null) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Discard", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ResizeHandle(
    modifier: Modifier,
    hourPx: Float,
    onBase: () -> Int,
    onDelta: (base: Int, deltaMinutes: Int) -> Unit
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(HandleHeight)
            .pointerInput(hourPx) {
                var base = 0; var acc = 0f
                detectVerticalDragGestures(
                    onDragStart = { base = onBase(); acc = 0f },
                    onVerticalDrag = { ch, dy ->
                        ch.consume(); acc += dy
                        onDelta(base, (acc / hourPx * 60f).roundToInt())
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(32.dp)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
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

/** Order a day's blocks so the longest sits at the back; overlapping shorter ones get indented. */
private fun placeDay(dayBlocks: List<ScheduleBlock>): List<Placed> {
    val sorted = dayBlocks.sortedWith(
        compareByDescending<ScheduleBlock> { it.endMinutes - it.startMinutes }.thenBy { it.startMinutes }
    )
    val placed = mutableListOf<Placed>()
    for (b in sorted) {
        val indent = placed.count { p ->
            p.block.startMinutes < b.endMinutes && b.startMinutes < p.block.endMinutes
        }
        placed += Placed(b, indent)
    }
    return placed
}

private fun snap(minutes: Float): Int = ((minutes / SNAP).roundToInt() * SNAP).coerceIn(0, 1440)

private fun hourLabel(hour24: Int): String {
    val period = if (hour24 < 12) "AM" else "PM"
    val h = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return "$h $period"
}
