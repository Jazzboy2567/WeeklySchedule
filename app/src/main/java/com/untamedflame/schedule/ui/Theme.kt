package com.untamedflame.schedule.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3B5BDB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE4FF),
    onPrimaryContainer = Color(0xFF0A1E63),
    secondary = Color(0xFF5C7CFA),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6ECFF),
    onSecondaryContainer = Color(0xFF16245C),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF1A1C22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C22),
    surfaceVariant = Color(0xFFE7E9F0),
    onSurfaceVariant = Color(0xFF454B58),
    outline = Color(0xFFB9BFCC),
    outlineVariant = Color(0xFFDCE0EA),
    error = Color(0xFFC0392B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE7E4),
    onErrorContainer = Color(0xFF7A1B12)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAAC0FF),
    onPrimary = Color(0xFF0A1E63),
    primaryContainer = Color(0xFF2A3B85),
    onPrimaryContainer = Color(0xFFDBE4FF),
    secondary = Color(0xFF9DB2FF),
    onSecondary = Color(0xFF12204D),
    secondaryContainer = Color(0xFF2C3B78),
    onSecondaryContainer = Color(0xFFE6ECFF),
    background = Color(0xFF0F1116),
    onBackground = Color(0xFFE6E8EE),
    surface = Color(0xFF171A21),
    onSurface = Color(0xFFE6E8EE),
    surfaceVariant = Color(0xFF2A2E38),
    onSurfaceVariant = Color(0xFFC3C8D4),
    outline = Color(0xFF5A6070),
    outlineVariant = Color(0xFF333947),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5F1109),
    errorContainer = Color(0xFF5A1A14),
    onErrorContainer = Color(0xFFFFDAD5)
)

@Composable
fun WeeklyScheduleTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
