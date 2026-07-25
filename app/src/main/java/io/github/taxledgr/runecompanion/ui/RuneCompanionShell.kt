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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import io.github.taxledgr.runecompanion.toolkit.ToolkitState
import io.github.taxledgr.runecompanion.features.FeatureState
import io.github.taxledgr.runecompanion.features.FeatureViewModel

private enum class CompanionTab(val label: String, val symbol: String) {
    STARS("Stars", "✦"),
    TIMERS("Timers", "◷"),
    JOURNAL("Journal", "✓"),
    TOOLS("Tools", "⌁"),
    WIKI("Wiki", "W"),
    MORE("More", "☰"),
}

@Composable
fun RuneCompanionShell(
    toolkitState: ToolkitState,
    featureState: FeatureState,
    featureViewModel: FeatureViewModel,
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
    onSaveTrackedPlayer: (String) -> Unit,
    onTrackedPlayerAutoRefreshChanged: (Boolean) -> Unit,
    onRefreshTrackedPlayer: () -> Unit,
    onResetTrackedPlayerBaseline: () -> Unit,
    onClearTrackedPlayer: () -> Unit,
    onOpenUrl: (String) -> Unit,
    starsContent: @Composable () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(CompanionTab.STARS) }
    val tabStateHolder = rememberSaveableStateHolder()

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
            tabStateHolder.SaveableStateProvider(selectedTab.name) {
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
                    CompanionTab.WIKI -> WikiPortalScreen(onOpenArticle = onOpenUrl)
                    CompanionTab.MORE -> FeatureHubScreen(
                        state = featureState,
                        viewModel = featureViewModel,
                        toolkitState = toolkitState,
                        onSaveTrackedPlayer = onSaveTrackedPlayer,
                        onAutoRefreshChanged = onTrackedPlayerAutoRefreshChanged,
                        onRefreshTrackedPlayer = onRefreshTrackedPlayer,
                        onResetTrackedPlayerBaseline = onResetTrackedPlayerBaseline,
                        onClearTrackedPlayer = onClearTrackedPlayer,
                        onOpenUrl = onOpenUrl,
                    )
                }
            }
        }
    }
}
