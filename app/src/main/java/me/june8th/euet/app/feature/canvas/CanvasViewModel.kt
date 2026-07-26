package me.june8th.euet.app.feature.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.june8th.euet.app.common.UiState
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.data.repository.CanvasRepository
import me.june8th.euet.core.datastore.SnapshotCache
import me.june8th.euet.core.datastore.load
import me.june8th.euet.core.datastore.save
import me.june8th.euet.core.model.CanvasCourse
import me.june8th.euet.core.model.CanvasSummary
import me.june8th.euet.core.model.MissingSubmission
import me.june8th.euet.core.model.PlannerItem

/** State of the paste-a-token connect form shown while Canvas is disconnected. */
data class CanvasConnectState(
    val token: String = "",
    val isConnecting: Boolean = false,
    val error: UiState.Error? = null,
) {
    val canSubmit: Boolean
        get() = token.isNotBlank() && !isConnecting
}

/** Everything the connected dashboard renders, fetched in one parallel round-trip. */
@Serializable
data class CanvasDashboard(
    val courses: List<CanvasCourse>,
    val upcoming: List<PlannerItem>,
    val missing: List<MissingSubmission>,
    val summary: CanvasSummary,
)

class CanvasViewModel(
    private val repository: CanvasRepository,
    private val cache: SnapshotCache,
) : ViewModel() {

    /** null until the DataStore's first emission resolves, then the live connection state. */
    private val _isConnected = MutableStateFlow<Boolean?>(null)
    val isConnected: StateFlow<Boolean?> = _isConnected.asStateFlow()

    private val _connect = MutableStateFlow(CanvasConnectState())
    val connectState: StateFlow<CanvasConnectState> = _connect.asStateFlow()

    private val _dashboard = MutableStateFlow<UiState<CanvasDashboard>>(UiState.Loading)
    val dashboard: StateFlow<UiState<CanvasDashboard>> = _dashboard.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        // Follow the stored token: loads the dashboard when a connection appears (either at
        // startup or right after connect()) and resets to the connect form when it goes away
        // (disconnect, or the interceptor dropping a token Canvas revoked).
        viewModelScope.launch {
            repository.isConnected.collect { connected ->
                val wasConnected = _isConnected.value
                _isConnected.value = connected
                if (connected && wasConnected != true) load()
                if (!connected) {
                    _dashboard.value = UiState.Loading
                    _connect.update { CanvasConnectState() }
                    // A disconnected Canvas must not leave its data readable offline.
                    cache.remove(CACHE_KEY)
                }
            }
        }
    }

    fun onTokenChange(value: String) = _connect.update { it.copy(token = value, error = null) }

    fun connect() {
        val state = _connect.value
        if (!state.canSubmit) return
        _connect.update { it.copy(isConnecting = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.connect(state.token)) {
                // Success stores the token; the isConnected collector flips the screen and loads.
                is NetworkResult.Success -> _connect.update { CanvasConnectState() }
                is NetworkResult.Error ->
                    _connect.update {
                        it.copy(isConnecting = false, error = UiState.Error(result.message, result.kind))
                    }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch { repository.disconnect() }
    }

    /** Offline-first: the cached dashboard renders instantly, then the network refreshes it. */
    fun load() {
        _dashboard.value = UiState.Loading
        viewModelScope.launch {
            cache.load<CanvasDashboard>(CACHE_KEY)?.value
                ?.let { _dashboard.value = UiState.Data(it) }
            applyFetch()
        }
    }

    /** Pull-to-refresh: re-fetch without dropping the visible dashboard into a loading state. */
    fun refresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            applyFetch()
            _refreshing.value = false
        }
    }

    /** Fetches, caches a successful dashboard, and never replaces visible data with an error. */
    private suspend fun applyFetch() {
        when (val result = fetch()) {
            is UiState.Error ->
                if (_dashboard.value !is UiState.Data) _dashboard.value = result
            else -> {
                (result as? UiState.Data)?.let { cache.save(CACHE_KEY, it.value) }
                _dashboard.value = result
            }
        }
    }

    /**
     * All four Canvas calls in parallel. Courses are the backbone — their failure fails the
     * screen — while the auxiliary sections degrade to empty so one flaky endpoint doesn't blank
     * the whole dashboard.
     */
    private suspend fun fetch(): UiState<CanvasDashboard> = coroutineScope {
        val courses = async { repository.getCourses() }
        val upcoming = async { repository.getUpcomingItems() }
        val missing = async { repository.getMissingSubmissions() }
        val unread = async { repository.getUnreadInboxCount() }

        when (val result = courses.await()) {
            is NetworkResult.Error -> UiState.Error(result.message, result.kind)
            is NetworkResult.Success -> {
                val upcomingItems = (upcoming.await() as? NetworkResult.Success)?.data.orEmpty()
                val missingItems = (missing.await() as? NetworkResult.Success)?.data.orEmpty()
                val unreadCount = (unread.await() as? NetworkResult.Success)?.data ?: 0
                if (result.data.isEmpty() && upcomingItems.isEmpty() && missingItems.isEmpty() && unreadCount == 0) {
                    UiState.Empty
                } else {
                    UiState.Data(
                        CanvasDashboard(
                            courses = result.data,
                            upcoming = upcomingItems,
                            missing = missingItems,
                            summary = CanvasSummary(
                                unreadInbox = unreadCount,
                                missingSubmissions = missingItems.size,
                            ),
                        ),
                    )
                }
            }
        }
    }

    private companion object {
        const val CACHE_KEY = "canvas.dashboard"
    }
}
