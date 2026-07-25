package io.github.taxledgr.runecompanion.overlay

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taxledgr.runecompanion.features.FeatureViewModel
import io.github.taxledgr.runecompanion.personalization.PersonalizationSettings
import io.github.taxledgr.runecompanion.toolkit.ToolkitViewModel
import io.github.taxledgr.runecompanion.ui.OverlayEditorFrame
import io.github.taxledgr.runecompanion.ui.RuneCompanionApp
import io.github.taxledgr.runecompanion.ui.RuneCompanionShell
import io.github.taxledgr.runecompanion.ui.StarViewModel
import io.github.taxledgr.runecompanion.ui.theme.RuneCompanionTheme
import io.github.taxledgr.runecompanion.util.TrustedUrlPolicy
import io.github.taxledgr.runecompanion.util.openTrustedExternalUrl

@Composable
fun OverlayEditorContent(
    context: Context,
    module: OverlayModule,
    toolkitViewModel: ToolkitViewModel,
    featureViewModel: FeatureViewModel,
    starViewModel: StarViewModel,
    initialOverlaySettings: OverlaySettings,
    initialPersonalizationSettings: PersonalizationSettings,
    initialQuestId: String? = null,
    onOverlaySettingsChanged: (OverlaySettings) -> Unit,
    onPersonalizationChanged: (PersonalizationSettings) -> Unit,
    onClose: () -> Unit,
    onStopOverlay: () -> Unit,
) {
    val starState by starViewModel.state.collectAsStateWithLifecycle()
    val alertSettings by starViewModel.alertSettings.collectAsStateWithLifecycle()
    val filterSettings by starViewModel.filterSettings.collectAsStateWithLifecycle()
    val toolkitState by toolkitViewModel.state.collectAsStateWithLifecycle()
    val featureState by featureViewModel.state.collectAsStateWithLifecycle()
    var overlaySettings by remember { mutableStateOf(initialOverlaySettings) }
    var personalizationSettings by remember {
        mutableStateOf(initialPersonalizationSettings)
    }
    val editorRoute = module.editorRoute()
    val notificationPermissionGranted = canPostNotifications(context)
    val openIntent: (Intent) -> Unit = { intent ->
        onClose()
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    val openUrl: (String) -> Unit = { url ->
        val trustedUrl = TrustedUrlPolicy.normalizeHttpsUrl(
            url,
            TrustedUrlPolicy.externalHosts,
        )
        if (trustedUrl != null) {
            onClose()
            context.openTrustedExternalUrl(trustedUrl)
        }
    }
    val openNotificationSettings = {
        openIntent(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            },
        )
    }

    RuneCompanionTheme(layoutDensity = personalizationSettings.overlayDensity) {
        OverlayEditorFrame(
            sectionLabel = module.label,
            onClose = onClose,
        ) {
            RuneCompanionShell(
                toolkitState = toolkitState,
                featureState = featureState,
                featureViewModel = featureViewModel,
                personalizationSettings = personalizationSettings,
                initialTab = editorRoute.tab,
                initialFeatureId = editorRoute.featureId,
                initialQuestId = initialQuestId,
                showNavigation = false,
                overlayEditorModule = module,
                onPersonalizationChanged = { settings ->
                    val normalized = settings.normalized()
                    personalizationSettings = normalized
                    onPersonalizationChanged(normalized)
                },
                notificationPermissionGranted = notificationPermissionGranted,
                onRequestNotificationPermission = openNotificationSettings,
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
                onOpenUrl = openUrl,
                starsContent = {
                    RuneCompanionApp(
                        state = starState,
                        featureData = featureState.data,
                        alertSettings = alertSettings,
                        filterSettings = filterSettings,
                        overlayPermissionGranted = Settings.canDrawOverlays(context),
                        overlayRunning = true,
                        overlaySettings = overlaySettings,
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
                            if (!enabled || notificationPermissionGranted) {
                                starViewModel.setAlertsEnabled(enabled)
                            } else {
                                openNotificationSettings()
                            }
                        },
                        onQuietHoursEnabledChanged = starViewModel::setQuietHoursEnabled,
                        onQuietHoursChanged = starViewModel::setQuietHours,
                        onOpenNotificationSettings = openNotificationSettings,
                        onGrantOverlayPermission = {
                            openIntent(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        },
                        onToggleOverlay = onStopOverlay,
                        onOverlayModuleToggled = { selectedModule ->
                            val updated = overlaySettings.toggled(selectedModule)
                            overlaySettings = updated
                            onOverlaySettingsChanged(updated)
                        },
                        onOverlayModuleSelected = { selectedModule ->
                            val updated = overlaySettings.selected(selectedModule)
                            overlaySettings = updated
                            onOverlaySettingsChanged(updated)
                        },
                        onOverlayModuleMoved = { selectedModule, delta ->
                            val updated = overlaySettings.moveModule(selectedModule, delta)
                            overlaySettings = updated
                            onOverlaySettingsChanged(updated)
                        },
                        onOverlayWidthChanged = { width ->
                            val updated = overlaySettings.copy(compactWidthDp = width).normalized()
                            overlaySettings = updated
                            onOverlaySettingsChanged(updated)
                        },
                        onOverlayLandscapeWidthChanged = { width ->
                            val updated = overlaySettings.copy(
                                landscapeWidthDp = width,
                            ).normalized()
                            overlaySettings = updated
                            onOverlaySettingsChanged(updated)
                        },
                        onOverlayOpacityChanged = { opacity ->
                            val updated = overlaySettings.copy(opacityPercent = opacity).normalized()
                            overlaySettings = updated
                            onOverlaySettingsChanged(updated)
                        },
                        onOverlayTextScaleChanged = { scale ->
                            val updated = overlaySettings.copy(
                                textScalePercent = scale,
                            ).normalized()
                            overlaySettings = updated
                            onOverlaySettingsChanged(updated)
                        },
                        onOverlaySnapChanged = { enabled ->
                            val updated = overlaySettings.copy(snapToEdge = enabled).normalized()
                            overlaySettings = updated
                            onOverlaySettingsChanged(updated)
                        },
                        onOverlayAvoidControlsChanged = { enabled ->
                            val updated = overlaySettings.copy(
                                avoidGameControls = enabled,
                            ).normalized()
                            overlaySettings = updated
                            onOverlaySettingsChanged(updated)
                        },
                        onOverlayResetPlacement = {
                            val updated = overlaySettings.copy(
                                portraitPlacement = OverlayPlacement(),
                                landscapePlacement = OverlayPlacement(),
                            ).normalized()
                            overlaySettings = updated
                            onOverlaySettingsChanged(updated)
                        },
                        onExpandedStarChanged = starViewModel::setExpandedStar,
                        onPreferredStarRouteChanged = starViewModel::setPreferredRoute,
                        onOpenStarMiners = { openUrl("https://map.starminers.site/") },
                        onOpenUrl = openUrl,
                        editorMode = true,
                    )
                },
            )
        }
    }
}

private fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
