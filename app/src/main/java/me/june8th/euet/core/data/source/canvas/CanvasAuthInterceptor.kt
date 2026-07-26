package me.june8th.euet.core.data.source.canvas

import kotlinx.coroutines.runBlocking
import me.june8th.euet.core.datastore.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the stored Canvas access token to outgoing Canvas requests. Deliberately separate from
 * the StudentHub [me.june8th.euet.core.network.AuthInterceptor]: the two providers have
 * independent tokens and sessions. Requests that already carry an Authorization header — the
 * connect-time validation call probing a pasted token — pass through untouched, so a bad paste can
 * never invalidate a stored good token.
 */
class CanvasAuthInterceptor(
    private val session: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header("Authorization") != null) return chain.proceed(original)

        val token = runBlocking { session.currentCanvasToken() }
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            original
        }

        val response = chain.proceed(request)

        // Canvas access tokens can be deleted server-side from Account → Settings. A 401 on a
        // request we did authenticate means this one is gone — there's no refresh path for manual
        // tokens — so drop it and let the Canvas screen fall back to its connect state.
        if (response.code == HTTP_UNAUTHORIZED && !token.isNullOrBlank()) {
            runBlocking { session.clearCanvasAuth() }
        }
        return response
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
