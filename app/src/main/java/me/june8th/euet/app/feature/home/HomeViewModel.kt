package me.june8th.euet.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.model.GpaSummary
import me.june8th.euet.core.model.TimetableEntry
import me.june8th.euet.core.data.repository.StudentRepository
import java.util.Calendar

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val profileName: String? = null,
    val gpa: GpaSummary? = null,
    val todayClasses: List<TimetableEntry> = emptyList(),
)

class HomeViewModel(
    private val repository: StudentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = HomeUiState(loading = true)
        viewModelScope.launch {
            val profileDeferred = async { repository.getProfile() }
            val gpaDeferred = async { repository.getGpaSummary() }
            val termsDeferred = async { repository.getTermsWithActive() }

            val profileResult = profileDeferred.await()
            val gpa = (gpaDeferred.await() as? NetworkResult.Success)?.data

            val today = vnWeekdayToday()
            val todayClasses = when (val terms = termsDeferred.await()) {
                is NetworkResult.Success -> {
                    val active = terms.data.second
                    if (active != null) {
                        (repository.getTimetable(active) as? NetworkResult.Success)
                            ?.data?.filter { it.weekday == today }
                            ?.sortedBy { it.sessionStart ?: 0 }
                            .orEmpty()
                    } else emptyList()
                }
                is NetworkResult.Error -> emptyList()
            }

            _state.value = when (profileResult) {
                is NetworkResult.Success -> HomeUiState(
                    loading = false,
                    profileName = profileResult.data.name.ifBlank { null },
                    gpa = gpa,
                    todayClasses = todayClasses,
                )
                is NetworkResult.Error -> HomeUiState(loading = false, error = profileResult.message, gpa = gpa, todayClasses = todayClasses)
            }
        }
    }

    /** Maps today onto the StudentHub weekday scheme (Mon=2 … Sat=7, Sun=8). */
    private fun vnWeekdayToday(): Int {
        val cal = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) // Sun=1 … Sat=7
        return if (cal == Calendar.SUNDAY) 8 else cal
    }
}
