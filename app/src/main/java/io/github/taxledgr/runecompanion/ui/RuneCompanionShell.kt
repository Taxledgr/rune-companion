package io.github.taxledgr.runecompanion.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import io.github.taxledgr.runecompanion.toolkit.ToolkitState
import io.github.taxledgr.runecompanion.features.FeatureState
import io.github.taxledgr.runecompanion.features.FeatureViewModel
import io.github.taxledgr.runecompanion.personalization.AppTab
import io.github.taxledgr.runecompanion.personalization.ActivityProfileState
import io.github.taxledgr.runecompanion.personalization.ActivityProfileTemplate
import io.github.taxledgr.runecompanion.personalization.PersonalizationSettings
import io.github.taxledgr.runecompanion.overlay.OverlayModule

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
    initialQuestId: String? = null,
    showNavigation: Boolean = true,
    overlayEditorModule: OverlayModule? = null,
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
    onUndoToolkitChange: () -> Unit = {},
    onDismissToolkitUndo: () -> Unit = {},
    onOpenUrl: (String) -> Unit,
    starsContent: @Composable () -> Unit,
) {
    var selectedTab by rememberSaveable {
        mutableStateOf(initialTab)
    }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var featureNavigationRequest by rememberSaveable { mutableStateOf<String?>(null) }
    val tabStateHolder = rememberSaveableStateHolder()
    val homeTab = personalizationSettings.effectiveStartTab
        .takeIf { it in personalizationSettings.navigationTabs }
        ?: personalizationSettings.navigationTabs.first()
    SafeBackHandler(
        enabled = showNavigation && (searchOpen || selectedTab != homeTab),
    ) {
        if (searchOpen) {
            searchOpen = false
        } else {
            selectedTab = homeTab
        }
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showNavigation) {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(APP_HEADER_HEIGHT)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${activityProfiles.activeProfile.symbol} " +
                                activityProfiles.activeProfile.name,
                        )
                        TextButton(onClick = { searchOpen = true }) {
                            Text("⌕ Search")
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (showNavigation) {
                NavigationBar(
                    modifier = Modifier.height(APP_NAVIGATION_HEIGHT),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
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
            if (searchOpen) {
                GlobalSearchScreen(
                    featureData = featureState.data,
                    toolkitState = toolkitState,
                    onClose = { searchOpen = false },
                    onNavigate = { tab, featureId ->
                        selectedTab = tab
                        featureNavigationRequest = featureId
                        searchOpen = false
                    },
                    onOpenWiki = { url ->
                        searchOpen = false
                        onOpenUrl(url)
                    },
                )
            } else tabStateHolder.SaveableStateProvider(selectedTab.name) {
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
                        focus = when (overlayEditorModule) {
                            OverlayModule.TIMERS -> TimerEditorFocus.REMINDERS
                            OverlayModule.TRIP -> TimerEditorFocus.TRIP
                            else -> TimerEditorFocus.ALL
                        },
                    )
                    AppTab.JOURNAL -> JournalScreen(
                        state = toolkitState,
                        onSetSlayerTask = onSetSlayerTask,
                        onAdjustSlayerRemaining = onAdjustSlayerRemaining,
                        onClearSlayerTask = onClearSlayerTask,
                        onAddChecklistEntry = onAddChecklistEntry,
                        onToggleChecklistEntry = onToggleChecklistEntry,
                        onDeleteChecklistEntry = onDeleteChecklistEntry,
                        focus = when (overlayEditorModule) {
                            OverlayModule.SLAYER -> JournalEditorFocus.SLAYER
                            OverlayModule.CHECKLIST -> JournalEditorFocus.CHECKLIST
                            else -> JournalEditorFocus.ALL
                        },
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
                        initialQuestId = initialQuestId,
                        navigationRequest = featureNavigationRequest,
                        onNavigationRequestConsumed = {
                            featureNavigationRequest = null
                        },
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
            toolkitState.undoLabel?.let { label ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                ) {
                    UndoChangeCard(
                        label = label,
                        onUndo = onUndoToolkitChange,
                        onDismiss = onDismissToolkitUndo,
                    )
                }
            }
        }
    }
}

private val APP_HEADER_HEIGHT = 48.dp
private val APP_NAVIGATION_HEIGHT = 68.dp
