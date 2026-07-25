package io.github.taxledgr.runecompanion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.taxledgr.runecompanion.toolkit.SkillScore
import io.github.taxledgr.runecompanion.toolkit.ToolkitState
import io.github.taxledgr.runecompanion.toolkit.skill
import io.github.taxledgr.runecompanion.toolkit.xpGainedSince
import io.github.taxledgr.runecompanion.ui.theme.RuneCyan
import io.github.taxledgr.runecompanion.ui.theme.RuneGold
import io.github.taxledgr.runecompanion.util.reportAge
import java.text.NumberFormat
import java.time.Instant

@Composable
fun SettingsScreen(
    state: ToolkitState,
    onCustomizeExperience: () -> Unit,
    onSaveTrackedPlayer: (String) -> Unit,
    onAutoRefreshChanged: (Boolean) -> Unit,
    onRefreshTrackedPlayer: () -> Unit,
    onResetTrackedPlayerBaseline: () -> Unit,
    onClearTrackedPlayer: () -> Unit,
) {
    val profile = state.trackedPlayer
    var username by rememberSaveable(profile.username) {
        mutableStateOf(profile.username)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                eyebrow = "APP & PROFILE",
                title = "Settings",
                subtitle = "Personalize Rune Companion and track public OSRS stats without connecting a Jagex account.",
            )
        }
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Your Rune Companion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Choose a focus, start screen, navigation tabs, and pinned quick-access tools.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onCustomizeExperience) {
                        Text("Customize experience")
                    }
                }
            }
        }
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Tracked player",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.take(MAX_PLAYER_NAME_LENGTH) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("OSRS display name") },
                        supportingText = {
                            Text("Public hiscores only • no password or login")
                        },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onSaveTrackedPlayer(username) },
                            enabled = username.isNotBlank() && !state.trackedPlayerLoading,
                        ) {
                            Text(
                                if (profile.username.isBlank()) {
                                    "Save & track"
                                } else {
                                    "Save & refresh"
                                },
                            )
                        }
                        if (profile.username.isNotBlank()) {
                            OutlinedButton(onClick = onClearTrackedPlayer) {
                                Text("Clear")
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic updates", fontWeight = FontWeight.Bold)
                            Text(
                                "Every 10 minutes while open; Android schedules background refreshes every 15 minutes or later.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = profile.autoRefreshEnabled,
                            onCheckedChange = onAutoRefreshChanged,
                            enabled = profile.username.isNotBlank(),
                        )
                    }
                }
            }
        }
        state.trackedPlayerError?.let { error ->
            item {
                Card {
                    Text(
                        error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        profile.latest?.let { latest ->
            item {
                Card {
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
                                    latest.player,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = RuneGold,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    profile.lastUpdatedEpochMillis?.let {
                                        "Updated ${reportAge(Instant.ofEpochMilli(it))}"
                                    } ?: "Waiting for first update",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedButton(
                                onClick = onRefreshTrackedPlayer,
                                enabled = !state.trackedPlayerLoading,
                            ) {
                                Text(if (state.trackedPlayerLoading) "Refreshing…" else "Refresh")
                            }
                        }

                        profile.baseline?.let { baseline ->
                            val totalGain = latest.xpGainedSince(baseline, "Overall")
                            Text(
                                "+${totalGain.formatted()} total XP since baseline",
                                color = RuneCyan,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        HorizontalDivider()
                        latest.skills.forEach { skill ->
                            SkillRow(
                                skill = skill,
                                xpGained = profile.baseline?.let { baseline ->
                                    latest.xpGainedSince(baseline, skill.name)
                                } ?: 0,
                                baselineLevel = profile.baseline?.skill(skill.name)?.level,
                            )
                        }

                        OutlinedButton(
                            onClick = onResetTrackedPlayerBaseline,
                            enabled = profile.baseline != null,
                        ) {
                            Text("Reset progress baseline")
                        }
                    }
                }
            }
        } ?: if (profile.username.isNotBlank()) {
            item {
                Card {
                    Text(
                        if (state.trackedPlayerLoading) {
                            "Loading ${profile.username} from the official OSRS hiscores…"
                        } else {
                            "Save or refresh the profile to load its first snapshot."
                        },
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Unit
        }
        item {
            Text(
                "Background timing is controlled by Android battery and network conditions, so a 15-minute job can run later. Hiscores may also lag behind recent in-game XP.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SkillRow(
    skill: SkillScore,
    xpGained: Long,
    baselineLevel: Int?,
) {
    if (skill.level < 0 || skill.xp < 0) return
    val levelGain = baselineLevel?.let { (skill.level - it).coerceAtLeast(0) } ?: 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            skill.name,
            modifier = Modifier.weight(1f),
            fontWeight = if (skill.name == "Overall") FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            if (skill.name == "Overall") "Total ${skill.level}" else "Lvl ${skill.level}",
            style = MaterialTheme.typography.bodySmall,
        )
        if (xpGained > 0 || levelGain > 0) {
            Text(
                buildString {
                    if (levelGain > 0) append("+$levelGain lvl ")
                    if (xpGained > 0) append("+${xpGained.formatted()} XP")
                }.trim(),
                style = MaterialTheme.typography.bodySmall,
                color = RuneCyan,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun Long.formatted(): String = NumberFormat.getIntegerInstance().format(this)

private const val MAX_PLAYER_NAME_LENGTH = 12
