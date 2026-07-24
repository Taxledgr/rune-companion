package io.github.taxledgr.runecompanion.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.taxledgr.runecompanion.features.CompanionCatalog
import io.github.taxledgr.runecompanion.toolkit.ChecklistCategory
import io.github.taxledgr.runecompanion.toolkit.ChecklistEntry
import io.github.taxledgr.runecompanion.toolkit.ToolkitState
import io.github.taxledgr.runecompanion.ui.theme.RuneCyan

@Composable
fun JournalScreen(
    state: ToolkitState,
    onSetSlayerTask: (String, Int) -> Unit,
    onAdjustSlayerRemaining: (Int) -> Unit,
    onClearSlayerTask: () -> Unit,
    onAddChecklistEntry: (String, ChecklistCategory) -> Unit,
    onToggleChecklistEntry: (String) -> Unit,
    onDeleteChecklistEntry: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                eyebrow = "MANUAL GAME LOG",
                title = "Journal",
                subtitle = "Slayer, quests, diaries, collection goals, and reusable loadouts.",
            )
        }
        item {
            SlayerCard(
                state = state,
                onSetTask = onSetSlayerTask,
                onAdjustRemaining = onAdjustSlayerRemaining,
                onClear = onClearSlayerTask,
            )
        }
        item {
            ChecklistBuilder(onAddChecklistEntry)
        }
        ChecklistCategory.entries.forEach { category ->
            val entries = state.checklist.filter { it.category == category }
            if (entries.isNotEmpty()) {
                item {
                    Text(
                        category.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(entries, key = ChecklistEntry::id) { entry ->
                    ChecklistCard(entry, onToggleChecklistEntry, onDeleteChecklistEntry)
                }
            }
        }
        if (state.checklist.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Add a quest, diary step, collection goal, or loadout above.",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SlayerCard(
    state: ToolkitState,
    onSetTask: (String, Int) -> Unit,
    onAdjustRemaining: (Int) -> Unit,
    onClear: () -> Unit,
) {
    var monster by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("150") }
    val taskPresets = CompanionCatalog.slayerTaskNames
    val categories = taskPresets.map(CompanionCatalog::slayerTaskCategory).distinct()
    var category by remember { mutableStateOf(categories.first()) }
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Slayer task", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            state.slayerTask?.let { task ->
                Text(
                    "${task.remaining} / ${task.target}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = RuneCyan,
                    fontWeight = FontWeight.Bold,
                )
                Text(task.monster, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onAdjustRemaining(-1) },
                        enabled = task.remaining > 0,
                    ) {
                        Text("Kill −1")
                    }
                    OutlinedButton(
                        onClick = { onAdjustRemaining(1) },
                        enabled = task.remaining < task.target,
                    ) {
                        Text("Undo +1")
                    }
                    OutlinedButton(onClick = onClear) {
                        Text("Clear")
                    }
                }
            } ?: run {
                Text("Choose a task", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    taskPresets.filter {
                        CompanionCatalog.slayerTaskCategory(it) == category
                    }.forEach { preset ->
                        FilterChip(
                            selected = monster.equals(preset, ignoreCase = true),
                            onClick = { monster = preset },
                            label = { Text(preset) },
                        )
                    }
                }
                OutlinedTextField(
                    value = monster,
                    onValueChange = { monster = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Monster") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all(Char::isDigit)) amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Task amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        onSetTask(monster, amount.toIntOrNull() ?: 0)
                        monster = ""
                    },
                    enabled = monster.isNotBlank() && (amount.toIntOrNull() ?: 0) > 0,
                ) {
                    Text("Start task")
                }
            }
            Text(
                "The task list is selectable, but the remaining-kill counter stays manual: " +
                    "public hiscores do not expose the player's current Slayer assignment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChecklistBuilder(
    onAdd: (String, ChecklistCategory) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ChecklistCategory.QUEST) }
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Add journal entry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChecklistCategory.entries.forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { category = option },
                        label = { Text(option.label) },
                    )
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        if (category == ChecklistCategory.LOADOUT) {
                            "Gear or inventory setup"
                        } else {
                            "Goal or step"
                        },
                    )
                },
                singleLine = true,
            )
            Button(
                onClick = {
                    onAdd(title, category)
                    title = ""
                },
                enabled = title.isNotBlank(),
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun ChecklistCard(
    entry: ChecklistEntry,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = entry.completed,
                onCheckedChange = { onToggle(entry.id) },
            )
            Text(
                text = entry.title,
                modifier = Modifier.weight(1f),
                textDecoration = if (entry.completed) TextDecoration.LineThrough else null,
                color = if (entry.completed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            OutlinedButton(onClick = { onDelete(entry.id) }) {
                Text("Delete")
            }
        }
    }
}
