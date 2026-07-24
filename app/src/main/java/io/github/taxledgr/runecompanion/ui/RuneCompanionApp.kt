package io.github.taxledgr.runecompanion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.taxledgr.runecompanion.alerts.StarAlertSettings
import io.github.taxledgr.runecompanion.alerts.StarFilterSettings
import io.github.taxledgr.runecompanion.data.ShootingStar
import io.github.taxledgr.runecompanion.ui.theme.RuneCyan
import io.github.taxledgr.runecompanion.ui.theme.RuneGold
import io.github.taxledgr.runecompanion.ui.theme.RuneSurfaceRaised
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
    alertSettings: StarAlertSettings,
    filterSettings: StarFilterSettings,
    overlayPermissionGranted: Boolean,
    overlayRunning: Boolean,
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
    onOpenStarMiners: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedTier by remember { mutableIntStateOf(0) }
    var accessFilter by remember { mutableStateOf(WorldAccessFilter.ANY) }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
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

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Header(
                    starCount = state.stars.size,
                    fetchedAt = state.fetchedAt?.let { reportAge(it) },
                )
            }
            item {
                OverlayCard(
                    permissionGranted = overlayPermissionGranted,
                    running = overlayRunning,
                    onGrantPermission = onGrantOverlayPermission,
                    onToggle = onToggleOverlay,
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
                StarCard(star, state.worlds[star.world])
            }
            item {
                Attribution(onOpenStarMiners)
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
    onGrantPermission: () -> Unit,
    onToggle: () -> Unit,
) {
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
                        text = "Floating star panel",
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
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
