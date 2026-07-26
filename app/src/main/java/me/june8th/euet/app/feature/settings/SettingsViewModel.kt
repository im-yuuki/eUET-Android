package me.june8th.euet.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.june8th.euet.core.data.repository.AuthRepository
import me.june8th.euet.core.datastore.SessionManager
import me.june8th.euet.core.model.SourceId

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val session: SessionManager,
) : ViewModel() {

    /** The source preferred for the capabilities both providers serve (profile, grades, exams). */
    val preferredSource: StateFlow<SourceId> = session.preferredSource
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SourceId.STUDENT_HUB)

    fun setPreferredSource(source: SourceId) {
        viewModelScope.launch { session.savePreferredSource(source) }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
