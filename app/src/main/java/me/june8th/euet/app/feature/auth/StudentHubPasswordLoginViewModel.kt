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
import me.june8th.euet.core.data.repository.AuthRepository
import me.june8th.euet.core.model.CaptchaChallenge

data class StudentHubPasswordLoginUiState(
    val userName: String = "",
    val password: String = "",
    val captchaAnswer: String = "",
    val captcha: CaptchaChallenge? = null,
    val isLoadingCaptcha: Boolean = false,
    val isSubmitting: Boolean = false,
    /**
     * Whether to keep the password (encrypted) on this device. Defaults on, matching the VNU
     * portal path — here it only prefills the form, since the captcha rules out silent renewal.
     */
    val rememberPassword: Boolean = true,
    val error: UiState.Error? = null,
    val isSignedIn: Boolean = false,
) {
    val canSubmit: Boolean
        get() = userName.isNotBlank() &&
            password.isNotBlank() &&
            captchaAnswer.isNotBlank() &&
            captcha != null &&
            !isSubmitting
}

/**
 * Drives the StudentHub student-ID sign-in. A captcha is fetched on creation and re-fetched after
 * every failed attempt, because the portal spends a challenge id on the attempt whether or not it
 * accepts the answer.
 */
class StudentHubPasswordLoginViewModel(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentHubPasswordLoginUiState())
    val uiState: StateFlow<StudentHubPasswordLoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.rememberedStudentHubLogin()?.let { stored ->
                _uiState.update {
                    it.copy(
                        userName = stored.username,
                        password = stored.password.orEmpty(),
                        rememberPassword = stored.password != null,
                    )
                }
            }
        }
        refreshCaptcha()
    }

    fun onUserNameChange(value: String) = _uiState.update { it.copy(userName = value, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun onCaptchaAnswerChange(value: String) =
        _uiState.update { it.copy(captchaAnswer = value, error = null) }

    fun onRememberPasswordChange(value: Boolean) = _uiState.update { it.copy(rememberPassword = value) }

    /** Loads a new challenge, discarding whatever the user had typed for the old one. */
    fun refreshCaptcha() {
        if (_uiState.value.isLoadingCaptcha) return
        _uiState.update { it.copy(isLoadingCaptcha = true, captcha = null, captchaAnswer = "") }
        viewModelScope.launch {
            when (val result = repository.fetchCaptcha()) {
                is NetworkResult.Success ->
                    _uiState.update { it.copy(isLoadingCaptcha = false, captcha = result.data) }
                is NetworkResult.Error ->
                    _uiState.update {
                        it.copy(
                            isLoadingCaptcha = false,
                            error = UiState.Error(result.message, result.kind),
                        )
                    }
            }
        }
    }

    fun submit() {
        val state = _uiState.value
        val captcha = state.captcha ?: return
        if (!state.canSubmit) return
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = repository.loginWithPassword(
                userName = state.userName.trim(),
                password = state.password,
                captchaId = captcha.id,
                captchaValue = state.captchaAnswer.trim(),
                rememberPassword = state.rememberPassword,
            )
            when (result) {
                is NetworkResult.Success ->
                    _uiState.update { it.copy(isSubmitting = false, isSignedIn = true) }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = UiState.Error(result.message, result.kind),
                        )
                    }
                    // The id just used is spent either way; the next attempt needs a new one.
                    refreshCaptcha()
                }
            }
        }
    }
}
