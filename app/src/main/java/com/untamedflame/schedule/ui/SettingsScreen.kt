package com.untamedflame.schedule.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.untamedflame.schedule.data.NotificationMode
import com.untamedflame.schedule.data.SettingsStore

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onChanged: () -> Unit, onThemeChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }

    var mode by remember { mutableStateOf(store.notificationMode) }
    var interval by remember { mutableIntStateOf(store.reminderIntervalMinutes) }
    var intervalText by remember { mutableStateOf(store.reminderIntervalMinutes.toString()) }
    var dark by remember { mutableStateOf(store.darkTheme) }

    fun persist() {
        store.notificationMode = mode
        store.reminderIntervalMinutes = interval
        onChanged()
    }

    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Settings") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ModeOption(
                        title = "Locked",
                        selected = mode == NotificationMode.LOCKED,
                        onClick = { mode = NotificationMode.LOCKED; persist() }
                    )

                    ModeOption(
                        title = "Reminder",
                        selected = mode == NotificationMode.REMINDER,
                        onClick = { mode = NotificationMode.REMINDER; persist() }
                    )

                    // Repeat interval only makes sense when the notification can be dismissed.
                    if (mode == NotificationMode.REMINDER) {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text("Repeat every", style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            INTERVALS.forEach { (minutes, label) ->
                                FilterChip(
                                    selected = interval == minutes,
                                    onClick = { interval = minutes; intervalText = minutes.toString(); persist() },
                                    label = { Text(label) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = intervalText,
                            onValueChange = { raw ->
                                intervalText = raw.filter { it.isDigit() }.take(3)
                                intervalText.toIntOrNull()?.let { interval = it.coerceIn(0, 720); persist() }
                            },
                            label = { Text("Custom minutes (0–720)") },
                            supportingText = { Text("Up to 12 hours. 0 = only at the start.") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(
                        "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    ModeOption(
                        title = "Light",
                        selected = !dark,
                        onClick = { dark = false; store.darkTheme = false; onThemeChange(false) }
                    )
                    ModeOption(
                        title = "Dark",
                        selected = dark,
                        onClick = { dark = true; store.darkTheme = true; onThemeChange(true) }
                    )

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(
                        "Feedback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { sendFeedback(context) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MailOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Send feedback", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Email the developer your suggestions or bugs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val FEEDBACK_EMAIL = "UntamedFlame@proton.me"

/** Opens the user's email app pre-addressed to the developer, with app/device info appended. */
private fun sendFeedback(context: Context) {
    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        "?"
    }
    val body = buildString {
        append("\n\n\n—— write your feedback above ——\n")
        append("App version: ").append(version).append('\n')
        append("Android: ").append(Build.VERSION.RELEASE).append('\n')
        append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
    }
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "Weekly Scheduler feedback")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Send feedback"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No email app found on this device.", Toast.LENGTH_LONG).show()
    }
}

private val INTERVALS = listOf(
    0 to "Only at start",
    15 to "15 min",
    30 to "30 min",
    60 to "60 min",
    120 to "2 hr",
    240 to "4 hr",
    360 to "6 hr",
    720 to "12 hr"
)

@Composable
private fun ModeOption(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
