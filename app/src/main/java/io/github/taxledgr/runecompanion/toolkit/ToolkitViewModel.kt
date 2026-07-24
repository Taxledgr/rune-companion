package io.github.taxledgr.runecompanion.toolkit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ToolkitViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = ToolkitPreferences(application)
    private val priceClient = PriceClient()
    private val hiscoreClient = HiscoreClient()
    private val persisted = preferences.load()
    private val _state = MutableStateFlow(
        ToolkitState(
            reminders = persisted.reminders,
            slayerTask = persisted.slayerTask,
            checklist = persisted.checklist,
            tripTimer = persisted.tripTimer,
            priceWatchlist = persisted.priceWatchlist,
        ),
    )
    val state: StateFlow<ToolkitState> = _state.asStateFlow()

    init {
        persisted.reminders
            .filter { it.endsAtEpochMillis > System.currentTimeMillis() }
            .forEach { ReminderScheduler.schedule(application, it) }
        viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(nowEpochMillis = System.currentTimeMillis()) }
                delay(1_000)
            }
        }
        if (persisted.priceWatchlist.isNotEmpty()) refreshPrices()
    }

    fun addReminder(title: String, category: ReminderCategory, minutes: Int) {
        if (title.isBlank() || minutes <= 0) return
        val reminder = CompanionReminder(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            category = category,
            endsAtEpochMillis = System.currentTimeMillis() + minutes * 60_000L,
        )
        _state.update { it.copy(reminders = it.reminders + reminder) }
        ReminderScheduler.schedule(getApplication(), reminder)
        persist()
    }

    fun deleteReminder(reminderId: String) {
        _state.update { state ->
            state.copy(reminders = state.reminders.filterNot { it.id == reminderId })
        }
        ReminderScheduler.cancel(getApplication(), reminderId)
        persist()
    }

    fun setSlayerTask(monster: String, target: Int) {
        if (monster.isBlank() || target <= 0) return
        _state.update {
            it.copy(slayerTask = SlayerTask(monster.trim(), target, target))
        }
        persist()
    }

    fun adjustSlayerRemaining(delta: Int) {
        _state.update { state ->
            val task = state.slayerTask ?: return@update state
            state.copy(
                slayerTask = task.copy(
                    remaining = (task.remaining + delta).coerceIn(0, task.target),
                ),
            )
        }
        persist()
    }

    fun clearSlayerTask() {
        _state.update { it.copy(slayerTask = null) }
        persist()
    }

    fun addChecklistEntry(title: String, category: ChecklistCategory) {
        if (title.isBlank()) return
        _state.update {
            it.copy(
                checklist = it.checklist + ChecklistEntry(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    category = category,
                ),
            )
        }
        persist()
    }

    fun toggleChecklistEntry(id: String) {
        _state.update { state ->
            state.copy(
                checklist = state.checklist.map { entry ->
                    if (entry.id == id) entry.copy(completed = !entry.completed) else entry
                },
            )
        }
        persist()
    }

    fun deleteChecklistEntry(id: String) {
        _state.update { state ->
            state.copy(checklist = state.checklist.filterNot { it.id == id })
        }
        persist()
    }

    fun setTripLabel(label: String) {
        _state.update { it.copy(tripTimer = it.tripTimer.copy(label = label)) }
        persist()
    }

    fun toggleTripTimer() {
        val now = System.currentTimeMillis()
        _state.update { state ->
            val timer = state.tripTimer
            val updated = if (timer.startedAtEpochMillis == null) {
                timer.copy(startedAtEpochMillis = now)
            } else {
                timer.copy(
                    startedAtEpochMillis = null,
                    elapsedBeforeStartMillis = timer.elapsedMillis(now),
                )
            }
            state.copy(tripTimer = updated)
        }
        persist()
    }

    fun resetTripTimer() {
        _state.update {
            it.copy(
                tripTimer = it.tripTimer.copy(
                    startedAtEpochMillis = null,
                    elapsedBeforeStartMillis = 0,
                ),
            )
        }
        persist()
    }

    fun searchPrices(query: String) {
        if (query.trim().length < 2) {
            _state.update { it.copy(priceSearchResults = emptyList(), priceError = null) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(priceLoading = true, priceError = null) }
            runCatching { priceClient.search(query) }
                .onSuccess { results ->
                    _state.update {
                        it.copy(priceLoading = false, priceSearchResults = results)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            priceLoading = false,
                            priceError = error.message ?: "Could not search item prices",
                        )
                    }
                }
        }
    }

    fun addPriceWatchItem(item: PriceSearchItem) {
        if (_state.value.priceWatchlist.any { it.id == item.id }) return
        _state.update {
            it.copy(
                priceWatchlist = it.priceWatchlist + PriceWatchItem(item.id, item.name),
                priceSearchResults = emptyList(),
            )
        }
        persist()
        refreshPrices()
    }

    fun removePriceWatchItem(id: Int) {
        _state.update {
            it.copy(priceWatchlist = it.priceWatchlist.filterNot { item -> item.id == id })
        }
        persist()
    }

    fun refreshPrices() {
        val watchlist = _state.value.priceWatchlist
        if (watchlist.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(priceLoading = true, priceError = null) }
            runCatching { priceClient.latest(watchlist) }
                .onSuccess { prices ->
                    _state.update {
                        it.copy(priceLoading = false, priceWatchlist = prices)
                    }
                    persist()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            priceLoading = false,
                            priceError = error.message ?: "Could not refresh prices",
                        )
                    }
                }
        }
    }

    fun lookupHiscores(player: String) {
        viewModelScope.launch {
            _state.update { it.copy(hiscoreLoading = true, hiscoreError = null) }
            runCatching { hiscoreClient.lookup(player) }
                .onSuccess { result ->
                    _state.update { it.copy(hiscoreLoading = false, hiscore = result) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            hiscoreLoading = false,
                            hiscoreError = error.message ?: "Could not load official hiscores",
                        )
                    }
                }
        }
    }

    private fun persist() {
        val state = _state.value
        preferences.save(
            PersistedToolkitData(
                reminders = state.reminders,
                slayerTask = state.slayerTask,
                checklist = state.checklist,
                tripTimer = state.tripTimer,
                priceWatchlist = state.priceWatchlist,
            ),
        )
    }
}
