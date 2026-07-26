package me.june8th.euet.core.data

import me.june8th.euet.core.datastore.SessionManager
import me.june8th.euet.core.model.SourceId

/** The data providers the app can connect to. Order here is meaningless; see [ProviderRegistry]. */
enum class ProviderId {
    STUDENT_HUB,
    CANVAS,
    VNU_DAOTAO,
}

/** A kind of data a provider can serve — one entry per aggregatable screen. */
enum class Capability {
    PROFILE,
    TIMETABLE,
    GRADES,
    GPA,
    EXAMS,
    NOTIFICATIONS,
    NEWS,
    TUITION,
    TRAINING_POINTS,
    DOCUMENTS,
    CANVAS,
}

/**
 * Resolves a [Capability] to the connected providers able to serve it, best first — the Android
 * port of the iOS `ProviderRegistry` priority design. StudentHub (richest, live REST data) wins
 * over the VNU portal for overlaps like grades and exams; the portal is the only source for the
 * data StudentHub doesn't expose (training points, syllabus documents).
 *
 * "Connected" is read live from [SessionManager], so resolution always reflects the current
 * sign-in state without any registration bookkeeping.
 */
class ProviderRegistry(
    private val session: SessionManager,
) {
    /** Preferred source order per capability when more than one provider offers it. */
    private val sourcePriority: Map<Capability, List<ProviderId>> = mapOf(
        Capability.PROFILE to listOf(ProviderId.STUDENT_HUB, ProviderId.VNU_DAOTAO),
        Capability.TIMETABLE to listOf(ProviderId.STUDENT_HUB),
        Capability.GRADES to listOf(ProviderId.STUDENT_HUB, ProviderId.VNU_DAOTAO),
        Capability.GPA to listOf(ProviderId.STUDENT_HUB),
        Capability.EXAMS to listOf(ProviderId.STUDENT_HUB, ProviderId.VNU_DAOTAO),
        Capability.NOTIFICATIONS to listOf(ProviderId.STUDENT_HUB),
        Capability.NEWS to listOf(ProviderId.STUDENT_HUB),
        Capability.TUITION to listOf(ProviderId.STUDENT_HUB),
        Capability.TRAINING_POINTS to listOf(ProviderId.VNU_DAOTAO),
        Capability.DOCUMENTS to listOf(ProviderId.VNU_DAOTAO),
        Capability.CANVAS to listOf(ProviderId.CANVAS),
    )

    /**
     * The overlap capabilities both StudentHub and the VNU portal can serve. For these — and only
     * these — the user's preferred-source setting reorders the priority list above.
     */
    private val preferenceApplies = setOf(Capability.PROFILE, Capability.GRADES, Capability.EXAMS)

    /**
     * The providers currently connected, according to the stored session. StudentHub counts as
     * connected under either scheme — a captured bearer token or a cookie session from the
     * password login.
     */
    suspend fun connected(): Set<ProviderId> = buildSet {
        if (session.hasStudentHubSessionNow()) add(ProviderId.STUDENT_HUB)
        if (session.currentDaotaoCredentials() != null) add(ProviderId.VNU_DAOTAO)
        if (!session.currentCanvasToken().isNullOrBlank()) add(ProviderId.CANVAS)
    }

    /** Connected sources able to serve [capability], the user's preferred source first. */
    suspend fun sourcesFor(capability: Capability): List<ProviderId> {
        val connected = connected()
        val priority = sourcePriority.getValue(capability).let { base ->
            if (capability in preferenceApplies &&
                session.currentPreferredSource() == SourceId.VNU_PORTAL
            ) {
                // Stable partition: the portal moves first, everything else keeps its order.
                base.sortedByDescending { it == ProviderId.VNU_DAOTAO }
            } else {
                base
            }
        }
        return priority.filter { it in connected }
    }
}
