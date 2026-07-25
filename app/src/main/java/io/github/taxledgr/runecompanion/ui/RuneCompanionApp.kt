package io.github.taxledgr.runecompanion.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.taxledgr.runecompanion.alerts.StarAlertSettings
import io.github.taxledgr.runecompanion.alerts.StarFilterSettings
import io.github.taxledgr.runecompanion.data.ShootingStar
import io.github.taxledgr.runecompanion.data.StarLocationCatalog
import io.github.taxledgr.runecompanion.data.StarMapCatalog
import io.github.taxledgr.runecompanion.data.StarMapPoint
import io.github.taxledgr.runecompanion.features.FeatureData
import io.github.taxledgr.runecompanion.features.RankedStarTravelRoute
import io.github.taxledgr.runecompanion.features.StarTravelCatalog
import io.github.taxledgr.runecompanion.features.StarTravelPlanner
import io.github.taxledgr.runecompanion.util.AllowlistedResourceWebViewClient
import io.github.taxledgr.runecompanion.util.applyPrivateWebSettings
import io.github.taxledgr.runecompanion.overlay.OverlayModule
import io.github.taxledgr.runecompanion.overlay.OverlaySettings
import io.github.taxledgr.runecompanion.ui.theme.RuneCyan
import io.github.taxledgr.runecompanion.ui.theme.RuneGold
import io.github.taxledgr.runecompanion.ui.theme.RuneSurfaceRaised
import io.github.taxledgr.runecompanion.ui.theme.LocalRuneLayout
import io.github.taxledgr.runecompanion.util.reportAge
import io.github.taxledgr.runecompanion.util.starTimingSummary

private enum class WorldAccessFilter(val label: String) {
    ANY("Any access"),
    FREE("F2P worlds"),
    MEMBERS("Members"),
}

