package io.github.taxledgr.runecompanion.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.taxledgr.runecompanion.alerts.StarAlertNotifier
import io.github.taxledgr.runecompanion.alerts.StarAlertPreferences
import io.github.taxledgr.runecompanion.alerts.StarAlertScheduler
import io.github.taxledgr.runecompanion.alerts.StarAlertSettings
import io.github.taxledgr.runecompanion.alerts.StarFilterPreferences
import io.github.taxledgr.runecompanion.alerts.StarFilterSettings
import io.github.taxledgr.runecompanion.alerts.parseLocations
import io.github.taxledgr.runecompanion.alerts.parseWorlds
import io.github.taxledgr.runecompanion.data.ShootingStar
import io.github.taxledgr.runecompanion.data.StarLocationCatalog
import io.github.taxledgr.runecompanion.data.StarRepository
import io.github.taxledgr.runecompanion.data.WorldDirectoryClient
import io.github.taxledgr.runecompanion.data.WorldInfo
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class StarUiState(
    val stars: List<ShootingStar> = emptyList(),
    val fetchedAt: Instant? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCached: Boolean = false,
    val worlds: Map<Int, WorldInfo> = emptyMap(),
)

class StarViewModel(application: Application) : AndroidViewModel(application) {
    private val alertPreferences = StarAlertPreferences(application)
    private val alertNotifier = StarAlertNotifier(application)
    private val filterPreferences = StarFilterPreferences(application)
    private val worldDirectoryClient = WorldDirectoryClient()
    private val _state = MutableStateFlow(StarUiState(isLoading = true))
    val state: StateFlow<StarUiState> = _state.asStateFlow()
    private val _alertSettings = MutableStateFlow(alertPreferences.load())
    val alertSettings: StateFlow<StarAlertSettings> = _alertSettings.asStateFlow()
    private val _filterSettings = MutableStateFlow(filterPreferences.load())
    val filterSettings: StateFlow<StarFilterSettings> = _filterSettings.asStateFlow()
    private var periodicRefreshJob: Job? = null
    private var refreshJob: Job? = null
    private var appInForeground = false

    init {
        StarAlertScheduler.sync(
            context = application,
            enabled = _alertSettings.value.enabled,
        )
        viewModelScope.launch {
            runCatching { worldDirectoryClient.fetch() }
                .onSuccess { worlds ->
                    _state.update { state ->
                        state.copy(worlds = worlds.associateBy(WorldInfo::world))
                    }
                    _filterSettings.value = filterPreferences.updateWorldSafety(worlds)
                }
        }
    }

    fun setAppInForeground(inForeground: Boolean) {
        if (appInForeground == inForeground) return
        appInForeground = inForeground
        periodicRefreshJob?.cancel()
        periodicRefreshJob = null
        if (!inForeground) return

        refresh()
        periodicRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
                refresh()
            }
        }
    }

    fun reloadPreferences() {
        _alertSettings.value = alertPreferences.load()
        _filterSettings.value = filterPreferences.load()
        StarAlertScheduler.sync(
            context = getApplication(),
            enabled = _alertSettings.value.enabled,
        )
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { StarRepository.latest() }
                .onSuccess { feed ->
                    val repositoryStatus = StarRepository.status()
                    _state.value = StarUiState(
                        stars = feed.stars,
                        fetchedAt = feed.fetchedAt,
                        error = repositoryStatus.lastFailureMessage,
                        isCached = repositoryStatus.lastFailureMessage != null,
                        worlds = _state.value.worlds,
                    )
                    alertNotifier.notifyForMatches(feed.stars)
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unable to load Shooting Stars",
                        )
                    }
                }
        }
    }

    fun setAlertWorlds(input: String) {
        updateAlertSettings(
            _alertSettings.value.copy(worlds = parseWorlds(input)),
        )
    }

    fun setHideDangerousWorlds(hide: Boolean) {
        updateFilterSettings(_filterSettings.value.copy(hideDangerousWorlds = hide))
    }

    fun setLocationSelected(location: String, selected: Boolean) {
        if (location !in StarLocationCatalog.allNames) return
        val current = _filterSettings.value
        val excluded = if (selected) {
            current.excludedLocations - location
        } else {
            current.excludedLocations + location
        }
        updateFilterSettings(current.copy(excludedLocations = excluded))
    }

    fun setLocationsSelected(locations: Collection<String>, selected: Boolean) {
        val validLocations = locations.filter { it in StarLocationCatalog.allNames }.toSet()
        val current = _filterSettings.value
        val excluded = if (selected) {
            current.excludedLocations - validLocations
        } else {
            current.excludedLocations + validLocations
        }
        updateFilterSettings(current.copy(excludedLocations = excluded))
    }

    fun setAllLocationsSelected(selected: Boolean) {
        updateFilterSettings(
            _filterSettings.value.copy(
                excludedLocations = if (selected) {
                    emptySet()
                } else {
                    StarLocationCatalog.allNames.toSet()
                },
            ),
        )
    }

    fun setAlertLocations(input: String) {
        updateAlertSettings(
            _alertSettings.value.copy(locations = parseLocations(input)),
        )
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        updateAlertSettings(_alertSettings.value.copy(quietHoursEnabled = enabled))
    }

    fun setQuietHours(startHour: Int, endHour: Int) {
        updateAlertSettings(
            _alertSettings.value.copy(
                quietStartHour = startHour.coerceIn(0, 23),
                quietEndHour = endHour.coerceIn(0, 23),
            ),
        )
    }

    fun toggleAlertTier(tier: Int) {
        if (tier !in 1..9) return
        val current = _alertSettings.value
        val nextTiers = if (tier in current.tiers) {
            current.tiers - tier
        } else {
            current.tiers + tier
        }
        updateAlertSettings(current.copy(tiers = nextTiers))
    }

    fun clearAlertTiers() {
        updateAlertSettings(_alertSettings.value.copy(tiers = emptySet()))
    }

    fun setAlertsEnabled(enabled: Boolean) {
        updateAlertSettings(_alertSettings.value.copy(enabled = enabled))
    }

    private fun updateAlertSettings(settings: StarAlertSettings) {
        if (settings == _alertSettings.value) return
        alertPreferences.save(settings)
        alertPreferences.clearSeenIds()
        alertNotifier.cancel()
        _alertSettings.value = settings
        StarAlertScheduler.sync(
            context = getApplication(),
            enabled = settings.enabled,
            runImmediately = settings.enabled,
        )
    }

    private fun updateFilterSettings(settings: StarFilterSettings) {
        if (settings == _filterSettings.value) return
        filterPreferences.save(settings)
        _filterSettings.value = settings
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 60_000L
    }
}
