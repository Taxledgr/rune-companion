package io.github.taxledgr.runecompanion.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.taxledgr.runecompanion.features.AccountProfile
import io.github.taxledgr.runecompanion.features.BossPreset
import io.github.taxledgr.runecompanion.features.ExpansionCatalog
import io.github.taxledgr.runecompanion.features.FeatureState
import io.github.taxledgr.runecompanion.features.FeatureViewModel
import io.github.taxledgr.runecompanion.features.MarketHistoryPoint
import io.github.taxledgr.runecompanion.features.ProgressPreset
import io.github.taxledgr.runecompanion.features.TeleportCatalog
import io.github.taxledgr.runecompanion.features.currentValue
import io.github.taxledgr.runecompanion.features.magicLevel
import io.github.taxledgr.runecompanion.features.progress
import io.github.taxledgr.runecompanion.features.skillOrEmpty
import io.github.taxledgr.runecompanion.toolkit.PriceSearchItem
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
internal fun BossReadinessScreen(
    state: FeatureState,
    viewModel: FeatureViewModel,
    onOpenUrl: (String) -> Unit,
) {
    var bossId by remember { mutableStateOf(ExpansionCatalog.bosses.first().id) }
    val boss = ExpansionCatalog.bosses.first { it.id == bossId }
    val plan = state.data.bossReadinessPlans.firstOrNull { it.bossId == bossId }
    var notes by remember(bossId, plan?.notes) { mutableStateOf(plan?.notes.orEmpty()) }
    val profile = selectedProfile(state)
    FeatureList {
        item {
            InfoCard(
                "Planning checklist",
                "Public hiscores confirm skill levels. Quests, equipment, inventory, prayers, " +
                    "supplies, and mechanics remain deliberate checks. Suggested levels are " +
                    "planning guidance—open the current Wiki article for authoritative details.",
            )
            CatalogPicker(
                title = "Boss or raid",
                options = ExpansionCatalog.bosses,
                category = BossPreset::category,
                label = BossPreset::name,
                selectedLabel = boss.name,
                onSelect = { bossId = it.id },
            )
            Text(
                "Using ${profile?.username ?: "no selected account"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SectionTitle("Public skill checks")
            boss.skills.forEach { requirement ->
                val current = profile?.latest?.summary?.skillOrEmpty(requirement.skill)?.level ?: 1
                CheckRow(
                    label = "${requirement.skill} ${requirement.level}",
                    detail = "Current public level: $current",
                    checked = current >= requirement.level,
                    enabled = false,
                )
            }
            SectionTitle("Manual readiness checks")
            boss.manualChecks.forEach { check ->
                CheckRow(
                    label = check,
                    checked = check in plan?.confirmedChecks.orEmpty(),
                    onChecked = { viewModel.toggleBossReadinessCheckForBoss(boss.id, check) },
                )
            }
            Field(notes, { notes = it }, "Personal notes")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.saveBossReadinessPlan(boss.id, notes) }) {
                    Text("Save plan")
                }
                OutlinedButton(onClick = { onOpenUrl(wikiUrl(boss.wikiTitle)) }) {
                    Text("Full Wiki guide")
                }
            }
        }
        if (state.data.bossReadinessPlans.isNotEmpty()) {
            item { SectionTitle("Saved boss plans") }
        }
        items(state.data.bossReadinessPlans) { saved ->
            val preset = ExpansionCatalog.bosses.firstOrNull { it.id == saved.bossId }
            val total = preset?.manualChecks?.size ?: 0
            RecordCard(
                preset?.name ?: saved.bossId,
                "${saved.confirmedChecks.size} / $total manual checks • ${saved.notes}",
                onDelete = { viewModel.deleteBossReadinessPlan(saved.id) },
            )
        }
    }
}

