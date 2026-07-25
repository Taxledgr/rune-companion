package io.github.taxledgr.runecompanion.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable

@Composable
internal fun SafeBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
) {
    if (LocalOnBackPressedDispatcherOwner.current != null) {
        BackHandler(enabled = enabled, onBack = onBack)
    }
}
