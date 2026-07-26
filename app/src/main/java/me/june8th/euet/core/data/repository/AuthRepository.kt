package me.june8th.euet.core.data.repository

import android.util.Base64
import kotlinx.coroutines.flow.Flow
import me.june8th.euet.core.common.ErrorKind
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.common.safeApiCall
import me.june8th.euet.core.datastore.SessionManager
import me.june8th.euet.core.datastore.SnapshotCache
import me.june8th.euet.core.datastore.StudentHubLogin
import me.june8th.euet.core.data.source.studenthub.LoginVerdict
import me.june8th.euet.core.data.source.studenthub.PasswordLoginRequest
import me.june8th.euet.core.data.source.studenthub.StudentHubApi
import me.june8th.euet.core.data.source.studenthub.StudentHubAuthParser
import me.june8th.euet.core.model.CaptchaChallenge
import me.june8th.euet.core.network.StudentHubCookieJar

/**
 * Owns getting into StudentHub, by either of the two schemes the portal accepts:
 *
 *  * the web view, which captures the bearer token the SPA's own XHRs carry, and
 *  * `api/auth/login`, which takes a student ID, a password and a captcha the user reads, and
 *    answers with a session cookie rather than a token.
 *
 * The two coexist: whichever established the session,
 * [me.june8th.euet.core.network.AuthInterceptor] attaches a token only when one is stored, and
 * [StudentHubCookieJar] replays cookies only when it holds any. Either alone signs the user in.
 */
class AuthRepository(
    private val session: SessionManager,
    private val api: StudentHubApi,
    private val cache: SnapshotCache,
    private val cookieJar: StudentHubCookieJar,
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

    /** The login the password form should start from, or null if that path was never used. */
    suspend fun rememberedStudentHubLogin(): StudentHubLogin? = session.currentStudentHubLogin()

    /**
     * Fetches a captcha challenge for the password login. Challenges are single-use, so every
     * attempt — including one that follows a rejection — needs a fresh one.
     */
    suspend fun fetchCaptcha(): NetworkResult<CaptchaChallenge> = safeApiCall {
        val payload = StudentHubAuthParser.parseCaptcha(api.getCaptcha())
            ?: error("The captcha response didn't contain an image.")
        val bytes = decodeBase64(payload.imageBase64)
            ?: error("The captcha image couldn't be decoded.")
        CaptchaChallenge(id = payload.captchaId, image = bytes)
    }

    /**
     * Signs in with a student ID, a password and the captcha the user typed.
     *
     * Both outcomes come back as HTTP 200, so the status code proves nothing and the body is only
     * advisory — a cookie-only success can legitimately answer with nothing useful. The authority
     * is therefore a profile probe issued right after the POST: the jar already holds whatever
     * cookies the response set, so a `/api/student/detail` that answers *is* the session. The
     * parsed verdict contributes two things only — the bearer token, when one came back, and the
     * copy shown for a failure (a captcha complaint reads differently from wrong credentials).
     * The iOS client resolves this identically, so the two apps agree on what "signed in" means.
     *
     * On success everything the session needs is persisted — the bearer token if there was one,
     * the cookies (the jar does that itself), the student code and the username. The password is
     * kept, encrypted, only when [rememberPassword] is set.
     */
    suspend fun loginWithPassword(
        userName: String,
        password: String,
        captchaId: String,
        captchaValue: String,
        rememberPassword: Boolean,
    ): NetworkResult<Unit> {
        val request = PasswordLoginRequest(
            userName = userName,
            password = password,
            captchaId = captchaId,
            captchaValue = captchaValue,
        )
        val response = safeApiCall { api.login(request) }
        val body = when (response) {
            is NetworkResult.Error -> return response
            is NetworkResult.Success -> response.data
        }

        val verdict = StudentHubAuthParser.parseLogin(body)
        val accepted = verdict as? LoginVerdict.Accepted

        // A token, when the server issues one, has to be stored before the probe so the auth
        // interceptor can attach it; the probe is what decides whether it survives.
        val token = accepted?.token
        if (!token.isNullOrBlank()) session.saveToken(token)

        val probedCode = probeStudentCode()
        if (probedCode == null) {
            // No usable session — undo the speculative token and report why, per the body.
            if (!token.isNullOrBlank()) session.clearStudentHubAuth()
            return rejection(verdict as? LoginVerdict.Rejected)
        }

        return persistSession(
            token = token,
            studentCode = probedCode.takeIf { it != PROBE_AUTHENTICATED } ?: accepted?.studentCode,
            userName = userName,
            password = password,
            rememberPassword = rememberPassword,
        )
    }

    /** Full sign-out: drops every stored credential and all cached snapshots with them. */
    suspend fun logout() {
        cookieJar.clear()
        session.clear()
        cache.clear()
    }

    /**
     * Flips StudentHub to connected exactly as the web-view path does — token (when there is one)
     * plus student code — and adds what the cookie path needs on top.
     */
    private suspend fun persistSession(
        token: String?,
        studentCode: String?,
        userName: String,
        password: String,
        rememberPassword: Boolean,
    ): NetworkResult<Unit> {
        if (!token.isNullOrBlank()) session.saveToken(token)
        session.saveStudentHubSession(userName, password.takeIf { rememberPassword })
        // The notifications endpoint keys off the real student code; the entered username is the
        // fallback for the (unlikely) case where neither the probe nor the body volunteered one.
        session.saveStudentCode(studentCode?.takeIf { it.isNotBlank() } ?: userName)
        return NetworkResult.Success(Unit)
    }

    /**
     * Asks an authenticated endpoint who we are — the single source of truth for "did the login
     * take?", since both outcomes are HTTP 200. Returns the student code when the portal gives
     * one, [PROBE_AUTHENTICATED] when it answers as us without naming a code, and null when the
     * session isn't usable.
     */
    private suspend fun probeStudentCode(): String? {
        val detail = runCatching { api.getDetail() }.getOrNull() ?: return null
        detail.studentCode?.takeIf { it.isNotBlank() }?.let { return it }
        // An unauthenticated portal can answer 200 with an empty shell, so a nameless response
        // only counts when *something* identifying came back.
        return PROBE_AUTHENTICATED.takeIf { !detail.name.isNullOrBlank() }
    }

    /**
     * Maps a rejected login onto the error kinds the UI localizes. A spent or mistyped captcha is
     * kept distinct from wrong credentials, because only one of them means "just try again". A
     * null verdict is a login the body said nothing about, which is treated as bad credentials.
     */
    private fun rejection(verdict: LoginVerdict.Rejected?): NetworkResult.Error =
        if (verdict?.captchaProblem == true) {
            NetworkResult.Error(
                "That captcha wasn't accepted. A new one is ready — try again.",
                kind = ErrorKind.CAPTCHA_REJECTED,
            )
        } else {
            NetworkResult.Error(
                "Incorrect student ID or password.",
                kind = ErrorKind.BAD_CREDENTIALS,
            )
        }

    /** Decodes a captcha payload, tolerating the URL-safe alphabet some encoders emit. */
    private fun decodeBase64(value: String): ByteArray? =
        runCatching { Base64.decode(value, Base64.DEFAULT) }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?: runCatching { Base64.decode(value, Base64.URL_SAFE) }.getOrNull()?.takeIf { it.isNotEmpty() }

    private companion object {
        /** Sentinel: the probe proved the session works but named no student code. */
        const val PROBE_AUTHENTICATED = ""
    }
}
