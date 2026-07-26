package me.june8th.euet.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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

        /** Plaintext keys from builds before tokens were encrypted; scrubbed on save. */
        val LEGACY_TOKEN = stringPreferencesKey("studenthub_token")
        val LEGACY_CANVAS_TOKEN = stringPreferencesKey("canvas_token")
    }

    val token: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.TOKEN]?.let(cipher::decrypt)
    }
    val studentCode: Flow<String?> = context.dataStore.data.map { it[Keys.STUDENT_CODE] }
    val activeTerm: Flow<String?> = context.dataStore.data.map { it[Keys.ACTIVE_TERM] }
    val canvasToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.CANVAS_TOKEN]?.let(cipher::decrypt)
    }

    val isLoggedIn: Flow<Boolean> = token.map { !it.isNullOrBlank() }

    /** One-shot read used by the OkHttp auth interceptor (runs off the main thread). */
    suspend fun currentToken(): String? = token.first()

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
     * Drops only the StudentHub credentials, leaving the Canvas token and selected term intact.
     * Used when the server rejects the bearer token, so an independent Canvas session isn't
     * collateral damage.
     */
    suspend fun clearStudentHubAuth() {
        context.dataStore.edit {
            it.remove(Keys.TOKEN)
            it.remove(Keys.LEGACY_TOKEN)
            it.remove(Keys.STUDENT_CODE)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