@Composable
internal fun ItineraryScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var presetTitle by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var teleport by remember { mutableStateOf("") }
    FeatureList {
        item {
            InfoCard(
                "One ordered companion run",
                "Build from farming patches and routines or add stops below. Optimisation groups " +
                    "nearby regions and leaves Wilderness stops last. It does not read your live position.",
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = viewModel::buildItineraryFromSaved) {
                    Text("Build from saved")
                }
                OutlinedButton(onClick = viewModel::optimiseItinerary) {
                    Text("Optimise order")
                }
            }
            CatalogPicker(
                title = "Suggested stop",
                options = ExpansionCatalog.itinerary,
                category = { it.category },
                label = { it.title },
                selectedLabel = presetTitle,
                onSelect = {
                    presetTitle = it.title
                    title = it.title
                    region = it.region
                    teleport = it.teleport
                },
            )
            Field(title, { title = it }, "Stop")
            Field(region, { region = it }, "Region")
            Field(teleport, { teleport = it }, "Suggested teleport / route")
            Button(
                onClick = {
                    viewModel.addItineraryStop(title, region, teleport)
                    title = ""
                    presetTitle = ""
                },
                enabled = title.isNotBlank(),
            ) { Text("Add stop") }
        }
        items(state.data.itineraryStops) { stop ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = stop.completed,
                        onCheckedChange = { viewModel.toggleItineraryStop(stop.id) },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(stop.title, fontWeight = FontWeight.Bold)
                        Text(
                            "${stop.region} • ${stop.teleport}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { viewModel.deleteItineraryStop(stop.id) }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
internal fun GearUpgradeScreen(
    state: FeatureState,
    viewModel: FeatureViewModel,
    onOpenUrl: (String) -> Unit,
) {
    var style by remember { mutableStateOf("Melee") }
    var currentItem by remember { mutableStateOf("") }
    var target by remember { mutableStateOf<PriceSearchItem?>(null) }
    var budget by remember { mutableStateOf("") }
    var benefit by remember { mutableStateOf("") }
    FeatureList {
        item {
            InfoCard(
                "Budgeted upgrade path",
                "Choose a target from the Wiki item catalogue. Prices use recent public GE data; " +
                    "stat benefits and account restrictions should be verified in the in-app Wiki.",
            )
            ChoiceRow(
                listOf("Melee", "Ranged", "Magic", "Prayer", "Skilling"),
                style,
            ) { style = it }
            Field(currentItem, { currentItem = it }, "Current item")
            PriceItemPicker(state, viewModel, target) { target = it }
            Field(budget, { budget = it }, "Available budget (gp)")
            Field(benefit, { benefit = it }, "Expected benefit / important stat change")
            Button(
                onClick = {
                    target?.let {
                        viewModel.addGearUpgrade(
                            style,
                            currentItem,
                            it,
                            budget.toLongOrNull() ?: 0,
                            benefit,
                        )
                    }
                    target = null
                    currentItem = ""
                },
                enabled = target != null,
            ) { Text("Add upgrade") }
        }
        items(state.data.gearUpgrades) { upgrade ->
            val gap = ((upgrade.targetPrice ?: 0) - upgrade.budget).coerceAtLeast(0)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(upgrade.targetItemName, fontWeight = FontWeight.Bold)
                    Text(
                        "${upgrade.style} • from ${upgrade.currentItem.ifBlank { "unspecified" }} • " +
                            "price ${coins(upgrade.targetPrice)} • budget ${coins(upgrade.budget)}",
                    )
                    if (upgrade.benefit.isNotBlank()) Text(upgrade.benefit)
                    if (gap > 0) Text("${coins(gap)} over budget", color = MaterialTheme.colorScheme.error)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = upgrade.obtained,
                            onClick = { viewModel.toggleGearUpgradeObtained(upgrade.id) },
                            label = { Text(if (upgrade.obtained) "Obtained" else "Planned") },
                        )
                        TextButton(onClick = { viewModel.deleteGearUpgrade(upgrade.id) }) {
                            Text("Delete")
                        }
                    }
                    OutlinedButton(onClick = { onOpenUrl(wikiUrl(upgrade.targetItemName)) }) {
                        Text("Stats and uses in Wiki")
                    }
                }
            }
        }
    }
}

