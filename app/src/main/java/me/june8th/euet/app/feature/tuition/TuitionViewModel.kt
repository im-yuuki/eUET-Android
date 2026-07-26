package me.june8th.euet.app.feature.tuition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.app.common.UiState
import me.june8th.euet.core.model.Bill
import me.june8th.euet.core.data.repository.AggregateRepository
import me.june8th.euet.core.datastore.SnapshotCache
import me.june8th.euet.core.datastore.load
import me.june8th.euet.core.datastore.save

class TuitionViewModel(
    private val repository: AggregateRepository,
    private val cache: SnapshotCache,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Bill>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Bill>>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init { load() }

    /** Offline-first: render the cached bills instantly (if any), then refresh from network. */
    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            cache.load<List<Bill>>(CACHE_KEY)?.value
                ?.takeIf { it.isNotEmpty() }
                ?.let { _state.value = UiState.Data(it) }
            applyFetch()
        }
    }

    /** Pull-to-refresh: re-fetch without dropping the visible list into a loading state. */
    fun refresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            applyFetch()
            _refreshing.value = false
        }
    }

    private suspend fun applyFetch() {
        when (val r = repository.getBills()) {
            is NetworkResult.Success -> {
                cache.save(CACHE_KEY, r.data)
                _state.value = if (r.data.isEmpty()) UiState.Empty else UiState.Data(r.data)
            }
            // Keep the cached/current list on screen; only error out when there is nothing.
            is NetworkResult.Error ->
                if (_state.value !is UiState.Data) _state.value = UiState.Error(r.message, r.kind)
        }
    }

    private companion object {
        const val CACHE_KEY = "tuition.bills"
    }
}
