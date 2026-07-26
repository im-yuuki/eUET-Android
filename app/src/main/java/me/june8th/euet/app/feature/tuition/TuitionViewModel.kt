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
import me.june8th.euet.core.data.repository.StudentRepository

class TuitionViewModel(
    private val repository: StudentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Bill>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Bill>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = repository.getBills()) {
                is NetworkResult.Success ->
                    if (r.data.isEmpty()) UiState.Empty else UiState.Data(r.data)
                is NetworkResult.Error -> UiState.Error(r.message)
            }
        }
    }
}
