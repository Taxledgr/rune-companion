package io.github.taxledgr.runecompanion.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.taxledgr.runecompanion.toolkit.ToolkitState

private enum class CompanionTab(val label: String, val symbol: String) {
    STARS("Stars", "✦"),
    TIMERS("Timers", "◷"),
    JOURNAL("Journal", "✓"),
    TOOLS("Tools", "⌁"),
}

@Composable
fun RuneCompanionShell(
    toolkitState: ToolkitState,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onAddReminder: (String, io.github.taxledgr.runecompanion.toolkit.ReminderCategory, Int) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onSetTripLabel: (String) -> Unit,
    onToggleTripTimer: () -> Unit,
    onResetTripTimer: () -> Unit,
    onSetSlayerTask: (String, Int) -> Unit,
    onAdjustSlayerRemaining: (Int) -> Unit,
    onClearSlayerTask: () -> Unit,
    onAddChecklistEntry: (String, io.github.taxledgr.runecompanion.toolkit.ChecklistCategory) -> Unit,
    onToggleChecklistEntry: (String) -> Unit,
    onDeleteChecklistEntry: (String) -> Unit,
    onSearchPrices: (String) -> Unit,
    onAddPriceWatchItem: (io.github.taxledgr.runecompanion.toolkit.PriceSearchItem) -> Unit,
    onRemovePriceWatchItem: (Int) -> Unit,
    onRefreshPrices: () -> Unit,
    onLookupHiscores: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    starsContent: @Composable () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(CompanionTab.STARS) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                CompanionTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.symbol) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selectedTab) {
                CompanionTab.STARS -> starsContent()
                CompanionTab.TIMERS -> TimersScreen(
                    state = toolkitState,
                    notificationPermissionGranted = notificationPermissionGranted,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onAddReminder = onAddReminder,
                    onDeleteReminder = onDeleteReminder,
                    onSetTripLabel = onSetTripLabel,
                    onToggleTripTimer = onToggleTripTimer,
                    onResetTripTimer = onResetTripTimer,
                )
                CompanionTab.JOURNAL -> JournalScreen(
                    state = toolkitState,
                    onSetSlayerTask = onSetSlayerTask,
                    onAdjustSlayerRemaining = onAdjustSlayerRemaining,
                    onClearSlayerTask = onClearSlayerTask,
                    onAddChecklistEntry = onAddChecklistEntry,
                    onToggleChecklistEntry = onToggleChecklistEntry,
                    onDeleteChecklistEntry = onDeleteChecklistEntry,
                )
                CompanionTab.TOOLS -> ToolsScreen(
                    state = toolkitState,
                    onSearchPrices = onSearchPrices,
                    onAddPriceWatchItem = onAddPriceWatchItem,
                    onRemovePriceWatchItem = onRemovePriceWatchItem,
                    onRefreshPrices = onRefreshPrices,
                    onLookupHiscores = onLookupHiscores,
                    onOpenUrl = onOpenUrl,
                )
            }
        }
    }
}
