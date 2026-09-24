package com.untamedflame.schedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.untamedflame.schedule.data.ScheduleBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditor(
    target: EditorTarget,
    onDismiss: () -> Unit,
    onSave: (EditorTarget) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember { mutableStateOf(target.title) }
    var days by remember { mutableStateOf(target.days) }
    var start by remember { mutableIntStateOf(target.startMinutes) }
    var end by remember { mutableIntStateOf(target.endMinutes) }
    var error by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableIntStateOf(0) } // 0 none, 1 start, 2 end
    val isEditing = target.groupId != null

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Anchor the menu at the top of the screen.
        Box(Modifier.fillMaxSize().padding(8.dp)) {
            Card(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                        Text(
                            "${ScheduleBlock.formatMinutes(start)} – ${ScheduleBlock.formatMinutes(end)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = {
                            when {
                                title.isBlank() -> error = "Enter what you should be doing."
                                days.isEmpty() -> error = "Pick at least one day."
                                end <= start -> error = "End must be after start."
                                else -> onSave(
                                    target.copy(
                                        days = days,
                                        startMinutes = start,
                                        endMinutes = end,
                                        title = title.trim()
                                    )
                                )
                            }
                        }) { Text("Save", fontWeight = FontWeight.Bold) }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("What should you do?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { picking = 1 },
                            modifier = Modifier.weight(1f)
                        ) { Text("From ${ScheduleBlock.formatMinutes(start)}") }
                        OutlinedButton(
                            onClick = { picking = 2 },
                            modifier = Modifier.weight(1f)
                        ) { Text("To ${ScheduleBlock.formatMinutes(end)}") }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DAY_LABELS.forEachIndexed { col, label ->
                            val day = colToDay(col)
                            val selected = day in days
                            FilterChip(
                                selected = selected,
                                onClick = { days = if (selected) days - day else days + day },
                                label = { Text(label.take(1)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    if (isEditing) {
                        TextButton(
                            onClick = { target.groupId?.let(onDelete) },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }

    if (picking != 0) {
        ClockTimeDialog(
            initialMinutes = if (picking == 1) start else end,
            onDismiss = { picking = 0 },
            onConfirm = { m ->
                if (picking == 1) {
                    start = m
                    if (end <= start) end = minOf(1440, start + 60)
                } else {
                    end = if (m > start) m else minOf(1440, start + 30)
                }
                picking = 0
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockTimeDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = false
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") }
                }
            }
        }
    }
}
