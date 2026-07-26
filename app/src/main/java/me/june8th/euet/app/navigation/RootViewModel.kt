package me.june8th.euet.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.june8th.euet.core.datastore.SessionManager

enum class AuthState { Loading, LoggedIn, LoggedOut }

class RootViewModel(
    session: SessionManager,
) : ViewModel() {
    val authState: StateFlow<AuthState> = session.isLoggedIn
        .map { if (it) AuthState.LoggedIn else AuthState.LoggedOut }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)
}
