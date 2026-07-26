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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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
import io.github.taxledgr.runecompanion.alerts.StarAlertSettings
import io.github.taxledgr.runecompanion.ui.theme.RuneCyan
import io.github.taxledgr.runecompanion.ui.theme.RuneSurfaceRaised

@Composable
fun StarAlertSettingsCard(
    settings: StarAlertSettings,
    onWorldsChanged: (String) -> Unit,
    onLocationsChanged: (String) -> Unit,
    onTierToggled: (Int) -> Unit,
    onClearTiers: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onQuietHoursEnabledChanged: (Boolean) -> Unit,
    onQuietHoursChanged: (Int, Int) -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    var worldInput by rememberSaveable(settings.worlds) {
        mutableStateOf(settings.worlds.sorted().joinToString(", "))
    }
    var locationInput by rememberSaveable(settings.locations) {
        mutableStateOf(settings.locations.joinToString(", "))
    }
    var quietStartInput by rememberSaveable(settings.quietStartHour) {
        mutableStateOf(settings.quietStartHour.toString())
    }
    var quietEndInput by rememberSaveable(settings.quietEndHour) {
        mutableStateOf(settings.quietEndHour.toString())
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = RuneSurfaceRaised),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Saved star alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (settings.enabled) "Watching for matching reports" else "Notifications are off",
                        color = if (settings.enabled) {
                            RuneCyan
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = onEnabledChanged,
                )
            }

            OutlinedTextField(
                value = worldInput,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == ',' || it.isWhitespace() }) {
                        worldInput = input
                        onWorldsChanged(input)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Preferred worlds") },
                supportingText = {
                    Text(
                        if (settings.worlds.isEmpty()) {
                            "Any world • example: 301, 302, 330"
                        } else {
                            "${settings.worlds.size} worlds saved"
                        },
                    )
                },
                singleLine = true,
            )

            OutlinedTextField(
                value = locationInput,
                onValueChange = { input ->
                    locationInput = input
                    onLocationsChanged(input)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Favourite locations or regions") },
                supportingText = {
                    Text(
                        if (settings.locations.isEmpty()) {
                            "Any location • example: Falador, Wilderness"
                        } else {
                            "${settings.locations.size} location filters saved"
                        },
                    )
                },
                singleLine = true,
            )

            Text(
                text = "Preferred tiers",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = settings.tiers.isEmpty(),
                    onClick = onClearTiers,
                    label = { Text("Any tier") },
                )
                (9 downTo 1).forEach { tier ->
                    FilterChip(
                        selected = tier in settings.tiers,
                        onClick = { onTierToggled(tier) },
                        label = { Text("T$tier") },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Quiet hours", fontWeight = FontWeight.Bold)
                    Text(
                        "Suppress matching-star alerts during this local-time window.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.quietHoursEnabled,
                    onCheckedChange = onQuietHoursEnabledChanged,
                )
            }
            if (settings.quietHoursEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = quietStartInput,
                        onValueChange = { input ->
                            if (input.length <= 2 && input.all(Char::isDigit)) {
                                quietStartInput = input
                                onQuietHoursChanged(
                                    input.toIntOrNull() ?: settings.quietStartHour,
                                    quietEndInput.toIntOrNull() ?: settings.quietEndHour,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("From (0–23)") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = quietEndInput,
                        onValueChange = { input ->
                            if (input.length <= 2 && input.all(Char::isDigit)) {
                                quietEndInput = input
                                onQuietHoursChanged(
                                    quietStartInput.toIntOrNull() ?: settings.quietStartHour,
                                    input.toIntOrNull() ?: settings.quietEndHour,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Until (0–23)") },
                        singleLine = true,
                    )
                }
            }
            OutlinedButton(onClick = onOpenNotificationSettings) {
                Text("Notification sounds & settings")
            }

            Text(
                text = "Checks every minute while the app or overlay is active. Android schedules background checks at 15-minute intervals or later depending on battery conditions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
