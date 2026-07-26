package me.june8th.euet.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.app.common.UiState
import me.june8th.euet.core.model.ConflictReport
import me.june8th.euet.core.model.StudentProfile
import me.june8th.euet.core.data.repository.AggregateRepository
import me.june8th.euet.core.datastore.SnapshotCache
import me.june8th.euet.core.datastore.load
import me.june8th.euet.core.datastore.save

data class ProfileUiState(
    val content: UiState<StudentProfile> = UiState.Loading,
    /** Dual-source disagreements; null when the sources agree (or only one is connected). */
    val conflicts: ConflictReport? = null,
)

class ProfileViewModel(
    private val repository: AggregateRepository,
    private val cache: SnapshotCache,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { load() }

    /** Offline-first: render the cached profile instantly (if any), then refresh from network. */
    fun load() {
        _state.value = ProfileUiState()
        viewModelScope.launch {
            val cachedConflicts = cache.load<ConflictReport>(KEY_CONFLICTS)?.value
                ?.takeIf { it.conflicts.isNotEmpty() }
            cache.load<StudentProfile>(CACHE_KEY)?.value?.let { cached ->
                _state.value = ProfileUiState(UiState.Data(cached), cachedConflicts)
            } ?: cachedConflicts?.let { _state.update { s -> s.copy(conflicts = it) } }
            when (val r = repository.getProfile(::onConflicts)) {
                is NetworkResult.Success -> {
                    cache.save(CACHE_KEY, r.data.value)
                    _state.update { it.copy(content = UiState.Data(r.data.value)) }
                }
                // Keep the cached profile on screen; only error out when there is nothing.
                is NetworkResult.Error -> _state.update {
                    if (it.content is UiState.Data) it else it.copy(content = UiState.Error(r.message, r.kind))
                }
            }
        }
    }

    /**
     * Secondary-source comparison finished (possibly after [load] returned). Cached beside the
     * profile so the banner survives offline restarts; overwritten on each successful dual fetch.
     */
    private suspend fun onConflicts(report: ConflictReport) {
        cache.save(KEY_CONFLICTS, report)
        _state.update { it.copy(conflicts = report.takeIf { r -> r.conflicts.isNotEmpty() }) }
    }

    private companion object {
        const val CACHE_KEY = "profile.detail"
        const val KEY_CONFLICTS = "profile.conflicts"
    }
}
