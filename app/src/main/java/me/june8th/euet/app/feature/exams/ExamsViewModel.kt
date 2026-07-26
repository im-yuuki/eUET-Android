package me.june8th.euet.app.feature.exams

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
import me.june8th.euet.core.model.ConflictReport
import me.june8th.euet.core.model.Exam
import me.june8th.euet.core.model.Term
import me.june8th.euet.core.data.repository.AggregateRepository
import me.june8th.euet.core.datastore.SnapshotCache
import me.june8th.euet.core.datastore.load
import me.june8th.euet.core.datastore.save

data class ExamsUiState(
    val terms: List<Term> = emptyList(),
    val selectedTerm: String? = null,
    val content: UiState<List<Exam>> = UiState.Loading,
    /** Dual-source disagreements for the selected term; null when the sources agree. */
    val conflicts: ConflictReport? = null,
    val refreshing: Boolean = false,
)

/** Cached term picker: the term list plus the term that was active when it was saved. */
@Serializable
private data class TermsSnapshot(
    val terms: List<Term>,
    val active: String?,
)

class ExamsViewModel(
    private val repository: AggregateRepository,
    private val cache: SnapshotCache,
) : ViewModel() {

    private val _state = MutableStateFlow(ExamsUiState())
    val state: StateFlow<ExamsUiState> = _state.asStateFlow()

    init { loadTerms() }

    /** Offline-first: cached terms and exams render instantly, then the network refreshes. */
    private fun loadTerms() {
        viewModelScope.launch {
            cache.load<TermsSnapshot>(KEY_TERMS)?.value?.takeIf { it.terms.isNotEmpty() }?.let { snap ->
                _state.update { it.copy(terms = snap.terms, selectedTerm = snap.active) }
                snap.active?.let { showCachedExams(it) }
            }
            when (val r = repository.getExamTermsWithActive()) {
                is NetworkResult.Success -> {
                    val (terms, active) = r.data
                    cache.save(KEY_TERMS, TermsSnapshot(terms, active))
                    _state.update { it.copy(terms = terms, selectedTerm = active) }
                    active?.let { fetchExams(it, keepOnError = true) }
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
        _state.update { it.copy(selectedTerm = termCode, conflicts = null) }
        loadExams(termCode)
    }

    fun loadExams(termCode: String) {
        viewModelScope.launch {
            val hadCache = showCachedExams(termCode)
            if (!hadCache) _state.update { it.copy(content = UiState.Loading) }
            fetchExams(termCode, keepOnError = hadCache)
        }
    }

    fun retry() {
        val term = _state.value.selectedTerm
        if (term != null) loadExams(term) else loadTerms()
    }

    /** Pull-to-refresh: re-fetch the current term without dropping the visible list. */
    fun refresh() {
        if (_state.value.refreshing) return
        val term = _state.value.selectedTerm ?: run { loadTerms(); return }
        _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            fetchExams(term, keepOnError = true)
            _state.update { it.copy(refreshing = false) }
        }
    }

    /** Puts the cached exams (and conflicts) for [termCode] on screen; true when data existed. */
    private suspend fun showCachedExams(termCode: String): Boolean {
        val cachedConflicts = cache.load<ConflictReport>(conflictsKey(termCode))?.value
            ?.takeIf { it.conflicts.isNotEmpty() }
        val cached = cache.load<List<Exam>>(examsKey(termCode))?.value
            ?.takeIf { it.isNotEmpty() }
        if (cached == null) {
            if (cachedConflicts != null && _state.value.selectedTerm == termCode) {
                _state.update { it.copy(conflicts = cachedConflicts) }
            }
            return false
        }
        _state.update { it.copy(content = UiState.Data(cached), conflicts = cachedConflicts) }
        return true
    }

    private suspend fun fetchExams(termCode: String, keepOnError: Boolean) {
        val result = repository.getExams(termCode) { report -> onConflicts(termCode, report) }
        val content = when (result) {
            is NetworkResult.Success -> {
                cache.save(examsKey(termCode), result.data.value)
                if (result.data.value.isEmpty()) UiState.Empty else UiState.Data(result.data.value)
            }
            is NetworkResult.Error ->
                // Keep the current list if the fetch fails while data is on screen.
                if (keepOnError && _state.value.content is UiState.Data) _state.value.content
                else UiState.Error(result.message, result.kind)
        }
        // Don't clobber the screen if the user switched terms while this fetch was in flight.
        if (_state.value.selectedTerm == termCode) _state.update { it.copy(content = content) }
    }

    /**
     * Secondary-source comparison finished (possibly after [fetchExams] returned). Cached per
     * term so banners survive offline restarts; overwritten on each successful dual fetch.
     */
    private suspend fun onConflicts(termCode: String, report: ConflictReport) {
        cache.save(conflictsKey(termCode), report)
        if (_state.value.selectedTerm == termCode) {
            _state.update { it.copy(conflicts = report.takeIf { r -> r.conflicts.isNotEmpty() }) }
        }
    }

    private fun examsKey(termCode: String) = "exams.$termCode"

    private fun conflictsKey(termCode: String) = "exams.$termCode.conflicts"

    private companion object {
        const val KEY_TERMS = "exams.terms"
    }
}
