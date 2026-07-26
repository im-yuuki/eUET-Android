package me.june8th.euet.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.june8th.euet.core.model.SourceId

private val Context.dataStore by preferencesDataStore(name = "euet_session")

/**
 * Persists the authenticated session: the StudentHub bearer token, the resolved student code, the
 * currently selected term, and an optional Canvas token. Backed by Preferences DataStore.
 *
 * Bearer tokens are encrypted at rest via [TokenCipher] (AES-GCM, key held in the Android
 * Keystore); only non-sensitive values are stored in the clear. A token that fails to decrypt is
 * reported as absent, so the app fails closed and prompts a fresh sign-in.
 */
class SessionManager(
    private val context: Context,
    private val cipher: TokenCipher = TokenCipher(),
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("studenthub_token_enc")
        val STUDENT_CODE = stringPreferencesKey("student_code")
        val ACTIVE_TERM = stringPreferencesKey("active_term")
        val CANVAS_TOKEN = stringPreferencesKey("canvas_token_enc")

        /**
         * VNU daotao is a classic-ASP cookie session with no refresh token, so the password is
         * kept (encrypted) to re-login silently when the session expires — the same trade-off the
         * iOS app makes with its Keychain-stored credentials.
         */
        val DAOTAO_USERNAME = stringPreferencesKey("daotao_username")
        val DAOTAO_PASSWORD = stringPreferencesKey("daotao_password_enc")

        /**
         * The StudentHub password sign-in. Its session lives in cookies, not a bearer token, so
         * three things are kept: the cookies (encrypted, since they *are* the credential), a flag
         * saying a session was established, and the username — plus the password when the user
         * opted in, which only prefills the form, because the captcha makes silent renewal
         * impossible.
         */
        val STUDENTHUB_COOKIES = stringPreferencesKey("studenthub_cookies_enc")
        val STUDENTHUB_SESSION = booleanPreferencesKey("studenthub_session_active")
        val STUDENTHUB_USERNAME = stringPreferencesKey("studenthub_username")
        val STUDENTHUB_PASSWORD = stringPreferencesKey("studenthub_password_enc")

        /** Plaintext keys from builds before tokens were encrypted; scrubbed on save. */
        val LEGACY_TOKEN = stringPreferencesKey("studenthub_token")
        val LEGACY_CANVAS_TOKEN = stringPreferencesKey("canvas_token")

        /** Which source wins for the capabilities both providers serve (profile, grades, exams). */
        val PREFERRED_SOURCE = stringPreferencesKey("preferred_source")
    }

    val token: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.TOKEN]?.let(cipher::decrypt)
    }
    val studentCode: Flow<String?> = context.dataStore.data.map { it[Keys.STUDENT_CODE] }
    val activeTerm: Flow<String?> = context.dataStore.data.map { it[Keys.ACTIVE_TERM] }
    val canvasToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.CANVAS_TOKEN]?.let(cipher::decrypt)
    }

    /** Stored VNU daotao credentials, or null when that provider isn't connected. */
    val daotaoCredentials: Flow<DaotaoCredentials?> = context.dataStore.data.map { prefs ->
        val username = prefs[Keys.DAOTAO_USERNAME]
        val password = prefs[Keys.DAOTAO_PASSWORD]?.let(cipher::decrypt)
        if (username.isNullOrBlank() || password.isNullOrBlank()) null
        else DaotaoCredentials(username, password)
    }

    /**
     * The username of the StudentHub password login, kept so the form can prefill it and so the
     * student code has a fallback. Null when that path was never used.
     */
    val studentHubUsername: Flow<String?> = context.dataStore.data.map { it[Keys.STUDENTHUB_USERNAME] }

    /** The remembered StudentHub password, or null when the user declined to store one. */
    val studentHubPassword: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.STUDENTHUB_PASSWORD]?.let(cipher::decrypt)
    }

    /**
     * True when StudentHub is connected by *either* scheme: a captured bearer token (web-view
     * sign-in) or an established cookie session (password sign-in).
     */
    val hasStudentHubSession: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[Keys.TOKEN]?.let(cipher::decrypt).isNullOrBlank() || prefs[Keys.STUDENTHUB_SESSION] == true
    }

    /**
     * True when *any* provider is connected. The app is usable with just the VNU portal, so this
     * can't be StudentHub-only.
     */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[Keys.TOKEN]?.let(cipher::decrypt).isNullOrBlank() ||
            prefs[Keys.STUDENTHUB_SESSION] == true ||
            !prefs[Keys.DAOTAO_USERNAME].isNullOrBlank()
    }

    /**
     * The user's preferred data source for the capabilities StudentHub and the VNU portal both
     * serve. Defaults to StudentHub — the behaviour before the setting existed.
     */
    val preferredSource: Flow<SourceId> = context.dataStore.data.map { prefs ->
        prefs[Keys.PREFERRED_SOURCE]
            ?.let { stored -> SourceId.entries.firstOrNull { it.name == stored } }
            ?: SourceId.STUDENT_HUB
    }

    /** One-shot read used by the provider registry when ordering sources. */
    suspend fun currentPreferredSource(): SourceId = preferredSource.first()

    suspend fun savePreferredSource(source: SourceId) {
        context.dataStore.edit { it[Keys.PREFERRED_SOURCE] = source.name }
    }

    /** One-shot read used by the OkHttp auth interceptor (runs off the main thread). */
    suspend fun currentToken(): String? = token.first()

    /** One-shot read used by the Canvas OkHttp interceptor (runs off the main thread). */
    suspend fun currentCanvasToken(): String? = canvasToken.first()

    suspend fun currentDaotaoCredentials(): DaotaoCredentials? = daotaoCredentials.first()

    /** One-shot read used by the auth interceptor to tell a live session from a stale request. */
    suspend fun hasStudentHubSessionNow(): Boolean = hasStudentHubSession.first()

    /**
     * The StudentHub login the form should start from: the username always, the password only if
     * the user asked for it to be remembered.
     */
    suspend fun currentStudentHubLogin(): StudentHubLogin? {
        val username = studentHubUsername.first()
        if (username.isNullOrBlank()) return null
        return StudentHubLogin(username, studentHubPassword.first())
    }

    suspend fun saveToken(token: String) {
        val encrypted = cipher.encrypt(token) ?: return
        context.dataStore.edit {
            it[Keys.TOKEN] = encrypted
            it.remove(Keys.LEGACY_TOKEN)
        }
    }

    suspend fun saveStudentCode(code: String) {
        context.dataStore.edit { it[Keys.STUDENT_CODE] = code }
    }

    suspend fun saveActiveTerm(termCode: String) {
        context.dataStore.edit { it[Keys.ACTIVE_TERM] = termCode }
    }

    suspend fun saveCanvasToken(token: String) {
        val encrypted = cipher.encrypt(token) ?: return
        context.dataStore.edit {
            it[Keys.CANVAS_TOKEN] = encrypted
            it.remove(Keys.LEGACY_CANVAS_TOKEN)
        }
    }

    /**
     * Records a successful StudentHub password sign-in. The cookies are already in the jar by the
     * time this runs; the flag is what the app reads to know the cookie session exists. [password]
     * is stored encrypted only when the user opted in, and null clears any earlier one.
     */
    suspend fun saveStudentHubSession(username: String, password: String?) {
        val encrypted = password?.let(cipher::encrypt)
        context.dataStore.edit {
            it[Keys.STUDENTHUB_SESSION] = true
            it[Keys.STUDENTHUB_USERNAME] = username
            if (encrypted != null) it[Keys.STUDENTHUB_PASSWORD] = encrypted
            else it.remove(Keys.STUDENTHUB_PASSWORD)
        }
    }

    /** The persisted StudentHub cookies, in `Set-Cookie` form. Empty when there is no session. */
    suspend fun loadStudentHubCookies(): List<String> =
        context.dataStore.data.first()[Keys.STUDENTHUB_COOKIES]
            ?.let(cipher::decrypt)
            ?.split(COOKIE_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    /** Persists the StudentHub cookies encrypted — for a cookie session they *are* the credential. */
    suspend fun saveStudentHubCookies(cookies: List<String>) {
        if (cookies.isEmpty()) {
            context.dataStore.edit { it.remove(Keys.STUDENTHUB_COOKIES) }
            return
        }
        val encrypted = cipher.encrypt(cookies.joinToString(COOKIE_SEPARATOR)) ?: return
        context.dataStore.edit { it[Keys.STUDENTHUB_COOKIES] = encrypted }
    }

    /**
     * Drops only the StudentHub credentials, leaving the Canvas token and selected term intact.
     * Used when the server rejects the session, so an independent Canvas session isn't collateral
     * damage. Both schemes go: the bearer token and the cookie session's flag and cookies.
     *
     * The remembered username/password survive on purpose — the captcha means the app can't renew
     * the session on its own, so the most it can do is hand the user a prefilled form.
     */
    suspend fun clearStudentHubAuth() {
        context.dataStore.edit {
            it.remove(Keys.TOKEN)
            it.remove(Keys.LEGACY_TOKEN)
            it.remove(Keys.STUDENT_CODE)
            it.remove(Keys.STUDENTHUB_SESSION)
            it.remove(Keys.STUDENTHUB_COOKIES)
        }
    }

    /** Full StudentHub disconnect: the session plus the remembered login. */
    suspend fun clearStudentHubCredentials() {
        clearStudentHubAuth()
        context.dataStore.edit {
            it.remove(Keys.STUDENTHUB_USERNAME)
            it.remove(Keys.STUDENTHUB_PASSWORD)
        }
    }

    /** Drops only the Canvas token — disconnecting Canvas must not touch the other providers. */
    suspend fun clearCanvasAuth() {
        context.dataStore.edit {
            it.remove(Keys.CANVAS_TOKEN)
            it.remove(Keys.LEGACY_CANVAS_TOKEN)
        }
    }

    suspend fun saveDaotaoCredentials(username: String, password: String) {
        val encrypted = cipher.encrypt(password) ?: return
        context.dataStore.edit {
            it[Keys.DAOTAO_USERNAME] = username
            it[Keys.DAOTAO_PASSWORD] = encrypted
        }
    }

    suspend fun clearDaotaoAuth() {
        context.dataStore.edit {
            it.remove(Keys.DAOTAO_USERNAME)
            it.remove(Keys.DAOTAO_PASSWORD)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    private companion object {
        /** Newline can't occur inside a serialized cookie, so it separates them safely. */
        const val COOKIE_SEPARATOR = "\n"
    }
}

/** Credentials for the VNU daotao cookie session. */
data class DaotaoCredentials(
    val username: String,
    val password: String,
)

/**
 * The StudentHub password login the sign-in form starts from. [password] is null when the user
 * declined to have it remembered — the username alone is still worth keeping.
 */
data class StudentHubLogin(
    val username: String,
    val password: String?,
)
