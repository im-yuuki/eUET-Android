package me.june8th.euet.feature.exams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.common.UiState
import me.june8th.euet.core.model.Exam
import me.june8th.euet.core.model.Term
import me.june8th.euet.data.repository.StudentRepository

data class ExamsUiState(
    val terms: List<Term> = emptyList(),
    val selectedTerm: String? = null,
    val content: UiState<List<Exam>> = UiState.Loading,
)

class ExamsViewModel(
    private val repository: StudentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExamsUiState())
    val state: StateFlow<ExamsUiState> = _state.asStateFlow()

    init { loadTerms() }

    private fun loadTerms() {
        viewModelScope.launch {
            when (val r = repository.getTermsWithActive()) {
                is NetworkResult.Success -> {
                    val (terms, active) = r.data
                    _state.update { it.copy(terms = terms, selectedTerm = active) }
                    active?.let { loadExams(it) }
                        ?: _state.update { it.copy(content = UiState.Empty) }
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(content = UiState.Error(r.message)) }
            }
        }
    }

    fun selectTerm(termCode: String) {
        if (termCode == _state.value.selectedTerm) return
        _state.update { it.copy(selectedTerm = termCode) }
        loadExams(termCode)
    }

    fun loadExams(termCode: String) {
        _state.update { it.copy(content = UiState.Loading) }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    content = when (val r = repository.getExams(termCode)) {
                        is NetworkResult.Success ->
                            if (r.data.isEmpty()) UiState.Empty else UiState.Data(r.data)
                        is NetworkResult.Error -> UiState.Error(r.message)
                    },
                )
            }
        }
    }

    fun retry() {
        val term = _state.value.selectedTerm
        if (term != null) loadExams(term) else loadTerms()
    }
}
