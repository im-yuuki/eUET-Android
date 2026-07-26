package me.june8th.euet.app.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.app.common.UiState
import me.june8th.euet.core.model.AppNotification
import me.june8th.euet.core.model.NewsItem
import me.june8th.euet.core.data.repository.AggregateRepository
import me.june8th.euet.core.datastore.SnapshotCache
import me.june8th.euet.core.datastore.load
import me.june8th.euet.core.datastore.save

data class NotificationsUiState(
    val news: List<NewsItem> = emptyList(),
    val content: UiState<List<AppNotification>> = UiState.Loading,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
)

class NotificationsViewModel(
    private val repository: AggregateRepository,
    private val cache: SnapshotCache,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    /** Next page to request; page 0 is the initial load. */
    private var nextPage = 1

    /** Size of the first page — later pages shorter than this are treated as the last one. */
    private var pageSize = 0

    init { load() }

    /** Offline-first: the cached first page and news render instantly, then the network refreshes. */
    fun load() {
        _state.value = NotificationsUiState(content = UiState.Loading)
        viewModelScope.launch {
            val cachedNews = cache.load<List<NewsItem>>(KEY_NEWS)?.value.orEmpty()
            val cachedPage = cache.load<List<AppNotification>>(KEY_PAGE0)?.value
                ?.takeIf { it.isNotEmpty() }
            if (cachedNews.isNotEmpty() || cachedPage != null) {
                _state.update {
                    it.copy(
                        news = cachedNews,
                        content = cachedPage?.let { page -> UiState.Data(page) } ?: UiState.Loading,
                    )
                }
            }
            loadFirstPage()
        }
    }

    fun refresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            loadFirstPage()
            _state.update { it.copy(refreshing = false) }
        }
    }

    /** Appends the next page once the list scrolls near its end. Later pages are never cached. */
    fun loadMore() {
        val current = _state.value
        if (current.loadingMore || current.endReached || current.refreshing) return
        if (current.content !is UiState.Data) return

        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            when (val r = repository.getNotifications(nextPage)) {
                is NetworkResult.Success -> {
                    val page = r.data
                    nextPage++
                    _state.update {
                        val existing = (it.content as? UiState.Data)?.value.orEmpty()
                        val known = existing.mapTo(mutableSetOf()) { n -> n.id }
                        it.copy(
                            content = UiState.Data(existing + page.filter { n -> n.id !in known }),
                            loadingMore = false,
                            endReached = page.isEmpty() || page.size < pageSize,
                        )
                    }
                }
                // Fail quietly: keep what we have and let the next scroll retry.
                is NetworkResult.Error -> _state.update { it.copy(loadingMore = false) }
            }
        }
    }

    private suspend fun loadFirstPage() {
        val newsDeferred = viewModelScope.async { repository.getNews() }
        val notiDeferred = viewModelScope.async { repository.getNotifications(page = 0) }

        val newsResult = newsDeferred.await() as? NetworkResult.Success
        newsResult?.let { cache.save(KEY_NEWS, it.data) }
        val news = newsResult?.data ?: _state.value.news
        when (val r = notiDeferred.await()) {
            is NetworkResult.Success -> {
                cache.save(KEY_PAGE0, r.data)
                nextPage = 1
                pageSize = r.data.size
                _state.update {
                    it.copy(
                        news = news,
                        content = if (r.data.isEmpty()) UiState.Empty else UiState.Data(r.data),
                        loadingMore = false,
                        endReached = r.data.isEmpty(),
                    )
                }
            }
            is NetworkResult.Error -> _state.update {
                // Keep the cached/already-loaded items when the fetch fails.
                val kept = it.content as? UiState.Data
                it.copy(news = news, content = kept ?: UiState.Error(r.message, r.kind))
            }
        }
    }

    private companion object {
        const val KEY_NEWS = "notifications.news"
        const val KEY_PAGE0 = "notifications.page0"
    }
}
