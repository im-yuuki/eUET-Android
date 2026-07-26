package me.june8th.euet.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.data.repository.AuthRepository

sealed interface LoginUiState {
    data object SignIn : LoginUiState
    data object Verifying : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.SignIn)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var handled = false

    /** Called (once) when the WebView intercepts a bearer token from an API request. */
    fun onTokenCaptured(token: String) {
        if (handled) return
        handled = true
        _uiState.value = LoginUiState.Verifying
        viewModelScope.launch {
            when (val result = authRepository.onTokenCaptured(token)) {
                is NetworkResult.Success -> _uiState.value = LoginUiState.Success
                is NetworkResult.Error -> {
                    handled = false
                    _uiState.value = LoginUiState.Error(result.message)
                }
            }
        }
    }

    fun retry() {
        handled = false
        _uiState.value = LoginUiState.SignIn
    }
}
