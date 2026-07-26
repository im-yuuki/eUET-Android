package me.june8th.euet.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.june8th.euet.core.common.ErrorKind
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.common.map
import me.june8th.euet.core.common.safeApiCall
import me.june8th.euet.core.datastore.SessionManager
import me.june8th.euet.core.model.AppNotification
import me.june8th.euet.core.model.Bill
import me.june8th.euet.core.model.Exam
import me.june8th.euet.core.model.GpaSummary
import me.june8th.euet.core.model.NewsItem
import me.june8th.euet.core.model.StudentProfile
import me.june8th.euet.core.model.Term
import me.june8th.euet.core.model.TermGrades
import me.june8th.euet.core.model.TimetableEntry
import me.june8th.euet.core.data.source.studenthub.StudentHubApi
import me.june8th.euet.core.data.source.studenthub.TermRequest
import me.june8th.euet.core.data.source.studenthub.toDomain
import me.june8th.euet.core.data.source.studenthub.toTermGrades

/** Read access to all StudentHub data, mapped to domain models. */
class StudentRepository(
    private val api: StudentHubApi,
    private val session: SessionManager,
) {
    val activeTerm: Flow<String?> = session.activeTerm

    suspend fun setActiveTerm(termCode: String) = session.saveActiveTerm(termCode)

    suspend fun getProfile(): NetworkResult<StudentProfile> =
        safeApiCall { api.getDetail().toDomain() }

    suspend fun getTerms(): NetworkResult<List<Term>> =
        safeApiCall { api.getTerms().map { it.toDomain() } }

    suspend fun getTimetable(termCode: String): NetworkResult<List<TimetableEntry>> =
        safeApiCall { api.getTimetable(TermRequest(termCode)).map { it.toDomain() } }

    suspend fun getTranscript(): NetworkResult<List<TermGrades>> =
        safeApiCall { api.getTranscript().toTermGrades() }

    suspend fun getGpaSummary(): NetworkResult<GpaSummary> =
        safeApiCall { api.getResults().toDomain() }

    suspend fun getExams(termCode: String): NetworkResult<List<Exam>> =
        safeApiCall { api.getExamSchedule(TermRequest(termCode)).map { it.toDomain() } }

    suspend fun getBills(): NetworkResult<List<Bill>> =
        safeApiCall { api.getBills().map { it.toDomain() } }

    suspend fun getNews(): NetworkResult<List<NewsItem>> =
        safeApiCall { api.getNews().map { it.toDomain() } }

    suspend fun getNotifications(page: Int = 0): NetworkResult<List<AppNotification>> {
        val code = session.studentCode.first()
            ?: return NetworkResult.Error(
                "No student code available. Try signing in again.",
                kind = ErrorKind.SESSION_EXPIRED,
            )
        return safeApiCall { api.getNotifications(code, page).content.map { it.toDomain() } }
    }

    /** Terms plus the resolved "current" term (persisted active term, else highest-index term). */
    suspend fun getTermsWithActive(): NetworkResult<Pair<List<Term>, String?>> =
        getTerms().map { terms ->
            val stored = session.activeTerm.first()
            val active = stored?.takeIf { code -> terms.any { it.code == code } }
                ?: terms.maxByOrNull { it.index ?: Int.MIN_VALUE }?.code
            terms to active
        }
}
