package me.june8th.euet.core.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.june8th.euet.core.common.ErrorKind
import me.june8th.euet.core.common.NetworkResult
import me.june8th.euet.core.common.map
import me.june8th.euet.core.data.Capability
import me.june8th.euet.core.data.ConflictDetector
import me.june8th.euet.core.data.ProviderId
import me.june8th.euet.core.data.ProviderRegistry
import me.june8th.euet.core.model.AppNotification
import me.june8th.euet.core.model.Bill
import me.june8th.euet.core.model.ConflictReport
import me.june8th.euet.core.model.DataConflict
import me.june8th.euet.core.model.Exam
import me.june8th.euet.core.model.GpaSummary
import me.june8th.euet.core.model.NewsItem
import me.june8th.euet.core.model.PortalDocument
import me.june8th.euet.core.model.SourceId
import me.june8th.euet.core.model.Sourced
import me.june8th.euet.core.model.StudentProfile
import me.june8th.euet.core.model.Term
import me.june8th.euet.core.model.TermGrades
import me.june8th.euet.core.model.TermPerformance
import me.june8th.euet.core.model.TimetableEntry

/** Receives the conflict report of a dual-source fetch once the secondary source answers. */
typealias ConflictListener = suspend (ConflictReport) -> Unit

/**
 * The single data façade the feature ViewModels talk to. For each capability it consults the
 * [ProviderRegistry] and serves data from the highest-priority connected provider (the user's
 * preferred source for the overlap capabilities), falling back to the next one when a source
 * fails or isn't connected — so a screen never needs to know whether its grades came from
 * StudentHub or were scraped off the VNU portal.
 *
 * For profile, grades and exams, when BOTH sources are connected the preferred one is fetched
 * and returned immediately while the secondary is fetched concurrently on [scope]; once both
 * succeed the [ConflictDetector] runs and the caller's [ConflictListener] receives the report.
 * If the preferred source fails, the secondary serves as fallback exactly as before — with no
 * conflict UI. With a single source connected, behaviour is unchanged (an empty report is
 * delivered so stale cached conflicts get cleared).
 */
