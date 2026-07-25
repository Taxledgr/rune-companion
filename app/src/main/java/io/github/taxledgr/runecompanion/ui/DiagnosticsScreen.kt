package io.github.taxledgr.runecompanion.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.taxledgr.runecompanion.BuildConfig
import io.github.taxledgr.runecompanion.features.FeaturePreferences
import io.github.taxledgr.runecompanion.features.FeatureState
import io.github.taxledgr.runecompanion.features.FeatureStorageDiagnostics
import io.github.taxledgr.runecompanion.personalization.ActivityProfileState
import io.github.taxledgr.runecompanion.toolkit.ToolkitState
import io.github.taxledgr.runecompanion.ui.theme.LocalRuneLayout
import io.github.taxledgr.runecompanion.ui.theme.RuneCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ScheduledJobStatus(
    val label: String,
    val state: String,
)

private data class DiagnosticsSnapshot(
    val storage: FeatureStorageDiagnostics,
    val jobs: List<ScheduledJobStatus>,
)

@Composable
internal fun DiagnosticsScreen(
    featureState: FeatureState,
    toolkitState: ToolkitState,
    activityProfiles: ActivityProfileState,
) {
    val context = LocalContext.current.applicationContext
    val layout = LocalRuneLayout.current
    var refreshGeneration by remember { mutableIntStateOf(0) }
    val snapshot by produceState<DiagnosticsSnapshot?>(
        initialValue = null,
        key1 = refreshGeneration,
    ) {
        value = withContext(Dispatchers.IO) { loadDiagnostics(context) }
    }
    val latestAccountUpdate = featureState.data.accounts
        .flatMap { it.snapshots }
        .maxOfOrNull { it.capturedAtEpochMillis }
    val toolkitUpdate = toolkitState.trackedPlayer.lastUpdatedEpochMillis
    val latestPublicUpdate = listOfNotNull(latestAccountUpdate, toolkitUpdate).maxOrNull()
    val localRecordCount = with(featureState.data) {
        goals.size + bankedXp.size + geAlerts.size + portfolio.size +
            farmPatches.size + slayerCards.size + sessions.size +
            collectionGoals.size + routines.size + combatAchievements.size +
            loadouts.size + bossReadinessPlans.size + itineraryStops.size +
            gearUpgrades.size + lootLedger.size + counterGoals.size +
            supplyLocker.size + wildernessRisk.size + customTeleports.size
    } + toolkitState.reminders.size + toolkitState.checklist.size
    val currentErrors = listOfNotNull(
        toolkitState.priceError,
        toolkitState.hiscoreError,
        toolkitState.trackedPlayerError,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(layout.screenPadding),
        verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing),
    ) {
        item {
            ScreenHeader(
                eyebrow = "PRIVATE LOCAL STATUS",
                title = "Companion diagnostics",
                subtitle = "Health information stays on this phone and is never uploaded.",
            )
        }
        item {
            StatusCard(
                title = "App",
                rows = listOf(
                    "Version" to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    "Active profile" to activityProfiles.activeProfile.name,
                    "Saved activity profiles" to activityProfiles.profiles.size.toString(),
                    "Local records" to localRecordCount.toString(),
                    "Recent errors" to currentErrors.size.toString(),
                ),
            )
        }
        item {
            val storage = snapshot?.storage
            StatusCard(
                title = "Local storage",
                rows = listOf(
                    "Transactional database" to when (storage?.databaseReady) {
                        true -> "Healthy"
                        false -> "Needs recovery"
                        null -> "Checking…"
                    },
                    "Recovery snapshot" to when (storage?.recoveryReady) {
                        true -> "Ready"
                        false -> "Created after the next change"
                        null -> "Checking…"
                    },
                    "Database size" to storage?.databaseBytes.formatBytes(),
                    "Backup-compatible mirror" to storage?.mirrorBytes.formatBytes(),
                    "Last local save" to storage?.lastSavedAtEpochMillis.ageLabel(),
                ),
            )
        }
        item {
            StatusCard(
                title = "Public data",
                rows = listOf(
                    "Tracked accounts" to featureState.data.accounts.size.toString(),
                    "Latest public update" to latestPublicUpdate.ageLabel(),
                    "Price API" to if (toolkitState.priceError == null) "Ready" else "Last request failed",
                    "Hiscores" to if (
                        toolkitState.hiscoreError == null &&
                        toolkitState.trackedPlayerError == null
                    ) {
                        "Ready"
                    } else {
                        "Last request failed"
                    },
                ),
            )
        }
        item {
            StatusCard(
                title = "Scheduled background work",
                rows = snapshot?.jobs?.map { it.label to it.state }
                    ?: listOf("Workers" to "Checking…"),
            )
        }
        item {
            StatusCard(
                title = "Privacy & security",
                rows = listOf(
                    "Analytics and advertising" to "None",
                    "Jagex login or password" to "Never requested",
                    "OCR and screen capture" to "Disabled",
                    "Accessibility control" to "Not used",
                    "Network traffic" to "HTTPS allowlisted public sources",
                    "Diagnostic upload" to "None — local display only",
                ),
            )
        }
        if (currentErrors.isNotEmpty()) {
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(layout.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
                    ) {
                        Text("Current messages", fontWeight = FontWeight.Bold)
                        currentErrors.forEach { error ->
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { refreshGeneration += 1 },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Refresh local diagnostics")
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    rows: List<Pair<String, String>>,
) {
    val layout = LocalRuneLayout.current
    Card {
        Column(
            modifier = Modifier.padding(layout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(label, modifier = Modifier.weight(1f))
                    Text(
                        value,
                        color = if (value == "Healthy" || value == "Ready") {
                            RuneCyan
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

private fun loadDiagnostics(context: Context): DiagnosticsSnapshot {
    val manager = WorkManager.getInstance(context)
    val jobs = DIAGNOSTIC_WORK_NAMES.map { (name, label) ->
        val states: List<WorkInfo.State> = runCatching {
            manager.getWorkInfosForUniqueWork(name).get().map { info -> info.state }
        }.getOrElse { emptyList() }
        ScheduledJobStatus(
            label = label,
            state = when {
                WorkInfo.State.RUNNING in states -> "Running"
                WorkInfo.State.ENQUEUED in states -> "Scheduled"
                WorkInfo.State.BLOCKED in states -> "Waiting"
                states.isEmpty() || states.all { it == WorkInfo.State.CANCELLED } -> "Off"
                else -> states.first().name.lowercase().replaceFirstChar { it.uppercase() }
            },
        )
    }
    return DiagnosticsSnapshot(
        storage = FeaturePreferences(context).storageDiagnostics(),
        jobs = jobs,
    )
}

private fun Long?.formatBytes(): String {
    if (this == null) return "Checking…"
    if (this < 1_024L) return "$this B"
    if (this < 1_024L * 1_024L) return "${this / 1_024L} KB"
    return String.format("%.1f MB", this / (1_024.0 * 1_024.0))
}

private fun Long?.ageLabel(): String {
    val value = this ?: return "Not yet"
    val seconds = ((System.currentTimeMillis() - value).coerceAtLeast(0) / 1_000)
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60} min ago"
        seconds < 86_400 -> "${seconds / 3_600} hr ago"
        else -> "${seconds / 86_400} days ago"
    }
}

private val DIAGNOSTIC_WORK_NAMES = listOf(
    "feature-account-refresh" to "Multi-account Hiscores",
    "tracked-player-hiscores" to "Tracked-player Hiscores",
    "shooting-star-alerts-periodic" to "Shooting Star alerts",
    "ge-price-alerts" to "GE price alerts",
)
