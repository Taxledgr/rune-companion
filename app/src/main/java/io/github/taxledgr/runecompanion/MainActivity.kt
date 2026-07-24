package io.github.taxledgr.runecompanion

import android.Manifest
import android.content.Context
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
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import io.github.taxledgr.runecompanion.overlay.OverlayPreferences
import io.github.taxledgr.runecompanion.overlay.OverlayService
import io.github.taxledgr.runecompanion.overlay.OverlaySettings
import io.github.taxledgr.runecompanion.features.FeatureViewModel
import io.github.taxledgr.runecompanion.ui.WikiReaderScreen
import io.github.taxledgr.runecompanion.ui.isWikiUrl
import io.github.taxledgr.runecompanion.ui.RuneCompanionApp
import io.github.taxledgr.runecompanion.ui.RuneCompanionShell
import io.github.taxledgr.runecompanion.ui.StarViewModel
import io.github.taxledgr.runecompanion.ui.theme.RuneCompanionTheme
import io.github.taxledgr.runecompanion.toolkit.ToolkitViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val overlayPermission = MutableStateFlow(false)
    private val notificationPermission = MutableStateFlow(false)
    private val toolkitViewModel: ToolkitViewModel by viewModels()
    private val featureViewModel: FeatureViewModel by viewModels()
    private val overlayPreferences by lazy { OverlayPreferences(this) }
    private val overlaySettings = MutableStateFlow(OverlaySettings())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlaySettings.value = overlayPreferences.load()
        setContent {
            RuneCompanionTheme {
                val starViewModel: StarViewModel = viewModel()
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
    }

    override fun onStart() {
        super.onStart()
        toolkitViewModel.setAppInForeground(true)
        featureViewModel.reloadFromDisk()
    }

    override fun onStop() {
        toolkitViewModel.setAppInForeground(false)
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

    private fun openStarMiners() {
        openUrl("https://map.starminers.site/")
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
        if (OverlayService.running.value) {
            startService(OverlayService.reloadIntent(this))
        }
    }
}

private fun startOverlay(context: Context) {
    ContextCompat.startForegroundService(
        context,
        Intent(context, OverlayService::class.java),
    )
}
