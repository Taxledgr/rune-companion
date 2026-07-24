package io.github.taxledgr.runecompanion.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.taxledgr.runecompanion.features.AccountProfile
import io.github.taxledgr.runecompanion.features.FeatureState
import io.github.taxledgr.runecompanion.features.FeatureViewModel
import io.github.taxledgr.runecompanion.features.Spellbook
import io.github.taxledgr.runecompanion.features.TeleportCatalog
import io.github.taxledgr.runecompanion.features.TeleportKind
import io.github.taxledgr.runecompanion.features.dropChancePercent
import io.github.taxledgr.runecompanion.features.magicLevel
import io.github.taxledgr.runecompanion.features.profitAfterTax
import io.github.taxledgr.runecompanion.features.remainingActions
import io.github.taxledgr.runecompanion.features.remainingXp
import io.github.taxledgr.runecompanion.features.skillOrEmpty
import io.github.taxledgr.runecompanion.features.tax
import io.github.taxledgr.runecompanion.features.totalXp
import io.github.taxledgr.runecompanion.features.xpGained
import io.github.taxledgr.runecompanion.toolkit.PriceSearchItem
import io.github.taxledgr.runecompanion.toolkit.ToolkitState
import io.github.taxledgr.runecompanion.toolkit.formatDuration
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max

private enum class CompanionFeature(
    val title: String,
    val summary: String,
    val group: String,
) {
    ACCOUNTS("Multi-account profiles", "Track automatic public hiscores for several characters.", "Progress"),
    XP_CHARTS("Daily & weekly XP", "Compare XP snapshots over 24 hours and 7 days.", "Progress"),
    GOALS("Skill goal planner", "Levels, remaining XP, and actions to target.", "Progress"),
    BANKED_XP("Banked-XP planner", "Turn item quantities into banked training XP.", "Progress"),
    GE_ALERTS("GE price alerts", "Watch Wiki live prices against your targets.", "Economy"),
    PORTFOLIO("GE portfolio & tax", "Track positions, 1% GE tax, and net profit.", "Economy"),
    FARMING("Farming dashboard", "Patch, crop, readiness, and notes.", "Activities"),
    SLAYER("Slayer knowledge cards", "Personal weaknesses, locations, gear, and notes.", "Activities"),
    SESSIONS("Boss & raid sessions", "Kills, time, loot, supplies, and net value.", "Activities"),
    COLLECTION("Collection & dry streaks", "Attempts and cumulative drop chance.", "Activities"),
    CLUES("Clue companion", "Search the OSRS Wiki directly for any clue step.", "Planners"),
    TELEPORTS("Teleport route planner", "Player-specific spells, tabs, items, jewellery, and POH.", "Planners"),
    DPS("Manual DPS comparison", "Compare two loadouts without reading the game client.", "Planners"),
    CONSUMABLES("Consumable planner", "Estimate food, doses, runes, and trip cost.", "Planners"),
    COMBAT_ACHIEVEMENTS("Combat Achievement planner", "Build and tick off a personal task list.", "Activities"),
    MINIGAMES("Minigame calculators", "Games needed from target and points per game.", "Planners"),
    ROUTINES("Daily & weekly routines", "Completion streaks for repeatable activities.", "Activities"),
    WIDGET("Android home widget", "One-tap launcher and tracked-player summary.", "Device"),
    BACKUP("Encrypted backup", "AES-GCM export/import protected by a passphrase.", "Device"),
    LOADOUTS("Shareable loadouts", "Save inventory, equipment, and setup notes.", "Activities"),
}

@Composable
fun FeatureHubScreen(
    state: FeatureState,
    viewModel: FeatureViewModel,
    toolkitState: ToolkitState,
    onSaveTrackedPlayer: (String) -> Unit,
    onAutoRefreshChanged: (Boolean) -> Unit,
    onRefreshTrackedPlayer: () -> Unit,
    onResetTrackedPlayerBaseline: () -> Unit,
    onClearTrackedPlayer: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var selected by remember { mutableStateOf<CompanionFeature?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }

    when {
        settingsOpen -> FeaturePage(title = "App settings", onBack = { settingsOpen = false }) {
            SettingsScreen(
                state = toolkitState,
                onSaveTrackedPlayer = onSaveTrackedPlayer,
                onAutoRefreshChanged = onAutoRefreshChanged,
                onRefreshTrackedPlayer = onRefreshTrackedPlayer,
                onResetTrackedPlayerBaseline = onResetTrackedPlayerBaseline,
                onClearTrackedPlayer = onClearTrackedPlayer,
            )
        }
        selected != null -> FeaturePage(
            title = selected!!.title,
            onBack = { selected = null },
        ) {
            FeatureContent(
                feature = selected!!,
                state = state,
                viewModel = viewModel,
                onOpenUrl = onOpenUrl,
            )
        }
        else -> FeatureHub(
            state = state,
            onSettings = { settingsOpen = true },
            onSelect = { selected = it },
        )
    }
}

