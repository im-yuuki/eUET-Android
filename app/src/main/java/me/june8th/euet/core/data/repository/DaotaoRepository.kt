package me.june8th.euet.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.june8th.euet.core.common.ErrorKind
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.common.map
import me.june8th.euet.core.data.source.daotao.DaotaoClient
import me.june8th.euet.core.data.source.daotao.DaotaoScraper
import me.june8th.euet.core.data.source.daotao.DaotaoStudentContext
import me.june8th.euet.core.datastore.SessionManager
import me.june8th.euet.core.model.Exam
import me.june8th.euet.core.model.PortalDocument
import me.june8th.euet.core.model.StudentProfile
import me.june8th.euet.core.model.Term
import me.june8th.euet.core.model.TermGrades
import me.june8th.euet.core.model.TermPerformance
import org.jsoup.nodes.Document

/**
 * VNU daotao access. Owns the cookie session and keeps it alive transparently: if a page comes
 * back unauthenticated mid-use, this re-logs in with the stored credentials and retries once
 * before surfacing an error (the same `withReauth` behaviour as the iOS provider).
 */
class DaotaoRepository(
    private val client: DaotaoClient,
    private val session: SessionManager,
) {
    val isConnected: Flow<Boolean> = session.daotaoCredentials.map { it != null }

    /** Internal ids scraped from the profile page, cached for the exam queries. */
    private var cachedContext: DaotaoStudentContext? = null

    /** Authenticates and, on success, persists the credentials for silent re-login. */
    suspend fun login(username: String, password: String): NetworkResult<StudentProfile> =
        when (val outcome = client.login(username, password)) {
            is DaotaoClient.LoginOutcome.Success -> {
                val profile = DaotaoScraper.parseProfile(outcome.page)
                if (profile == null) {
                    NetworkResult.Error("Signed in, but the portal's profile page couldn't be read.")
                } else {
                    cachedContext = DaotaoScraper.parseStudentContext(outcome.page)
                    session.saveDaotaoCredentials(username, password)
                    NetworkResult.Success(profile)
                }
            }
            DaotaoClient.LoginOutcome.InvalidCredentials ->
                NetworkResult.Error("Incorrect student ID or password.", kind = ErrorKind.BAD_CREDENTIALS)
            is DaotaoClient.LoginOutcome.Failed ->
                NetworkResult.Error(outcome.message)
        }

    suspend fun getProfile(): NetworkResult<StudentProfile> =
        authenticated(DaotaoClient.PATH_PROFILE) { document ->
            when (val profile = DaotaoScraper.parseProfile(document)) {
                null -> NetworkResult.Error("Couldn't read your profile from the portal.")
                else -> NetworkResult.Success(profile)
            }
        }

    /** The transcript grouped by term, newest first. */
    suspend fun getTranscript(): NetworkResult<List<TermGrades>> =
        authenticated(DaotaoClient.PATH_GRADES) { document ->
            NetworkResult.Success(DaotaoScraper.parseGrades(document))
        }

    /** The terms offered by the exam page's dropdown, newest first. */
    suspend fun getExamTerms(): NetworkResult<List<Term>> =
        withStudentContext { univId, stdId ->
            authenticated(DaotaoClient.PATH_EXAMS, examQuery(univId, stdId)) { document ->
                NetworkResult.Success(DaotaoScraper.parseExamTerms(document))
            }
        }

    /** The exam schedule for [termCode] (a term id from [getExamTerms]). */
    suspend fun getExams(termCode: String): NetworkResult<List<Exam>> =
        withStudentContext { univId, stdId ->
            authenticated(DaotaoClient.PATH_EXAMS, examQuery(univId, stdId, termCode)) { document ->
                NetworkResult.Success(DaotaoScraper.parseExams(document))
            }
        }

    /**
     * Term-by-term GPA/CPA, newest first. Prefers the study tab's table; when the portal loads
     * that table lazily (empty on first render), falls back to the transcript-derived
     * computation — the credit-weighted path iOS verified live. Conduct scores stay null until
     * the conduct sub-tab's request is captured.
     */
    suspend fun getTrainingPoints(): NetworkResult<List<TermPerformance>> {
        val fromStudyTab = authenticated(DaotaoClient.PATH_TRAINING) { document ->
            NetworkResult.Success(DaotaoScraper.parseTermPerformance(document))
        }
        if (fromStudyTab is NetworkResult.Success && fromStudyTab.data.isNotEmpty()) return fromStudyTab
        return getTranscript().map(DaotaoScraper::computeTermPerformance)
    }

    /** The syllabus PDF listing (first page only). */
    suspend fun getSyllabusDocuments(): NetworkResult<List<PortalDocument>> =
        authenticated(DaotaoClient.PATH_SYLLABUS) { document ->
            NetworkResult.Success(DaotaoScraper.parseSyllabusDocuments(document))
        }

    suspend fun logout() {
        session.clearDaotaoAuth()
        client.clearSession()
        cachedContext = null
    }

    /**
     * Resolves the internal `selUniv` / `selStd` ids the exam page requires, scraping the
     * profile page once and caching the result for the session.
     */
    private suspend fun <T> withStudentContext(
        block: suspend (univId: String, stdId: String) -> NetworkResult<T>,
    ): NetworkResult<T> {
        val context = cachedContext?.takeIf { it.internalStudentId != null }
            ?: when (
                val result = authenticated(DaotaoClient.PATH_PROFILE) { document ->
                    NetworkResult.Success(DaotaoScraper.parseStudentContext(document))
                }
            ) {
                is NetworkResult.Success -> result.data.also { cachedContext = it }
                is NetworkResult.Error -> return result
            }
        val univId = context.universityId
        val stdId = context.internalStudentId
        if (univId == null || stdId == null) {
            return NetworkResult.Error("Couldn't read the student ids the exam page needs.")
        }
        return block(univId, stdId)
    }

    /** Query string for `StdExamination.asp`; the term is omitted when discovering terms. */
    private fun examQuery(univId: String, stdId: String, termCode: String? = null): Map<String, String> =
        buildMap {
            put("selViewType", "StdExam")
            put("selUniv", univId)
            put("selStd", stdId)
            termCode?.let { put("vTermID", it) }
        }

    /**
     * Fetches [path], re-authenticating once if the session has lapsed, then hands the parsed
     * document to [transform].
     */
    private suspend fun <T> authenticated(
        path: String,
        query: Map<String, String> = emptyMap(),
        transform: (Document) -> NetworkResult<T>,
    ): NetworkResult<T> = try {
        var document = client.fetchDocument(path, query)

        if (!DaotaoScraper.isAuthenticated(document)) {
            val credentials = session.currentDaotaoCredentials()
                ?: return NetworkResult.Error(
                    "Your VNU portal session expired. Please sign in again.",
                    kind = ErrorKind.SESSION_EXPIRED,
                )

            when (client.login(credentials.username, credentials.password)) {
                is DaotaoClient.LoginOutcome.Success -> document = client.fetchDocument(path, query)
                DaotaoClient.LoginOutcome.InvalidCredentials -> {
                    // The stored password no longer works — drop it so the UI prompts a re-login.
                    session.clearDaotaoAuth()
                    return NetworkResult.Error(
                        "Your saved VNU password is no longer valid. Please sign in again.",
                        kind = ErrorKind.SESSION_EXPIRED,
                    )
                }
                is DaotaoClient.LoginOutcome.Failed ->
                    return NetworkResult.Error(
                        "Couldn't reach the VNU portal. Check your connection.",
                        kind = ErrorKind.NETWORK,
                    )
            }
        }

        transform(document)
    } catch (e: Exception) {
        NetworkResult.Error(e.message ?: "Couldn't load data from the VNU portal.", e)
    }
}
