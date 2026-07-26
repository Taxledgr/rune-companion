package io.github.taxledgr.runecompanion.ui

import android.net.Uri
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val history = remember { SearchHistoryPreferences(context) }
    var recentQueries by remember { mutableStateOf(history.load()) }
    var query by rememberSaveable { mutableStateOf("") }
    val entries = remember(featureData, toolkitState) {
        globalSearchEntries(featureData, toolkitState)
    }
    val results = if (query.isBlank()) {
        entries.filter { it.featureId in QUICK_SEARCH_IDS }.take(12)
    } else {
        entries.mapNotNull { entry ->
            searchScore(query, entry).takeIf { it > 0 }?.let { score -> entry to score }
        }.sortedWith(
            compareByDescending<Pair<GlobalSearchEntry, Int>> { it.second }
                .thenBy { it.first.title },
        ).map { it.first }.take(MAX_RESULTS)
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
        if (query.isBlank() && recentQueries.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(layout.itemSpacing)) {
                    Text("Recent searches", fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(layout.itemSpacing),
                    ) {
                        recentQueries.forEach { recent ->
                            FilterChip(
                                selected = false,
                                onClick = { query = recent },
                                label = { Text(recent) },
                            )
                        }
                        TextButton(
                            onClick = {
                                history.clear()
                                recentQueries = emptyList()
                            },
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
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
                    query.trim().takeIf(String::isNotBlank)?.let {
                        history.record(it)
                        recentQueries = history.load()
                    }
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
                        history.record(query.trim())
                        recentQueries = history.load()
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

internal fun searchScore(query: String, entry: GlobalSearchEntry): Int {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isBlank()) return 0
    val title = entry.title.lowercase()
    val searchable = "$title ${entry.summary.lowercase()} ${entry.keywords.lowercase()}"
    val terms = normalizedQuery.split(Regex("\\s+"))
    var score = when {
        title == normalizedQuery -> 240
        title.startsWith(normalizedQuery) -> 180
        title.contains(normalizedQuery) -> 140
        searchable.contains(normalizedQuery) -> 100
        else -> 0
    }
    val words = searchable.split(Regex("[^a-z0-9]+")).filter(String::isNotBlank)
    terms.forEach { term ->
        when {
            title.split(Regex("\\s+")).any { it.startsWith(term) } -> score += 35
            words.any { it.startsWith(term) } -> score += 24
            words.any { word -> typoMatch(term, word) } -> score += 12
            else -> return 0
        }
    }
    return score.coerceAtLeast(1)
}

private fun typoMatch(term: String, word: String): Boolean {
    if (term.length < 4 || word.length < 4) return false
    val allowed = if (term.length >= 8) 2 else 1
    if (kotlin.math.abs(term.length - word.length) > allowed) return false
    var previous = IntArray(word.length + 1) { it }
    term.forEachIndexed { row, character ->
        val current = IntArray(word.length + 1)
        current[0] = row + 1
        word.forEachIndexed { column, other ->
            current[column + 1] = minOf(
                current[column] + 1,
                previous[column + 1] + 1,
                previous[column] + if (character == other) 0 else 1,
            )
        }
        previous = current
    }
    return previous.last() <= allowed
}

private class SearchHistoryPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): List<String> = preferences.getString(KEY_RECENT, "")
        .orEmpty()
        .split(SEPARATOR)
        .filter(String::isNotBlank)
        .take(MAX_RECENT_SEARCHES)

    fun record(query: String) {
        val clean = query.trim().take(MAX_QUERY_LENGTH)
        if (clean.isBlank()) return
        val updated = (listOf(clean) + load().filterNot { it.equals(clean, true) })
            .take(MAX_RECENT_SEARCHES)
        preferences.edit().putString(KEY_RECENT, updated.joinToString(SEPARATOR)).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_RECENT).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "rune_companion_search_history"
        const val KEY_RECENT = "recent_queries"
        const val SEPARATOR = "\u001F"
        const val MAX_RECENT_SEARCHES = 6
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
