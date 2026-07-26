package me.june8th.euet.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.june8th.euet.core.common.ErrorKind
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.common.safeApiCall
import me.june8th.euet.core.data.source.canvas.CanvasApi
import me.june8th.euet.core.data.source.canvas.toDomain
import me.june8th.euet.core.datastore.SessionManager
import me.june8th.euet.core.model.CanvasCourse
import me.june8th.euet.core.model.MissingSubmission
import me.june8th.euet.core.model.PlannerItem
import retrofit2.HttpException
import java.time.LocalDate

/**
 * Canvas LMS access. The connection is a user-generated access token (Canvas → Account →
 * Settings → "New access token") — no OAuth dance against the UET instance. The token is
 * validated against `/users/self` and only stored (encrypted, via [SessionManager]) once Canvas
 * accepts it; every later request is authenticated by the Canvas OkHttp interceptor.
 */
class CanvasRepository(
    private val api: CanvasApi,
    private val session: SessionManager,
) {
    val isConnected: Flow<Boolean> = session.canvasToken.map { !it.isNullOrBlank() }

    /**
     * Validates [token] and persists it on success. Returns the account's display name so the UI
     * can confirm who just connected.
     */
    suspend fun connect(token: String): NetworkResult<String> {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return NetworkResult.Error("Paste an access token first.")
        return when (val result = safeApiCall { api.getSelf("Bearer $trimmed") }) {
            is NetworkResult.Success -> {
                session.saveCanvasToken(trimmed)
                NetworkResult.Success(result.data.shortName ?: result.data.name ?: "Canvas")
            }
            is NetworkResult.Error ->
                if ((result.cause as? HttpException)?.code() == 401) {
                    NetworkResult.Error(
                        "Canvas rejected that token. Generate a new one and try again.",
                        kind = ErrorKind.CANVAS_TOKEN_REJECTED,
                    )
                } else {
                    result
                }
        }
    }

    suspend fun disconnect() = session.clearCanvasAuth()

    /** Active courses, as shown on the Canvas dashboard. */
    suspend fun getCourses(): NetworkResult<List<CanvasCourse>> =
        safeApiCall { api.getDashboardCards().mapNotNull { it.toDomain() } }

    /** Upcoming planner items (assignments, quizzes, announcements, …) from today onwards. */
    suspend fun getUpcomingItems(): NetworkResult<List<PlannerItem>> =
        safeApiCall {
            api.getPlannerItems(startDate = LocalDate.now().toString())
                .mapNotNull { it.toDomain() }
        }

    /** Submittable assignments whose due date passed without a submission. */
    suspend fun getMissingSubmissions(): NetworkResult<List<MissingSubmission>> =
        safeApiCall { api.getMissingSubmissions().mapNotNull { it.toDomain() } }

    /** Unread Canvas inbox conversations (the API returns the count as a string). */
    suspend fun getUnreadInboxCount(): NetworkResult<Int> =
        safeApiCall { api.getUnreadCount().unreadCount?.trim()?.toIntOrNull() ?: 0 }
}
