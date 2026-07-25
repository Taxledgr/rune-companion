package io.github.taxledgr.runecompanion

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import io.github.taxledgr.runecompanion.overlay.OverlayPreferences
import io.github.taxledgr.runecompanion.overlay.OverlayService
import io.github.taxledgr.runecompanion.overlay.OverlaySettings
import io.github.taxledgr.runecompanion.alerts.StarFilterPreferences
import io.github.taxledgr.runecompanion.features.FeatureViewModel
import io.github.taxledgr.runecompanion.personalization.ActivityProfile
import io.github.taxledgr.runecompanion.personalization.ActivityProfilePreferences
import io.github.taxledgr.runecompanion.personalization.ActivityProfileState
import io.github.taxledgr.runecompanion.personalization.ActivityProfileTemplate
import io.github.taxledgr.runecompanion.personalization.ActivityStarFilters
import io.github.taxledgr.runecompanion.personalization.PersonalizationPreferences
import io.github.taxledgr.runecompanion.personalization.PersonalizationSettings
import io.github.taxledgr.runecompanion.ui.WikiReaderScreen
import io.github.taxledgr.runecompanion.ui.isWikiUrl
import io.github.taxledgr.runecompanion.ui.RuneCompanionApp
import io.github.taxledgr.runecompanion.ui.RuneCompanionShell
import io.github.taxledgr.runecompanion.ui.StarViewModel
import io.github.taxledgr.runecompanion.ui.theme.RuneCompanionTheme
import io.github.taxledgr.runecompanion.toolkit.ToolkitViewModel
import io.github.taxledgr.runecompanion.util.openTrustedExternalUrl
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val overlayPermission = MutableStateFlow(false)
    private val notificationPermission = MutableStateFlow(false)
    private val toolkitViewModel: ToolkitViewModel by viewModels()
    private val featureViewModel: FeatureViewModel by viewModels()
    private val starViewModel: StarViewModel by viewModels()
    private val overlayPreferences by lazy { OverlayPreferences(this) }
    private val overlaySettings = MutableStateFlow(OverlaySettings())
    private val personalizationPreferences by lazy { PersonalizationPreferences(this) }
    private val personalizationSettings = MutableStateFlow(PersonalizationSettings())
    private val starFilterPreferences by lazy { StarFilterPreferences(this) }
    private val activityProfilePreferences by lazy { ActivityProfilePreferences(this) }
    private val activityProfiles = MutableStateFlow(
        ActivityProfileState(
            profiles = listOf(ActivityProfileTemplate.SHOOTING_STARS.profile()),
            activeProfileId = ActivityProfileTemplate.SHOOTING_STARS.id,
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(false)
        }
        window.decorView.filterTouchesWhenObscured = true
        WebView.setWebContentsDebuggingEnabled(false)
        overlaySettings.value = overlayPreferences.load()
        personalizationSettings.value = personalizationPreferences.load()
        activityProfiles.value = loadActivityProfiles()
        applyActivityProfile(activityProfiles.value.activeProfile, refreshViewModels = false)
        setContent {
            val personalization = personalizationSettings.collectAsStateWithLifecycle()
            RuneCompanionTheme(layoutDensity = personalization.value.appDensity) {
                val state = starViewModel.state.collectAsStateWithLifecycle()
                val alertSettings = starViewModel.alertSettings.collectAsStateWithLifecycle()
                val starFilterSettings =
                    starViewModel.filterSettings.collectAsStateWithLifecycle()
                val toolkitState = toolkitViewModel.state.collectAsStateWithLifecycle()
                val featureState = featureViewModel.state.collectAsStateWithLifecycle()
                val permission = overlayPermission.collectAsStateWithLifecycle()
                val notificationsGranted = notificationPermission.collectAsStateWithLifecycle()
                val overlayRunning = OverlayService.running.collectAsStateWithLifecycle()
                val configuredOverlay = overlaySettings.collectAsStateWithLifecycle()
                val configuredActivityProfiles =
                    activityProfiles.collectAsStateWithLifecycle()
                LaunchedEffect(
                    featureState.value.initializing,
                    featureState.value.data.selectedAccount,
                ) {
                    if (featureState.value.initializing) return@LaunchedEffect
                    updateActiveProfile {
                        it.copy(selectedAccount = featureState.value.data.selectedAccount)
                    }
                }
                LaunchedEffect(
                    starFilterSettings.value.hideDangerousWorlds,
                    starFilterSettings.value.excludedLocations,
                ) {
                    updateActiveProfile {
                        it.copy(
                            starFilters = ActivityStarFilters.from(
                                starFilterSettings.value,
                            ),
                        )
                    }
                }
                LaunchedEffect(featureState.value.restoreGeneration) {
                    if (featureState.value.restoreGeneration > 0) {
                        toolkitViewModel.reloadFromDisk()
                        starViewModel.reloadPreferences()
                        overlaySettings.value = overlayPreferences.load()
                        personalizationSettings.value = personalizationPreferences.load()
                        activityProfiles.value = loadActivityProfiles()
                        applyActivityProfile(
                            activityProfiles.value.activeProfile,
                            refreshViewModels = true,
                        )
                        if (OverlayService.running.value) {
                            startService(OverlayService.reloadIntent(this@MainActivity))
                        }
                    }
                }
                var activeWikiUrl by rememberSaveable { mutableStateOf<String?>(null) }
                val openCompanionUrl: (String) -> Unit = { url ->
                    if (isWikiUrl(url)) activeWikiUrl = url else openUrl(url)
                }
                val overlayNotificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) startOverlay(this)
                }
                val alertNotificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    notificationPermission.value = granted
                    if (granted) starViewModel.setAlertsEnabled(true)
                }
                val generalNotificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    notificationPermission.value = granted
                }

                if (activeWikiUrl != null) {
                    WikiReaderScreen(
                        initialUrl = requireNotNull(activeWikiUrl),
                        onClose = { activeWikiUrl = null },
                        onOpenExternal = ::openUrl,
                    )
                } else {
                    RuneCompanionShell(
                    toolkitState = toolkitState.value,
                    featureState = featureState.value,
                    featureViewModel = featureViewModel,
                    personalizationSettings = personalization.value,
                    activityProfiles = configuredActivityProfiles.value,
                    onPersonalizationChanged = ::updatePersonalizationSettings,
                    onActivityProfileSelected = ::selectActivityProfile,
                    onActivityProfileCreated = ::createActivityProfile,
                    onActivityProfileRenamed = ::renameActivityProfile,
                    onActivityProfileDeleted = ::deleteActivityProfile,
                    notificationPermissionGranted = notificationsGranted.value,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            generalNotificationPermission.launch(
                                Manifest.permission.POST_NOTIFICATIONS,
                            )
                        } else {
                            notificationPermission.value = true
                        }
                    },
                    onAddReminder = toolkitViewModel::addReminder,
                    onDeleteReminder = toolkitViewModel::deleteReminder,
                    onSetTripLabel = toolkitViewModel::setTripLabel,
                    onToggleTripTimer = toolkitViewModel::toggleTripTimer,
                    onResetTripTimer = toolkitViewModel::resetTripTimer,
                    onSetSlayerTask = toolkitViewModel::setSlayerTask,
                    onAdjustSlayerRemaining = toolkitViewModel::adjustSlayerRemaining,
                    onClearSlayerTask = toolkitViewModel::clearSlayerTask,
                    onAddChecklistEntry = toolkitViewModel::addChecklistEntry,
                    onToggleChecklistEntry = toolkitViewModel::toggleChecklistEntry,
                    onDeleteChecklistEntry = toolkitViewModel::deleteChecklistEntry,
                    onSearchPrices = toolkitViewModel::searchPrices,
                    onAddPriceWatchItem = toolkitViewModel::addPriceWatchItem,
                    onRemovePriceWatchItem = toolkitViewModel::removePriceWatchItem,
                    onRefreshPrices = toolkitViewModel::refreshPrices,
                    onLookupHiscores = toolkitViewModel::lookupHiscores,
                    onSaveTrackedPlayer = toolkitViewModel::saveTrackedPlayer,
                    onTrackedPlayerAutoRefreshChanged =
                        toolkitViewModel::setTrackedPlayerAutoRefresh,
                    onRefreshTrackedPlayer = toolkitViewModel::refreshTrackedPlayer,
                    onResetTrackedPlayerBaseline =
                        toolkitViewModel::resetTrackedPlayerBaseline,
                    onClearTrackedPlayer = toolkitViewModel::clearTrackedPlayer,
                    onUndoToolkitChange = toolkitViewModel::undoLastChange,
                    onDismissToolkitUndo = toolkitViewModel::dismissUndo,
                    onOpenUrl = openCompanionUrl,
                    starsContent = {
                        RuneCompanionApp(
                            state = state.value,
                            featureData = featureState.value.data,
                            alertSettings = alertSettings.value,
                            filterSettings = starFilterSettings.value,
                            overlayPermissionGranted = permission.value,
                            overlayRunning = overlayRunning.value,
                            overlaySettings = configuredOverlay.value,
                            onRefresh = starViewModel::refresh,
                            onAlertWorldsChanged = starViewModel::setAlertWorlds,
                            onHideDangerousWorldsChanged =
                                starViewModel::setHideDangerousWorlds,
                            onLocationSelected = starViewModel::setLocationSelected,
                            onLocationsSelected = starViewModel::setLocationsSelected,
                            onAllLocationsSelected = starViewModel::setAllLocationsSelected,
                            onAlertLocationsChanged = starViewModel::setAlertLocations,
                            onAlertTierToggled = starViewModel::toggleAlertTier,
                            onClearAlertTiers = starViewModel::clearAlertTiers,
                            onAlertsEnabledChanged = { enabled ->
                                if (!enabled) {
                                    starViewModel.setAlertsEnabled(false)
                                } else if (canPostNotifications()) {
                                    starViewModel.setAlertsEnabled(true)
                                } else {
                                    alertNotificationPermission.launch(
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    )
                                }
                            },
                            onQuietHoursEnabledChanged = starViewModel::setQuietHoursEnabled,
                            onQuietHoursChanged = starViewModel::setQuietHours,
                            onOpenNotificationSettings = ::openNotificationSettings,
                            onGrantOverlayPermission = ::openOverlaySettings,
                            onToggleOverlay = {
                                if (overlayRunning.value) {
                                    stopService(Intent(this, OverlayService::class.java))
                                } else if (!permission.value) {
                                    openOverlaySettings()
                                } else if (
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        this,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    overlayNotificationPermission.launch(
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    )
                                } else {
                                    startOverlay(this)
                                }
                            },
                            onOverlayModuleToggled = { module ->
                                updateOverlaySettings { it.toggled(module) }
                            },
                            onOverlayModuleSelected = { module ->
                                updateOverlaySettings { it.selected(module) }
                            },
                            onOverlayModuleMoved = { module, delta ->
                                updateOverlaySettings { it.moveModule(module, delta) }
                            },
                            onOverlayWidthChanged = { width ->
                                updateOverlaySettings { it.copy(compactWidthDp = width) }
                            },
                            onOverlayOpacityChanged = { opacity ->
                                updateOverlaySettings { it.copy(opacityPercent = opacity) }
                            },
                            onOpenStarMiners = ::openStarMiners,
                            onOpenUrl = openCompanionUrl,
                        )
                    },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayPermission.value = Settings.canDrawOverlays(this)
        notificationPermission.value = canPostNotifications()
        overlaySettings.value = overlayPreferences.load()
        personalizationSettings.value = personalizationPreferences.load()
        activityProfiles.value = loadActivityProfiles()
    }

    override fun onStart() {
        super.onStart()
        starViewModel.setAppInForeground(true)
        toolkitViewModel.setAppInForeground(true)
        featureViewModel.setAppInForeground(true)
    }

    override fun onStop() {
        starViewModel.setAppInForeground(false)
        toolkitViewModel.setAppInForeground(false)
        featureViewModel.setAppInForeground(false)
        super.onStop()
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun updatePersonalizationSettings(settings: PersonalizationSettings) {
        val normalized = settings.normalized()
        personalizationSettings.value = normalized
        personalizationPreferences.save(normalized)
        updateActiveProfile { it.copy(personalization = normalized) }
    }

    private fun openStarMiners() {
        openUrl("https://map.starminers.site/")
    }

    private fun openUrl(url: String) {
        if (!openTrustedExternalUrl(url)) {
            Toast.makeText(
                this,
                "Rune Companion blocked an untrusted link.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            },
        )
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun updateOverlaySettings(transform: (OverlaySettings) -> OverlaySettings) {
        val updated = transform(overlaySettings.value).normalized()
        overlayPreferences.save(updated)
        overlaySettings.value = updated
        updateActiveProfile { it.copy(overlay = updated) }
        if (OverlayService.running.value) {
            startService(OverlayService.reloadIntent(this))
        }
    }

    private fun loadActivityProfiles(): ActivityProfileState =
        activityProfilePreferences.load(
            legacyPersonalization = personalizationPreferences.load(),
            legacyOverlay = overlayPreferences.load(),
            selectedAccount = null,
            legacyFilters = starFilterPreferences.load(),
        )

    private fun updateActiveProfile(
        transform: (ActivityProfile) -> ActivityProfile,
    ) {
        val updated = activityProfiles.value.updateActive(transform)
        if (updated == activityProfiles.value) return
        activityProfiles.value = updated
        activityProfilePreferences.save(updated)
    }

    private fun captureActiveProfile() {
        val filters = starFilterPreferences.load()
        val updated = activityProfiles.value.updateActive { active ->
            active.copy(
                personalization = personalizationSettings.value,
                overlay = overlaySettings.value,
                selectedAccount = featureViewModel.state.value.data.selectedAccount,
                starFilters = ActivityStarFilters.from(filters),
            )
        }
        activityProfiles.value = updated
        activityProfilePreferences.save(updated)
    }

    private fun selectActivityProfile(profileId: String) {
        if (profileId == activityProfiles.value.activeProfileId) return
        captureActiveProfile()
        val selected = activityProfiles.value.select(profileId)
        activityProfiles.value = selected
        activityProfilePreferences.save(selected)
        applyActivityProfile(selected.activeProfile, refreshViewModels = true)
    }

    private fun createActivityProfile(name: String) {
        if (name.isBlank()) return
        captureActiveProfile()
        val updated = activityProfiles.value.addCopy(
            id = UUID.randomUUID().toString(),
            name = name,
        )
        activityProfiles.value = updated
        activityProfilePreferences.save(updated)
        applyActivityProfile(updated.activeProfile, refreshViewModels = true)
    }

    private fun renameActivityProfile(name: String) {
        if (name.isBlank()) return
        val updated = activityProfiles.value.renameActive(name)
        activityProfiles.value = updated
        activityProfilePreferences.save(updated)
    }

    private fun deleteActivityProfile() {
        if (activityProfiles.value.profiles.size <= 1) return
        val updated = activityProfiles.value.deleteActive()
        activityProfiles.value = updated
        activityProfilePreferences.save(updated)
        applyActivityProfile(updated.activeProfile, refreshViewModels = true)
    }

    private fun applyActivityProfile(
        profile: ActivityProfile,
        refreshViewModels: Boolean,
    ) {
        val personalization = profile.personalization.normalized()
        val overlay = profile.overlay.normalized()
        personalizationPreferences.save(personalization)
        overlayPreferences.save(overlay)
        starFilterPreferences.save(
            profile.starFilters.applyTo(starFilterPreferences.load()),
        )
        personalizationSettings.value = personalization
        overlaySettings.value = overlay
        if (refreshViewModels) {
            starViewModel.reloadPreferences()
            featureViewModel.selectAccountForProfile(profile.selectedAccount)
            if (OverlayService.running.value) {
                startService(OverlayService.reloadIntent(this))
            }
        }
    }
}

private fun startOverlay(context: Context) {
    ContextCompat.startForegroundService(
        context,
        Intent(context, OverlayService::class.java),
    )
}
