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
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.github.taxledgr.runecompanion.ui.theme.LocalRuneLayout

enum class JournalEditorFocus {
    ALL,
    SLAYER,
    CHECKLIST,
}

@Composable
fun JournalScreen(
    state: ToolkitState,
    onSetSlayerTask: (String, Int) -> Unit,
    onAdjustSlayerRemaining: (Int) -> Unit,
    onClearSlayerTask: () -> Unit,
    onAddChecklistEntry: (String, ChecklistCategory) -> Unit,
    onToggleChecklistEntry: (String) -> Unit,
    onDeleteChecklistEntry: (String) -> Unit,
    focus: JournalEditorFocus = JournalEditorFocus.ALL,
) {
    val layout = LocalRuneLayout.current
    val showSlayer = focus != JournalEditorFocus.CHECKLIST
    val showChecklist = focus != JournalEditorFocus.SLAYER
    var addingEntry by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(layout.screenPadding),
        verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing),
    ) {
        item {
            ScreenHeader(
                eyebrow = "MANUAL GAME LOG",
                title = when (focus) {
                    JournalEditorFocus.SLAYER -> "Slayer task"
                    JournalEditorFocus.CHECKLIST -> "Checklist"
                    JournalEditorFocus.ALL -> "Journal"
                },
                subtitle = when (focus) {
                    JournalEditorFocus.SLAYER -> "Update the current manual Slayer counter."
                    JournalEditorFocus.CHECKLIST ->
                        "Edit quests, diaries, collection goals, and loadout notes."
                    JournalEditorFocus.ALL ->
                        "Slayer, quests, diaries, collection goals, and reusable loadouts."
                },
            )
        }
        if (showSlayer) item {
            SlayerCard(
                state = state,
                onSetTask = onSetSlayerTask,
                onAdjustRemaining = onAdjustSlayerRemaining,
                onClear = onClearSlayerTask,
            )
        }
        if (showChecklist) item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Journal entries",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${state.checklist.count { !it.completed }} open • " +
                            "${state.checklist.count { it.completed }} complete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = { addingEntry = !addingEntry }) {
                    Text(if (addingEntry) "Cancel" else "Add entry")
                }
            }
        }
        if (showChecklist && addingEntry) {
            item {
                ChecklistBuilder { title, category ->
                    onAddChecklistEntry(title, category)
                    addingEntry = false
                }
            }
        }
        if (showChecklist) ChecklistCategory.entries.forEach { category ->
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
        if (showChecklist && state.checklist.isEmpty()) {
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
    var monster by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("150") }
    val taskPresets = CompanionCatalog.slayerTaskNames
    val categories = taskPresets.map(CompanionCatalog::slayerTaskCategory).distinct()
    var category by rememberSaveable { mutableStateOf(categories.first()) }
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
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(ChecklistCategory.QUEST) }
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