@Composable
fun RuneCompanionApp(
    state: StarUiState,
    featureData: FeatureData,
    alertSettings: StarAlertSettings,
    filterSettings: StarFilterSettings,
    overlayPermissionGranted: Boolean,
    overlayRunning: Boolean,
    overlaySettings: OverlaySettings,
    onRefresh: () -> Unit,
    onAlertWorldsChanged: (String) -> Unit,
    onHideDangerousWorldsChanged: (Boolean) -> Unit,
    onLocationSelected: (String, Boolean) -> Unit,
    onLocationsSelected: (Collection<String>, Boolean) -> Unit,
    onAllLocationsSelected: (Boolean) -> Unit,
    onAlertLocationsChanged: (String) -> Unit,
    onAlertTierToggled: (Int) -> Unit,
    onClearAlertTiers: () -> Unit,
    onAlertsEnabledChanged: (Boolean) -> Unit,
    onQuietHoursEnabledChanged: (Boolean) -> Unit,
    onQuietHoursChanged: (Int, Int) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onGrantOverlayPermission: () -> Unit,
    onToggleOverlay: () -> Unit,
    onOverlayModuleToggled: (OverlayModule) -> Unit,
    onOverlayModuleSelected: (OverlayModule) -> Unit,
    onOverlayModuleMoved: (OverlayModule, Int) -> Unit,
    onOverlayWidthChanged: (Int) -> Unit,
    onOverlayLandscapeWidthChanged: (Int) -> Unit,
    onOverlayOpacityChanged: (Int) -> Unit,
    onOverlayTextScaleChanged: (Int) -> Unit,
    onOverlaySnapChanged: (Boolean) -> Unit,
    onOverlayAvoidControlsChanged: (Boolean) -> Unit,
    onOverlayResetPlacement: () -> Unit,
    onExpandedStarChanged: (String?) -> Unit,
    onPreferredStarRouteChanged: (String, String?) -> Unit,
    onOpenStarMiners: () -> Unit,
    onOpenUrl: (String) -> Unit,
    editorMode: Boolean = false,
) {
    val layout = LocalRuneLayout.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTier by rememberSaveable { mutableIntStateOf(0) }
    var accessFilter by rememberSaveable { mutableStateOf(WorldAccessFilter.ANY) }
    var selectedRegion by rememberSaveable { mutableStateOf<String?>(null) }
    var controlsExpanded by rememberSaveable { mutableStateOf(editorMode) }
    val filteredStars = remember(
        state.stars,
        state.worlds,
        query,
        selectedTier,
        accessFilter,
        selectedRegion,
        filterSettings,
    ) {
        state.stars.filter { star ->
            val matchesTier = selectedTier == 0 || star.tier == selectedTier
            val matchesQuery = query.isBlank() ||
                star.world.toString().contains(query, ignoreCase = true) ||
                star.locationName.contains(query, ignoreCase = true) ||
                star.calledBy.contains(query, ignoreCase = true)
            val worldInfo = state.worlds[star.world]
            val matchesAccess = when (accessFilter) {
                WorldAccessFilter.ANY -> true
                WorldAccessFilter.FREE -> worldInfo?.members == false
                WorldAccessFilter.MEMBERS -> worldInfo?.members == true
            }
            val matchesRegion = selectedRegion == null || worldInfo?.region == selectedRegion
            val matchesSavedFilters = filterSettings.includes(star)
            matchesTier &&
                matchesQuery &&
                matchesAccess &&
                matchesRegion &&
                matchesSavedFilters
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing),
        ) {
            item {
                Header(
                    starCount = state.stars.size,
                    fetchedAt = state.fetchedAt?.let { reportAge(it) },
                )
            }
            item {
                DataFreshnessCard(
                    source = "Star Miners",
                    updatedAt = state.fetchedAt,
                    expectedRefreshMinutes = 1,
                    refreshing = state.isLoading,
                    cached = state.isCached,
                    error = state.error,
                    onRetry = onRefresh,
                )
            }
            item {
                CompanionControlsCard(
                    expanded = controlsExpanded,
                    running = overlayRunning,
                    overlayPermissionGranted = overlayPermissionGranted,
                    alertsEnabled = alertSettings.enabled,
                    hideDangerousWorlds = filterSettings.hideDangerousWorlds,
                    selectedLocations = StarLocationCatalog.allNames.count { location ->
                        filterSettings.excludedLocations.none {
                            it.equals(location, ignoreCase = true)
                        }
                    },
                    totalLocations = StarLocationCatalog.allNames.size,
                    onOverlayAction = if (overlayPermissionGranted) {
                        onToggleOverlay
                    } else {
                        onGrantOverlayPermission
                    },
                    onExpandedChanged = { controlsExpanded = it },
                )
            }
            if (controlsExpanded) {
                item {
                    OverlayCard(
                        permissionGranted = overlayPermissionGranted,
                        running = overlayRunning,
                        settings = overlaySettings,
                        onGrantPermission = onGrantOverlayPermission,
                        onToggle = onToggleOverlay,
                        onModuleToggled = onOverlayModuleToggled,
                        onModuleSelected = onOverlayModuleSelected,
                        onModuleMoved = onOverlayModuleMoved,
                        onWidthChanged = onOverlayWidthChanged,
                        onLandscapeWidthChanged = onOverlayLandscapeWidthChanged,
                        onOpacityChanged = onOverlayOpacityChanged,
                        onTextScaleChanged = onOverlayTextScaleChanged,
                        onSnapChanged = onOverlaySnapChanged,
                        onAvoidControlsChanged = onOverlayAvoidControlsChanged,
                        onResetPlacement = onOverlayResetPlacement,
                    )
                }
                item {
                    StarAlertSettingsCard(
                        settings = alertSettings,
                        onWorldsChanged = onAlertWorldsChanged,
                        onLocationsChanged = onAlertLocationsChanged,
                        onTierToggled = onAlertTierToggled,
                        onClearTiers = onClearAlertTiers,
                        onEnabledChanged = onAlertsEnabledChanged,
                        onQuietHoursEnabledChanged = onQuietHoursEnabledChanged,
                        onQuietHoursChanged = onQuietHoursChanged,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                    )
                }
                item {
                    StarLocationFilterCard(
                        settings = filterSettings,
                        onHideDangerousWorldsChanged = onHideDangerousWorldsChanged,
                        onLocationSelected = onLocationSelected,
                        onLocationsSelected = onLocationsSelected,
                        onAllLocationsSelected = onAllLocationsSelected,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onRefresh,
                        enabled = !state.isLoading,
                    ) {
                        Text(if (state.isLoading) "Refreshing…" else "Refresh stars")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onOpenStarMiners,
                    ) {
                        Text("Official map")
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("World, location, or scout") },
                    singleLine = true,
                )
            }
            item {
                TierFilters(
                    selectedTier = selectedTier,
                    onTierSelected = { selectedTier = it },
                )
            }
            item {
                WorldFilters(
                    selectedAccess = accessFilter,
                    onAccessSelected = { accessFilter = it },
                    selectedRegion = selectedRegion,
                    regions = state.worlds.values
                        .filter { world -> state.stars.any { it.world == world.world } }
                        .map { it.region }
                        .distinct()
                        .sorted(),
                    onRegionSelected = { selectedRegion = it },
                )
            }
            state.error?.let { error ->
                item {
                    ErrorCard(message = error, onRetry = onRefresh)
                }
            }
            if (!state.isLoading && filteredStars.isEmpty()) {
                item {
                    EmptyState()
                }
            }
            items(
                items = filteredStars,
                key = { "${it.world}-${it.locationId}-${it.calledAt}" },
            ) { star ->
                StarCard(
                    star = star,
                    worldInfo = state.worlds[star.world],
                    featureData = featureData,
                    routeExpanded = state.displaySettings.expandedStarKey == star.displayKey(),
                    preferredRouteMethod = state.displaySettings.preferredRoutes[star.locationName],
                    onExpandedChanged = { expanded ->
                        onExpandedStarChanged(if (expanded) star.displayKey() else null)
                    },
                    onPreferredRouteChanged = { method ->
                        onPreferredStarRouteChanged(star.locationName, method)
                    },
                    onOpenRouteGuide = {
                        onOpenUrl(
                            "https://oldschool.runescape.wiki/w/Shooting_Stars#Landing_sites",
                        )
                    },
                )
            }
            item {
                Attribution(onOpenStarMiners)
            }
        }
    }
}

