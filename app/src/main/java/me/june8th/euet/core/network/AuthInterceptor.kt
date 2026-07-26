package me.june8th.euet.core.network

import kotlinx.coroutines.runBlocking
import me.june8th.euet.core.datastore.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/** Attaches the StudentHub bearer token to every outgoing request that lacks one. */
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
        return chain.proceed(request)
    }
}
