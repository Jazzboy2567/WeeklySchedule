package com.untamedflame.schedule.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    var title by remember { mutableStateOf(target.title) }
    var days by remember { mutableStateOf(target.days) }
    var start by remember { mutableStateOf(target.startMinutes) }
    var end by remember { mutableStateOf(target.endMinutes) }
    var error by remember { mutableStateOf<String?>(null) }
    val isEditing = target.groupId != null

    fun pickTime(initial: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(context, { _, h, m -> onPicked(h * 60 + m) }, initial / 60, initial % 60, false).show()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (isEditing) "Edit block" else "New block") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        },
                        actions = {
                            TextButton(onClick = {
                                when {
                                    title.isBlank() -> error = "Enter what you should be doing."
                                    days.isEmpty() -> error = "Pick at least one day."
                                    end <= start -> error = "End time must be after start time."
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
                    )
                }
            ) { padding ->
                Column(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("What should you do?") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Repeat on", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DAY_LABELS.forEachIndexed { col, label ->
                                val day = colToDay(col)
                                val selected = day in days
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        days = if (selected) days - day else days + day
                                    },
                                    label = { Text(label.take(1)) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors()
                                )
                            }
                        }
                        Text(
                            if (days.size <= 1) "Repeats every ${days.firstOrNull()?.let { dayFullName(it) } ?: "…"}"
                            else "Repeats on ${days.size} days each week",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { pickTime(start) { start = it } },
                            modifier = Modifier.weight(1f)
                        ) { Text("Start\n${ScheduleBlock.formatMinutes(start)}") }
                        OutlinedButton(
                            onClick = { pickTime(end) { end = it } },
                            modifier = Modifier.weight(1f)
                        ) { Text("End\n${ScheduleBlock.formatMinutes(end)}") }
                    }

                    error?.let {
                        Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    }

                    if (isEditing) {
                        Spacer(Modifier.height(8.dp))
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
}
