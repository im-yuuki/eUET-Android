package me.june8th.euet.core.model

import kotlinx.serialization.Serializable

/**
 * Source-agnostic domain models shared across features.
 *
 * All of them are [Serializable] so screen snapshots can be persisted by the offline cache
 * ([me.june8th.euet.core.datastore.SnapshotCache]) without a parallel set of cache entities.
 */

@Serializable
data class StudentProfile(
    val code: String,
    val name: String,
    val email: String?,
    val className: String?,
    val program: String?,
    val major: String?,
)

@Serializable
data class Term(
    val id: Long?,
    val index: Int?,
    val code: String,
    val name: String,
)

@Serializable
data class TimetableEntry(
    val courseCode: String,
    val courseName: String,
    val room: String?,
    val weekday: Int,
    val sessionStart: Int?,
    val sessionEnd: Int?,
)

@Serializable
data class CourseGrade(
    val code: String,
    val name: String?,
    val credits: Double?,
    val point10: Double?,
    val point4: Double?,
    val letter: String?,
)

@Serializable
data class TermGrades(
    val termCode: String,
    val courses: List<CourseGrade>,
)

@Serializable
data class GpaSummary(
    val cpa: Double?,
    val gpa: Double?,
    val totalCredits: Double?,
    val accumulatedCredits: Double?,
)

@Serializable
data class Exam(
    val courseCode: String?,
    val courseName: String?,
    val date: String?,
    val startTime: String?,
    val room: String?,
    val method: String?,
    val type: String?,
    val seat: String?,
)

@Serializable
data class TermPerformance(
    val termCode: String,
    val termName: String?,
    val termGpa: Double?,
    val cumulativeGpa: Double?,
    /** Conduct score (0–100). Null until the portal's conduct sub-tab request is captured. */
    val conductScore: Int?,
    val credits: Double?,
)

/** A downloadable portal document (syllabus / curriculum PDF, form). */
@Serializable
data class PortalDocument(
    val title: String,
    val url: String,
)

@Serializable
data class Bill(
    val name: String?,
    val termCode: String?,
    val amount: Double?,
    val remaining: Double?,
    val status: String?,
    val invoiceUrl: String?,
)

@Serializable
data class AppNotification(
    val id: Long,
    val title: String,
    val content: String?,
    val createdAt: String?,
    val read: Boolean,
)

@Serializable
data class NewsItem(
    val id: Long,
    val title: String,
    val summary: String?,
    val imageUrl: String?,
    val createdAt: String?,
)

// --- Canvas LMS (naming mirrors the iOS Core/Models/Canvas.swift) ---

/** A Canvas LMS course card. */
@Serializable
data class CanvasCourse(
    val id: String,
    val name: String,
    val courseCode: String?,
    val term: String?,
    val imageUrl: String?,
    val url: String?,
)

/** A Canvas planner item (announcement, quiz, assignment, …). */
@Serializable
data class PlannerItem(
    val id: String,
    val kind: Kind,
    val title: String,
    val courseName: String?,
    /** ISO-8601 timestamp the item is due/scheduled for, exactly as Canvas returns it. */
    val dueDate: String?,
    val url: String?,
    val isSubmitted: Boolean,
) {
    enum class Kind { ANNOUNCEMENT, QUIZ, ASSIGNMENT, DISCUSSION, CALENDAR_EVENT, OTHER }
}

/** A submittable assignment whose due date passed without a submission. */
@Serializable
data class MissingSubmission(
    val id: Long,
    val name: String,
    val dueAt: String?,
    val courseId: Long?,
    val url: String?,
)

/** Aggregate Canvas dashboard counters. */
@Serializable
data class CanvasSummary(
    val unreadInbox: Int = 0,
    val missingSubmissions: Int = 0,
)

/**
 * One StudentHub captcha challenge: the id the login body has to echo back, and the already
 * decoded image bytes ready for `BitmapFactory`. Not serializable — challenges are single-use and
 * must never be cached.
 */
class CaptchaChallenge(
    val id: String,
    val image: ByteArray,
)
