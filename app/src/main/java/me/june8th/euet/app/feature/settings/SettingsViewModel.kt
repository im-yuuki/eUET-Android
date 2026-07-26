package me.june8th.euet.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.june8th.euet.core.data.repository.AuthRepository

class SettingsViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