@Composable
internal fun LootLedgerScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var activity by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<PriceSearchItem?>(null) }
    var quantity by remember { mutableStateOf("1") }
    FeatureList {
        item {
            InfoCard(
                "Itemised loot",
                "Log drops deliberately and value them with public GE prices. This never reads " +
                    "the loot interface or game client.",
            )
            CatalogPicker(
                title = "Activity",
                options = io.github.taxledgr.runecompanion.features.CompanionCatalog.activities,
                category = { it.category },
                label = { it.name },
                selectedLabel = activity,
                onSelect = { activity = it.name },
            )
            Field(activity, { activity = it }, "Boss / raid / activity")
            PriceItemPicker(state, viewModel, selected) { selected = it }
            Field(quantity, { quantity = it }, "Quantity")
            Button(
                onClick = {
                    selected?.let {
                        viewModel.addLootLedgerEntry(activity, it, quantity.toIntOrNull() ?: 0)
                    }
                    selected = null
                },
                enabled = activity.isNotBlank() && selected != null,
            ) { Text("Add loot") }
            val total = state.data.lootLedger.sumOf { it.totalValue }
            Text("Recorded loot value: ${coins(total)}", fontWeight = FontWeight.Bold)
        }
        items(state.data.lootLedger.sortedByDescending { it.createdAtEpochMillis }) { entry ->
            RecordCard(
                "${entry.quantity} × ${entry.itemName}",
                "${entry.activity} • ${coins(entry.unitValue)} each • ${coins(entry.totalValue)}",
                onDelete = { viewModel.deleteLootLedgerEntry(entry.id) },
            )
        }
    }
}

