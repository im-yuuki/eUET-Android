package me.june8th.euet.core.network

import kotlinx.coroutines.runBlocking
import me.june8th.euet.core.datastore.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the StudentHub bearer token to every outgoing request that lacks one, and invalidates
 * the stored session when the server rejects it.
 */
class AuthInterceptor(
    private val session: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header("Authorization") != null) return chain.proceed(original)

        val token = runBlocking { session.currentToken() }
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            original
        }

        val response = chain.proceed(request)

        // A 401 on a request we did authenticate means the captured token is no longer valid.
        // There's no silent refresh path — the token comes from a WebView OAuth capture with no
        // stored credentials — so drop it. RootViewModel observes `isLoggedIn`, so the app
        // returns to the sign-in screen instead of leaving the user stranded on error states.
        if (response.code == HTTP_UNAUTHORIZED && !token.isNullOrBlank()) {
            runBlocking { session.clearStudentHubAuth() }
        }
        return response
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
