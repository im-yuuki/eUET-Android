package me.june8th.euet.core.data.source.daotao

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

/**
 * HTTP access to `daotao.vnu.edu.vn` — a classic-ASP portal with cookie-session auth and
 * HTML responses (no API, no tokens).
 *
 * The login sequence mirrors the one verified live on the iOS app:
 *  1. `GET /dkmh/login.asp` so IIS issues an `ASPSESSIONID*` cookie.
 *  2. `POST /dkmh/login.asp` with `txtLoginId`, `txtPassword`, and **`chkSubmit=ok`**.
 *     The portal's own `chkLogin()` script sets that hidden field to `"ok"`; any other value and
 *     the server silently re-renders the form instead of authenticating.
 *  3. Fetch an authenticated page and check it isn't the login form / "session expired" notice —
 *     failure re-renders with HTTP 200, so the status code alone proves nothing.
 *
 * Cookies live in a private in-memory jar so this session can't collide with any other client.
 * They aren't persisted; instead the (encrypted) password is stored and used to re-login on
 * demand, which also covers expiry.
 */
class DaotaoClient(
    baseUrl: String = BASE_URL,
    private val client: OkHttpClient = defaultClient(),
) {
    private val base: HttpUrl = baseUrl.toHttpUrl()

    /** Result of an authentication attempt. */
    sealed interface LoginOutcome {
        data class Success(val page: Document) : LoginOutcome
        data object InvalidCredentials : LoginOutcome
        data class Failed(val message: String) : LoginOutcome
    }

    /** Runs the full login sequence. On success the returned document is the profile page. */
    suspend fun login(username: String, password: String): LoginOutcome = withContext(Dispatchers.IO) {
        try {
            // 1. Seed the ASP session cookie; the POST is rejected without it.
            runCatching { execute(Request.Builder().url(loginUrl).get().build()) }

            // 2. Submit the form. chkSubmit must be "ok" (see class docs).
            val form = FormBody.Builder()
                .add("txtLoginId", username)
                .add("txtPassword", password)
                .add("chkSubmit", "ok")
                .build()
            runCatching {
                execute(
                    Request.Builder()
                        .url(loginUrl)
                        .header("Referer", loginUrl.toString())
                        .post(form)
                        .build(),
                )
            }

            // 3. Verify against a real authenticated page.
            val profile = fetchDocument(PATH_PROFILE)
            if (DaotaoScraper.isAuthenticated(profile)) {
                LoginOutcome.Success(profile)
            } else {
                LoginOutcome.InvalidCredentials
            }
        } catch (e: Exception) {
            Log.w(TAG, "daotao login failed", e)
            LoginOutcome.Failed(e.message ?: "Couldn't reach the VNU portal.")
        }
    }

    /** Fetches and parses a portal page. Does not check authentication. */
    suspend fun fetchDocument(path: String, query: Map<String, String> = emptyMap()): Document =
        withContext(Dispatchers.IO) {
            val url = base.newBuilder()
                .addPathSegments(path)
                .apply { query.forEach { (name, value) -> addQueryParameter(name, value) } }
                .build()
            val body = execute(Request.Builder().url(url).get().build())
            Jsoup.parse(body, url.toString())
        }

    /** Drops all session cookies for this client. */
    fun clearSession() = cookieJar.clear()

    private fun execute(request: Request): String =
        client.newCall(request).execute().use { response -> response.body.string() }

    private val loginUrl: HttpUrl
        get() = base.newBuilder().addPathSegments(PATH_LOGIN).build()

    private val cookieJar: InMemoryCookieJar
        get() = client.cookieJar as InMemoryCookieJar

    companion object {
        private const val TAG = "DaotaoClient"
        const val BASE_URL = "https://daotao.vnu.edu.vn/"

        const val PATH_LOGIN = "dkmh/login.asp"
        const val PATH_PROFILE = "StdInfo/TabStdSelf.asp"
        const val PATH_GRADES = "ListPoint/listpoint_Brc1.asp"
        const val PATH_TRAINING = "StdInfo/TabStdStudy.asp"
        const val PATH_EXAMS = "StdExamination/StdExamination.asp"
        const val PATH_SYLLABUS = "Syllabus/default.asp"

        /** Builds a client with its own cookie jar, isolated from the StudentHub client. */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .cookieJar(InMemoryCookieJar())
            .followRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

/** Process-lifetime cookie store, scoped to a single [OkHttpClient]. */
class InMemoryCookieJar : CookieJar {
    private val cookies = mutableMapOf<String, Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { this.cookies[it.name] = it }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        cookies.values.filter { it.matches(url) && (it.expiresAt > System.currentTimeMillis()) }

    @Synchronized
    fun clear() = cookies.clear()
}
