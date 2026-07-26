package me.june8th.euet.core.data.source.canvas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * Canvas REST DTOs — same lenient posture as the StudentHub DTOs: every field nullable with a
 * default (plus ignoreUnknownKeys/isLenient on the shared Json), so shape drift on the UET
 * instance never crashes deserialization. Fields Canvas is known to return with unstable types
 * (term, submissions) stay raw [JsonElement]s and are untangled in the mappers.
 */

/** `GET /api/v1/users/self` — used only to validate a pasted access token. */
@Serializable
data class CanvasSelfDto(
    val id: Long? = null,
    val name: String? = null,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("primary_email") val primaryEmail: String? = null,
)

/** `GET /api/v1/dashboard/dashboard_cards` — note this endpoint uses camelCase keys. */
@Serializable
data class DashboardCardDto(
    val id: Long? = null,
    val shortName: String? = null,
    val originalName: String? = null,
    val courseCode: String? = null,
    /** Usually a plain string ("HK1 2025-2026"), occasionally an object — parsed in the mapper. */
    val term: JsonElement? = null,
    val href: String? = null,
    val image: String? = null,
)

/** `GET /api/v1/planner/items` element. */
@Serializable
data class PlannerItemDto(
    @SerialName("plannable_id") val plannableId: Long? = null,
    @SerialName("plannable_type") val plannableType: String? = null,
    @SerialName("plannable_date") val plannableDate: String? = null,
    @SerialName("context_name") val contextName: String? = null,
    @SerialName("course_id") val courseId: Long? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val plannable: PlannableDto? = null,
    /** Either the boolean `false` or an object with submission flags — hence the raw element. */
    val submissions: JsonElement? = null,
)

@Serializable
data class PlannableDto(
    val id: Long? = null,
    val title: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("todo_date") val todoDate: String? = null,
)

/** `GET /api/v1/users/self/missing_submissions` element (a trimmed Assignment object). */
@Serializable
data class MissingSubmissionDto(
    val id: Long? = null,
    val name: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("course_id") val courseId: Long? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
)

/** `GET /api/v1/conversations/unread_count` body. */
@Serializable
data class UnreadCountDto(
    /** Canvas returns the count as a JSON *string* (`{"unread_count":"3"}`); lenient parsing also accepts a bare number. */
    @SerialName("unread_count") val unreadCount: String? = null,
)
