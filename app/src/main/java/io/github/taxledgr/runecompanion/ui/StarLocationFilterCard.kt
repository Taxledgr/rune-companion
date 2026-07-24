package io.github.taxledgr.runecompanion.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.taxledgr.runecompanion.alerts.StarFilterSettings
import io.github.taxledgr.runecompanion.data.StarLocationCatalog
import io.github.taxledgr.runecompanion.ui.theme.RuneCyan
import io.github.taxledgr.runecompanion.ui.theme.RuneSurfaceRaised

@Composable
fun StarLocationFilterCard(
    settings: StarFilterSettings,
    onHideDangerousWorldsChanged: (Boolean) -> Unit,
    onLocationSelected: (String, Boolean) -> Unit,
    onLocationsSelected: (Collection<String>, Boolean) -> Unit,
    onAllLocationsSelected: (Boolean) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedArea by rememberSaveable {
        mutableStateOf(StarLocationCatalog.areas.keys.first())
    }
    val selectedCount = remember(settings.excludedLocations) {
        StarLocationCatalog.allNames.count { location ->
            settings.excludedLocations.none { it.equals(location, ignoreCase = true) }
        }
    }

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
                        "Safe-world & area filters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "$selectedCount of ${StarLocationCatalog.allNames.size} landing sites enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = RuneCyan,
                    )
                }
                OutlinedButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Done" else "Locations")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hide dangerous worlds", fontWeight = FontWeight.Bold)
                    Text(
                        "Excludes PvP, Bounty Hunter, High Risk, Wilderness PK, Deadman, and similar worlds using Jagex's live activity labels.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.hideDangerousWorlds,
                    onCheckedChange = onHideDangerousWorldsChanged,
                )
            }
            if (settings.hideDangerousWorlds && !settings.worldSafetyLoaded) {
                Text(
                    "Updating official world safety labels…",
                    style = MaterialTheme.typography.bodySmall,
                    color = RuneCyan,
                )
            }

            if (expanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onAllLocationsSelected(true) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Select all")
                    }
                    OutlinedButton(
                        onClick = { onAllLocationsSelected(false) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Deselect all")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StarLocationCatalog.areas.keys.forEach { area ->
                        FilterChip(
                            selected = selectedArea == area,
                            onClick = { selectedArea = area },
                            label = { Text(area) },
                        )
                    }
                }

                val areaLocations = StarLocationCatalog.areas[selectedArea].orEmpty()
                val wholeAreaSelected = areaLocations.all { location ->
                    settings.excludedLocations.none {
                        it.equals(location, ignoreCase = true)
                    }
                }
                FilterRow(
                    label = "All $selectedArea locations",
                    selected = wholeAreaSelected,
                    bold = true,
                    onSelected = { onLocationsSelected(areaLocations, it) },
                )
                areaLocations.forEach { location ->
                    val selected = settings.excludedLocations.none {
                        it.equals(location, ignoreCase = true)
                    }
                    FilterRow(
                        label = location,
                        selected = selected,
                        onSelected = { onLocationSelected(location, it) },
                    )
                }

                Text(
                    "The 82-site catalogue follows the locations published by Star Miners. Filters are stored on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    label: String,
    selected: Boolean,
    bold: Boolean = false,
    onSelected: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onSelected,
        )
        Text(
            label,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
