package me.june8th.euet.core.model

/** Source-agnostic domain models shared across features. */

data class StudentProfile(
    val code: String,
    val name: String,
    val email: String?,
    val className: String?,
    val program: String?,
    val major: String?,
)

data class Term(
    val id: Long?,
    val index: Int?,
    val code: String,
    val name: String,
)

data class TimetableEntry(
    val courseCode: String,
    val courseName: String,
    val room: String?,
    val weekday: Int,
    val sessionStart: Int?,
    val sessionEnd: Int?,
)

data class CourseGrade(
    val code: String,
    val name: String?,
    val credits: Double?,
    val point10: Double?,
    val point4: Double?,
    val letter: String?,
)

data class TermGrades(
    val termCode: String,
    val courses: List<CourseGrade>,
)

data class GpaSummary(
    val cpa: Double?,
    val gpa: Double?,
    val totalCredits: Double?,
    val accumulatedCredits: Double?,
)

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

data class Bill(
    val name: String?,
    val termCode: String?,
    val amount: Double?,
    val remaining: Double?,
    val status: String?,
    val invoiceUrl: String?,
)

data class AppNotification(
    val id: Long,
    val title: String,
    val content: String?,
    val createdAt: String?,
    val read: Boolean,
)

data class NewsItem(
    val id: Long,
    val title: String,
    val summary: String?,
    val imageUrl: String?,
    val createdAt: String?,
)