@Composable
internal fun CounterGoalsScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var activity by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    val profile = selectedProfile(state)
    val current = profile?.latest?.summary?.activities
        ?.firstOrNull { it.name.equals(activity, ignoreCase = true) }?.score
    FeatureList {
        item {
            InfoCard(
                "Automatic public progress",
                "Goals update from Jagex's public activity counters whenever that account is " +
                    "refreshed. Personal bests, unranked values, inventory, and private state are unavailable.",
            )
            Text("Using ${profile?.username ?: "no selected account"}")
            CatalogPicker(
                title = "Public activity",
                options = io.github.taxledgr.runecompanion.features.CompanionCatalog.activities,
                category = { it.category },
                label = { it.name },
                selectedLabel = activity,
                onSelect = {
                    activity = it.name
                    val value = profile?.latest?.summary?.activities
                        ?.firstOrNull { score -> score.name.equals(it.name, true) }?.score
                    if (value != null) target = (value + 100).toString()
                },
            )
            Text("Current public count: ${number(current)}")
            if (profile != null) {
                OutlinedButton(onClick = { viewModel.refreshAccount(profile.username) }) {
                    Text("Refresh public counters")
                }
            }
            Field(target, { target = it }, "Target count")
            Button(
                onClick = { viewModel.addCounterGoal(activity, target.toLongOrNull() ?: 0) },
                enabled = profile != null && current != null,
            ) { Text("Add public goal") }
        }
        items(state.data.counterGoals) { goal ->
            val goalProfile = state.data.accounts.firstOrNull {
                it.username.equals(goal.account, ignoreCase = true)
            }
            val currentValue = goal.currentValue(goalProfile)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("${goal.activity} • ${goal.account}", fontWeight = FontWeight.Bold)
                    Text("${number(currentValue)} / ${number(goal.targetValue)}")
                    LinearProgressIndicator(
                        progress = { goal.progress(goalProfile) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { viewModel.deleteCounterGoal(goal.id) }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProgressNavigatorScreen(
    state: FeatureState,
    viewModel: FeatureViewModel,
    onOpenUrl: (String) -> Unit,
) {
    var category by remember { mutableStateOf("Quest") }
    var query by remember { mutableStateOf("") }
    val profile = selectedProfile(state)
    val visible = ExpansionCatalog.progress.filter {
        it.category == category && (query.isBlank() || it.name.contains(query, ignoreCase = true))
    }
    FeatureList {
        item {
            InfoCard(
                "Quest and diary navigator",
                "Public levels are checked automatically. Quest points, prerequisite completion, " +
                    "boosts, items, and diary tasks remain manual. Every entry opens its complete " +
                    "current Wiki article inside Rune Companion.",
            )
            ChoiceRow(listOf("Quest", "Achievement Diary"), category) { category = it }
            Field(query, { query = it }, "Search $category")
            Text(
                "${state.data.completedProgressIds.size} progression entries marked complete",
                fontWeight = FontWeight.Bold,
            )
        }
        items(visible) { entry ->
            ProgressCard(entry, profile, entry.id in state.data.completedProgressIds, viewModel, onOpenUrl)
        }
    }
}

@Composable
private fun ProgressCard(
    entry: ProgressPreset,
    profile: AccountProfile?,
    completed: Boolean,
    viewModel: FeatureViewModel,
    onOpenUrl: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = completed, onCheckedChange = { viewModel.toggleProgress(entry.id) })
                Text(entry.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            entry.skills.forEach { requirement ->
                val current = profile?.latest?.summary?.skillOrEmpty(requirement.skill)?.level ?: 1
                Text(
                    "${if (current >= requirement.level) "✓" else "○"} " +
                        "${requirement.skill} ${requirement.level} • current $current",
                    color = if (current >= requirement.level) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text("Prerequisites: ${entry.prerequisites}")
            OutlinedButton(onClick = { onOpenUrl(wikiUrl(entry.wikiTitle)) }) {
                Text("Full requirements in Wiki")
            }
        }
    }
}

@Composable
internal fun SupplyLockerScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var selected by remember { mutableStateOf<PriceSearchItem?>(null) }
    var quantity by remember { mutableStateOf("") }
    var lowAt by remember { mutableStateOf("10") }
    FeatureList {
        item {
            InfoCard(
                "Manual stock, automatic value",
                "Track tablets, charged jewellery, runes, ammunition, cannonballs, food, and " +
                    "potion doses. Counts stay manual; prices are refreshed when an item is saved.",
            )
            PriceItemPicker(state, viewModel, selected) { selected = it }
            Field(quantity, { quantity = it }, "Current quantity / charges / doses")
            Field(lowAt, { lowAt = it }, "Low-stock threshold")
            Button(
                onClick = {
                    selected?.let {
                        viewModel.addSupplyLockerItem(
                            it,
                            quantity.toIntOrNull() ?: 0,
                            lowAt.toIntOrNull() ?: 0,
                        )
                    }
                    selected = null
                },
                enabled = selected != null,
            ) { Text("Save stock") }
            Text(
                "Total locker value: ${coins(state.data.supplyLocker.sumOf { it.stockValue })}",
                fontWeight = FontWeight.Bold,
            )
        }
        items(state.data.supplyLocker) { stock ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(stock.itemName, fontWeight = FontWeight.Bold)
                    Text(
                        "${stock.quantity} in stock • low at ${stock.lowAt} • ${coins(stock.stockValue)}",
                        color = if (stock.quantity <= stock.lowAt) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { viewModel.adjustSupplyLockerItem(stock.id, -1) }) {
                            Text("−1")
                        }
                        Button(onClick = { viewModel.adjustSupplyLockerItem(stock.id, 1) }) {
                            Text("+1")
                        }
                        TextButton(onClick = { viewModel.deleteSupplyLockerItem(stock.id) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MarketHistoryScreen(state: FeatureState, viewModel: FeatureViewModel) {
    var selected by remember { mutableStateOf<PriceSearchItem?>(state.marketItem) }
    var timestep by remember { mutableStateOf("24h") }
    FeatureList {
        item {
            InfoCard(
                "Wiki real-time market history",
                "Charts use the OSRS Wiki price API and represent observed trades, not guaranteed " +
                    "buy or sell prices. Volumes and spreads can be thin for infrequently traded items.",
            )
            PriceItemPicker(state, viewModel, selected) { selected = it }
            ChoiceRow(listOf("5m", "1h", "6h", "24h"), timestep) { timestep = it }
            Button(
                onClick = { selected?.let { viewModel.loadMarketHistory(it, timestep) } },
                enabled = selected != null && !state.marketHistoryLoading,
            ) {
                Text(if (state.marketHistoryLoading) "Loading…" else "Load history")
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.marketHistory.isNotEmpty()) {
                MarketChart(state.marketHistory)
                val values = state.marketHistory.mapNotNull(MarketHistoryPoint::midpoint)
                val latest = state.marketHistory.lastOrNull()
                val spread = if (latest?.averageHigh != null && latest.averageLow != null) {
                    latest.averageHigh - latest.averageLow
                } else {
                    null
                }
                val tax = latest?.averageHigh?.let { (it / 100).coerceAtMost(5_000_000) }
                val marginAfterTax = if (
                    latest?.averageHigh != null &&
                    latest.averageLow != null &&
                    tax != null
                ) {
                    latest.averageHigh - latest.averageLow - tax
                } else {
                    null
                }
                Text(
                    "${state.marketItem?.name.orEmpty()} • latest ${coins(values.lastOrNull())} • " +
                        "range ${coins(values.minOrNull())}–${coins(values.maxOrNull())} • " +
                        "spread ${coins(spread)}",
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Estimated one-item margin after GE tax: ${coins(marginAfterTax)}",
                    color = if ((marginAfterTax ?: 0) > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    "${number(latest?.highVolume)} high-volume • ${number(latest?.lowVolume)} low-volume",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MarketChart(history: List<MarketHistoryPoint>) {
    val values = history.mapNotNull(MarketHistoryPoint::midpoint)
    if (values.size < 2) return
    val minimum = values.minOrNull() ?: return
    val maximum = values.maxOrNull() ?: return
    val range = max(1L, maximum - minimum)
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier.fillMaxWidth().height(180.dp).padding(vertical = 12.dp),
    ) {
        val points = values.mapIndexed { index, value ->
            Offset(
                x = if (values.size == 1) 0f else size.width * index / (values.size - 1),
                y = size.height - size.height * (value - minimum).toFloat() / range,
            )
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(lineColor, start, end, strokeWidth = 4f)
        }
        drawLine(
            Color.Gray.copy(alpha = 0.35f),
            Offset(0f, size.height),
            Offset(size.width, size.height),
            strokeWidth = 2f,
        )
    }
}

@Composable
internal fun WildernessRiskScreen(
    state: FeatureState,
    viewModel: FeatureViewModel,
    onOpenUrl: (String) -> Unit,
) {
    var selected by remember { mutableStateOf<PriceSearchItem?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var protected by remember { mutableStateOf(false) }
    val carried = state.data.wildernessRisk.sumOf { it.totalValue }
    val protectedValue = state.data.wildernessRisk.filter { it.protected }.sumOf { it.totalValue }
    val atRisk = (carried - protectedValue).coerceAtLeast(0)
    val profile = selectedProfile(state)
    val escapeNames = setOf(
        "Royal seed pod",
        "Amulet of glory",
        "Ring of wealth",
        "Combat bracelet",
        "Games necklace",
        "Slayer ring",
    )
    val configuredEscapes = TeleportCatalog.all
        .filter { it.name in escapeNames }
        .filter { it.isAvailable(state.data.teleportProfile, profile?.magicLevel() ?: 1) }
        .distinctBy { it.name }
    FeatureList {
        item {
            InfoCard(
                "Manual estimate only",
                "Death mechanics depend on skull, Protect Item, Wilderness level, special areas, " +
                    "untradeables, Trouver protection, supplies, and current game rules. Mark what " +
                    "you expect to protect and verify the current rules before travelling.",
            )
            PriceItemPicker(state, viewModel, selected) { selected = it }
            Field(quantity, { quantity = it }, "Quantity")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Treat as protected")
                Spacer(Modifier.weight(1f))
                Switch(checked = protected, onCheckedChange = { protected = it })
            }
            Button(
                onClick = {
                    selected?.let {
                        viewModel.addWildernessRiskItem(
                            it,
                            quantity.toIntOrNull() ?: 0,
                            protected,
                        )
                    }
                    selected = null
                },
                enabled = selected != null,
            ) { Text("Add risk item") }
            SectionTitle("Risk estimate")
            Text("Carried: ${coins(carried)}")
            Text("Marked protected: ${coins(protectedValue)}")
            Text("Estimated at risk: ${coins(atRisk)}", fontWeight = FontWeight.Bold)
            if (configuredEscapes.isNotEmpty()) {
                Text(
                    "Configured escape references: " +
                        configuredEscapes.joinToString { it.name },
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Confirm the Wilderness-level restriction and combat state before relying on a teleport.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(onClick = { onOpenUrl(wikiUrl("Death")) }) {
                Text("Current death rules in Wiki")
            }
        }
        items(state.data.wildernessRisk) { risk ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("${risk.quantity} × ${risk.itemName}", fontWeight = FontWeight.Bold)
                    Text("${coins(risk.totalValue)} • ${if (risk.protected) "marked protected" else "at risk"}")
                    Row {
                        FilterChip(
                            selected = risk.protected,
                            onClick = { viewModel.toggleWildernessRiskProtected(risk.id) },
                            label = { Text(if (risk.protected) "Protected" else "At risk") },
                        )
                        TextButton(onClick = { viewModel.deleteWildernessRiskItem(risk.id) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MonsterExplorerScreen(onOpenUrl: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val categories = listOf("All") + ExpansionCatalog.monsters.map { it.category }.distinct()
    val visible = ExpansionCatalog.monsters.filter {
        (category == "All" || it.category == category) &&
            (query.isBlank() ||
                listOf(it.name, it.style, it.locations, it.notableDrops)
                    .any { value -> value.contains(query, ignoreCase = true) })
    }
    FeatureList {
        item {
            InfoCard(
                "Fast reference plus the complete Wiki",
                "Search the built-in quick catalogue or open the full current monster page inside " +
                    "Rune Companion for stats, mechanics, maps, drops, strategies, and changes.",
            )
            Field(query, { query = it }, "Monster, drop, style, or location")
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                categories.forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { category = option },
                        label = { Text(option) },
                    )
                }
            }
            OutlinedButton(
                onClick = { onOpenUrl(wikiSearchUrl(query.ifBlank { "Monster" })) },
            ) { Text("Search the complete Wiki") }
        }
        items(visible) { monster ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(monster.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Style: ${monster.style}")
                    Text("Locations: ${monster.locations}")
                    Text("Requirement: ${monster.requirement}")
                    Text("Notable drops: ${monster.notableDrops}")
                    OutlinedButton(onClick = { onOpenUrl(wikiUrl(monster.wikiTitle)) }) {
                        Text("Full monster & drop page")
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceItemPicker(
    state: FeatureState,
    viewModel: FeatureViewModel,
    selected: PriceSearchItem?,
    onSelect: (PriceSearchItem) -> Unit,
) {
    var query by remember(selected?.id) { mutableStateOf(selected?.name.orEmpty()) }
    Field(
        query,
        {
            query = it
            viewModel.searchPrices(it)
        },
        "Search OSRS item",
    )
    state.priceSearchResults.take(8).forEach { result ->
        OutlinedButton(
            onClick = {
                onSelect(result)
                query = result.name
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("${result.name}${if (result.members) " • Members" else ""}")
        }
    }
    selected?.let {
        Text("Selected: ${it.name}", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CheckRow(
    label: String,
    detail: String = "",
    checked: Boolean,
    enabled: Boolean = true,
    onChecked: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { if (enabled) onChecked() },
        )
        Column(Modifier.weight(1f)) {
            Text(label)
            if (detail.isNotBlank()) {
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun ChoiceRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option) },
            )
        }
    }
}

private fun selectedProfile(state: FeatureState): AccountProfile? =
    state.data.accounts.firstOrNull {
        it.username.equals(state.data.selectedAccount, ignoreCase = true)
    }

internal fun wikiUrl(title: String): String =
    "https://oldschool.runescape.wiki/w/${Uri.encode(title.replace(' ', '_'))}"

internal fun wikiSearchUrl(query: String): String =
    "https://oldschool.runescape.wiki/?search=${Uri.encode(query)}"

private fun coins(value: Long?): String =
    value?.let { "${NumberFormat.getIntegerInstance().format(it)} gp" } ?: "—"

private val DATE_TIME = DateTimeFormatter.ofPattern("d MMM HH:mm")

@Suppress("unused")
private fun timeLabel(epochSeconds: Long): String =
    DATE_TIME.format(Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()))
