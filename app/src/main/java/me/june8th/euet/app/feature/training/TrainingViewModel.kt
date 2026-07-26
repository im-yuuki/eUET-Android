package me.june8th.euet.app.feature.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.june8th.euet.app.common.UiState
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.data.repository.AggregateRepository
import me.june8th.euet.core.datastore.SnapshotCache
import me.june8th.euet.core.datastore.load
import me.june8th.euet.core.datastore.save
import me.june8th.euet.core.model.TermPerformance

class TrainingViewModel(
    private val repository: AggregateRepository,
    private val cache: SnapshotCache,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<TermPerformance>>>(UiState.Loading)
    val state: StateFlow<UiState<List<TermPerformance>>> = _state.asStateFlow()

    init { load() }

    /** Offline-first: render the cached terms instantly (if any), then refresh from the portal. */
    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            cache.load<List<TermPerformance>>(CACHE_KEY)?.value
                ?.takeIf { it.isNotEmpty() }
                ?.let { _state.value = UiState.Data(it) }
            when (val r = repository.getTrainingPoints()) {
                is NetworkResult.Success -> {
                    cache.save(CACHE_KEY, r.data)
                    _state.value = if (r.data.isEmpty()) UiState.Empty else UiState.Data(r.data)
                }
                // Keep the cached list on screen; only error out when there is nothing.
                is NetworkResult.Error ->
                    if (_state.value !is UiState.Data) _state.value = UiState.Error(r.message, r.kind)
            }
        }
    }

    private companion object {
        const val CACHE_KEY = "training.points"
    }
}
