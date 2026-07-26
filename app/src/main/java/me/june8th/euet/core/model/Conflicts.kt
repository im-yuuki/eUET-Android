package me.june8th.euet.core.model

import kotlinx.serialization.Serializable

/**
 * Cross-source conflict model, shared with the iOS app's identical feature: when both StudentHub
 * and the VNU portal are connected, the preferred source's records are displayed verbatim and any
 * disagreement with the secondary source is surfaced as [DataConflict]s — never merged.
 */

/** The two providers that can serve the same capability (profile, grades/transcript, exams). */
@Serializable
enum class SourceId {
    STUDENT_HUB,
    VNU_PORTAL,
}

/** The opposite source, used to label the secondary column in the diff sheet. */
fun SourceId.other(): SourceId = when (this) {
    SourceId.STUDENT_HUB -> SourceId.VNU_PORTAL
    SourceId.VNU_PORTAL -> SourceId.STUDENT_HUB
}

/**
 * Stable field keys carried in [FieldDiff.fieldLabel]. The core layer can't reach string
 * resources, so it emits these keys and the UI maps them to localized labels at display time
 * (unknown keys render as-is).
 */
object ConflictFields {
    const val COURSE_NAME = "course_name"
    const val CREDITS = "credits"
    const val SCORE_10 = "score10"
    const val SCORE_4 = "score4"
    const val LETTER = "letter"
    const val EXAM_DATE = "exam_date"
    const val EXAM_TIME = "exam_time"
    const val EXAM_ROOM = "exam_room"
    const val EXAM_SEAT = "exam_seat"
    const val EXAM_METHOD = "exam_method"
    const val FULL_NAME = "full_name"
    const val STUDENT_CODE = "student_code"
    const val EMAIL = "email"
    const val CLASS_NAME = "class_name"
    const val MAJOR = "major"
    const val PROGRAM = "program"
}

/** One field whose value differs between the preferred and the secondary source. */
@Serializable
data class FieldDiff(
    /** A stable key from [ConflictFields]; localized by the UI. */
    val fieldLabel: String,
    val preferredValue: String?,
    val otherValue: String?,
)

/**
 * One record that disagrees across sources. Either [fields] is non-empty (a matched record whose
 * values differ) or [onlyIn] is set with no field diffs (the record exists in a single source).
 */
@Serializable
data class DataConflict(
    val recordKey: String,
    val recordLabel: String,
    val fields: List<FieldDiff>,
    val onlyIn: SourceId? = null,
)

/**
 * The outcome of one dual-source comparison. Cached under a sibling key of the payload (e.g.
 * "grades.conflicts") so banners survive offline restarts; overwritten on every successful
 * dual fetch.
 */
@Serializable
data class ConflictReport(
    /** The preferred source — the one whose records are displayed. */
    val source: SourceId,
    val conflicts: List<DataConflict>,
)

/**
 * A payload tagged with the source that served it. [conflicts] is empty at fetch time — the
 * secondary source is compared in the background and its report arrives via the repository's
 * conflict listener so the UI is never blocked on it.
 */
data class Sourced<T>(
    val value: T,
    val source: SourceId,
    val conflicts: List<DataConflict> = emptyList(),
)