@Composable
private fun CompanionControlsCard(
    expanded: Boolean,
    running: Boolean,
    overlayPermissionGranted: Boolean,
    alertsEnabled: Boolean,
    hideDangerousWorlds: Boolean,
    selectedLocations: Int,
    totalLocations: Int,
    onOverlayAction: () -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RuneSurfaceRaised),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Companion controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                listOf(
                    if (running) "Overlay active" else "Overlay stopped",
                    if (alertsEnabled) "alerts on" else "alerts off",
                    if (hideDangerousWorlds) "safe worlds" else "all world types",
                    "$selectedLocations/$totalLocations sites",
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onOverlayAction,
                    colors = if (running) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                ) {
                    Text(
                        when {
                            running -> "Stop overlay"
                            !overlayPermissionGranted -> "Allow overlay"
                            else -> "Start overlay"
                        },
                    )
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onExpandedChanged(!expanded) },
                ) {
                    Text(if (expanded) "Done" else "Manage")
                }
            }
        }
    }
}

@Composable
private fun WorldFilters(
    selectedAccess: WorldAccessFilter,
    onAccessSelected: (WorldAccessFilter) -> Unit,
    selectedRegion: String?,
    regions: List<String>,
    onRegionSelected: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WorldAccessFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedAccess == filter,
                    onClick = { onAccessSelected(filter) },
                    label = { Text(filter.label) },
                )
            }
        }
        if (regions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedRegion == null,
                    onClick = { onRegionSelected(null) },
                    label = { Text("Any server region") },
                )
                regions.forEach { region ->
                    FilterChip(
                        selected = selectedRegion == region,
                        onClick = { onRegionSelected(region) },
                        label = { Text(region) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(starCount: Int, fetchedAt: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(RuneGold, CircleShape),
            )
            Text(
                text = "  RUNE COMPANION",
                color = RuneGold,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "Shooting Stars",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = when {
                fetchedAt == null -> "Loading the Star Miners feed…"
                starCount == 1 -> "1 live report • updated $fetchedAt"
                else -> "$starCount live reports • updated $fetchedAt"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OverlayCard(
    permissionGranted: Boolean,
    running: Boolean,
    settings: OverlaySettings,
    onGrantPermission: () -> Unit,
    onToggle: () -> Unit,
    onModuleToggled: (OverlayModule) -> Unit,
    onModuleSelected: (OverlayModule) -> Unit,
    onModuleMoved: (OverlayModule, Int) -> Unit,
    onWidthChanged: (Int) -> Unit,
    onLandscapeWidthChanged: (Int) -> Unit,
    onOpacityChanged: (Int) -> Unit,
    onTextScaleChanged: (Int) -> Unit,
    onSnapChanged: (Boolean) -> Unit,
    onAvoidControlsChanged: (Boolean) -> Unit,
    onResetPlacement: () -> Unit,
) {
    var configuring by rememberSaveable { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = RuneSurfaceRaised),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Floating companion panel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (running) "Active over OSRS" else "User-controlled Android overlay",
                        color = if (running) RuneCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = if (permissionGranted) onToggle else onGrantPermission,
                    colors = if (running) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                ) {
                    Text(
                        when {
                            !permissionGranted -> "Allow"
                            running -> "Stop"
                            else -> "Start"
                        },
                    )
                }
            }
            Text(
                text = "Passive display only — no OCR, automation, or OSRS client access.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${settings.enabledModules.size} " +
                    "${if (settings.enabledModules.size == 1) "section" else "sections"} " +
                    "enabled • showing ${settings.selectedModule.label}",
                style = MaterialTheme.typography.bodySmall,
                color = RuneGold,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedButton(
                onClick = { configuring = !configuring },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (configuring) "Hide overlay configuration" else "Configure overlay sections")
            }
            if (configuring) {
                Text(
                    "Portrait panel size",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        280 to "Small",
                        310 to "Standard",
                        360 to "Large",
                    ).forEach { (width, label) ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = settings.compactWidthDp == width,
                            onClick = { onWidthChanged(width) },
                            label = { Text(label) },
                        )
                    }
                }
                Text(
                    "Landscape panel size",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        280 to "Small",
                        330 to "Standard",
                        370 to "Large",
                    ).forEach { (width, label) ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = settings.landscapeWidthDp == width,
                            onClick = { onLandscapeWidthChanged(width) },
                            label = { Text(label) },
                        )
                    }
                }
                Text(
                    "Panel opacity",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        70 to "70%",
                        85 to "85%",
                        100 to "100%",
                    ).forEach { (opacity, label) ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = settings.opacityPercent == opacity,
                            onClick = { onOpacityChanged(opacity) },
                            label = { Text(label) },
                        )
                    }
                }
                Text(
                    "Overlay text size",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        90 to "Compact",
                        100 to "Standard",
                        120 to "Large",
                    ).forEach { (scale, label) ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = settings.textScalePercent == scale,
                            onClick = { onTextScaleChanged(scale) },
                            label = { Text(label) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = settings.snapToEdge,
                        onClick = { onSnapChanged(!settings.snapToEdge) },
                        label = { Text("Snap to edges") },
                    )
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = settings.avoidGameControls,
                        onClick = { onAvoidControlsChanged(!settings.avoidGameControls) },
                        label = { Text("Keep centre-safe") },
                    )
                }
                Text(
                    "Centre-safe keeps the landscape panel away from the usual left and right " +
                        "OSRS control rails. Position and size are remembered separately for " +
                        "portrait and landscape.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onResetPlacement,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reset overlay position")
                }
                Text(
                    "Choose everything available in the pop-out",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                OverlayModule.entries.groupBy(OverlayModule::category).forEach { (category, modules) ->
                    Text(
                        category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    modules.chunked(2).forEach { rowModules ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowModules.forEach { module ->
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = module in settings.enabledModules,
                                    onClick = { onModuleToggled(module) },
                                    label = {
                                        Text(
                                            "${module.symbol} ${module.label}",
                                            maxLines = 2,
                                        )
                                    },
                                )
                            }
                            if (rowModules.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Text(
                    "Section order",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                settings.orderedModules.forEachIndexed { index, module ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${index + 1}. ${module.symbol} ${module.label}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(
                            onClick = { onModuleMoved(module, -1) },
                            enabled = index > 0,
                        ) { Text("↑") }
                        TextButton(
                            onClick = { onModuleMoved(module, 1) },
                            enabled = index < settings.orderedModules.lastIndex,
                        ) { Text("↓") }
                    }
                }
                Text(
                    "Current section when the panel opens",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    settings.orderedModules.forEach { module ->
                        FilterChip(
                            selected = module == settings.selectedModule,
                            onClick = { onModuleSelected(module) },
                            label = { Text("${module.symbol} ${module.label}") },
                        )
                    }
                }
                Text(
                    "Use ‹ and › in the floating panel to cycle enabled sections. " +
                        "Changes apply immediately while it is running.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TierFilters(selectedTier: Int, onTierSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(0, 9, 8, 7, 6, 5, 4, 3, 2, 1).forEach { tier ->
            FilterChip(
                selected = selectedTier == tier,
                onClick = { onTierSelected(tier) },
                label = { Text(if (tier == 0) "All tiers" else "T$tier") },
            )
        }
    }
}

@Composable
private fun StarCard(
    star: ShootingStar,
    worldInfo: io.github.taxledgr.runecompanion.data.WorldInfo?,
    featureData: FeatureData,
    routeExpanded: Boolean,
    preferredRouteMethod: String?,
    onExpandedChanged: (Boolean) -> Unit,
    onPreferredRouteChanged: (String?) -> Unit,
    onOpenRouteGuide: () -> Unit,
) {
    val guide = remember(star.locationName) {
        StarTravelCatalog.guideFor(star.locationName)
    }
    val rankedRoutes = remember(guide, featureData) {
        guide?.let { StarTravelPlanner.rank(it, featureData) }.orEmpty()
    }
    val mapPoint = remember(star.locationName) {
        StarMapCatalog.pointFor(star.locationName)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (routeExpanded) {
                            "Hide travel route"
                        } else {
                            "Show travel route and map"
                        },
                    ) { onExpandedChanged(!routeExpanded) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(RuneGold.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "T${star.tier}",
                        color = RuneGold,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "World ${star.world}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = star.locationName,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    worldInfo?.let { world ->
                        Text(
                            "${if (world.members) "Members" else "F2P"} • ${world.region}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (world.dangerous) {
                            Text(
                                "Dangerous world • ${world.activity}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "${reportAge(star.calledAt)} • ${star.calledBy}",
                        style = MaterialTheme.typography.labelMedium,
                        color = RuneCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (routeExpanded) {
                            "Tap to hide directions and map"
                        } else {
                            "Tap for exact route, teleports and map"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = RuneGold,
                        fontWeight = FontWeight.SemiBold,
                    )
                    starTimingSummary(
                        calledAt = star.calledAt,
                        tier = star.tier,
                        minimumArrival = star.minimumArrival,
                        maximumArrival = star.maximumArrival,
                    )?.let { timing ->
                        Text(
                            timing,
                            style = MaterialTheme.typography.labelMedium,
                            color = RuneGold,
                        )
                    }
                }
            }
            val displayRoutes = remember(rankedRoutes, preferredRouteMethod) {
                val preferred = rankedRoutes.firstOrNull {
                    it.available && it.route.method == preferredRouteMethod
                }
                if (preferred == null) {
                    rankedRoutes
                } else {
                    listOf(preferred) + rankedRoutes.filterNot { it === preferred }
                }
            }
            if (routeExpanded) displayRoutes.firstOrNull()?.let { best ->
                HorizontalDivider()
                StarRouteSummary(
                    best = best,
                    allRoutes = displayRoutes,
                    mapPoint = mapPoint,
                    preferredRouteMethod = preferredRouteMethod,
                    onPreferredRouteChanged = onPreferredRouteChanged,
                    onCollapse = { onExpandedChanged(false) },
                    onOpenRouteGuide = onOpenRouteGuide,
                )
            }
        }
    }
}

@Composable
private fun StarRouteSummary(
    best: RankedStarTravelRoute,
    allRoutes: List<RankedStarTravelRoute>,
    mapPoint: StarMapPoint?,
    preferredRouteMethod: String?,
    onPreferredRouteChanged: (String?) -> Unit,
    onCollapse: () -> Unit,
    onOpenRouteGuide: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            when {
                best.route.method == preferredRouteMethod -> "YOUR SAVED ROUTE"
                best.available -> "FASTEST AVAILABLE FOR YOU"
                else -> "FASTEST KNOWN ROUTE"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (best.available) RuneCyan else RuneGold,
            fontWeight = FontWeight.Bold,
        )
        Text(best.route.method, fontWeight = FontWeight.Bold)
        Text(
            best.route.steps,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RouteRequirements(best)
        TextButton(
            onClick = {
                onPreferredRouteChanged(
                    if (best.route.method == preferredRouteMethod) null else best.route.method,
                )
            },
            enabled = best.available,
        ) {
            Text(
                if (best.route.method == preferredRouteMethod) {
                    "Remove saved route"
                } else {
                    "Save as my preferred route"
                },
            )
        }
        mapPoint?.let { point ->
            Spacer(Modifier.height(4.dp))
            Text(
                "EXACT LANDING SITE • ${point.region}",
                style = MaterialTheme.typography.labelSmall,
                color = RuneGold,
                fontWeight = FontWeight.Bold,
            )
            StarMapPreview(point)
        }
        allRoutes.drop(1).forEachIndexed { index, ranked ->
            HorizontalDivider()
            Text(
                if (ranked.available) {
                    "AVAILABLE ALTERNATIVE ${index + 2} • ${ranked.route.method}"
                } else {
                    "UNAVAILABLE • ${ranked.route.method}"
                },
                fontWeight = FontWeight.SemiBold,
                color = if (ranked.available) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                ranked.route.steps,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RouteRequirements(ranked)
            if (ranked.available) {
                TextButton(
                    onClick = {
                        onPreferredRouteChanged(
                            if (ranked.route.method == preferredRouteMethod) {
                                null
                            } else {
                                ranked.route.method
                            },
                        )
                    },
                ) {
                    Text(
                        if (ranked.route.method == preferredRouteMethod) {
                            "Remove saved route"
                        } else {
                            "Use this route next time"
                        },
                    )
                }
            }
        }
        Text(
            "Availability uses the selected profile's public Agility/Magic levels " +
                "and More → Teleport route planner → My teleports.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCollapse) {
                Text("Hide route & map")
            }
            TextButton(onClick = onOpenRouteGuide) {
                Text("Wiki landing sites")
            }
        }
    }
}

private fun ShootingStar.displayKey(): String =
    "$world|$locationId|$calledAt"

@Composable
private fun StarMapPreview(point: StarMapPoint) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(12.dp)),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.rgb(7, 19, 28))
                applyPrivateWebSettings()
                settings.loadsImagesAutomatically = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webViewClient = AllowlistedResourceWebViewClient(
                    setOf(StarMapCatalog.MAP_IMAGE_URL),
                )
                tag = point.locationName
                loadDataWithBaseURL(
                    StarMapCatalog.MAP_BASE_URL,
                    StarMapCatalog.previewHtml(point),
                    "text/html",
                    "UTF-8",
                    null,
                )
                webView = this
            }
        },
        update = { view ->
            if (view.tag != point.locationName) {
                view.tag = point.locationName
                view.loadDataWithBaseURL(
                    StarMapCatalog.MAP_BASE_URL,
                    StarMapCatalog.previewHtml(point),
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        },
    )
    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                webViewClient = WebViewClient()
                destroy()
            }
        }
    }
}

@Composable
private fun RouteRequirements(ranked: RankedStarTravelRoute) {
    val details = buildList {
        if (ranked.available) add("Configured")
        addAll(ranked.missing)
        ranked.route.agilityLevel?.let { add("$it Agility shortcut") }
        addAll(ranked.route.requirements)
        if (ranked.route.dangerous) add("WILDERNESS — risk items and check the world")
    }
    if (details.isNotEmpty()) {
        Text(
            details.joinToString(" • "),
            style = MaterialTheme.typography.labelSmall,
            color = if (ranked.route.dangerous) {
                MaterialTheme.colorScheme.error
            } else if (ranked.available) {
                RuneCyan
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Could not update the feed", fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onRetry) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No current reports match these filters.",
            modifier = Modifier.padding(24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Attribution(onOpenStarMiners: () -> Unit) {
    Column(
        modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider()
        Text(
            text = "Community reports provided by Star Miners. Rune Companion is independent and is not affiliated with Jagex, RuneLite, or Star Miners.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onOpenStarMiners) {
            Text("Open map.starminers.site")
        }
    }
}
