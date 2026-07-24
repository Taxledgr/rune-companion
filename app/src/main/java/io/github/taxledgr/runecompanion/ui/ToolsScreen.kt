package io.github.taxledgr.runecompanion.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.taxledgr.runecompanion.toolkit.PriceSearchItem
import io.github.taxledgr.runecompanion.toolkit.ToolkitState
import io.github.taxledgr.runecompanion.toolkit.attemptsForChance
import io.github.taxledgr.runecompanion.toolkit.dropChancePercent
import io.github.taxledgr.runecompanion.toolkit.xpForLevel
import io.github.taxledgr.runecompanion.ui.theme.RuneCyan
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ToolsScreen(
    state: ToolkitState,
    onSearchPrices: (String) -> Unit,
    onAddPriceWatchItem: (PriceSearchItem) -> Unit,
    onRemovePriceWatchItem: (Int) -> Unit,
    onRefreshPrices: () -> Unit,
    onLookupHiscores: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                eyebrow = "PUBLIC DATA & CALCULATORS",
                title = "Tools",
                subtitle = "Prices, hiscores, drop odds, XP, supplies, and OSRS references.",
            )
        }
        item {
            PriceWatchCard(
                state,
                onSearchPrices,
                onAddPriceWatchItem,
                onRemovePriceWatchItem,
                onRefreshPrices,
            )
        }
        item {
            HiscoreCard(state, onLookupHiscores)
        }
        item {
            XpCalculatorCard()
        }
        item {
            DropCalculatorCard()
        }
        item {
            SupplyCalculatorCard()
        }
        item {
            ReferenceCard(onOpenUrl)
        }
        item {
            Text(
                "GE data is provided by the OSRS Wiki real-time prices API. Hiscores come from Jagex's public Old School endpoint. Values are informational and can be delayed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PriceWatchCard(
    state: ToolkitState,
    onSearch: (String) -> Unit,
    onAdd: (PriceSearchItem) -> Unit,
    onRemove: (Int) -> Unit,
    onRefresh: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
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
                Text("GE price watchlist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = state.priceWatchlist.isNotEmpty() && !state.priceLoading,
                ) {
                    Text(if (state.priceLoading) "Loading…" else "Refresh")
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search items") },
                singleLine = true,
            )
            state.priceSearchResults.forEach { result ->
                OutlinedButton(
                    onClick = {
                        onAdd(result)
                        query = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("${result.name}${if (result.members) " • Members" else ""}")
                }
            }
            state.priceError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.priceWatchlist.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.Bold)
                        Text(
                            "High ${item.high.coins()} • Low ${item.low.coins()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RuneCyan,
                        )
                    }
                    OutlinedButton(onClick = { onRemove(item.id) }) {
                        Text("Remove")
                    }
                }
            }
            if (state.priceWatchlist.isEmpty()) {
                Text(
                    "Search above to save items. No account or login is used.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HiscoreCard(
    state: ToolkitState,
    onLookup: (String) -> Unit,
) {
    var player by remember { mutableStateOf("") }
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Official hiscores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = player,
                onValueChange = { player = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Player name") },
                singleLine = true,
            )
            Button(
                onClick = { onLookup(player) },
                enabled = player.isNotBlank() && !state.hiscoreLoading,
            ) {
                Text(if (state.hiscoreLoading) "Looking up…" else "Look up")
            }
            state.hiscoreError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.hiscore?.let { result ->
                Text(result.player, style = MaterialTheme.typography.titleMedium, color = RuneCyan)
                result.skills
                    .filter { it.name in HISCORE_SPOTLIGHT }
                    .forEach { skill ->
                        Text(
                            "${skill.name}: level ${skill.level} • rank ${skill.rank.formatted()} • ${skill.xp.formatted()} XP",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
            }
        }
    }
}

@Composable
private fun XpCalculatorCard() {
    var currentLevel by remember { mutableStateOf("1") }
    var targetLevel by remember { mutableStateOf("99") }
    val current = currentLevel.toIntOrNull()?.coerceIn(1, 126) ?: 1
    val target = targetLevel.toIntOrNull()?.coerceIn(1, 126) ?: current
    val remaining = (xpForLevel(target) - xpForLevel(current)).coerceAtLeast(0)
    CalculatorCard("XP calculator") {
        NumberField("Current level", currentLevel) { currentLevel = it }
        NumberField("Target level", targetLevel) { targetLevel = it }
        CalculatorResult("${remaining.formatted()} XP between levels")
    }
}

@Composable
private fun DropCalculatorCard() {
    var denominator by remember { mutableStateOf("5000") }
    var attempts by remember { mutableStateOf("1000") }
    val dropRate = denominator.toIntOrNull() ?: 0
    val kills = attempts.toIntOrNull() ?: 0
    val chance = dropChancePercent(kills, dropRate)
    val halfway = attemptsForChance(50.0, dropRate)
    CalculatorCard("Drop-rate calculator") {
        NumberField("Drop rate: 1 in…", denominator) { denominator = it }
        NumberField("Kills / attempts", attempts) { attempts = it }
        CalculatorResult(
            "%.2f%% chance by %,d attempts%s".format(
                Locale.US,
                chance,
                kills,
                if (halfway > 0) " • 50% at about ${halfway.formatted()}" else "",
            ),
        )
    }
}

@Composable
private fun SupplyCalculatorCard() {
    var quantity by remember { mutableStateOf("100") }
    var unitPrice by remember { mutableStateOf("500") }
    val total = (quantity.toLongOrNull() ?: 0) * (unitPrice.toLongOrNull() ?: 0)
    CalculatorCard("Supply cost calculator") {
        NumberField("Quantity", quantity) { quantity = it }
        NumberField("Price each", unitPrice) { unitPrice = it }
        CalculatorResult("${total.formatted()} coins total")
    }
}

@Composable
private fun CalculatorCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all(Char::isDigit)) onValueChange(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

@Composable
private fun CalculatorResult(text: String) {
    Text(text, color = RuneCyan, fontWeight = FontWeight.Bold)
}

@Composable
private fun ReferenceCard(onOpenUrl: (String) -> Unit) {
    val links = listOf(
        "Clue helper" to "https://oldschool.runescape.wiki/w/Treasure_Trails/Guide",
        "Fairy rings" to "https://oldschool.runescape.wiki/w/Fairy_rings",
        "Teleports" to "https://oldschool.runescape.wiki/w/Transportation",
        "Quest guides" to "https://oldschool.runescape.wiki/w/Optimal_quest_guide",
        "Achievement diaries" to "https://oldschool.runescape.wiki/w/Achievement_Diary",
        "Boss guides" to "https://oldschool.runescape.wiki/w/Boss",
    )
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Quick references", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                links.take(3).forEach { (label, url) ->
                    OutlinedButton(onClick = { onOpenUrl(url) }) {
                        Text(label)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                links.drop(3).forEach { (label, url) ->
                    OutlinedButton(onClick = { onOpenUrl(url) }) {
                        Text(label)
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenHeader(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = RuneCyan,
            fontWeight = FontWeight.Bold,
        )
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Long?.coins(): String = this?.let { "${it.formatted()} gp" } ?: "—"
private fun Long.formatted(): String = NumberFormat.getIntegerInstance().format(this)
private fun Int.formatted(): String = NumberFormat.getIntegerInstance().format(this)

private val HISCORE_SPOTLIGHT = setOf(
    "Overall",
    "Attack",
    "Defence",
    "Strength",
    "Hitpoints",
    "Ranged",
    "Prayer",
    "Magic",
    "Slayer",
)
