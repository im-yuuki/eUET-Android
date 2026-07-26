package me.june8th.euet.core.network

import kotlinx.coroutines.runBlocking
import me.june8th.euet.core.datastore.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the StudentHub bearer token to every outgoing request that lacks one, and invalidates
 * the stored session when the server rejects it.
 *
 * StudentHub has two authentication schemes and this interceptor leaves room for both. The
 * web-view sign-in captures a bearer token; the password sign-in establishes a cookie session and
 * sends no `Authorization` header at all (that is what the captured traffic shows). Attaching a
 * token is therefore a no-op when none is stored — [StudentHubCookieJar] carries that session on
 * its own.
 */
class AuthInterceptor(
    private val session: SessionManager,
    private val cookieJar: StudentHubCookieJar,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // The sign-in endpoints establish the session: they must never carry a stale token, and a
        // rejection there must not invalidate a session the user may still hold.
        if (original.url.encodedPath.contains(AUTH_PATH_PREFIX)) return chain.proceed(original)

        if (original.header("Authorization") != null) return chain.proceed(original)

        val token = runBlocking { session.currentToken() }
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            original
        }

        val response = chain.proceed(request)

        // A 401 against a live session means that session is gone — the captured token expired or
        // the cookie session lapsed. Neither can be refreshed silently: the token comes from a
        // WebView OAuth capture, and the password login needs a captcha the user has to read. So
        // drop it. RootViewModel observes `isLoggedIn`, so the app returns to the sign-in screen
        // instead of leaving the user stranded on error states.
        if (response.code == HTTP_UNAUTHORIZED) {
            runBlocking {
                if (!token.isNullOrBlank() || session.hasStudentHubSessionNow()) {
                    session.clearStudentHubAuth()
                    // The cookies are half the session; leaving them in the jar would send a
                    // credential the server has already disowned.
                    cookieJar.clear()
                }
            }
        }
        return response
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val AUTH_PATH_PREFIX = "/api/auth/"
    }
}
