package me.june8th.euet.feature.grades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.common.UiState
import me.june8th.euet.core.model.GpaSummary
import me.june8th.euet.core.model.TermGrades
import me.june8th.euet.data.repository.StudentRepository

data class GradesUiState(
    val gpa: GpaSummary? = null,
    val content: UiState<List<TermGrades>> = UiState.Loading,
)

class GradesViewModel(
    private val repository: StudentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GradesUiState())
    val state: StateFlow<GradesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = GradesUiState(content = UiState.Loading)
        viewModelScope.launch {
            val gpaDeferred = async { repository.getGpaSummary() }
            val transcriptDeferred = async { repository.getTranscript() }

            val gpa = (gpaDeferred.await() as? NetworkResult.Success)?.data
            val content = when (val r = transcriptDeferred.await()) {
                is NetworkResult.Success ->
                    if (r.data.isEmpty()) UiState.Empty else UiState.Data(r.data)
                is NetworkResult.Error -> UiState.Error(r.message)
            }
            _state.value = GradesUiState(gpa = gpa, content = content)
        }
    }
}
