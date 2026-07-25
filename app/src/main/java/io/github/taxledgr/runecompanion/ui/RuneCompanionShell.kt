package io.github.taxledgr.runecompanion.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import io.github.taxledgr.runecompanion.toolkit.ToolkitState
import io.github.taxledgr.runecompanion.features.FeatureState
import io.github.taxledgr.runecompanion.features.FeatureViewModel
import io.github.taxledgr.runecompanion.personalization.AppTab
import io.github.taxledgr.runecompanion.personalization.ActivityProfileState
import io.github.taxledgr.runecompanion.personalization.ActivityProfileTemplate
import io.github.taxledgr.runecompanion.personalization.PersonalizationSettings

@Composable
fun RuneCompanionShell(
    toolkitState: ToolkitState,
    featureState: FeatureState,
    featureViewModel: FeatureViewModel,
    personalizationSettings: PersonalizationSettings,
    activityProfiles: ActivityProfileState = ActivityProfileState(
        profiles = listOf(ActivityProfileTemplate.SHOOTING_STARS.profile()),
        activeProfileId = ActivityProfileTemplate.SHOOTING_STARS.id,
    ),
    initialTab: AppTab = personalizationSettings.effectiveStartTab,
    initialFeatureId: String? = personalizationSettings.startFeatureId,
    showNavigation: Boolean = true,
    onPersonalizationChanged: (PersonalizationSettings) -> Unit,
    onActivityProfileSelected: (String) -> Unit = {},
    onActivityProfileCreated: (String) -> Unit = {},
    onActivityProfileRenamed: (String) -> Unit = {},
    onActivityProfileDeleted: () -> Unit = {},
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
    var selectedTab by rememberSaveable {
        mutableStateOf(initialTab)
    }
    val tabStateHolder = rememberSaveableStateHolder()
    val homeTab = personalizationSettings.effectiveStartTab
        .takeIf { it in personalizationSettings.navigationTabs }
        ?: personalizationSettings.navigationTabs.first()
    BackHandler(enabled = showNavigation && selectedTab != homeTab) {
        selectedTab = homeTab
    }
    LaunchedEffect(personalizationSettings.navigationTabs, showNavigation) {
        if (showNavigation && selectedTab !in personalizationSettings.navigationTabs) {
            selectedTab = personalizationSettings.effectiveStartTab
                .takeIf { it in personalizationSettings.navigationTabs }
                ?: personalizationSettings.navigationTabs.first()
        }
    }
    LaunchedEffect(activityProfiles.activeProfileId) {
        if (showNavigation) {
            selectedTab = personalizationSettings.effectiveStartTab
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showNavigation) {
                NavigationBar {
                    personalizationSettings.navigationTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Text(tab.symbol) },
                            label = { Text(tab.label) },
                        )
                    }
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
                    AppTab.STARS -> starsContent()
                    AppTab.TIMERS -> TimersScreen(
                        state = toolkitState,
                        notificationPermissionGranted = notificationPermissionGranted,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onAddReminder = onAddReminder,
                        onDeleteReminder = onDeleteReminder,
                        onSetTripLabel = onSetTripLabel,
                        onToggleTripTimer = onToggleTripTimer,
                        onResetTripTimer = onResetTripTimer,
                    )
                    AppTab.JOURNAL -> JournalScreen(
                        state = toolkitState,
                        onSetSlayerTask = onSetSlayerTask,
                        onAdjustSlayerRemaining = onAdjustSlayerRemaining,
                        onClearSlayerTask = onClearSlayerTask,
                        onAddChecklistEntry = onAddChecklistEntry,
                        onToggleChecklistEntry = onToggleChecklistEntry,
                        onDeleteChecklistEntry = onDeleteChecklistEntry,
                    )
                    AppTab.TOOLS -> ToolsScreen(
                        state = toolkitState,
                        onSearchPrices = onSearchPrices,
                        onAddPriceWatchItem = onAddPriceWatchItem,
                        onRemovePriceWatchItem = onRemovePriceWatchItem,
                        onRefreshPrices = onRefreshPrices,
                        onLookupHiscores = onLookupHiscores,
                        onOpenUrl = onOpenUrl,
                    )
                    AppTab.WIKI -> WikiPortalScreen(onOpenArticle = onOpenUrl)
                    AppTab.MORE -> FeatureHubScreen(
                        state = featureState,
                        viewModel = featureViewModel,
                        toolkitState = toolkitState,
                        personalizationSettings = personalizationSettings,
                        activityProfiles = activityProfiles,
                        initialFeatureId = initialFeatureId,
                        onPersonalizationChanged = onPersonalizationChanged,
                        onActivityProfileSelected = onActivityProfileSelected,
                        onActivityProfileCreated = onActivityProfileCreated,
                        onActivityProfileRenamed = onActivityProfileRenamed,
                        onActivityProfileDeleted = onActivityProfileDeleted,
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
