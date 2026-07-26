package me.june8th.euet.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.model.GpaSummary
import me.june8th.euet.core.model.TimetableEntry
import me.june8th.euet.core.data.repository.AggregateRepository
import me.june8th.euet.core.datastore.SnapshotCache
import me.june8th.euet.core.datastore.load
import me.june8th.euet.core.datastore.save
import java.util.Calendar

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val profileName: String? = null,
    val gpa: GpaSummary? = null,
    val todayClasses: List<TimetableEntry> = emptyList(),
)

/**
 * Cached home summary. The active term's *whole* timetable is stored (not just today's slice), so
 * a snapshot saved on Monday still shows the right classes when opened offline on Tuesday.
 */
@Serializable
private data class HomeSnapshot(
    val profileName: String?,
    val gpa: GpaSummary?,
    val termClasses: List<TimetableEntry> = emptyList(),
)

class HomeViewModel(
    private val repository: AggregateRepository,
    private val cache: SnapshotCache,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { load() }

    /** Offline-first: the cached summary renders instantly, then the network refreshes it. */
    fun load() {
        _state.value = HomeUiState(loading = true)
        viewModelScope.launch {
            cache.load<HomeSnapshot>(CACHE_KEY)?.value?.let { snap ->
                _state.value = HomeUiState(
                    loading = false,
                    profileName = snap.profileName,
                    gpa = snap.gpa,
                    todayClasses = todayClasses(snap.termClasses),
                )
            }
            fetch()
        }
    }

    /** Pull-to-refresh: re-fetch without collapsing the screen into its loading state. */
    fun refresh() {
        if (_state.value.refreshing) return
        _state.value = _state.value.copy(refreshing = true)
        viewModelScope.launch { fetch() }
    }

    private suspend fun fetch() {
        coroutineScope {
            val profileDeferred = async { repository.getProfile() }
            val gpaDeferred = async { repository.getGpaSummary() }
            val termsDeferred = async { repository.getTermsWithActive() }

            val profileResult = profileDeferred.await()
            val gpa = (gpaDeferred.await() as? NetworkResult.Success)?.data

            val termClasses = when (val terms = termsDeferred.await()) {
                is NetworkResult.Success -> {
                    val active = terms.data.second
                    if (active != null) {
                        (repository.getTimetable(active) as? NetworkResult.Success)?.data.orEmpty()
                    } else emptyList()
                }
                is NetworkResult.Error -> emptyList()
            }

            when (profileResult) {
                is NetworkResult.Success -> {
                    val snapshot = HomeSnapshot(
                        profileName = profileResult.data.value.name.ifBlank { null },
                        gpa = gpa,
                        termClasses = termClasses,
                    )
                    cache.save(CACHE_KEY, snapshot)
                    _state.value = HomeUiState(
                        loading = false,
                        profileName = snapshot.profileName,
                        gpa = gpa,
                        todayClasses = todayClasses(termClasses),
                    )
                }
                is NetworkResult.Error -> {
                    val current = _state.value
                    val hasCachedContent = current.error == null && !current.loading &&
                        (current.profileName != null || current.gpa != null || current.todayClasses.isNotEmpty())
                    _state.value = if (hasCachedContent) {
                        // Keep the cached summary on screen; a refresh failure stays quiet.
                        current.copy(refreshing = false, gpa = gpa ?: current.gpa)
                    } else {
                        HomeUiState(
                            loading = false,
                            error = profileResult.message,
                            gpa = gpa,
                            todayClasses = todayClasses(termClasses),
                        )
                    }
                }
            }
        }
    }

    private fun todayClasses(termClasses: List<TimetableEntry>): List<TimetableEntry> {
        val today = vnWeekdayToday()
        return termClasses.filter { it.weekday == today }.sortedBy { it.sessionStart ?: 0 }
    }

    /** Maps today onto the StudentHub weekday scheme (Mon=2 … Sat=7, Sun=8). */
    private fun vnWeekdayToday(): Int {
        val cal = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) // Sun=1 … Sat=7
        return if (cal == Calendar.SUNDAY) 8 else cal
    }

    private companion object {
        const val CACHE_KEY = "home.summary"
    }
}
