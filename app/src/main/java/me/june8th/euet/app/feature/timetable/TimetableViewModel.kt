package me.june8th.euet.app.feature.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.app.common.UiState
import me.june8th.euet.core.model.Term
import me.june8th.euet.core.model.TimetableEntry
import me.june8th.euet.core.data.repository.AggregateRepository
import me.june8th.euet.core.datastore.SnapshotCache
import me.june8th.euet.core.datastore.load
import me.june8th.euet.core.datastore.save

data class TimetableUiState(
    val terms: List<Term> = emptyList(),
    val selectedTerm: String? = null,
    val content: UiState<List<TimetableEntry>> = UiState.Loading,
    val refreshing: Boolean = false,
)

/** Cached term picker: the term list plus the term that was active when it was saved. */
@Serializable
private data class TermsSnapshot(
    val terms: List<Term>,
    val active: String?,
)

class TimetableViewModel(
    private val repository: AggregateRepository,
    private val cache: SnapshotCache,
) : ViewModel() {

    private val _state = MutableStateFlow(TimetableUiState())
    val state: StateFlow<TimetableUiState> = _state.asStateFlow()

    init { loadTerms() }

    /** Offline-first: cached terms and timetable render instantly, then the network refreshes. */
    private fun loadTerms() {
        viewModelScope.launch {
            cache.load<TermsSnapshot>(KEY_TERMS)?.value?.takeIf { it.terms.isNotEmpty() }?.let { snap ->
                _state.update { it.copy(terms = snap.terms, selectedTerm = snap.active) }
                snap.active?.let { showCachedTimetable(it) }
            }
            when (val r = repository.getTermsWithActive()) {
                is NetworkResult.Success -> {
                    val (terms, active) = r.data
                    cache.save(KEY_TERMS, TermsSnapshot(terms, active))
                    _state.update { it.copy(terms = terms, selectedTerm = active) }
                    active?.let { fetchTimetable(it, keepOnError = true) }
                        ?: _state.update { it.copy(content = UiState.Empty) }
                }
                is NetworkResult.Error -> _state.update {
                    // Keep the cached screen when the term fetch fails; error only when empty.
                    if (it.content is UiState.Data) it else it.copy(content = UiState.Error(r.message, r.kind))
                }
            }
        }
    }

    fun selectTerm(termCode: String) {
        if (termCode == _state.value.selectedTerm) return
        _state.update { it.copy(selectedTerm = termCode) }
        viewModelScope.launch { repository.setActiveTerm(termCode) }
        loadTimetable(termCode)
    }

    fun loadTimetable(termCode: String) {
        viewModelScope.launch {
            val hadCache = showCachedTimetable(termCode)
            if (!hadCache) _state.update { it.copy(content = UiState.Loading) }
            fetchTimetable(termCode, keepOnError = hadCache)
        }
    }

    fun retry() {
        val term = _state.value.selectedTerm
        if (term != null) loadTimetable(term) else loadTerms()
    }

    /** Pull-to-refresh: re-fetch the current term without dropping the visible list. */
    fun refresh() {
        if (_state.value.refreshing) return
        val term = _state.value.selectedTerm ?: run { loadTerms(); return }
        _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            fetchTimetable(term, keepOnError = true)
            _state.update { it.copy(refreshing = false) }
        }
    }

    /** Puts the cached timetable for [termCode] on screen; true when one existed. */
    private suspend fun showCachedTimetable(termCode: String): Boolean {
        val cached = cache.load<List<TimetableEntry>>(timetableKey(termCode))?.value
            ?.takeIf { it.isNotEmpty() }
            ?: return false
        _state.update { it.copy(content = UiState.Data(cached)) }
        return true
    }

    private suspend fun fetchTimetable(termCode: String, keepOnError: Boolean) {
        val content = when (val r = repository.getTimetable(termCode)) {
            is NetworkResult.Success -> {
                cache.save(timetableKey(termCode), r.data)
                if (r.data.isEmpty()) UiState.Empty else UiState.Data(r.data)
            }
            is NetworkResult.Error ->
                // Keep the current list if the fetch fails while data is on screen.
                if (keepOnError && _state.value.content is UiState.Data) _state.value.content
                else UiState.Error(r.message, r.kind)
        }
        // Don't clobber the screen if the user switched terms while this fetch was in flight.
        if (_state.value.selectedTerm == termCode) _state.update { it.copy(content = content) }
    }

    private fun timetableKey(termCode: String) = "timetable.$termCode"

    private companion object {
        const val KEY_TERMS = "timetable.terms"
    }
}
