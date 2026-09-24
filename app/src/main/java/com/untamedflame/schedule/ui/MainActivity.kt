package com.untamedflame.schedule.ui

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.untamedflame.schedule.data.ScheduleBlock
import com.untamedflame.schedule.notify.NotificationHelper

private val DAY_NAMES = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
)

class MainActivity : ComponentActivity() {

    private lateinit var vm: ScheduleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.ensureChannel(this)
        setContent {
            WeeklyScheduleTheme {
                vm = viewModel()
                ScheduleScreen(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::vm.isInitialized) vm.resync()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(vm: ScheduleViewModel) {
    val context = LocalContext.current
    val blocks by vm.blocks.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<ScheduleBlock?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    // Ask for notification permission on Android 13+.
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.resync() }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Detect whether exact alarms are allowed (Android 12+).
    var exactAllowed by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(AlarmManager::class.java)
            exactAllowed = am?.canScheduleExactAlarms() ?: true
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Weekly Schedule") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showEditor = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add block") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            if (!exactAllowed) {
                ExactAlarmBanner {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData(Uri.parse("package:" + context.packageName))
                        )
                    }
                }
            }

            if (blocks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No blocks yet.\nTap “Add block” to schedule your week.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (day in 1..7) {
                        val dayBlocks = blocks.filter { it.dayOfWeek == day }
                        if (dayBlocks.isNotEmpty()) {
                            item(key = "header_$day") {
                                Text(
                                    DAY_NAMES[day - 1],
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(dayBlocks, key = { it.id }) { block ->
                                BlockRow(
                                    block = block,
                                    onClick = { editing = block; showEditor = true },
                                    onDelete = { vm.delete(block) }
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showEditor) {
        BlockEditorDialog(
            existing = editing,
            onDismiss = { showEditor = false },
            onSave = { vm.addOrUpdate(it); showEditor = false }
        )
    }
}

@Composable
private fun ExactAlarmBanner(onFix: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Allow exact alarms so reminders switch on time.",
                Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = onFix) { Text("Allow") }
        }
    }
}

@Composable
private fun BlockRow(block: ScheduleBlock, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(block.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${block.startLabel} – ${block.endLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockEditorDialog(
    existing: ScheduleBlock?,
    onDismiss: () -> Unit,
    onSave: (ScheduleBlock) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var day by remember { mutableStateOf(existing?.dayOfWeek ?: 1) }
    var start by remember { mutableStateOf(existing?.startMinutes ?: 9 * 60) }
    var end by remember { mutableStateOf(existing?.endMinutes ?: 10 * 60) }
    var dayMenuOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun pickTime(initial: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, h, m -> onPicked(h * 60 + m) },
            initial / 60, initial % 60, false
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New block" else "Edit block") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What should you do?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = dayMenuOpen,
                    onExpandedChange = { dayMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = DAY_NAMES[day - 1],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dayMenuOpen) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dayMenuOpen,
                        onDismissRequest = { dayMenuOpen = false }
                    ) {
                        DAY_NAMES.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { day = index + 1; dayMenuOpen = false }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { pickTime(start) { start = it } },
                        modifier = Modifier.weight(1f)
                    ) { Text("Start: ${ScheduleBlock.formatMinutes(start)}") }
                    OutlinedButton(
                        onClick = { pickTime(end) { end = it } },
                        modifier = Modifier.weight(1f)
                    ) { Text("End: ${ScheduleBlock.formatMinutes(end)}") }
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    title.isBlank() -> error = "Enter what you should be doing."
                    end <= start -> error = "End time must be after start time."
                    else -> onSave(
                        (existing ?: ScheduleBlock(dayOfWeek = day, startMinutes = start,
                            endMinutes = end, title = title.trim())).copy(
                            dayOfWeek = day, startMinutes = start, endMinutes = end,
                            title = title.trim()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
