package me.june8th.euet.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.june8th.euet.app.common.UiState
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.data.repository.DaotaoRepository

data class DaotaoLoginUiState(
    val username: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: UiState.Error? = null,
    val isSignedIn: Boolean = false,
) {
    val canSubmit: Boolean
        get() = username.isNotBlank() && password.isNotBlank() && !isSubmitting
}

class DaotaoLoginViewModel(
    private val repository: DaotaoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DaotaoLoginUiState())
    val uiState: StateFlow<DaotaoLoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.login(state.username.trim(), state.password)) {
                is NetworkResult.Success ->
                    _uiState.update { it.copy(isSubmitting = false, isSignedIn = true) }
                is NetworkResult.Error ->
                    _uiState.update {
                        it.copy(isSubmitting = false, error = UiState.Error(result.message, result.kind))
                    }
            }
        }
    }
}
