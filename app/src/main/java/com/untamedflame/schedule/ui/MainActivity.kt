package com.untamedflame.schedule.ui

import android.Manifest
import android.app.AlarmManager
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.untamedflame.schedule.data.ScheduleBlock
import com.untamedflame.schedule.notify.NotificationHelper

class MainActivity : ComponentActivity() {

    private lateinit var vm: ScheduleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.ensureChannels(this)
        setContent {
            WeeklyScheduleTheme {
                vm = viewModel()
                AppRoot(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::vm.isInitialized) vm.resync()
    }
}

@Composable
private fun AppRoot(vm: ScheduleViewModel) {
    var showSettings by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }

    ScheduleScreen(
        vm = vm,
        onOpenSettings = { showSettings = true },
        onCreate = { days, start, end -> editor = EditorTarget(null, days, start, end, "") },
        onEdit = { editor = it }
    )

    editor?.let { target ->
        BlockEditor(
            target = target,
            onDismiss = { editor = null },
            onSave = {
                vm.saveGroup(it.groupId, it.days, it.startMinutes, it.endMinutes, it.title)
                editor = null
            },
            onDelete = { gid ->
                vm.deleteGroup(gid)
                editor = null
            }
        )
    }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false }, onChanged = { vm.resync() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleScreen(
    vm: ScheduleViewModel,
    onOpenSettings: () -> Unit,
    onCreate: (Set<Int>, Int, Int) -> Unit,
    onEdit: (EditorTarget) -> Unit
) {
    val context = LocalContext.current
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf<DraftSel?>(null) }

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

    var exactAllowed by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(AlarmManager::class.java)
            exactAllowed = am?.canScheduleExactAlarms() ?: true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Schedule") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
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
            WeekGrid(
                blocks = blocks,
                draft = draft,
                onDraftChange = { draft = it },
                onCommitDraft = {
                    draft?.let { d -> onCreate(setOf(d.dayOfWeek), d.startMinutes, d.endMinutes) }
                    draft = null
                },
                onCreate = onCreate,
                onBlockClick = { block -> onEdit(targetFor(block, blocks)) }
            )
        }
    }
}

/** Build an editor target for an existing block, gathering its whole day-group. */
private fun targetFor(block: ScheduleBlock, all: List<ScheduleBlock>): EditorTarget {
    val group = all.filter { it.groupId == block.groupId }.ifEmpty { listOf(block) }
    return EditorTarget(
        groupId = block.groupId,
        days = group.map { it.dayOfWeek }.toSet(),
        startMinutes = block.startMinutes,
        endMinutes = block.endMinutes,
        title = block.title
    )
}

@Composable
private fun ExactAlarmBanner(onFix: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Allow exact alarms so reminders fire on time.",
                Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = onFix) { Text("Allow") }
        }
    }
}
