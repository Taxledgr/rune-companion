package io.github.taxledgr.runecompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taxledgr.runecompanion.data.ShootingStar
import io.github.taxledgr.runecompanion.data.StarRepository
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
)

class StarViewModel : ViewModel() {
    private val _state = MutableStateFlow(StarUiState(isLoading = true))
    val state: StateFlow<StarUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(REFRESH_INTERVAL_MS)
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
                    )
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

    private companion object {
        const val REFRESH_INTERVAL_MS = 60_000L
    }
}
