package io.github.taxledgr.runecompanion.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.taxledgr.runecompanion.personalization.AppTab
import io.github.taxledgr.runecompanion.personalization.ActivityProfile
import io.github.taxledgr.runecompanion.personalization.ActivityProfileState
import io.github.taxledgr.runecompanion.personalization.ExperiencePreset
import io.github.taxledgr.runecompanion.personalization.PersonalizationSettings

@Composable
fun PersonalizationScreen(
    settings: PersonalizationSettings,
    activityProfiles: ActivityProfileState,
    onSettingsChanged: (PersonalizationSettings) -> Unit,
    onActivityProfileSelected: (String) -> Unit,
    onActivityProfileCreated: (String) -> Unit,
    onActivityProfileRenamed: (String) -> Unit,
    onActivityProfileDeleted: () -> Unit,
) {
    var featureSearch by rememberSaveable { mutableStateOf("") }
    var newProfileName by rememberSaveable { mutableStateOf("") }
    var activeProfileName by rememberSaveable {
        mutableStateOf(activityProfiles.activeProfile.name)
    }
    LaunchedEffect(activityProfiles.activeProfileId) {
        activeProfileName = activityProfiles.activeProfile.name
    }
    val pinnedFeatures = settings.pinnedFeatureIds.mapNotNull { id ->
        CompanionFeature.entries.firstOrNull { it.name == id }
    }
    val startLabel = settings.startFeatureId
        ?.let { id -> CompanionFeature.entries.firstOrNull { it.name == id }?.title }
        ?: settings.startTab.label
    val visibleFeatures = CompanionFeature.entries
        .filter { feature ->
            featureSearch.isBlank() ||
                feature.title.contains(featureSearch, ignoreCase = true) ||
                feature.summary.contains(featureSearch, ignoreCase = true) ||
                feature.group.contains(featureSearch, ignoreCase = true)
        }
        .sortedWith(
            compareByDescending<CompanionFeature> {
                it.name in settings.pinnedFeatureIds
            }.thenBy { it.title },
        )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                eyebrow = "YOUR EXPERIENCE",
                title = "Customize Rune Companion",
                subtitle = "Switch complete activity setups, then tune what opens first and stays in reach.",
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Active activity profile", fontWeight = FontWeight.Bold)
                    Text(
                        "${activityProfiles.activeProfile.symbol} " +
                            activityProfiles.activeProfile.name,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        "Opens to $startLabel • ${settings.navigationTabs.size} tabs • " +
                            "${settings.pinnedFeatureIds.size} pinned tools",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Text("Switch activity", style = MaterialTheme.typography.titleMedium)
            Text(
                "Each profile remembers its navigation, shortcuts, overlay, character, and Star filters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activityProfiles.profiles, key = ActivityProfile::id) { profile ->
                    FilterChip(
                        selected = profile.id == activityProfiles.activeProfileId,
                        onClick = { onActivityProfileSelected(profile.id) },
                        label = { Text("${profile.symbol} ${profile.name}") },
                    )
                }
            }
        }
        item {
            Card {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Manage this profile", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = activeProfileName,
                        onValueChange = {
                            activeProfileName = it.take(ActivityProfile.MAX_NAME_LENGTH)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Profile name") },
                        singleLine = true,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onActivityProfileRenamed(activeProfileName) },
                            modifier = Modifier.weight(1f),
                            enabled = activeProfileName.isNotBlank() &&
                                activeProfileName.trim() !=
                                activityProfiles.activeProfile.name,
                        ) {
                            Text("Rename")
                        }
                        OutlinedButton(
                            onClick = onActivityProfileDeleted,
                            modifier = Modifier.weight(1f),
                            enabled = activityProfiles.profiles.size > 1,
                        ) {
                            Text("Delete")
                        }
                    }
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = {
                            newProfileName = it.take(ActivityProfile.MAX_NAME_LENGTH)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("New profile name") },
                        supportingText = {
                            Text("Copies the current setup so you can customise it separately.")
                        },
                        singleLine = true,
                    )
                    OutlinedButton(
                        onClick = {
                            onActivityProfileCreated(newProfileName)
                            newProfileName = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newProfileName.isNotBlank() &&
                            activityProfiles.profiles.size <
                            ActivityProfileState.MAX_PROFILES,
                    ) {
                        Text("Save current setup as new profile")
                    }
                }
            }
        }
        item {
            Text("Apply a focus layout", style = MaterialTheme.typography.titleMedium)
            Text(
                "This replaces the layout inside the active profile. Your other profiles are unchanged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ExperiencePreset.entries) { preset ->
                    Card(
                        modifier = Modifier
                            .width(230.dp)
                            .clickable {
                                onSettingsChanged(preset.settings.normalized())
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(preset.label, fontWeight = FontWeight.Bold)
                            Text(
                                preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Apply to current profile",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
        item {
            Text("Open app to", style = MaterialTheme.typography.titleMedium)
            Text(
                "Choose a main screen, or set one of your pinned tools as the start screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AppTab.entries) { tab ->
                    FilterChip(
                        selected = settings.startFeatureId == null &&
                            settings.startTab == tab,
                        onClick = {
                            onSettingsChanged(settings.withStartTab(tab))
                        },
                        label = { Text("${tab.symbol} ${tab.label}") },
                    )
                }
            }
            if (pinnedFeatures.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                pinnedFeatures.forEach { feature ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            feature.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(
                            onClick = {
                                onSettingsChanged(settings.withStartFeature(feature.name))
                            },
                        ) {
                            Text(
                                if (settings.startFeatureId == feature.name) {
                                    "Starts here"
                                } else {
                                    "Start here"
                                },
                            )
                        }
                    }
                }
            }
        }
        item {
            Text("Bottom navigation", style = MaterialTheme.typography.titleMedium)
            Text(
                "Show at least three tabs. More always stays available so settings can be reached.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(AppTab.entries, key = AppTab::name) { tab ->
            val visible = tab in settings.navigationTabs
            val position = settings.navigationTabs.indexOf(tab)
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = visible,
                        onCheckedChange = { checked ->
                            onSettingsChanged(settings.withTabVisibility(tab, checked))
                        },
                        enabled = tab != AppTab.MORE &&
                            (
                                !visible ||
                                    settings.navigationTabs.size >
                                    PersonalizationSettings.MINIMUM_NAVIGATION_TABS
                                ),
                    )
                    Text(
                        "${tab.symbol}  ${tab.label}",
                        modifier = Modifier.weight(1f),
                        fontWeight = if (visible) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    if (visible) {
                        TextButton(
                            onClick = {
                                onSettingsChanged(settings.moveTab(tab, -1))
                            },
                            enabled = position > 0,
                        ) { Text("↑") }
                        TextButton(
                            onClick = {
                                onSettingsChanged(settings.moveTab(tab, 1))
                            },
                            enabled = position in 0 until settings.navigationTabs.lastIndex,
                        ) { Text("↓") }
                    }
                }
            }
        }
        item {
            Text("Pinned quick access", style = MaterialTheme.typography.titleMedium)
            Text(
                "Pin up to ${PersonalizationSettings.MAX_PINNED_FEATURES} helpers. " +
                    "They appear first in More and can also become your start screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = featureSearch,
                onValueChange = { featureSearch = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Find a helper to pin") },
                singleLine = true,
            )
        }
        items(visibleFeatures, key = CompanionFeature::name) { feature ->
            val pinned = feature.name in settings.pinnedFeatureIds
            Card(
                modifier = Modifier.fillMaxWidth().clickable(
                    enabled = pinned ||
                        settings.pinnedFeatureIds.size <
                        PersonalizationSettings.MAX_PINNED_FEATURES,
                ) {
                    onSettingsChanged(
                        settings.withPinnedFeature(feature.name, !pinned),
                    )
                },
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = pinned,
                        onCheckedChange = {
                            onSettingsChanged(
                                settings.withPinnedFeature(feature.name, it),
                            )
                        },
                        enabled = pinned ||
                            settings.pinnedFeatureIds.size <
                            PersonalizationSettings.MAX_PINNED_FEATURES,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(feature.title, fontWeight = FontWeight.SemiBold)
                        Text(
                            feature.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    onSettingsChanged(PersonalizationSettings())
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset to Stars default")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Navigation and quick access update immediately. The chosen start screen is used the next time Rune Companion opens.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
