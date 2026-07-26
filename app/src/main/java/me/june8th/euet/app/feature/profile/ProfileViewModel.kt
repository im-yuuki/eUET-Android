package me.june8th.euet.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.app.common.UiState
import me.june8th.euet.core.model.StudentProfile
import me.june8th.euet.core.data.repository.StudentRepository

class ProfileViewModel(
    private val repository: StudentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<StudentProfile>>(UiState.Loading)
    val state: StateFlow<UiState<StudentProfile>> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = repository.getProfile()) {
                is NetworkResult.Success -> UiState.Data(r.data)
                is NetworkResult.Error -> UiState.Error(r.message)
            }
        }
    }
}
