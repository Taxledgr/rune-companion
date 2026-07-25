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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.taxledgr.runecompanion.personalization.ActivityProfile
import io.github.taxledgr.runecompanion.personalization.ActivityProfileState
import io.github.taxledgr.runecompanion.personalization.AppTab
import io.github.taxledgr.runecompanion.personalization.CustomizationMode
import io.github.taxledgr.runecompanion.personalization.ExperiencePreset
import io.github.taxledgr.runecompanion.personalization.LayoutDensity
import io.github.taxledgr.runecompanion.personalization.PersonalizationSettings
import io.github.taxledgr.runecompanion.ui.theme.LocalRuneLayout

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
    val layout = LocalRuneLayout.current
    val activeProfile = activityProfiles.activeProfile
    var draft by remember(settings, activityProfiles.activeProfileId) {
        mutableStateOf(settings)
    }
    var history by remember(settings, activityProfiles.activeProfileId) {
        mutableStateOf(emptyList<PersonalizationSettings>())
    }
    var featureSearch by rememberSaveable { mutableStateOf("") }
    var newProfileName by rememberSaveable { mutableStateOf("") }
    var activeProfileName by rememberSaveable {
        mutableStateOf(activeProfile.name)
    }
    var pendingProfileId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmProfileDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(activityProfiles.activeProfileId) {
        activeProfileName = activityProfiles.activeProfile.name
        pendingProfileId = null
    }

    fun updateDraft(updated: PersonalizationSettings) {
        val normalized = updated.normalized()
        if (normalized == draft) return
        history = (history + draft).takeLast(MAX_DRAFT_HISTORY)
        draft = normalized
    }

    val dirty = draft != settings
    val pinnedFeatures = draft.pinnedFeatureIds.mapNotNull { id ->
        CompanionFeature.entries.firstOrNull { it.name == id }
    }
    val startLabel = draft.startFeatureId
        ?.let { id -> CompanionFeature.entries.firstOrNull { it.name == id }?.title }
        ?: draft.startTab.label
    val matchingFeatures = CompanionFeature.entries
        .filter { feature ->
            featureSearch.isBlank() ||
                feature.title.contains(featureSearch, ignoreCase = true) ||
                feature.summary.contains(featureSearch, ignoreCase = true) ||
                feature.group.contains(featureSearch, ignoreCase = true)
        }
        .sortedWith(
            compareByDescending<CompanionFeature> {
                it.name in draft.pinnedFeatureIds
            }.thenBy { it.title },
        )
    val quickChoices = if (
        draft.customizationMode == CustomizationMode.BASIC &&
        featureSearch.isBlank()
    ) {
        (
            pinnedFeatures +
                listOf(
                    CompanionFeature.TELEPORTS,
                    CompanionFeature.XP_CHARTS,
                    CompanionFeature.GOALS,
                    CompanionFeature.SLAYER,
                    CompanionFeature.PROGRESS_NAVIGATOR,
                    CompanionFeature.BOSS_READINESS,
                )
            ).distinct().take(10)
    } else {
        matchingFeatures
    }

    pendingProfileId?.let { profileId ->
        val destination = activityProfiles.profiles.firstOrNull { it.id == profileId }
        AlertDialog(
            onDismissRequest = { pendingProfileId = null },
            title = { Text("Discard unsaved changes?") },
            text = {
                Text(
                    "Switching to ${destination?.name ?: "that profile"} will discard " +
                        "the customization changes currently in the preview.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        draft = settings
                        history = emptyList()
                        pendingProfileId = null
                        onActivityProfileSelected(profileId)
                    },
                ) {
                    Text("Discard & switch")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingProfileId = null }) {
                    Text("Keep editing")
                }
            },
        )
    }
    if (confirmProfileDelete) {
        AlertDialog(
            onDismissRequest = { confirmProfileDelete = false },
            title = { Text("Delete ${activeProfile.name}?") },
            text = {
                Text(
                    "This removes only this activity setup. Saved accounts, timers, " +
                        "journals, and gameplay data remain on the phone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmProfileDelete = false
                        onActivityProfileDeleted()
                    },
                ) {
                    Text("Delete profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmProfileDelete = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(layout.screenPadding),
        verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing),
    ) {
        item {
            ScreenHeader(
                eyebrow = "YOUR EXPERIENCE",
                title = "Customize Rune Companion",
                subtitle = "Preview changes first, then save when the setup feels right.",
            )
        }
        item {
            EditActionBar(
                dirty = dirty,
                canUndo = history.isNotEmpty(),
                onSave = {
                    onSettingsChanged(draft)
                    history = emptyList()
                },
                onCancel = {
                    draft = settings
                    history = emptyList()
                },
                onReset = {
                    updateDraft(
                        PersonalizationSettings(
                            customizationMode = draft.customizationMode,
                            appDensity = draft.appDensity,
                            overlayDensity = draft.overlayDensity,
                        ),
                    )
                },
                onUndo = {
                    history.lastOrNull()?.let { previous ->
                        draft = previous
                        history = history.dropLast(1)
                    }
                },
            )
        }
        item {
            Text("Customization level", style = MaterialTheme.typography.titleMedium)
            Text(
                "Basic keeps the important choices together. Advanced reveals every control.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(layout.itemSpacing))
            Row(horizontalArrangement = Arrangement.spacedBy(layout.itemSpacing)) {
                CustomizationMode.entries.forEach { mode ->
                    FilterChip(
                        selected = draft.customizationMode == mode,
                        onClick = { updateDraft(draft.copy(customizationMode = mode)) },
                        label = { Text(mode.label) },
                    )
                }
            }
        }
        item {
            CustomizationPreview(
                settings = draft,
                profile = activeProfile,
                startLabel = startLabel,
            )
        }
        item {
            ProfileSummaryCard(
                profile = activeProfile,
                settings = draft,
                startLabel = startLabel,
            )
        }
        item {
            Text("Switch activity", style = MaterialTheme.typography.titleMedium)
            Text(
                "Each profile remembers its character, navigation, shortcuts, overlay, and Star filters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(layout.itemSpacing))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(layout.itemSpacing)) {
                items(activityProfiles.profiles, key = ActivityProfile::id) { profile ->
                    FilterChip(
                        selected = profile.id == activityProfiles.activeProfileId,
                        onClick = {
                            if (profile.id == activityProfiles.activeProfileId) {
                                Unit
                            } else if (dirty) {
                                pendingProfileId = profile.id
                            } else {
                                onActivityProfileSelected(profile.id)
                            }
                        },
                        label = { Text("${profile.symbol} ${profile.name}") },
                    )
                }
            }
        }
        item {
            Text("Apply a focus layout", style = MaterialTheme.typography.titleMedium)
            Text(
                "A layout updates the current preview only. Save to apply it to this profile.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(layout.itemSpacing))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(layout.itemSpacing)) {
                items(ExperiencePreset.entries) { preset ->
                    Card(
                        modifier = Modifier
                            .width(230.dp)
                            .clickable {
                                updateDraft(
                                    preset.settings.copy(
                                        customizationMode = draft.customizationMode,
                                        appDensity = draft.appDensity,
                                        overlayDensity = draft.overlayDensity,
                                    ),
                                )
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(layout.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
                        ) {
                            Text(preset.label, fontWeight = FontWeight.Bold)
                            Text(
                                preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Preview layout",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
        item {
            DensityEditor(
                title = "App layout size",
                selected = draft.appDensity,
                onSelected = { updateDraft(draft.copy(appDensity = it)) },
            )
            Spacer(Modifier.height(layout.itemSpacing))
            DensityEditor(
                title = "Floating overlay size",
                selected = draft.overlayDensity,
                onSelected = { updateDraft(draft.copy(overlayDensity = it)) },
            )
        }
        item {
            Text("Open app to", style = MaterialTheme.typography.titleMedium)
            Text(
                "Choose a main tab or one of the pinned tools below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(layout.itemSpacing))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(layout.itemSpacing)) {
                items(AppTab.entries) { tab ->
                    FilterChip(
                        selected = draft.startFeatureId == null && draft.startTab == tab,
                        onClick = { updateDraft(draft.withStartTab(tab)) },
                        label = { Text("${tab.symbol} ${tab.label}") },
                    )
                }
            }
            pinnedFeatures.forEach { feature ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(feature.title, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { updateDraft(draft.withStartFeature(feature.name)) },
                    ) {
                        Text(
                            if (draft.startFeatureId == feature.name) {
                                "Starts here"
                            } else {
                                "Start here"
                            },
                        )
                    }
                }
            }
        }
        item {
            Text("Pinned quick access", style = MaterialTheme.typography.titleMedium)
            Text(
                "Choose up to ${PersonalizationSettings.MAX_PINNED_FEATURES} helpers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(layout.itemSpacing))
            OutlinedTextField(
                value = featureSearch,
                onValueChange = { featureSearch = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Find a helper") },
                singleLine = true,
            )
        }
        items(quickChoices, key = CompanionFeature::name) { feature ->
            val pinned = feature.name in draft.pinnedFeatureIds
            Card(
                modifier = Modifier.fillMaxWidth().clickable(
                    enabled = pinned ||
                        draft.pinnedFeatureIds.size <
                        PersonalizationSettings.MAX_PINNED_FEATURES,
                ) {
                    updateDraft(draft.withPinnedFeature(feature.name, !pinned))
                },
            ) {
                Row(
                    modifier = Modifier.padding(layout.cardPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = pinned,
                        onCheckedChange = {
                            updateDraft(draft.withPinnedFeature(feature.name, it))
                        },
                        enabled = pinned ||
                            draft.pinnedFeatureIds.size <
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
        if (draft.customizationMode == CustomizationMode.ADVANCED) {
            item {
                Text("Bottom navigation", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Show at least three tabs. More remains available so settings are reachable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(AppTab.entries, key = AppTab::name) { tab ->
                NavigationTabEditor(
                    tab = tab,
                    settings = draft,
                    onSettingsChanged = ::updateDraft,
                )
            }
            item {
                ProfileManager(
                    activityProfiles = activityProfiles,
                    activeProfileName = activeProfileName,
                    newProfileName = newProfileName,
                    onActiveProfileNameChanged = { activeProfileName = it },
                    onNewProfileNameChanged = { newProfileName = it },
                    onRename = { onActivityProfileRenamed(activeProfileName) },
                    onDelete = { confirmProfileDelete = true },
                    onCreate = {
                        onActivityProfileCreated(newProfileName)
                        newProfileName = ""
                    },
                )
            }
        }
        item {
            EditActionBar(
                dirty = dirty,
                canUndo = history.isNotEmpty(),
                onSave = {
                    onSettingsChanged(draft)
                    history = emptyList()
                },
                onCancel = {
                    draft = settings
                    history = emptyList()
                },
                onReset = {
                    updateDraft(
                        PersonalizationSettings(
                            customizationMode = draft.customizationMode,
                            appDensity = draft.appDensity,
                            overlayDensity = draft.overlayDensity,
                        ),
                    )
                },
                onUndo = {
                    history.lastOrNull()?.let { previous ->
                        draft = previous
                        history = history.dropLast(1)
                    }
                },
            )
        }
    }
}

@Composable
private fun CustomizationPreview(
    settings: PersonalizationSettings,
    profile: ActivityProfile,
    startLabel: String,
) {
    val layout = LocalRuneLayout.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(layout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
        ) {
            Text("LIVE PREVIEW", color = MaterialTheme.colorScheme.secondary)
            Text(startLabel, style = MaterialTheme.typography.headlineSmall)
            Text(
                settings.navigationTabs.joinToString("   ") { "${it.symbol} ${it.label}" },
                style = MaterialTheme.typography.bodySmall,
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(layout.cardPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${profile.overlay.selectedModule.symbol} " +
                            profile.overlay.selectedModule.label,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                    )
                    Text("${profile.overlay.enabledModules.size} sections • bubble")
                }
            }
            Text(
                "${settings.appDensity.label} app • ${settings.overlayDensity.label} overlay",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    profile: ActivityProfile,
    settings: PersonalizationSettings,
    startLabel: String,
) {
    val layout = LocalRuneLayout.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(layout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
        ) {
            Text(
                "${profile.symbol} ${profile.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text("Character: ${profile.selectedAccount ?: "No character selected"}")
            Text("Starts at: $startLabel")
            Text(
                "${settings.navigationTabs.size} tabs • ${settings.pinnedFeatureIds.size} quick tools • " +
                    "${profile.overlay.enabledModules.size} overlay sections",
            )
            Text(
                if (profile.starFilters.hideDangerousWorlds) {
                    "Dangerous worlds hidden • ${profile.starFilters.excludedLocations.size} locations hidden"
                } else {
                    "Dangerous worlds allowed • ${profile.starFilters.excludedLocations.size} locations hidden"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DensityEditor(
    title: String,
    selected: LayoutDensity,
    onSelected: (LayoutDensity) -> Unit,
) {
    Text(title, fontWeight = FontWeight.Bold)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(LocalRuneLayout.current.itemSpacing)) {
        items(LayoutDensity.entries) { density ->
            FilterChip(
                selected = selected == density,
                onClick = { onSelected(density) },
                label = { Text(density.label) },
            )
        }
    }
    Text(
        selected.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NavigationTabEditor(
    tab: AppTab,
    settings: PersonalizationSettings,
    onSettingsChanged: (PersonalizationSettings) -> Unit,
) {
    val visible = tab in settings.navigationTabs
    val position = settings.navigationTabs.indexOf(tab)
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalRuneLayout.current.itemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = visible,
                onCheckedChange = {
                    onSettingsChanged(settings.withTabVisibility(tab, it))
                },
                enabled = tab != AppTab.MORE &&
                    (!visible ||
                        settings.navigationTabs.size >
                        PersonalizationSettings.MINIMUM_NAVIGATION_TABS),
            )
            Text(
                "${tab.symbol}  ${tab.label}",
                modifier = Modifier.weight(1f),
                fontWeight = if (visible) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (visible) {
                TextButton(
                    onClick = { onSettingsChanged(settings.moveTab(tab, -1)) },
                    enabled = position > 0,
                ) { Text("↑") }
                TextButton(
                    onClick = { onSettingsChanged(settings.moveTab(tab, 1)) },
                    enabled = position in 0 until settings.navigationTabs.lastIndex,
                ) { Text("↓") }
            }
        }
    }
}

@Composable
private fun ProfileManager(
    activityProfiles: ActivityProfileState,
    activeProfileName: String,
    newProfileName: String,
    onActiveProfileNameChanged: (String) -> Unit,
    onNewProfileNameChanged: (String) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCreate: () -> Unit,
) {
    val layout = LocalRuneLayout.current
    Card {
        Column(
            modifier = Modifier.padding(layout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
        ) {
            Text("Manage this profile", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = activeProfileName,
                onValueChange = {
                    onActiveProfileNameChanged(it.take(ActivityProfile.MAX_NAME_LENGTH))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Profile name") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(layout.itemSpacing)) {
                OutlinedButton(
                    onClick = onRename,
                    enabled = activeProfileName.isNotBlank() &&
                        activeProfileName.trim() != activityProfiles.activeProfile.name,
                    modifier = Modifier.weight(1f),
                ) { Text("Rename") }
                OutlinedButton(
                    onClick = onDelete,
                    enabled = activityProfiles.profiles.size > 1,
                    modifier = Modifier.weight(1f),
                ) { Text("Delete") }
            }
            OutlinedTextField(
                value = newProfileName,
                onValueChange = {
                    onNewProfileNameChanged(it.take(ActivityProfile.MAX_NAME_LENGTH))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New profile name") },
                supportingText = { Text("Copies the current saved setup.") },
                singleLine = true,
            )
            OutlinedButton(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                enabled = newProfileName.isNotBlank() &&
                    activityProfiles.profiles.size < ActivityProfileState.MAX_PROFILES,
            ) {
                Text("Save current setup as new profile")
            }
        }
    }
}

private const val MAX_DRAFT_HISTORY = 20
