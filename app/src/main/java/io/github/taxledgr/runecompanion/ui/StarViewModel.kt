package io.github.taxledgr.runecompanion.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.taxledgr.runecompanion.alerts.StarAlertNotifier
import io.github.taxledgr.runecompanion.alerts.StarAlertPreferences
import io.github.taxledgr.runecompanion.alerts.StarAlertScheduler
import io.github.taxledgr.runecompanion.alerts.StarAlertSettings
import io.github.taxledgr.runecompanion.alerts.parseLocations
import io.github.taxledgr.runecompanion.alerts.parseWorlds
import io.github.taxledgr.runecompanion.data.ShootingStar
import io.github.taxledgr.runecompanion.data.StarRepository
import io.github.taxledgr.runecompanion.data.WorldDirectoryClient
import io.github.taxledgr.runecompanion.data.WorldInfo
import java.time.Instant
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
    val worlds: Map<Int, WorldInfo> = emptyMap(),
)

class StarViewModel(application: Application) : AndroidViewModel(application) {
    private val alertPreferences = StarAlertPreferences(application)
    private val alertNotifier = StarAlertNotifier(application)
    private val worldDirectoryClient = WorldDirectoryClient()
    private val _state = MutableStateFlow(StarUiState(isLoading = true))
    val state: StateFlow<StarUiState> = _state.asStateFlow()
    private val _alertSettings = MutableStateFlow(alertPreferences.load())
    val alertSettings: StateFlow<StarAlertSettings> = _alertSettings.asStateFlow()

    init {
        StarAlertScheduler.sync(
            context = application,
            enabled = _alertSettings.value.enabled,
        )
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(REFRESH_INTERVAL_MS)
            }
        }
        viewModelScope.launch {
            runCatching { worldDirectoryClient.fetch() }
                .onSuccess { worlds ->
                    _state.update { state ->
                        state.copy(worlds = worlds.associateBy(WorldInfo::world))
                    }
                }
        }
    }

    fun refresh() {
        if (_state.value.isLoading && _state.value.stars.isNotEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { StarRepository.latest() }
                .onSuccess { feed ->
                    _state.value = StarUiState(
                        stars = feed.stars,
                        fetchedAt = feed.fetchedAt,
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

    private companion object {
        const val REFRESH_INTERVAL_MS = 60_000L
    }
}
