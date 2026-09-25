package com.untamedflame.schedule.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.untamedflame.schedule.data.ScheduleBlock
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val GutterWidth = 52.dp
private val HeaderHeight = 44.dp
private val HandleHeight = 16.dp
private val HandleWidth = 16.dp
private val IndentStep = 14.dp
private val DefaultHour = 56.dp
private val MinHour = 30.dp
private val MaxHour = 150.dp
private const val HOURS = 24
private const val SNAP = 15
private const val MIN_DURATION = 30

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
    // Vertical time scale (pinch to zoom) and focused day are shared across the transition.
    var hourHeight by remember { mutableStateOf(DefaultHour) }
    var focusedCol by remember { mutableStateOf<Int?>(null) }

    Crossfade(
        targetState = focusedCol,
        animationSpec = tween(300),
        label = "dayFocus",
        modifier = modifier.fillMaxSize()
    ) { fc ->
        GridContent(
            visibleCols = if (fc == null) (0..6).toList() else listOf(fc),
            focused = fc != null,
            hourHeight = hourHeight,
            onZoom = { factor -> hourHeight = (hourHeight * factor).coerceIn(MinHour, MaxHour) },
            onHeaderTap = { col -> focusedCol = if (fc == null) col else null },
            blocks = blocks,
            draft = draft,
            onDraftChange = onDraftChange,
            onCommitDraft = onCommitDraft,
            onCreate = onCreate,
            onBlockClick = onBlockClick
        )
    }
}