class AggregateRepository(
    private val registry: ProviderRegistry,
    private val studentHub: StudentRepository,
    private val daotao: DaotaoRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    val activeTerm: Flow<String?> = studentHub.activeTerm

    suspend fun setActiveTerm(termCode: String) = studentHub.setActiveTerm(termCode)

    /**
     * The provider that most recently served the exam term list. Exam term codes are
     * provider-specific (StudentHub term codes vs the portal's internal term ids), so the exams
     * for a term must come from the same source that produced the term list.
     */
    private var examsSource: ProviderId? = null

    /** The exam terms that source produced, kept to map a term onto the secondary source. */
    private var examTerms: List<Term> = emptyList()

    // --- Aggregated capabilities (preferred source + fallback + conflict detection) ---

    suspend fun getProfile(onConflicts: ConflictListener? = null): NetworkResult<Sourced<StudentProfile>> =
        resolveDual(
            Capability.PROFILE,
            onConflicts,
            { preferred, other, _, _ -> ConflictDetector.compareProfiles(preferred, other) },
            ProviderId.STUDENT_HUB to { studentHub.getProfile() },
            ProviderId.VNU_DAOTAO to { daotao.getProfile() },
        )

    suspend fun getTranscript(onConflicts: ConflictListener? = null): NetworkResult<Sourced<List<TermGrades>>> =
        resolveDual(
            Capability.GRADES,
            onConflicts,
            ConflictDetector::compareTranscripts,
            ProviderId.STUDENT_HUB to { studentHub.getTranscript() },
            ProviderId.VNU_DAOTAO to { daotao.getTranscript() },
        )

    /** Exam terms plus the resolved "current" term, from the best connected exam source. */
    suspend fun getExamTermsWithActive(): NetworkResult<Pair<List<Term>, String?>> {
        val sources = registry.sourcesFor(Capability.EXAMS)
        if (sources.isEmpty()) return notConnectedError(Capability.EXAMS)
        var lastError: NetworkResult.Error? = null
        for (source in sources) {
            val result = when (source) {
                ProviderId.STUDENT_HUB -> studentHub.getTermsWithActive()
                ProviderId.VNU_DAOTAO ->
                    // The portal's dropdown is newest first, so default to its first term.
                    daotao.getExamTerms().map { terms -> terms to terms.firstOrNull()?.code }
                ProviderId.CANVAS -> continue
            }
            when (result) {
                is NetworkResult.Success -> {
                    examsSource = source
                    examTerms = result.data.first
                    return result
                }
                is NetworkResult.Error -> lastError = result
            }
        }
        return lastError ?: notConnectedError(Capability.EXAMS)
    }

    /** Exams for [termCode], served by whichever provider produced the current term list. */
    suspend fun getExams(
        termCode: String,
        onConflicts: ConflictListener? = null,
    ): NetworkResult<Sourced<List<Exam>>> {
        val sources = registry.sourcesFor(Capability.EXAMS)
        val serving = examsSource ?: sources.firstOrNull()
        val servingSource = serving?.toSourceId() ?: return notConnectedError(Capability.EXAMS)
        val result = when (serving) {
            ProviderId.STUDENT_HUB -> studentHub.getExams(termCode)
            ProviderId.VNU_DAOTAO -> daotao.getExams(termCode)
            else -> return notConnectedError(Capability.EXAMS)
        }
        when (result) {
            is NetworkResult.Error -> return result
            is NetworkResult.Success -> {
                // Compare only when the serving source is the preferred one; a fallback fetch
                // shows no conflict UI (matching the profile/grades behaviour).
                if (onConflicts != null && serving == sources.firstOrNull()) {
                    val secondary = sources.getOrNull(1)
                    val otherSource = secondary?.toSourceId()
                    val preferredTerm = examTerms.firstOrNull { it.code == termCode }
                    if (secondary != null && otherSource != null && preferredTerm != null) {
                        scope.launch {
                            val other = fetchExamsFrom(secondary, preferredTerm)
                            if (other is NetworkResult.Success) {
                                val conflicts = ConflictDetector.compareExams(
                                    termCode, result.data, other.data, servingSource, otherSource,
                                )
                                onConflicts(ConflictReport(servingSource, conflicts))
                            }
                        }
                    } else {
                        onConflicts(ConflictReport(servingSource, emptyList()))
                    }
                }
                return NetworkResult.Success(Sourced(result.data, servingSource))
            }
        }
    }

    // --- StudentHub-only capabilities ---

    suspend fun getGpaSummary(): NetworkResult<GpaSummary> = resolve(
        Capability.GPA,
        ProviderId.STUDENT_HUB to { studentHub.getGpaSummary() },
    )

    suspend fun getTermsWithActive(): NetworkResult<Pair<List<Term>, String?>> = resolve(
        Capability.TIMETABLE,
        ProviderId.STUDENT_HUB to { studentHub.getTermsWithActive() },
    )

    suspend fun getTimetable(termCode: String): NetworkResult<List<TimetableEntry>> = resolve(
        Capability.TIMETABLE,
        ProviderId.STUDENT_HUB to { studentHub.getTimetable(termCode) },
    )

    suspend fun getBills(): NetworkResult<List<Bill>> = resolve(
        Capability.TUITION,
        ProviderId.STUDENT_HUB to { studentHub.getBills() },
    )

    suspend fun getNews(): NetworkResult<List<NewsItem>> = resolve(
        Capability.NEWS,
        ProviderId.STUDENT_HUB to { studentHub.getNews() },
    )

    suspend fun getNotifications(page: Int = 0): NetworkResult<List<AppNotification>> = resolve(
        Capability.NOTIFICATIONS,
        ProviderId.STUDENT_HUB to { studentHub.getNotifications(page) },
    )

    // --- VNU-portal-only capabilities ---

    suspend fun getTrainingPoints(): NetworkResult<List<TermPerformance>> = resolve(
        Capability.TRAINING_POINTS,
        ProviderId.VNU_DAOTAO to { daotao.getTrainingPoints() },
    )

    suspend fun getSyllabusDocuments(): NetworkResult<List<PortalDocument>> = resolve(
        Capability.DOCUMENTS,
        ProviderId.VNU_DAOTAO to { daotao.getSyllabusDocuments() },
    )

    /**
     * Runs the fetcher of each connected source in priority order, returning the first success.
     * A failure falls through to the next source; the last failure surfaces if all fail.
     */
    private suspend fun <T> resolve(
        capability: Capability,
        vararg fetchers: Pair<ProviderId, suspend () -> NetworkResult<T>>,
    ): NetworkResult<T> {
        val sources = registry.sourcesFor(capability)
        if (sources.isEmpty()) return notConnectedError(capability)
        var lastError: NetworkResult.Error? = null
        for (source in sources) {
            val fetch = fetchers.firstOrNull { it.first == source }?.second ?: continue
            when (val result = fetch()) {
                is NetworkResult.Success -> return result
                is NetworkResult.Error -> lastError = result
            }
        }
        return lastError ?: notConnectedError(capability)
    }

    /**
     * [resolve] plus conflict detection: fetches the preferred source and returns it without
     * waiting for the secondary, which runs on [scope] and reports through [onConflicts] once
     * both sides succeeded. Fallback semantics on preferred failure are identical to [resolve].
     */
    private suspend fun <T> resolveDual(
        capability: Capability,
        onConflicts: ConflictListener?,
        detect: (preferred: T, other: T, preferredSource: SourceId, otherSource: SourceId) -> List<DataConflict>,
        vararg fetchers: Pair<ProviderId, suspend () -> NetworkResult<T>>,
    ): NetworkResult<Sourced<T>> {
        val sources = registry.sourcesFor(capability)
        if (sources.isEmpty()) return notConnectedError(capability)

        val primary = sources.first()
        val primarySource = primary.toSourceId()
        val secondary = sources.getOrNull(1)
        val secondaryFetch = secondary?.let { s -> fetchers.firstOrNull { it.first == s }?.second }

        // Both sources connected and a listener attached: start the secondary alongside the
        // preferred fetch so the conflict check never delays the screen.
        val secondaryDeferred: Deferred<NetworkResult<T>>? =
            if (secondaryFetch != null && onConflicts != null) scope.async { secondaryFetch() } else null

        var lastError: NetworkResult.Error? = null
        val primaryFetch = fetchers.firstOrNull { it.first == primary }?.second
        when (val result = primaryFetch?.invoke()) {
            is NetworkResult.Success -> {
                if (onConflicts != null && primarySource != null) {
                    val otherSource = secondary?.toSourceId()
                    if (secondaryDeferred != null && otherSource != null) {
                        scope.launch {
                            val other = secondaryDeferred.await()
                            if (other is NetworkResult.Success) {
                                onConflicts(
                                    ConflictReport(
                                        primarySource,
                                        detect(result.data, other.data, primarySource, otherSource),
                                    ),
                                )
                            }
                        }
                    } else {
                        // Single-source fetch: overwrite stale conflicts from an earlier dual one.
                        onConflicts(ConflictReport(primarySource, emptyList()))
                    }
                }
                return NetworkResult.Success(Sourced(result.data, primarySource ?: SourceId.STUDENT_HUB))
            }
            is NetworkResult.Error -> lastError = result
            null -> Unit
        }

        // The preferred source failed: fall back in priority order, with no conflict UI.
        for (source in sources.drop(1)) {
            val result =
                if (source == secondary && secondaryDeferred != null) {
                    secondaryDeferred.await()
                } else {
                    fetchers.firstOrNull { it.first == source }?.second?.invoke() ?: continue
                }
            when (result) {
                is NetworkResult.Success ->
                    return NetworkResult.Success(
                        Sourced(result.data, source.toSourceId() ?: SourceId.STUDENT_HUB),
                    )
                is NetworkResult.Error -> lastError = result
            }
        }
        return lastError ?: notConnectedError(capability)
    }

    /**
     * Fetches the exams the secondary source holds for the term the screen shows. Term ids are
     * provider-specific, so the secondary's own term list is matched by normalized term key; when
     * no equivalent term exists there is nothing to compare against.
     */
    private suspend fun fetchExamsFrom(
        provider: ProviderId,
        preferredTerm: Term,
    ): NetworkResult<List<Exam>> {
        val targetKey = ConflictDetector.termKey(preferredTerm.code, preferredTerm.name)
        val terms = when (provider) {
            ProviderId.STUDENT_HUB -> studentHub.getTermsWithActive().map { it.first }
            ProviderId.VNU_DAOTAO -> daotao.getExamTerms()
            ProviderId.CANVAS -> return NetworkResult.Error("Canvas has no exam schedule.")
        }
        return when (terms) {
            is NetworkResult.Error -> terms
            is NetworkResult.Success -> {
                val match = terms.data.firstOrNull {
                    ConflictDetector.termKey(it.code, it.name) == targetKey
                } ?: return NetworkResult.Error("No matching term on the secondary source.")
                when (provider) {
                    ProviderId.STUDENT_HUB -> studentHub.getExams(match.code)
                    ProviderId.VNU_DAOTAO -> daotao.getExams(match.code)
                    ProviderId.CANVAS -> NetworkResult.Error("Canvas has no exam schedule.")
                }
            }
        }
    }

    private fun ProviderId.toSourceId(): SourceId? = when (this) {
        ProviderId.STUDENT_HUB -> SourceId.STUDENT_HUB
        ProviderId.VNU_DAOTAO -> SourceId.VNU_PORTAL
        ProviderId.CANVAS -> null
    }

    private fun notConnectedError(capability: Capability): NetworkResult.Error = when (capability) {
        Capability.TRAINING_POINTS, Capability.DOCUMENTS ->
            NetworkResult.Error("Sign in to the VNU portal to see this.", kind = ErrorKind.SIGN_IN_DAOTAO)
        Capability.CANVAS ->
            NetworkResult.Error("Connect Canvas to see this.", kind = ErrorKind.CONNECT_CANVAS)
        else ->
            NetworkResult.Error("Sign in to StudentHub to see this.", kind = ErrorKind.SIGN_IN_STUDENTHUB)
    }
}
