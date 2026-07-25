package io.github.taxledgr.runecompanion.toolkit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
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
    private var foregroundHiscoreJob: Job? = null
    private var priceRefreshJob: Job? = null
    private var priceSearchJob: Job? = null
    private var latestPriceQuery = ""
    private var lastUndoData: PersistedToolkitData? = null
    private val _state = MutableStateFlow(
        ToolkitState(
            reminders = persisted.reminders,
            slayerTask = persisted.slayerTask,
            checklist = persisted.checklist,
            tripTimer = persisted.tripTimer,
            priceWatchlist = persisted.priceWatchlist,
            trackedPlayer = persisted.trackedPlayer,
        ),
    )
    val state: StateFlow<ToolkitState> = _state.asStateFlow()

    init {
        persisted.reminders
            .filter { it.endsAtEpochMillis > System.currentTimeMillis() }
            .forEach { ReminderScheduler.schedule(application, it) }
        HiscoreRefreshScheduler.sync(
            context = application,
            enabled = persisted.trackedPlayer.autoRefreshEnabled &&
                persisted.trackedPlayer.username.isNotBlank(),
        )
        if (persisted.priceWatchlist.isNotEmpty()) refreshPrices()
        if (
            persisted.trackedPlayer.autoRefreshEnabled &&
            persisted.trackedPlayer.username.isNotBlank()
        ) {
            refreshTrackedPlayer()
        }
    }

    fun setAppInForeground(inForeground: Boolean) {
        foregroundHiscoreJob?.cancel()
        foregroundHiscoreJob = null
        if (!inForeground) return

        val storedProfile = preferences.load().trackedPlayer
        val stateProfile = _state.value.trackedPlayer
        if (
            (storedProfile.lastUpdatedEpochMillis ?: 0) >
            (stateProfile.lastUpdatedEpochMillis ?: 0)
        ) {
            _state.update { it.copy(trackedPlayer = storedProfile) }
        }
        val profile = _state.value.trackedPlayer
        val lastUpdated = profile.lastUpdatedEpochMillis ?: 0
        if (
            profile.autoRefreshEnabled &&
            profile.username.isNotBlank() &&
            System.currentTimeMillis() - lastUpdated >= FOREGROUND_HISCORE_INTERVAL_MS
        ) {
            refreshTrackedPlayer()
        }
        foregroundHiscoreJob = viewModelScope.launch {
            while (isActive) {
                delay(FOREGROUND_HISCORE_INTERVAL_MS)
                val currentProfile = _state.value.trackedPlayer
                if (
                    currentProfile.autoRefreshEnabled &&
                    currentProfile.username.isNotBlank()
                ) {
                    refreshTrackedPlayer()
                }
            }
        }
    }

    fun reloadFromDisk() {
        val restored = preferences.load()
        val restoredReminderIds = restored.reminders.mapTo(mutableSetOf()) { it.id }
        _state.value.reminders
            .filterNot { it.id in restoredReminderIds }
            .forEach { ReminderScheduler.cancel(getApplication(), it.id) }
        _state.update {
            it.copy(
                reminders = restored.reminders,
                slayerTask = restored.slayerTask,
                checklist = restored.checklist,
                tripTimer = restored.tripTimer,
                priceWatchlist = restored.priceWatchlist,
                trackedPlayer = restored.trackedPlayer,
                priceSearchResults = emptyList(),
                priceLoading = false,
                priceError = null,
                trackedPlayerLoading = false,
                trackedPlayerError = null,
            )
        }
        restored.reminders
            .filter { it.endsAtEpochMillis > System.currentTimeMillis() }
            .forEach { ReminderScheduler.schedule(getApplication(), it) }
        HiscoreRefreshScheduler.sync(
            context = getApplication(),
            enabled = restored.trackedPlayer.autoRefreshEnabled &&
                restored.trackedPlayer.username.isNotBlank(),
        )
        if (restored.priceWatchlist.isNotEmpty()) refreshPrices()
    }

    fun addReminder(title: String, category: ReminderCategory, minutes: Int) {
        if (title.isBlank() || minutes <= 0) return
        captureUndo("Timer added")
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
        captureUndo("Timer removed")
        _state.update { state ->
            state.copy(reminders = state.reminders.filterNot { it.id == reminderId })
        }
        ReminderScheduler.cancel(getApplication(), reminderId)
        persist()
    }

    fun setSlayerTask(monster: String, target: Int) {
        if (monster.isBlank() || target <= 0) return
        captureUndo("Slayer task changed")
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
        captureUndo("Slayer task cleared")
        _state.update { it.copy(slayerTask = null) }
        persist()
    }

    fun addChecklistEntry(title: String, category: ChecklistCategory) {
        if (title.isBlank()) return
        captureUndo("Journal entry added")
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
        captureUndo("Journal entry deleted")
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
        captureUndo("Trip timer reset")
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
        val cleanQuery = query.trim()
        latestPriceQuery = cleanQuery
        priceSearchJob?.cancel()
        if (cleanQuery.length < 2) {
            _state.update {
                it.copy(
                    priceLoading = false,
                    priceSearchResults = emptyList(),
                    priceError = null,
                )
            }
            return
        }
        priceSearchJob = viewModelScope.launch {
            delay(PRICE_SEARCH_DEBOUNCE_MS)
            _state.update { it.copy(priceLoading = true, priceError = null) }
            runCatching { priceClient.search(cleanQuery) }
                .onSuccess { results ->
                    if (cleanQuery != latestPriceQuery) return@onSuccess
                    _state.update {
                        it.copy(priceLoading = false, priceSearchResults = results)
                    }
                }
                .onFailure { error ->
                    if (cleanQuery != latestPriceQuery) return@onFailure
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
        captureUndo("Price watch added")
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
        captureUndo("Price watch removed")
        _state.update {
            it.copy(priceWatchlist = it.priceWatchlist.filterNot { item -> item.id == id })
        }
        persist()
    }

    fun refreshPrices() {
        val watchlist = _state.value.priceWatchlist
        if (watchlist.isEmpty() || priceRefreshJob?.isActive == true) return
        priceRefreshJob = viewModelScope.launch {
            _state.update { it.copy(priceLoading = true, priceError = null) }
            runCatching { priceClient.latest(watchlist) }
                .onSuccess { prices ->
                    val pricesById = prices.associateBy(PriceWatchItem::id)
                    _state.update {
                        it.copy(
                            priceLoading = false,
                            priceWatchlist = it.priceWatchlist.map { item ->
                                pricesById[item.id] ?: item
                            },
                        )
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

    fun saveTrackedPlayer(username: String) {
        val cleanUsername = username.trim().take(MAX_PLAYER_NAME_LENGTH)
        if (cleanUsername.isBlank()) return
        captureUndo("Tracked player changed")
        _state.update { state ->
            val current = state.trackedPlayer
            val samePlayer = current.username.equals(cleanUsername, ignoreCase = true)
            state.copy(
                trackedPlayer = if (samePlayer) {
                    current.copy(username = cleanUsername, autoRefreshEnabled = true)
                } else {
                    TrackedPlayerProfile(
                        username = cleanUsername,
                        autoRefreshEnabled = true,
                    )
                },
                trackedPlayerError = null,
            )
        }
        persist()
        HiscoreRefreshScheduler.sync(getApplication(), enabled = true)
        refreshTrackedPlayer()
    }

    fun setTrackedPlayerAutoRefresh(enabled: Boolean) {
        val canEnable = _state.value.trackedPlayer.username.isNotBlank()
        _state.update { state ->
            state.copy(
                trackedPlayer = state.trackedPlayer.copy(
                    autoRefreshEnabled = enabled && canEnable,
                ),
            )
        }
        persist()
        HiscoreRefreshScheduler.sync(
            context = getApplication(),
            enabled = enabled && canEnable,
        )
        if (enabled && canEnable) refreshTrackedPlayer()
    }

    fun refreshTrackedPlayer() {
        val profile = _state.value.trackedPlayer
        if (
            profile.username.isBlank() ||
            _state.value.trackedPlayerLoading
        ) {
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(trackedPlayerLoading = true, trackedPlayerError = null)
            }
            runCatching { hiscoreClient.lookup(profile.username) }
                .onSuccess { result ->
                    _state.update { state ->
                        if (
                            !state.trackedPlayer.username.equals(
                                profile.username,
                                ignoreCase = true,
                            )
                        ) {
                            state.copy(trackedPlayerLoading = false)
                        } else {
                            state.copy(
                                trackedPlayerLoading = false,
                                trackedPlayer = state.trackedPlayer.copy(
                                    baseline = state.trackedPlayer.baseline ?: result,
                                    latest = result,
                                    lastUpdatedEpochMillis = System.currentTimeMillis(),
                                ),
                            )
                        }
                    }
                    persist()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            trackedPlayerLoading = false,
                            trackedPlayerError = error.message
                                ?: "Could not refresh tracked-player hiscores",
                        )
                    }
                }
        }
    }

    fun resetTrackedPlayerBaseline() {
        captureUndo("Progress baseline reset")
        _state.update { state ->
            val latest = state.trackedPlayer.latest ?: return@update state
            state.copy(
                trackedPlayer = state.trackedPlayer.copy(baseline = latest),
            )
        }
        persist()
    }

    fun clearTrackedPlayer() {
        captureUndo("Tracked player cleared")
        _state.update {
            it.copy(
                trackedPlayer = TrackedPlayerProfile(),
                trackedPlayerLoading = false,
                trackedPlayerError = null,
            )
        }
        persist()
        HiscoreRefreshScheduler.sync(getApplication(), enabled = false)
    }

    fun undoLastChange() {
        val restored = lastUndoData ?: return
        val current = persistedSnapshot()
        lastUndoData = current
        current.reminders.forEach { reminder ->
            ReminderScheduler.cancel(getApplication(), reminder.id)
        }
        _state.update { state ->
            state.copy(
                reminders = restored.reminders,
                slayerTask = restored.slayerTask,
                checklist = restored.checklist,
                tripTimer = restored.tripTimer,
                priceWatchlist = restored.priceWatchlist,
                trackedPlayer = restored.trackedPlayer,
                undoLabel = "Previous change restored",
            )
        }
        restored.reminders
            .filter { it.endsAtEpochMillis > System.currentTimeMillis() }
            .forEach { ReminderScheduler.schedule(getApplication(), it) }
        HiscoreRefreshScheduler.sync(
            getApplication(),
            restored.trackedPlayer.autoRefreshEnabled &&
                restored.trackedPlayer.username.isNotBlank(),
        )
        persist()
    }

    fun dismissUndo() {
        lastUndoData = null
        _state.update { it.copy(undoLabel = null) }
    }

    private fun captureUndo(label: String) {
        lastUndoData = persistedSnapshot()
        _state.update { it.copy(undoLabel = label) }
    }

    private fun persistedSnapshot(): PersistedToolkitData {
        val state = _state.value
        return PersistedToolkitData(
            reminders = state.reminders,
            slayerTask = state.slayerTask,
            checklist = state.checklist,
            tripTimer = state.tripTimer,
            priceWatchlist = state.priceWatchlist,
            trackedPlayer = state.trackedPlayer,
        )
    }

    private fun persist() {
        preferences.save(persistedSnapshot())
    }

    private companion object {
        const val FOREGROUND_HISCORE_INTERVAL_MS = 10 * 60_000L
        const val PRICE_SEARCH_DEBOUNCE_MS = 300L
        const val MAX_PLAYER_NAME_LENGTH = 12
    }
}
