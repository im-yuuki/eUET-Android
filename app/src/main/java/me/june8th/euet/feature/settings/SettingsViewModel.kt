package me.june8th.euet.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.june8th.euet.data.repository.AuthRepository

class SettingsViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
