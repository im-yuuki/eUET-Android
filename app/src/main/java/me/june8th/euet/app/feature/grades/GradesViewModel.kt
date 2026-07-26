package me.june8th.euet.app.feature.grades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.app.common.UiState
import me.june8th.euet.core.model.ConflictReport
import me.june8th.euet.core.model.GpaSummary
import me.june8th.euet.core.model.TermGrades
import me.june8th.euet.core.data.repository.AggregateRepository
import me.june8th.euet.core.datastore.SnapshotCache
import me.june8th.euet.core.datastore.load
import me.june8th.euet.core.datastore.save

data class GradesUiState(
    val gpa: GpaSummary? = null,
    val content: UiState<List<TermGrades>> = UiState.Loading,
    /** Dual-source disagreements; null when the sources agree (or only one is connected). */
    val conflicts: ConflictReport? = null,
    val refreshing: Boolean = false,
)

class GradesViewModel(
    private val repository: AggregateRepository,
    private val cache: SnapshotCache,
) : ViewModel() {

    private val _state = MutableStateFlow(GradesUiState())
    val state: StateFlow<GradesUiState> = _state.asStateFlow()

    init { load() }

    /** Offline-first: render the cached transcript instantly (if any), then refresh from network. */
    fun load() {
        _state.value = GradesUiState(content = UiState.Loading)
        viewModelScope.launch {
            val cachedGpa = cache.load<GpaSummary>(KEY_GPA)?.value
            val cachedTranscript = cache.load<List<TermGrades>>(KEY_TRANSCRIPT)?.value
                ?.takeIf { it.isNotEmpty() }
            val cachedConflicts = cache.load<ConflictReport>(KEY_CONFLICTS)?.value
                ?.takeIf { it.conflicts.isNotEmpty() }
            if (cachedGpa != null || cachedTranscript != null || cachedConflicts != null) {
                _state.value = GradesUiState(
                    gpa = cachedGpa,
                    content = cachedTranscript?.let { UiState.Data(it) } ?: UiState.Loading,
                    conflicts = cachedConflicts,
                )
            }
            fetch(keepDataOnError = cachedTranscript != null)
        }
    }

    /** Pull-to-refresh: re-fetch without dropping the visible list into a loading state. */
    fun refresh() {
        if (_state.value.refreshing) return
        _state.value = _state.value.copy(refreshing = true)
        viewModelScope.launch { fetch(keepDataOnError = true) }
    }

    private suspend fun fetch(keepDataOnError: Boolean = false) {
        val gpaDeferred = viewModelScope.async { repository.getGpaSummary() }
        val transcriptDeferred = viewModelScope.async { repository.getTranscript(::onConflicts) }

        val gpa = (gpaDeferred.await() as? NetworkResult.Success)?.data
        gpa?.let { cache.save(KEY_GPA, it) }
        val content = when (val r = transcriptDeferred.await()) {
            is NetworkResult.Success -> {
                cache.save(KEY_TRANSCRIPT, r.data.value)
                if (r.data.value.isEmpty()) UiState.Empty else UiState.Data(r.data.value)
            }
            is NetworkResult.Error ->
                if (keepDataOnError && _state.value.content is UiState.Data) _state.value.content
                else UiState.Error(r.message, r.kind)
        }
        _state.update { it.copy(gpa = gpa ?: it.gpa, content = content, refreshing = false) }
    }

    /**
     * Secondary-source comparison finished (may land after [fetch] returned — the repository
     * never blocks the screen on it). Cached under a sibling key so banners survive offline
     * restarts; each successful dual fetch overwrites the previous report.
     */
    private suspend fun onConflicts(report: ConflictReport) {
        cache.save(KEY_CONFLICTS, report)
        _state.update { it.copy(conflicts = report.takeIf { r -> r.conflicts.isNotEmpty() }) }
    }

    private companion object {
        const val KEY_GPA = "grades.gpa"
        const val KEY_TRANSCRIPT = "grades.transcript"
        const val KEY_CONFLICTS = "grades.conflicts"
    }
}
