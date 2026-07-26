package me.june8th.euet.core.data.repository

import kotlinx.coroutines.flow.Flow
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.common.safeApiCall
import me.june8th.euet.core.datastore.SessionManager
import me.june8th.euet.core.data.source.studenthub.StudentHubApi

class AuthRepository(
    private val session: SessionManager,
    private val api: StudentHubApi,
) {
    val isLoggedIn: Flow<Boolean> = session.isLoggedIn

    /**
     * Called once the WebView captures a bearer token. Persists it, then resolves and stores the
     * student code from `/api/student/detail` (needed by the notifications endpoint).
     */
    suspend fun onTokenCaptured(token: String): NetworkResult<Unit> {
        session.saveToken(token)
        return safeApiCall {
            val detail = api.getDetail()
            detail.studentCode?.let { session.saveStudentCode(it) }
            Unit
        }
    }

    suspend fun logout() = session.clear()
}
