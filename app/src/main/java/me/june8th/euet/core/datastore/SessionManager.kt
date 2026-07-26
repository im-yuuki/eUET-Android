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
 */
class SessionManager(
    private val context: Context,
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("studenthub_token")
        val STUDENT_CODE = stringPreferencesKey("student_code")
        val ACTIVE_TERM = stringPreferencesKey("active_term")
        val CANVAS_TOKEN = stringPreferencesKey("canvas_token")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }
    val studentCode: Flow<String?> = context.dataStore.data.map { it[Keys.STUDENT_CODE] }
    val activeTerm: Flow<String?> = context.dataStore.data.map { it[Keys.ACTIVE_TERM] }
    val canvasToken: Flow<String?> = context.dataStore.data.map { it[Keys.CANVAS_TOKEN] }

    val isLoggedIn: Flow<Boolean> = token.map { !it.isNullOrBlank() }

    /** One-shot read used by the OkHttp auth interceptor (runs off the main thread). */
    suspend fun currentToken(): String? = token.first()

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[Keys.TOKEN] = token }
    }

    suspend fun saveStudentCode(code: String) {
        context.dataStore.edit { it[Keys.STUDENT_CODE] = code }
    }

    suspend fun saveActiveTerm(termCode: String) {
        context.dataStore.edit { it[Keys.ACTIVE_TERM] = termCode }
    }

    suspend fun saveCanvasToken(token: String) {
        context.dataStore.edit { it[Keys.CANVAS_TOKEN] = token }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