@Composable
private fun GridContent(
    visibleCols: List<Int>,
    focused: Boolean,
    hourHeight: Dp,
    onZoom: (Float) -> Unit,
    onHeaderTap: (Int) -> Unit,
    blocks: List<ScheduleBlock>,
    draft: DraftSel?,
    onDraftChange: (DraftSel?) -> Unit,
    onCommitDraft: () -> Unit,
    onCreate: (days: Set<Int>, start: Int, end: Int) -> Unit,
    onBlockClick: (ScheduleBlock) -> Unit
) {
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    val totalHeight = hourHeight * HOURS
    val currentDraft by rememberUpdatedState(draft)
    val n = visibleCols.size
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        scroll.scrollTo(with(density) { (hourHeight.toPx() * 6).roundToInt() })
    }

    val gridBackground = Brush.verticalGradient(
        listOf(scheme.primaryContainer.copy(alpha = 0.35f), scheme.surface, scheme.surface)
    )

    Column(Modifier.fillMaxSize()) {
        // Header: tap a day to focus it; tap the focused day to return to the week.
        Row(
            Modifier
                .fillMaxWidth()
                .height(HeaderHeight)
                .background(scheme.primaryContainer.copy(alpha = 0.45f))
        ) {
            Spacer(Modifier.width(GutterWidth))
            visibleCols.forEach { col ->
                val weekend = col == 0 || col == 6
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable { onHeaderTap(col) },
                    contentAlignment = Alignment.Center
                ) {
                    if (focused) {
                        Text(
                            "‹  ${dayFullName(colToDay(col))}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary
                        )
                    } else {
                        Text(
                            DAY_LABELS[col],
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (weekend) scheme.primary else scheme.onSurface
                        )
                    }
                }
            }
        }
        HorizontalDivider()

        Box(Modifier.weight(1f).verticalScroll(scroll).background(gridBackground)) {
            Row(Modifier.height(totalHeight)) {
                TimeGutter(hourHeight)
                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // Two-finger pinch adjusts the time scale without disturbing scroll/taps.
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.count { it.pressed } >= 2) {
                                        val zoom = event.calculateZoom()
                                        if (zoom != 1f) {
                                            onZoom(zoom)
                                            event.changes.forEach { if (it.pressed) it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    val colWidth = maxWidth / n
                    val colWidthPx = with(density) { colWidth.toPx() }
                    val hourPx = with(density) { hourHeight.toPx() }

                    var selStart by remember { mutableStateOf<Offset?>(null) }
                    var selCur by remember { mutableStateOf<Offset?>(null) }

                    fun snapMin(y: Float) = snap(y / hourPx * 60f)
                    fun idxAt(x: Float) = (x / colWidthPx).toInt().coerceIn(0, n - 1)

                    // Grid shading + lines.
                    val lineColor = scheme.outlineVariant
                    val weekendTint = scheme.primary.copy(alpha = 0.06f)
                    val bandTint = scheme.onSurface.copy(alpha = 0.025f)
                    Canvas(Modifier.fillMaxSize()) {
                        for (h in 0 until HOURS step 2) {
                            drawRect(bandTint, Offset(0f, hourPx * h), Size(size.width, hourPx))
                        }
                        visibleCols.forEachIndexed { i, col ->
                            if (col == 0 || col == 6) {
                                drawRect(weekendTint, Offset(colWidthPx * i, 0f), Size(colWidthPx, size.height))
                            }
                        }
                        for (h in 0..HOURS) {
                            val y = hourPx * h
                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1f)
                        }
                        for (c in 0..n) {
                            val x = colWidthPx * c
                            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 1f)
                        }
                    }

                    // Interaction: tap = place a draft; long-press + drag = multi-day create.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(colWidthPx, hourPx, n) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { o -> selStart = o; selCur = o },
                                    onDrag = { ch, _ -> ch.consume(); selCur = ch.position },
                                    onDragEnd = {
                                        val s = selStart; val c = selCur
                                        if (s != null && c != null) {
                                            val iMin = min(idxAt(s.x), idxAt(c.x))
                                            val iMax = max(idxAt(s.x), idxAt(c.x))
                                            var start = min(snapMin(s.y), snapMin(c.y))
                                            var end = max(snapMin(s.y), snapMin(c.y))
                                            if (end - start < MIN_DURATION) end = start + MIN_DURATION
                                            start = start.coerceIn(0, 1440 - MIN_DURATION)
                                            end = end.coerceIn(start + MIN_DURATION, 1440)
                                            val days = (iMin..iMax).map { colToDay(visibleCols[it]) }.toSet()
                                            onCreate(days, start, end)
                                        }
                                        selStart = null; selCur = null
                                    },
                                    onDragCancel = { selStart = null; selCur = null }
                                )
                            }
                            .pointerInput(colWidthPx, hourPx, n) {
                                detectTapGestures { o ->
                                    val gridCol = visibleCols[idxAt(o.x)]
                                    val start = snapMin(o.y).coerceIn(0, 1440 - 60)
                                    onDraftChange(DraftSel(gridCol, gridCol, start, start + 60))
                                }
                            }
                    )

                    // Existing blocks (overlaps: longest at back, shorter indented on top).
                    blocks.groupBy { it.dayOfWeek }.forEach { (day, dayBlocks) ->
                        val idx = visibleCols.indexOf(dayToCol(day))
                        if (idx >= 0) {
                            placeDay(dayBlocks).forEach { placed ->
                                val block = placed.block
                                val indentDp = minOf(IndentStep * placed.indent, colWidth * 0.55f)
                                val top = hourHeight * (block.startMinutes / 60f)
                                val h = hourHeight * ((block.endMinutes - block.startMinutes) / 60f)
                                Box(
                                    Modifier
                                        .offset(x = colWidth * idx + indentDp, y = top)
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
                                        if (h > hourHeight * 0.5f) {
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
                    }

                    // Live long-press selection highlight.
                    val s = selStart; val c = selCur
                    if (s != null && c != null) {
                        val iMin = min(idxAt(s.x), idxAt(c.x))
                        val iMax = max(idxAt(s.x), idxAt(c.x))
                        var start = min(snapMin(s.y), snapMin(c.y))
                        var end = max(snapMin(s.y), snapMin(c.y))
                        if (end - start < MIN_DURATION) end = start + MIN_DURATION
                        Box(
                            Modifier
                                .offset(x = colWidth * iMin, y = hourHeight * (start / 60f))
                                .width(colWidth * (iMax - iMin + 1))
                                .height(hourHeight * ((end - start) / 60f))
                                .padding(1.dp)
                                .background(scheme.primary.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
                        )
                    }

                    // The draft, if any of its columns are visible.
                    if (draft != null) {
                        val idxL = visibleCols.indexOf(draft.colMin)
                        val idxR = visibleCols.indexOf(draft.colMax)
                        if (idxL >= 0 && idxR >= 0) {
                            DraftBlock(
                                draft = draft,
                                leftIndex = idxL,
                                spanCols = idxR - idxL + 1,
                                colWidth = colWidth,
                                colWidthPx = colWidthPx,
                                hourPx = hourPx,
                                hourHeight = hourHeight,
                                allowHResize = !focused,
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
}

@Composable
private fun DraftBlock(
    draft: DraftSel,
    leftIndex: Int,
    spanCols: Int,
    colWidth: Dp,
    colWidthPx: Float,
    hourPx: Float,
    hourHeight: Dp,
    allowHResize: Boolean,
    currentDraft: () -> DraftSel?,
    onDraftChange: (DraftSel?) -> Unit,
    onCommit: () -> Unit
) {
    val top = hourHeight * (draft.startMinutes / 60f)
    val h = hourHeight * ((draft.endMinutes - draft.startMinutes) / 60f)
    val w = colWidth * spanCols
    val accent = MaterialTheme.colorScheme.primary

    Box(Modifier.offset(x = colWidth * leftIndex, y = top).width(w).height(h)) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(1.dp)
                .background(accent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .border(1.5.dp, accent, RoundedCornerShape(6.dp))
                .pointerInput(colWidthPx, hourPx) { detectTapGestures { onCommit() } }
                .pointerInput(colWidthPx, hourPx, allowHResize) {
                    var bStart = 0; var bEnd = 0; var bColMin = 0; var bColMax = 0
                    var aX = 0f; var aY = 0f
                    detectDragGestures(
                        onDragStart = {
                            val d = currentDraft(); if (d != null) {
                                bStart = d.startMinutes; bEnd = d.endMinutes
                                bColMin = d.colMin; bColMax = d.colMax
                            }
                            aX = 0f; aY = 0f
                        },
                        onDrag = { ch, off ->
                            ch.consume(); aX += off.x; aY += off.y
                            val dur = bEnd - bStart
                            val span = bColMax - bColMin
                            val ns = snap(bStart + aY / hourPx * 60f).coerceIn(0, 1440 - dur)
                            val ncMin = if (allowHResize)
                                ((bColMin * colWidthPx + aX) / colWidthPx).roundToInt().coerceIn(0, 6 - span)
                            else bColMin
                            onDraftChange(DraftSel(ncMin, ncMin + span, ns, ns + dur))
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

        VResizeHandle(
            modifier = Modifier.align(Alignment.TopCenter), hourPx = hourPx,
            onBase = { currentDraft()?.startMinutes ?: 0 },
            onDelta = { base, d ->
                val cur = currentDraft() ?: return@VResizeHandle
                onDraftChange(cur.copy(startMinutes = snap((base + d).toFloat()).coerceIn(0, cur.endMinutes - MIN_DURATION)))
            }
        )
        VResizeHandle(
            modifier = Modifier.align(Alignment.BottomCenter), hourPx = hourPx,
            onBase = { currentDraft()?.endMinutes ?: 0 },
            onDelta = { base, d ->
                val cur = currentDraft() ?: return@VResizeHandle
                onDraftChange(cur.copy(endMinutes = snap((base + d).toFloat()).coerceIn(cur.startMinutes + MIN_DURATION, 1440)))
            }
        )

        if (allowHResize) {
            HResizeHandle(
                modifier = Modifier.align(Alignment.CenterStart), colWidthPx = colWidthPx,
                onBase = { currentDraft()?.colMin ?: 0 },
                onDelta = { base, d ->
                    val cur = currentDraft() ?: return@HResizeHandle
                    onDraftChange(cur.copy(colMin = (base + d).coerceIn(0, cur.colMax)))
                }
            )
            HResizeHandle(
                modifier = Modifier.align(Alignment.CenterEnd), colWidthPx = colWidthPx,
                onBase = { currentDraft()?.colMax ?: 0 },
                onDelta = { base, d ->
                    val cur = currentDraft() ?: return@HResizeHandle
                    onDraftChange(cur.copy(colMax = (base + d).coerceIn(cur.colMin, 6)))
                }
            )
        }

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
private fun VResizeHandle(modifier: Modifier, hourPx: Float, onBase: () -> Int, onDelta: (Int, Int) -> Unit) {
    Box(
        modifier.fillMaxWidth().height(HandleHeight).pointerInput(hourPx) {
            var base = 0; var acc = 0f
            detectVerticalDragGestures(
                onDragStart = { base = onBase(); acc = 0f },
                onVerticalDrag = { ch, dy -> ch.consume(); acc += dy; onDelta(base, (acc / hourPx * 60f).roundToInt()) }
            )
        },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.width(32.dp).height(4.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
    }
}

@Composable
private fun HResizeHandle(modifier: Modifier, colWidthPx: Float, onBase: () -> Int, onDelta: (Int, Int) -> Unit) {
    Box(
        modifier.fillMaxHeight().padding(vertical = HandleHeight).width(HandleWidth).pointerInput(colWidthPx) {
            var base = 0; var acc = 0f
            detectHorizontalDragGestures(
                onDragStart = { base = onBase(); acc = 0f },
                onHorizontalDrag = { ch, dx -> ch.consume(); acc += dx; onDelta(base, (acc / colWidthPx).roundToInt()) }
            )
        },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.width(4.dp).height(32.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
    }
}

@Composable
private fun TimeGutter(hourHeight: Dp) {
    Column(Modifier.width(GutterWidth).fillMaxHeight()) {
        for (h in 0 until HOURS) {
            Box(Modifier.fillMaxWidth().height(hourHeight), contentAlignment = Alignment.TopEnd) {
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

private fun placeDay(dayBlocks: List<ScheduleBlock>): List<Placed> {
    val sorted = dayBlocks.sortedWith(
        compareByDescending<ScheduleBlock> { it.endMinutes - it.startMinutes }.thenBy { it.startMinutes }
    )
    val placed = mutableListOf<Placed>()
    for (b in sorted) {
        val indent = placed.count { p -> p.block.startMinutes < b.endMinutes && b.startMinutes < p.block.endMinutes }
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