@Composable
private fun FeatureHub(
    state: FeatureState,
    onSettings: () -> Unit,
    onSelect: (CompanionFeature) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(10.dp))
            Text("Rune Companion", style = MaterialTheme.typography.headlineMedium)
            Text(
                "20 gameplay helpers. All game-state inputs are manual or use approved public data.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSettings),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("App settings", fontWeight = FontWeight.Bold)
                    Text("Tracked player, background hiscores, and preferences")
                }
            }
        }
        CompanionFeature.entries.groupBy { it.group }.forEach { (group, features) ->
            item {
                Text(
                    group,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(features) { feature ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(feature) },
                ) {
                    Column(Modifier.padding(14.dp)) {
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
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun FeaturePage(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack) { Text("‹ Back") }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        content()
    }
}

@Composable
private fun FeatureContent(
    feature: CompanionFeature,
    state: FeatureState,
    viewModel: FeatureViewModel,
    onOpenUrl: (String) -> Unit,
) {
    when (feature) {
        CompanionFeature.ACCOUNTS,
        CompanionFeature.XP_CHARTS,
        CompanionFeature.GOALS,
        CompanionFeature.BANKED_XP,
        -> ProgressFeature(feature, state, viewModel)

        CompanionFeature.GE_ALERTS,
        CompanionFeature.PORTFOLIO,
        -> EconomyFeature(feature, state, viewModel)

        CompanionFeature.FARMING,
        CompanionFeature.SLAYER,
        CompanionFeature.SESSIONS,
        CompanionFeature.COLLECTION,
        CompanionFeature.COMBAT_ACHIEVEMENTS,
        CompanionFeature.ROUTINES,
        CompanionFeature.LOADOUTS,
        -> ActivityFeature(feature, state, viewModel)

        CompanionFeature.CLUES,
        CompanionFeature.TELEPORTS,
        CompanionFeature.DPS,
        CompanionFeature.CONSUMABLES,
        CompanionFeature.MINIGAMES,
        -> PlannerFeature(feature, state, viewModel, onOpenUrl)

        CompanionFeature.WIDGET -> WidgetFeature()
        CompanionFeature.BACKUP -> BackupFeature(viewModel)
    }
}

@Composable
private fun ProgressFeature(
    feature: CompanionFeature,
    state: FeatureState,
    viewModel: FeatureViewModel,
) {
    val data = state.data
    val selectedProfile = data.accounts.firstOrNull {
        it.username.equals(data.selectedAccount, true)
    }
    when (feature) {
        CompanionFeature.ACCOUNTS -> AccountsScreen(state, viewModel)
        CompanionFeature.XP_CHARTS -> XpChartsScreen(data.accounts, selectedProfile, viewModel)
        CompanionFeature.GOALS -> GoalsScreen(selectedProfile, state, viewModel)
        CompanionFeature.BANKED_XP -> BankedXpScreen(state, viewModel)
        else -> Unit
    }
}

@Composable
private fun AccountsScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var username by remember { mutableStateOf("") }
    FeatureList {
        item {
            InfoCard(
                "Public hiscores only",
                "Rune Companion refreshes saved accounts every 10 minutes while open. " +
                    "Hiscores expose levels and XP, not inventory, quests, or current activity.",
            )
            Field(username, { username = it }, "OSRS display name")
            Button(
                onClick = {
                    viewModel.addAccount(username)
                    username = ""
                },
                enabled = username.isNotBlank(),
            ) { Text("Add & refresh") }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
        items(state.data.accounts) { profile ->
            val latest = profile.latest?.summary
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(profile.username, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (latest == null) "Waiting for first snapshot"
                        else "Total ${latest.skillOrEmpty("Overall").level} • " +
                            "${number(latest.skillOrEmpty("Overall").xp)} XP • " +
                            "${profile.snapshots.size} snapshots",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.selectAccount(profile.username) }) {
                            Text(
                                if (profile.username.equals(state.data.selectedAccount, true)) {
                                    "Selected"
                                } else {
                                    "Select"
                                },
                            )
                        }
                        OutlinedButton(onClick = { viewModel.refreshAccount(profile.username) }) {
                            Text("Refresh")
                        }
                        TextButton(onClick = { viewModel.removeAccount(profile.username) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XpChartsScreen(
    profiles: List<AccountProfile>,
    selected: AccountProfile?,
    viewModel: FeatureViewModel,
) {
    var skill by remember { mutableStateOf("Overall") }
    val now = System.currentTimeMillis()
    val day = selected?.xpGained(skill, now - 24 * 60 * 60_000L) ?: 0
    val week = selected?.xpGained(skill, now - 7 * 24 * 60 * 60_000L) ?: 0
    val scale = max(1L, max(day, week))
    FeatureList {
        item {
            if (profiles.isEmpty()) {
                InfoCard("Add an account first", "The chart is built from automatic hiscore snapshots.")
                return@item
            }
            AccountChooser(profiles, selected, viewModel)
            Field(skill, { skill = it }, "Skill (for example Mining)")
            Spacer(Modifier.height(8.dp))
            Text("24 hours  ${number(day)} XP")
            LinearProgressIndicator(
                progress = { day.toFloat() / scale },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text("7 days  ${number(week)} XP")
            LinearProgressIndicator(
                progress = { week.toFloat() / scale },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "${selected?.snapshots?.size ?: 0} saved snapshots. The chart fills as " +
                    "Rune Companion collects public hiscores over time.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GoalsScreen(
    profile: AccountProfile?,
    state: FeatureState,
    viewModel: FeatureViewModel,
) {
    var skill by remember { mutableStateOf("Mining") }
    var target by remember { mutableStateOf("99") }
    var xpAction by remember { mutableStateOf("35") }
    FeatureList {
        item {
            Text("Using ${profile?.username ?: "no selected profile"}")
            Field(skill, { skill = it }, "Skill")
            Field(target, { target = it }, "Target level")
            Field(xpAction, { xpAction = it }, "XP per action")
            Button(onClick = {
                viewModel.addGoal(
                    skill,
                    target.toIntOrNull() ?: 0,
                    xpAction.toDoubleOrNull() ?: 0.0,
                )
            }) { Text("Add goal") }
        }
        items(state.data.goals) { goal ->
            RecordCard(
                goal.skill + " → " + goal.targetLevel,
                "${number(goal.remainingXp(profile))} XP • " +
                    "${number(goal.remainingActions(profile))} actions",
                onDelete = { viewModel.deleteGoal(goal.id) },
            )
        }
    }
}

@Composable
private fun BankedXpScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var item by remember { mutableStateOf("") }
    var skill by remember { mutableStateOf("Herblore") }
    var quantity by remember { mutableStateOf("") }
    var xpEach by remember { mutableStateOf("") }
    FeatureList {
        item {
            Field(item, { item = it }, "Banked item / action")
            Field(skill, { skill = it }, "Skill")
            Field(quantity, { quantity = it }, "Quantity")
            Field(xpEach, { xpEach = it }, "XP each")
            Button(onClick = {
                viewModel.addBankedXp(
                    item, skill, quantity.toIntOrNull() ?: 0, xpEach.toDoubleOrNull() ?: 0.0,
                )
            }) { Text("Add banked XP") }
            val total = state.data.bankedXp.sumOf { it.totalXp() }
            Text("Total banked: ${number(total)} XP", fontWeight = FontWeight.Bold)
        }
        items(state.data.bankedXp) { entry ->
            RecordCard(
                "${entry.item} • ${entry.skill}",
                "${number(entry.quantity.toLong())} × ${entry.xpEach} = ${number(entry.totalXp())} XP",
                onDelete = { viewModel.deleteBankedXp(entry.id) },
            )
        }
    }
}

@Composable
private fun EconomyFeature(
    feature: CompanionFeature,
    state: FeatureState,
    viewModel: FeatureViewModel,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<PriceSearchItem?>(null) }
    var valueA by remember { mutableStateOf("") }
    var valueB by remember { mutableStateOf("") }
    var above by remember { mutableStateOf(true) }
    FeatureList {
        item {
            InfoCard(
                "OSRS Wiki live prices",
                "Prices come from the public Wiki real-time API and can differ from the exact price you trade at.",
            )
            Field(query, {
                query = it
                selected = null
                viewModel.searchPrices(it)
            }, "Search item")
            state.priceSearchResults.forEach { item ->
                TextButton(
                    onClick = {
                        selected = item
                        query = item.name
                    },
                ) { Text("${item.name}${if (item.members) " (members)" else ""}") }
            }
            selected?.let { Text("Selected: ${it.name}", fontWeight = FontWeight.Bold) }
            if (feature == CompanionFeature.GE_ALERTS) {
                Field(valueA, { valueA = it }, "Target price")
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Alert when above")
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = above, onCheckedChange = { above = it })
                }
                Button(
                    onClick = {
                        selected?.let {
                            viewModel.addGeAlert(it, valueA.toLongOrNull() ?: 0, above)
                        }
                    },
                    enabled = selected != null,
                ) { Text("Add price alert") }
                OutlinedButton(onClick = viewModel::refreshPrices) { Text("Refresh prices") }
            } else {
                Field(valueA, { valueA = it }, "Quantity")
                Field(valueB, { valueB = it }, "Buy price each")
                Button(
                    onClick = {
                        selected?.let {
                            viewModel.addPortfolio(
                                it,
                                valueA.toIntOrNull() ?: 0,
                                valueB.toLongOrNull() ?: 0,
                            )
                        }
                    },
                    enabled = selected != null,
                ) { Text("Add position") }
                OutlinedButton(onClick = viewModel::refreshPrices) { Text("Refresh prices") }
            }
        }
        if (feature == CompanionFeature.GE_ALERTS) {
            items(state.data.geAlerts) { alert ->
                val hit = alert.latestPrice?.let {
                    if (alert.alertWhenAbove) it >= alert.targetPrice else it <= alert.targetPrice
                } ?: false
                RecordCard(
                    alert.itemName,
                    "Latest ${number(alert.latestPrice)} • target " +
                        "${if (alert.alertWhenAbove) "≥" else "≤"} ${number(alert.targetPrice)}" +
                        if (hit) " • TARGET HIT" else "",
                    onDelete = { viewModel.deleteGeAlert(alert.id) },
                )
            }
        } else {
            item {
                val profit = state.data.portfolio.sumOf { it.profitAfterTax() }
                Text("Portfolio net: ${number(profit)} gp", fontWeight = FontWeight.Bold)
            }
            items(state.data.portfolio) { entry ->
                RecordCard(
                    "${entry.quantity} × ${entry.itemName}",
                    "Buy ${number(entry.buyPrice)} • latest ${number(entry.latestPrice)} • " +
                        "tax ${number(entry.tax())} • net ${number(entry.profitAfterTax())}",
                    onDelete = { viewModel.deletePortfolio(entry.id) },
                )
            }
        }
    }
}

@Composable
private fun ActivityFeature(
    feature: CompanionFeature,
    state: FeatureState,
    viewModel: FeatureViewModel,
) {
    when (feature) {
        CompanionFeature.FARMING -> FarmingScreen(state, viewModel)
        CompanionFeature.SLAYER -> SlayerCardsScreen(state, viewModel)
        CompanionFeature.SESSIONS -> SessionsScreen(state, viewModel)
        CompanionFeature.COLLECTION -> CollectionScreen(state, viewModel)
        CompanionFeature.COMBAT_ACHIEVEMENTS -> CombatAchievementScreen(state, viewModel)
        CompanionFeature.ROUTINES -> RoutinesScreen(state, viewModel)
        CompanionFeature.LOADOUTS -> LoadoutsScreen(state, viewModel)
        else -> Unit
    }
}

@Composable
private fun FarmingScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var patch by remember { mutableStateOf("") }
    var crop by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val now = System.currentTimeMillis()
    FeatureList {
        item {
            Field(patch, { patch = it }, "Patch (for example Catherby herb)")
            Field(crop, { crop = it }, "Crop")
            Field(minutes, { minutes = it }, "Ready in minutes")
            Field(note, { note = it }, "Compost / protection / note")
            Button(onClick = {
                viewModel.addFarmPatch(
                    patch, crop, minutes.toIntOrNull() ?: 0, note,
                )
            }) { Text("Add patch") }
        }
        items(state.data.farmPatches.sortedBy { it.readyAtEpochMillis }) { patchItem ->
            val remaining = patchItem.readyAtEpochMillis - now
            RecordCard(
                "${patchItem.patch} • ${patchItem.crop}",
                (if (remaining <= 0) "READY" else "Ready in ${formatDuration(remaining)}") +
                    patchItem.note.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty(),
                onDelete = { viewModel.deleteFarmPatch(patchItem.id) },
            )
        }
    }
}

@Composable
private fun SlayerCardsScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var monster by remember { mutableStateOf("") }
    var weakness by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var itemsText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    FeatureList {
        item {
            Field(monster, { monster = it }, "Monster")
            Field(weakness, { weakness = it }, "Weakness / attack style")
            Field(location, { location = it }, "Locations")
            Field(itemsText, { itemsText = it }, "Required items")
            Field(notes, { notes = it }, "Notes")
            Button(onClick = {
                viewModel.addSlayerCard(monster, weakness, location, itemsText, notes)
            }) { Text("Save knowledge card") }
        }
        items(state.data.slayerCards) { card ->
            RecordCard(
                card.monster,
                listOf(card.weakness, card.locations, card.requiredItems, card.notes)
                    .filter { it.isNotBlank() }.joinToString(" • "),
                onDelete = { viewModel.deleteSlayerCard(card.id) },
            )
        }
    }
}

@Composable
private fun SessionsScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var activity by remember { mutableStateOf("") }
    var kills by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var loot by remember { mutableStateOf("") }
    var supplies by remember { mutableStateOf("") }
    FeatureList {
        item {
            Field(activity, { activity = it }, "Boss / raid")
            Field(kills, { kills = it }, "Kills / completions")
            Field(minutes, { minutes = it }, "Minutes")
            Field(loot, { loot = it }, "Loot value")
            Field(supplies, { supplies = it }, "Supply cost")
            Button(onClick = {
                viewModel.addSession(
                    activity,
                    kills.toIntOrNull() ?: 0,
                    minutes.toIntOrNull() ?: 0,
                    loot.toLongOrNull() ?: 0,
                    supplies.toLongOrNull() ?: 0,
                )
            }) { Text("Log session") }
        }
        items(state.data.sessions.sortedByDescending { it.createdAtEpochMillis }) { session ->
            val net = session.lootValue - session.supplyCost
            val perHour = if (session.durationMinutes > 0) net * 60 / session.durationMinutes else 0
            RecordCard(
                "${session.activity} • ${session.kills} kills",
                "${session.durationMinutes} min • net ${number(net)} • ${number(perHour)} gp/hr",
                onDelete = { viewModel.deleteSession(session.id) },
            )
        }
    }
}

@Composable
private fun CollectionScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var item by remember { mutableStateOf("") }
    var denominator by remember { mutableStateOf("") }
    FeatureList {
        item {
            Field(item, { item = it }, "Drop / collection item")
            Field(denominator, { denominator = it }, "Drop rate denominator (1 in …)")
            Button(onClick = {
                viewModel.addCollectionGoal(item, denominator.toIntOrNull() ?: 0)
            }) { Text("Add goal") }
        }
        items(state.data.collectionGoals) { goal ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(goal.item, fontWeight = FontWeight.Bold)
                    Text(
                        "${goal.attempts} attempts at 1/${goal.dropRateDenominator} • " +
                            "%.1f%% chance by now".format(goal.dropChancePercent()),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        OutlinedButton(onClick = { viewModel.adjustCollection(goal.id, -1) }) {
                            Text("−")
                        }
                        Button(onClick = { viewModel.adjustCollection(goal.id, 1) }) { Text("+ kill") }
                        FilterChip(
                            selected = goal.obtained,
                            onClick = { viewModel.toggleCollectionObtained(goal.id) },
                            label = { Text(if (goal.obtained) "Obtained" else "Not obtained") },
                        )
                        TextButton(onClick = { viewModel.deleteCollectionGoal(goal.id) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CombatAchievementScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var task by remember { mutableStateOf("") }
    var tier by remember { mutableStateOf("Easy") }
    FeatureList {
        item {
            Field(task, { task = it }, "Combat Achievement task")
            Field(tier, { tier = it }, "Tier")
            Button(onClick = { viewModel.addCombatAchievement(task, tier) }) {
                Text("Add task")
            }
            val completed = state.data.combatAchievements.count { it.completed }
            Text("$completed / ${state.data.combatAchievements.size} planned tasks complete")
        }
        items(state.data.combatAchievements) { taskItem ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = taskItem.completed,
                    onCheckedChange = { viewModel.toggleCombatAchievement(taskItem.id) },
                )
                Column(Modifier.weight(1f)) {
                    Text(taskItem.task)
                    Text(taskItem.tier, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { viewModel.deleteCombatAchievement(taskItem.id) }) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun RoutinesScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var title by remember { mutableStateOf("") }
    var weekly by remember { mutableStateOf(false) }
    FeatureList {
        item {
            Field(title, { title = it }, "Routine")
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(if (weekly) "Weekly" else "Daily")
                Spacer(Modifier.width(8.dp))
                Switch(checked = weekly, onCheckedChange = { weekly = it })
            }
            Button(onClick = { viewModel.addRoutine(title, weekly) }) { Text("Add routine") }
        }
        items(state.data.routines) { routine ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(routine.title, fontWeight = FontWeight.Bold)
                    Text("${if (routine.weekly) "Weekly" else "Daily"} • streak ${routine.streak}")
                    Row {
                        Button(onClick = { viewModel.completeRoutine(routine.id) }) {
                            Text("Complete today")
                        }
                        TextButton(onClick = { viewModel.deleteRoutine(routine.id) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadoutsScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var name by remember { mutableStateOf("") }
    var inventory by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    FeatureList {
        item {
            InfoCard(
                "Manual templates",
                "Paste item names as comma-separated text. Rune Companion never reads your equipment or inventory.",
            )
            Field(name, { name = it }, "Loadout name")
            Field(inventory, { inventory = it }, "Inventory")
            Field(equipment, { equipment = it }, "Equipment")
            Field(notes, { notes = it }, "Notes")
            Button(onClick = {
                viewModel.addLoadout(name, inventory, equipment, notes)
            }) { Text("Save loadout") }
        }
        items(state.data.loadouts) { loadout ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(loadout.name, style = MaterialTheme.typography.titleMedium)
                    Text("Inventory: ${loadout.inventory}")
                    Text("Equipment: ${loadout.equipment}")
                    if (loadout.notes.isNotBlank()) Text(loadout.notes)
                    Row {
                        OutlinedButton(onClick = {
                            clipboard.setText(
                                AnnotatedString(
                                    "${loadout.name}\nInventory: ${loadout.inventory}\n" +
                                        "Equipment: ${loadout.equipment}\n${loadout.notes}",
                                ),
                            )
                        }) { Text("Copy to share") }
                        TextButton(onClick = { viewModel.deleteLoadout(loadout.id) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannerFeature(
    feature: CompanionFeature,
    state: FeatureState,
    viewModel: FeatureViewModel,
    onOpenUrl: (String) -> Unit,
) {
    when (feature) {
        CompanionFeature.CLUES -> ClueScreen(onOpenUrl)
        CompanionFeature.TELEPORTS -> TeleportPlannerScreen(state, viewModel)
        CompanionFeature.DPS -> DpsScreen()
        CompanionFeature.CONSUMABLES -> ConsumablesScreen()
        CompanionFeature.MINIGAMES -> MinigameScreen()
        else -> Unit
    }
}

@Composable
private fun ClueScreen(onOpenUrl: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    FeatureList {
        item {
            InfoCard(
                "Search every clue step",
                "This opens current OSRS Wiki results, including coordinates, anagrams, maps, and challenge answers.",
            )
            Field(query, { query = it }, "Clue text, NPC, anagram, or coordinates")
            Button(
                onClick = {
                    onOpenUrl("https://oldschool.runescape.wiki/?search=${Uri.encode(query)}")
                },
                enabled = query.isNotBlank(),
            ) { Text("Search OSRS Wiki") }
            OutlinedButton(
                onClick = { onOpenUrl("https://oldschool.runescape.wiki/w/Treasure_Trails/Full_guide") },
            ) { Text("Open full clue guide") }
        }
    }
}

@Composable
private fun TeleportPlannerScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var configure by remember { mutableStateOf(false) }
    var destination by remember { mutableStateOf("") }
    var safeOnly by remember { mutableStateOf(true) }
    val data = state.data
    val profile = data.teleportProfile
    val account = data.accounts.firstOrNull { it.username.equals(data.selectedAccount, true) }
    val magic = account?.magicLevel() ?: 1
    val available = TeleportCatalog.all.map {
        if (it.id in setOf("spell_poh", "tab_house", "poh_access")) {
            it.copy(destination = "${profile.pohLocation} house portal")
        } else {
            it
        }
    }.filter {
        it.isAvailable(profile, magic) &&
            (!safeOnly || !it.dangerous) &&
            (destination.isBlank() ||
                it.destination.contains(destination, true) ||
                it.region.contains(destination, true) ||
                it.name.contains(destination, true))
    }.distinctBy { it.name to it.destination }

    FeatureList {
        item {
            InfoCard(
                "How availability works",
                "Magic level comes from ${account?.username ?: "the selected hiscore profile (none selected)"}. " +
                    "Everything else is configured below because public APIs cannot see your spellbook, " +
                    "quests, bank, inventory, POH, jewellery charges, or item charges.",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !configure,
                    onClick = { configure = false },
                    label = { Text("Plan route") },
                )
                FilterChip(
                    selected = configure,
                    onClick = { configure = true },
                    label = { Text("My teleports") },
                )
            }
        }
        if (configure) {
            item {
                SectionTitle("Spellbooks available")
                Spellbook.entries.forEach { book ->
                    ToggleRow(
                        label = book.label,
                        checked = book in profile.spellbooks,
                        onToggle = { viewModel.toggleSpellbook(book) },
                    )
                }
                Text(
                    "Magic level: $magic${if (account == null) " (add/select an account)" else ""}",
                    fontWeight = FontWeight.Bold,
                )
                Field(profile.pohLocation, viewModel::setPohLocation, "House portal location")
            }
            TeleportCatalog.capabilityGroups.forEach { (label, options) ->
                item { SectionTitle(label) }
                items(options.distinctBy { it.capability }) { option ->
                    val capability = option.capability ?: return@items
                    ToggleRow(
                        label = option.name,
                        detail = option.destination,
                        checked = capability in profile.capabilities,
                        onToggle = { viewModel.toggleTeleportCapability(capability) },
                    )
                }
            }
            item { SectionTitle("POH portal chamber / nexus destinations") }
            items(TeleportCatalog.nexusDestinations) { option ->
                val key = option.id.removePrefix("poh_nexus_")
                ToggleRow(
                    label = option.destination,
                    detail = if (option.dangerous) "Wilderness destination" else option.region,
                    checked = key in profile.pohDestinations,
                    onToggle = { viewModel.togglePohDestination(key) },
                )
            }
        } else {
            item {
                Field(destination, { destination = it }, "Where are you going? Area or destination")
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Hide Wilderness routes")
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = safeOnly, onCheckedChange = { safeOnly = it })
                }
                Text(
                    "${available.size} available routes",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(available) { option ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(option.name, fontWeight = FontWeight.Bold)
                        Text("${option.destination} • ${option.kind.label} • ${option.region}")
                        val requirements = buildList {
                            if (option.magicLevel > 1) add("${option.magicLevel} Magic")
                            option.spellbook?.let { add(it.label) }
                            if (option.kind == TeleportKind.POH) add("via POH")
                            if (option.dangerous) add("DANGER: Wilderness")
                            if (option.note.isNotBlank()) add(option.note)
                        }
                        if (requirements.isNotEmpty()) {
                            Text(
                                requirements.joinToString(" • "),
                                color = if (option.dangerous) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DpsScreen() {
    var hitA by remember { mutableStateOf("40") }
    var accuracyA by remember { mutableStateOf("75") }
    var speedA by remember { mutableStateOf("4") }
    var hitB by remember { mutableStateOf("35") }
    var accuracyB by remember { mutableStateOf("85") }
    var speedB by remember { mutableStateOf("4") }
    fun dps(hit: String, accuracy: String, speed: String): Double {
        val maxHit = hit.toDoubleOrNull() ?: 0.0
        val chance = (accuracy.toDoubleOrNull() ?: 0.0) / 100.0
        val seconds = (speed.toDoubleOrNull() ?: 0.0) * 0.6
        return if (seconds <= 0) 0.0 else maxHit / 2 * chance / seconds
    }
    val a = dps(hitA, accuracyA, speedA)
    val b = dps(hitB, accuracyB, speedB)
    FeatureList {
        item {
            InfoCard(
                "Manual comparison",
                "Enter max hit, estimated hit chance, and weapon speed. This does not inspect combat or automate actions.",
            )
            SectionTitle("Loadout A")
            Field(hitA, { hitA = it }, "Max hit")
            Field(accuracyA, { accuracyA = it }, "Accuracy / hit chance %")
            Field(speedA, { speedA = it }, "Attack speed (ticks)")
            SectionTitle("Loadout B")
            Field(hitB, { hitB = it }, "Max hit")
            Field(accuracyB, { accuracyB = it }, "Accuracy / hit chance %")
            Field(speedB, { speedB = it }, "Attack speed (ticks)")
            Text("A: %.2f DPS".format(a), style = MaterialTheme.typography.titleMedium)
            Text("B: %.2f DPS".format(b), style = MaterialTheme.typography.titleMedium)
            Text(
                if (a >= b) "A is %.1f%% higher in this estimate".format(
                    if (b > 0) (a / b - 1) * 100 else 0.0,
                ) else "B is %.1f%% higher in this estimate".format(
                    if (a > 0) (b / a - 1) * 100 else 0.0,
                ),
            )
        }
    }
}

@Composable
private fun ConsumablesScreen() {
    var trips by remember { mutableStateOf("10") }
    var food by remember { mutableStateOf("12") }
    var doses by remember { mutableStateOf("4") }
    var runes by remember { mutableStateOf("100") }
    var cost by remember { mutableStateOf("25000") }
    val count = trips.toLongOrNull()?.coerceAtLeast(0) ?: 0
    FeatureList {
        item {
            Field(trips, { trips = it }, "Planned trips")
            Field(food, { food = it }, "Food per trip")
            Field(doses, { doses = it }, "Potion doses per trip")
            Field(runes, { runes = it }, "Runes / ammo per trip")
            Field(cost, { cost = it }, "Supply cost per trip")
            SectionTitle("Required supplies")
            Text("${number(count * (food.toLongOrNull() ?: 0))} food")
            Text("${number(count * (doses.toLongOrNull() ?: 0))} potion doses")
            Text("${number(count * (runes.toLongOrNull() ?: 0))} runes / ammo")
            Text("${number(count * (cost.toLongOrNull() ?: 0))} gp total")
        }
    }
}

@Composable
private fun MinigameScreen() {
    var name by remember { mutableStateOf("Guardians of the Rift") }
    var current by remember { mutableStateOf("0") }
    var target by remember { mutableStateOf("1000") }
    var perGame by remember { mutableStateOf("50") }
    val remaining = ((target.toDoubleOrNull() ?: 0.0) - (current.toDoubleOrNull() ?: 0.0))
        .coerceAtLeast(0.0)
    val rate = perGame.toDoubleOrNull() ?: 0.0
    val games = if (rate > 0) ceil(remaining / rate).toLong() else 0
    FeatureList {
        item {
            InfoCard(
                "Works for any points-based minigame",
                "Use it for Guardians of the Rift, Tempoross, Wintertodt, Pest Control, " +
                    "Barbarian Assault, Mahogany Homes, or your own target.",
            )
            Field(name, { name = it }, "Minigame")
            Field(current, { current = it }, "Current points / reward units")
            Field(target, { target = it }, "Target")
            Field(perGame, { perGame = it }, "Average per game")
            Text("$games games remaining", style = MaterialTheme.typography.headlineSmall)
            Text("${number(remaining.toLong())} $name units remaining")
        }
    }
}

@Composable
private fun WidgetFeature() {
    FeatureList {
        item {
            InfoCard(
                "Rune Companion home widget",
                "A 3×1 Android widget is included. It shows the selected account and latest " +
                    "total level, and opens Rune Companion when tapped.",
            )
            SectionTitle("Add it on Samsung")
            Text("1. Long-press an empty area of the home screen.")
            Text("2. Tap Widgets.")
            Text("3. Search for Rune Companion.")
            Text("4. Drag the widget onto the home screen.")
        }
    }
}

@Composable
private fun BackupFeature(viewModel: FeatureViewModel) {
    var passphrase by remember { mutableStateOf("") }
    var payload by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    FeatureList {
        item {
            InfoCard(
                "Encrypted local export",
                "Backups include companion data and preferences. AES-256-GCM and a " +
                    "passphrase-derived key protect the exported text. There is no password recovery.",
            )
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("Passphrase (8+ characters)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = {
                runCatching { viewModel.exportBackup(passphrase) }
                    .onSuccess {
                        payload = it
                        clipboard.setText(AnnotatedString(it))
                        status = "Encrypted backup copied to clipboard"
                    }
                    .onFailure { status = it.message ?: "Export failed" }
            }) { Text("Export & copy") }
            OutlinedTextField(
                value = payload,
                onValueChange = { payload = it },
                label = { Text("Encrypted backup text") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = {
                runCatching { viewModel.importBackup(payload, passphrase) }
                    .onSuccess { status = "Backup imported. Restart Rune Companion." }
                    .onFailure { status = it.message ?: "Import failed" }
            }) { Text("Import backup") }
            if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AccountChooser(
    profiles: List<AccountProfile>,
    selected: AccountProfile?,
    viewModel: FeatureViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        profiles.forEach { profile ->
            FilterChip(
                selected = profile.username.equals(selected?.username, true),
                onClick = { viewModel.selectAccount(profile.username) },
                label = { Text(profile.username) },
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    detail: String = "",
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Text(label)
            if (detail.isNotBlank()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body)
        }
    }
}

@Composable
private fun RecordCard(title: String, detail: String, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun FeatureList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

private fun number(value: Long?): String =
    value?.let(NumberFormat.getIntegerInstance()::format) ?: "—"
