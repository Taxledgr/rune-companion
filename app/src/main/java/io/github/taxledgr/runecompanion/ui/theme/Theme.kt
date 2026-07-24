package io.github.taxledgr.runecompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RuneGold = Color(0xFFF4C95D)
val RuneCyan = Color(0xFF53D8D4)
val RuneBackground = Color(0xFF071019)
val RuneSurface = Color(0xFF101F2B)
val RuneSurfaceRaised = Color(0xFF182B39)

private val RuneColorScheme = darkColorScheme(
    primary = RuneGold,
    onPrimary = Color(0xFF2B2100),
    secondary = RuneCyan,
    onSecondary = Color(0xFF00201F),
    background = RuneBackground,
    onBackground = Color(0xFFE6EDF2),
    surface = RuneSurface,
    onSurface = Color(0xFFE6EDF2),
    surfaceVariant = RuneSurfaceRaised,
    onSurfaceVariant = Color(0xFFB9C9D4),
    error = Color(0xFFFFB4AB),
)

@Composable
fun RuneCompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RuneColorScheme,
        content = content,
    )
}
