package io.github.taxledgr.runecompanion.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import io.github.taxledgr.runecompanion.features.FeatureData
import io.github.taxledgr.runecompanion.personalization.AppTab
import io.github.taxledgr.runecompanion.toolkit.ToolkitState
import io.github.taxledgr.runecompanion.ui.theme.LocalRuneLayout

internal data class GlobalSearchEntry(
    val title: String,
    val summary: String,
    val keywords: String,
    val tab: AppTab,
    val featureId: String? = null,
)

@Composable
internal fun GlobalSearchScreen(
    featureData: FeatureData,
    toolkitState: ToolkitState,
    onClose: () -> Unit,
    onNavigate: (AppTab, String?) -> Unit,
    onOpenWiki: (String) -> Unit,
) {
    val layout = LocalRuneLayout.current
    var query by rememberSaveable { mutableStateOf("") }
    val entries = globalSearchEntries(featureData, toolkitState)
    val results = if (query.isBlank()) {
        entries.filter { it.featureId in QUICK_SEARCH_IDS }.take(12)
    } else {
        val terms = query.trim().split(Regex("\\s+"))
        entries.filter { entry ->
            val searchable = "${entry.title} ${entry.summary} ${entry.keywords}"
            terms.all { searchable.contains(it, ignoreCase = true) }
        }.take(MAX_RESULTS)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(layout.screenPadding),
        verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Search Rune Companion",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        "Tools, settings, saved entries, teleports, Slayer cards, and the Wiki.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onClose) { Text("Close") }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(MAX_QUERY_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What do you need?") },
                placeholder = { Text("Example: teleport, Slayer, backup, Vorkath") },
                singleLine = true,
            )
        }
        if (results.isEmpty() && query.isNotBlank()) {
            item {
                Text(
                    "No local match. Search the OSRS Wiki below.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(results, key = { "${it.tab}:${it.featureId}:${it.title}" }) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    onNavigate(entry.tab, entry.featureId)
                },
            ) {
                Column(
                    modifier = Modifier.padding(layout.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(layout.itemSpacing),
                ) {
                    Text(entry.title, fontWeight = FontWeight.Bold)
                    Text(
                        entry.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Open ${entry.tab.label}",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (query.trim().length >= MIN_WIKI_QUERY_LENGTH) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onOpenWiki(
                            "https://oldschool.runescape.wiki/w/Special:Search?search=" +
                                Uri.encode(query.trim()),
                        )
                    },
                ) {
                    Column(modifier = Modifier.padding(layout.cardPadding)) {
                        Text("Search the OSRS Wiki", fontWeight = FontWeight.Bold)
                        Text(
                            query.trim(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

internal fun globalSearchEntries(
    data: FeatureData,
    toolkit: ToolkitState,
): List<GlobalSearchEntry> = buildList {
    add(
        GlobalSearchEntry(
            "Customize experience",
            "Profiles, startup, quick access, navigation, density, and overlay preview.",
            "settings profile basic advanced layout",
            AppTab.MORE,
            NAVIGATION_CUSTOMIZE,
        ),
    )
    add(
        GlobalSearchEntry(
            "App settings & tracked player",
            "Automatic public Hiscores refresh and player progress.",
            "settings username hiscores account refresh",
            AppTab.MORE,
            NAVIGATION_SETTINGS,
        ),
    )
    AppTab.entries.forEach { tab ->
        add(
            GlobalSearchEntry(
                tab.label,
                "Open the ${tab.label} main section.",
                "tab navigation ${tab.name}",
                tab,
            ),
        )
    }
    CompanionFeature.entries.forEach { feature ->
        add(
            GlobalSearchEntry(
                feature.title,
                feature.summary,
                "${feature.group} ${feature.name}",
                AppTab.MORE,
                feature.name,
            ),
        )
    }
    data.customTeleports.forEach { teleport ->
        add(
            GlobalSearchEntry(
                teleport.name,
                "Custom teleport to ${teleport.destination} • ${teleport.region}",
                "teleport route ${teleport.region}",
                AppTab.MORE,
                CompanionFeature.TELEPORTS.name,
            ),
        )
    }
    data.slayerCards.forEach { card ->
        add(
            GlobalSearchEntry(
                card.monster,
                "Saved Slayer card • ${card.locations}",
                "slayer weakness ${card.weakness} ${card.requiredItems}",
                AppTab.MORE,
                CompanionFeature.SLAYER.name,
            ),
        )
    }
    data.loadouts.forEach { loadout ->
        add(
            GlobalSearchEntry(
                loadout.name,
                "Saved loadout",
                "gear inventory equipment ${loadout.notes}",
                AppTab.MORE,
                CompanionFeature.LOADOUTS.name,
            ),
        )
    }
    data.accounts.forEach { account ->
        add(
            GlobalSearchEntry(
                account.username,
                "Saved public-Hiscores profile",
                "character account xp stats",
                AppTab.MORE,
                CompanionFeature.ACCOUNTS.name,
            ),
        )
    }
    toolkit.reminders.forEach { reminder ->
        add(
            GlobalSearchEntry(
                reminder.title,
                "${reminder.category.label} reminder",
                "timer",
                AppTab.TIMERS,
            ),
        )
    }
    toolkit.slayerTask?.let { task ->
        add(
            GlobalSearchEntry(
                task.monster,
                "Active Slayer task • ${task.remaining} remaining",
                "journal counter",
                AppTab.JOURNAL,
            ),
        )
    }
}

internal const val NAVIGATION_CUSTOMIZE = "__CUSTOMIZE"
internal const val NAVIGATION_SETTINGS = "__SETTINGS"
private const val MIN_WIKI_QUERY_LENGTH = 2
private const val MAX_QUERY_LENGTH = 120
private const val MAX_RESULTS = 40
private val QUICK_SEARCH_IDS = setOf(
    NAVIGATION_CUSTOMIZE,
    NAVIGATION_SETTINGS,
    CompanionFeature.DIAGNOSTICS.name,
    CompanionFeature.TELEPORTS.name,
    CompanionFeature.XP_CHARTS.name,
    CompanionFeature.SLAYER.name,
    CompanionFeature.PROGRESS_NAVIGATOR.name,
    CompanionFeature.BACKUP.name,
)
