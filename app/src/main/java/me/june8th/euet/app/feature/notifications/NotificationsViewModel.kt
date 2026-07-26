package me.june8th.euet.app.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.app.common.UiState
import me.june8th.euet.core.model.AppNotification
import me.june8th.euet.core.model.NewsItem
import me.june8th.euet.core.data.repository.StudentRepository

data class NotificationsUiState(
    val news: List<NewsItem> = emptyList(),
    val content: UiState<List<AppNotification>> = UiState.Loading,
)

class NotificationsViewModel(
    private val repository: StudentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = NotificationsUiState(content = UiState.Loading)
        viewModelScope.launch {
            val newsDeferred = async { repository.getNews() }
            val notiDeferred = async { repository.getNotifications() }

            val news = (newsDeferred.await() as? NetworkResult.Success)?.data.orEmpty()
            val content = when (val r = notiDeferred.await()) {
                is NetworkResult.Success ->
                    if (r.data.isEmpty()) UiState.Empty else UiState.Data(r.data)
                is NetworkResult.Error -> UiState.Error(r.message)
            }
            _state.value = NotificationsUiState(news = news, content = content)
        }
    }
}
