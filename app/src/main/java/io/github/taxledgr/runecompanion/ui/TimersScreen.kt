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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.taxledgr.runecompanion.toolkit.CompanionReminder
import io.github.taxledgr.runecompanion.toolkit.ReminderCategory
import io.github.taxledgr.runecompanion.toolkit.ToolkitState
import io.github.taxledgr.runecompanion.toolkit.formatDuration
import io.github.taxledgr.runecompanion.ui.theme.RuneCyan
import kotlinx.coroutines.delay

@Composable
fun TimersScreen(
    state: ToolkitState,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onAddReminder: (String, ReminderCategory, Int) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onSetTripLabel: (String) -> Unit,
    onToggleTripTimer: () -> Unit,
    onResetTripTimer: () -> Unit,
) {
    val nowEpochMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    var addingReminder by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                eyebrow = "REMINDERS & TRIPS",
                title = "Timers",
                subtitle = "Local Android timers for routines, bosses, and raids.",
            )
        }
        if (!notificationPermissionGranted) {
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Allow notifications", fontWeight = FontWeight.Bold)
                        Text(
                            "Timers can still be saved, but Android needs notification permission to alert you.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedButton(onClick = onRequestNotificationPermission) {
                            Text("Allow timer alerts")
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Saved reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${state.reminders.count { it.endsAtEpochMillis <= nowEpochMillis }} ready • " +
                            "${state.reminders.size} total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = { addingReminder = !addingReminder }) {
                    Text(if (addingReminder) "Cancel" else "Add timer")
                }
            }
        }
        if (addingReminder) {
            item {
                ReminderBuilder { title, category, minutes ->
                    onAddReminder(title, category, minutes)
                    addingReminder = false
                }
            }
        }
        if (state.reminders.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No saved reminders yet.",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(
            items = state.reminders.sortedBy(CompanionReminder::endsAtEpochMillis),
            key = CompanionReminder::id,
        ) { reminder ->
            ReminderCard(reminder, nowEpochMillis, onDeleteReminder)
        }
        item {
            TripTimerCard(
                timer = state.tripTimer,
                nowEpochMillis = nowEpochMillis,
                onSetTripLabel = onSetTripLabel,
                onToggle = onToggleTripTimer,
                onReset = onResetTripTimer,
            )
        }
    }
}

@Composable
private fun ReminderBuilder(
    onAddReminder: (String, ReminderCategory, Int) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("50") }
    var category by rememberSaveable { mutableStateOf(ReminderCategory.CUSTOM) }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("New reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    Triple("Herb run", ReminderCategory.FARMING, 80),
                    Triple("Birdhouses", ReminderCategory.BIRDHOUSE, 50),
                    Triple("Daily reset", ReminderCategory.DAILY, 1_440),
                ).forEach { (label, presetCategory, presetMinutes) ->
                    OutlinedButton(
                        onClick = {
                            onAddReminder(label, presetCategory, presetMinutes)
                        },
                    ) {
                        Text(label)
                    }
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reminder name") },
                singleLine = true,
            )
            OutlinedTextField(
                value = minutes,
                onValueChange = { if (it.all(Char::isDigit)) minutes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Minutes from now") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReminderCategory.entries.forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { category = option },
                        label = { Text(option.label) },
                    )
                }
            }
            Button(
                onClick = {
                    val parsedMinutes = minutes.toIntOrNull() ?: return@Button
                    onAddReminder(title, category, parsedMinutes)
                    title = ""
                },
                enabled = title.isNotBlank() && (minutes.toIntOrNull() ?: 0) > 0,
            ) {
                Text("Save timer")
            }
            Text(
                "Android may defer a timer slightly under battery restrictions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TripTimerCard(
    timer: io.github.taxledgr.runecompanion.toolkit.TripTimer,
    nowEpochMillis: Long,
    onSetTripLabel: (String) -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Boss / raid stopwatch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = timer.label,
                onValueChange = onSetTripLabel,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Trip label") },
                singleLine = true,
            )
            Text(
                formatDuration(timer.elapsedMillis(nowEpochMillis)),
                style = MaterialTheme.typography.headlineMedium,
                color = RuneCyan,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onToggle) {
                    Text(if (timer.startedAtEpochMillis == null) "Start" else "Pause")
                }
                OutlinedButton(onClick = onReset) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: CompanionReminder,
    nowEpochMillis: Long,
    onDelete: (String) -> Unit,
) {
    val remaining = reminder.endsAtEpochMillis - nowEpochMillis
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.title, fontWeight = FontWeight.Bold)
                Text(
                    reminder.category.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (remaining <= 0) "Ready" else formatDuration(remaining),
                color = RuneCyan,
                fontWeight = FontWeight.Bold,
            )
            OutlinedButton(onClick = { onDelete(reminder.id) }) {
                Text("Remove")
            }
        }
    }
}
