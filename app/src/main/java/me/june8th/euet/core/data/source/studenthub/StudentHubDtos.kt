package me.june8th.euet.core.data.source.studenthub

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * DTOs mirror the fields documented in the StudentHub HAR notes. Every field is nullable with a
 * default so that a partial/varying live response never crashes deserialization (the Json config
 * also sets ignoreUnknownKeys = true). If live probing shows responses are wrapped in an envelope
 * like { "data": ... }, introduce [ApiEnvelope] below and change the API return types accordingly.
 */

@Serializable
data class ApiEnvelope<T>(
    val data: T? = null,
    val message: String? = null,
    val status: Int? = null,
    val code: Int? = null,
)

@Serializable
data class StudentDetailDto(
    val studentCode: String? = null,
    val name: String? = null,
    val schoolEmail: String? = null,
    val classCode: String? = null,
    val programName: String? = null,
    val majorName: String? = null,
)

@Serializable
data class TermDto(
    val id: Long? = null,
    val index: Int? = null,
    val termCode: String? = null,
    val name: String? = null,
)

@Serializable
data class TkbItemDto(
    val courseCode: String? = null,
    val courseName: String? = null,
    val roomName: String? = null,
    val sessionStart: Int? = null,
    val sessionEnd: Int? = null,
    val weekday: Int? = null,
)

@Serializable
data class GradeDto(
    val courseCode: String? = null,
    val courseName: String? = null,
    val courseCredit: Double? = null,
    val point4: Double? = null,
    val point10: Double? = null,
    val letterGrade: String? = null,
    val termCode: String? = null,
)

@Serializable
data class ResultsDto(
    val cpa: Double? = null,
    val gpa: Double? = null,
    val totalCredits: Double? = null,
    val totalAccumulatedCredits: Double? = null,
)

@Serializable
data class ExamDto(
    val courseCode: String? = null,
    val courseName: String? = null,
    val date: String? = null,
    val startTime: String? = null,
    val room: String? = null,
    val method: String? = null,
    val type: String? = null,
    val seatNumber: String? = null,
)

@Serializable
data class BillDto(
    val name: String? = null,
    val termCode: String? = null,
    val amount: Double? = null,
    @SerialName("remainingAmount") val remaining: Double? = null,
    val status: String? = null,
    val invoiceUrl: String? = null,
)

@Serializable
data class NotiDto(
    val id: Long? = null,
    val title: String? = null,
    val content: String? = null,
    val createdAt: String? = null,
    @SerialName("read") val isRead: Boolean? = null,
)

@Serializable
data class NewsDto(
    val id: Long? = null,
    val title: String? = null,
    val summary: String? = null,
    val content: String? = null,
    val imageUrl: String? = null,
    val createdAt: String? = null,
)

/** Spring Data Page envelope (notifications and lookup endpoints use this shape). */
@Serializable
data class PageDto<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
    val size: Int = 0,
    val last: Boolean = true,
)

// --- Request bodies ---

@Serializable
data class TermRequest(val termCode: String)

@Serializable
class EmptyRequest
