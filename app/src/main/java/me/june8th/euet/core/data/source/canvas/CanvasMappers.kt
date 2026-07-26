package me.june8th.euet.core.data.source.canvas

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import me.june8th.euet.core.model.CanvasCourse
import me.june8th.euet.core.model.MissingSubmission
import me.june8th.euet.core.model.PlannerItem

fun DashboardCardDto.toDomain(): CanvasCourse? {
    val id = id ?: return null
    return CanvasCourse(
        id = id.toString(),
        name = shortName ?: originalName ?: courseCode ?: "Course $id",
        courseCode = courseCode,
        term = term.asTermName(),
        imageUrl = image,
        url = href,
    )
}

fun PlannerItemDto.toDomain(): PlannerItem? {
    val title = plannable?.title?.takeIf { it.isNotBlank() } ?: return null
    return PlannerItem(
        id = "${plannableType ?: "item"}-${plannableId ?: plannable.id ?: 0}",
        kind = plannerKind(plannableType),
        title = title,
        courseName = contextName,
        dueDate = plannableDate ?: plannable.dueAt ?: plannable.todoDate,
        url = htmlUrl,
        isSubmitted = submissions.isSubmitted(),
    )
}

fun MissingSubmissionDto.toDomain(): MissingSubmission? {
    val id = id ?: return null
    return MissingSubmission(
        id = id,
        name = name?.takeIf { it.isNotBlank() } ?: "Assignment $id",
        dueAt = dueAt,
        courseId = courseId,
        url = htmlUrl,
    )
}

private fun plannerKind(raw: String?): PlannerItem.Kind = when (raw?.lowercase()) {
    "announcement" -> PlannerItem.Kind.ANNOUNCEMENT
    "quiz" -> PlannerItem.Kind.QUIZ
    "assignment", "sub_assignment" -> PlannerItem.Kind.ASSIGNMENT
    "discussion_topic", "discussion" -> PlannerItem.Kind.DISCUSSION
    "calendar_event" -> PlannerItem.Kind.CALENDAR_EVENT
    else -> PlannerItem.Kind.OTHER
}

/** Dashboard cards usually carry the term as a string, but tolerate `{"name": …}` too. */
private fun JsonElement?.asTermName(): String? = when (this) {
    is JsonPrimitive -> contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
    is JsonObject -> (this["name"] as? JsonPrimitive)?.contentOrNull
    else -> null
}

/** `submissions` is `false` for non-submittables and an object with flags for assignments. */
private fun JsonElement?.isSubmitted(): Boolean =
    ((this as? JsonObject)?.get("submitted") as? JsonPrimitive)?.booleanOrNull ?: false
