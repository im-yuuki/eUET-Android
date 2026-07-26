package me.june8th.euet.core.data.source.canvas

import kotlinx.serialization.json.Json
import me.june8th.euet.core.model.PlannerItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasMappersTest {

    /** Mirrors the Json configuration in AppContainer (shared by the Canvas Retrofit). */
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    // --- unread_count: string-typed, with a bare-number variant ---

    @Test
    fun `unread count decodes from the documented string form`() {
        val dto = json.decodeFromString<UnreadCountDto>("""{"unread_count":"3"}""")
        assertEquals("3", dto.unreadCount)
        assertEquals(3, dto.unreadCount?.trim()?.toIntOrNull() ?: 0) // CanvasRepository's conversion
    }

    @Test
    fun `unread count tolerates a bare number thanks to lenient parsing`() {
        val dto = json.decodeFromString<UnreadCountDto>("""{"unread_count":3}""")
        assertEquals("3", dto.unreadCount)
    }

    @Test
    fun `unread count non-numeric or missing falls back to zero`() {
        val garbage = json.decodeFromString<UnreadCountDto>("""{"unread_count":"n/a"}""")
        assertEquals(0, garbage.unreadCount?.trim()?.toIntOrNull() ?: 0)
        val missing = json.decodeFromString<UnreadCountDto>("""{}""")
        assertEquals(0, missing.unreadCount?.trim()?.toIntOrNull() ?: 0)
    }

    // --- submissions: boolean false vs object polymorphism ---

    @Test
    fun `submissions false means not submitted`() {
        val dto = json.decodeFromString<PlannerItemDto>(
            """{"plannable_id":1,"plannable_type":"assignment","plannable":{"title":"HW1"},"submissions":false}""",
        )
        assertFalse(dto.toDomain()!!.isSubmitted)
    }

    @Test
    fun `submissions object with submitted true means submitted`() {
        val dto = json.decodeFromString<PlannerItemDto>(
            """
            {"plannable_id":1,"plannable_type":"assignment","plannable":{"title":"HW1"},
             "submissions":{"submitted":true,"graded":false,"missing":false}}
            """,
        )
        assertTrue(dto.toDomain()!!.isSubmitted)
    }

    @Test
    fun `submissions object without the flag or missing entirely means not submitted`() {
        val noFlag = json.decodeFromString<PlannerItemDto>(
            """{"plannable_id":1,"plannable_type":"assignment","plannable":{"title":"HW1"},"submissions":{"graded":true}}""",
        )
        assertFalse(noFlag.toDomain()!!.isSubmitted)
        val absent = PlannerItemDto(plannableId = 1, plannableType = "assignment", plannable = PlannableDto(title = "HW1"))
        assertFalse(absent.toDomain()!!.isSubmitted)
    }

    // --- planner kind mapping ---

    @Test
    fun `planner kinds map from plannable_type case-insensitively`() {
        fun kindOf(type: String?) =
            PlannerItemDto(plannableId = 1, plannableType = type, plannable = PlannableDto(title = "t"))
                .toDomain()!!.kind

        assertEquals(PlannerItem.Kind.ANNOUNCEMENT, kindOf("announcement"))
        assertEquals(PlannerItem.Kind.QUIZ, kindOf("Quiz"))
        assertEquals(PlannerItem.Kind.ASSIGNMENT, kindOf("assignment"))
        assertEquals(PlannerItem.Kind.ASSIGNMENT, kindOf("sub_assignment"))
        assertEquals(PlannerItem.Kind.DISCUSSION, kindOf("discussion_topic"))
        assertEquals(PlannerItem.Kind.DISCUSSION, kindOf("discussion"))
        assertEquals(PlannerItem.Kind.CALENDAR_EVENT, kindOf("calendar_event"))
        assertEquals(PlannerItem.Kind.OTHER, kindOf("wiki_page"))
        assertEquals(PlannerItem.Kind.OTHER, kindOf(null))
    }

    // --- planner item mapping ---

    @Test
    fun `planner item requires a title and prefers plannable_date`() {
        val noTitle = PlannerItemDto(plannableId = 1, plannable = PlannableDto(title = "  "))
        assertNull(noTitle.toDomain())

        val dto = PlannerItemDto(
            plannableId = 42,
            plannableType = "assignment",
            plannableDate = "2026-07-30T17:00:00Z",
            contextName = "INT1008",
            htmlUrl = "https://canvas.uet.vnu.edu.vn/courses/1/assignments/42",
            plannable = PlannableDto(id = 42, title = "HW1", dueAt = "2026-07-29T17:00:00Z"),
        )
        val item = dto.toDomain()!!
        assertEquals("assignment-42", item.id)
        assertEquals("2026-07-30T17:00:00Z", item.dueDate) // ISO string kept verbatim
        assertEquals("INT1008", item.courseName)
    }

    @Test
    fun `planner item falls back to due_at then todo_date`() {
        val dueAt = PlannerItemDto(
            plannableId = 1,
            plannableType = "assignment",
            plannable = PlannableDto(title = "t", dueAt = "2026-07-29T17:00:00Z", todoDate = "2026-08-01T00:00:00Z"),
        ).toDomain()!!
        assertEquals("2026-07-29T17:00:00Z", dueAt.dueDate)

        val todo = PlannerItemDto(
            plannableId = 1,
            plannableType = "wiki_page",
            plannable = PlannableDto(title = "t", todoDate = "2026-08-01T00:00:00Z"),
        ).toDomain()!!
        assertEquals("2026-08-01T00:00:00Z", todo.dueDate)

        val none = PlannerItemDto(plannableType = "assignment", plannable = PlannableDto(id = 9, title = "t")).toDomain()!!
        assertNull(none.dueDate)
        assertEquals("assignment-9", none.id) // plannable_id absent → plannable.id fallback
    }

    // --- dashboard cards ---

    @Test
    fun `dashboard card term accepts string, object and null forms`() {
        val asString = json.decodeFromString<DashboardCardDto>(
            """{"id":101,"shortName":"NMLT","term":"HK1 2025-2026"}""",
        ).toDomain()!!
        assertEquals("HK1 2025-2026", asString.term)

        val asObject = json.decodeFromString<DashboardCardDto>(
            """{"id":101,"shortName":"NMLT","term":{"id":3,"name":"HK1 2025-2026"}}""",
        ).toDomain()!!
        assertEquals("HK1 2025-2026", asObject.term)

        val asNull = json.decodeFromString<DashboardCardDto>(
            """{"id":101,"shortName":"NMLT","term":null}""",
        ).toDomain()!!
        assertNull(asNull.term)

        val asEmpty = json.decodeFromString<DashboardCardDto>(
            """{"id":101,"shortName":"NMLT","term":""}""",
        ).toDomain()!!
        assertNull(asEmpty.term) // blank strings are treated as no term
    }

    @Test
    fun `dashboard card name falls back through shortName originalName courseCode`() {
        assertNull(DashboardCardDto(id = null).toDomain()) // no id → unusable card
        assertEquals("A", DashboardCardDto(id = 1, shortName = "A", originalName = "B", courseCode = "C").toDomain()!!.name)
        assertEquals("B", DashboardCardDto(id = 1, originalName = "B", courseCode = "C").toDomain()!!.name)
        assertEquals("C", DashboardCardDto(id = 1, courseCode = "C").toDomain()!!.name)
        assertEquals("Course 1", DashboardCardDto(id = 1).toDomain()!!.name)
    }

    // --- missing submissions ---

    @Test
    fun `missing submission maps and falls back on a blank name`() {
        assertNull(MissingSubmissionDto(id = null, name = "x").toDomain())

        val dto = json.decodeFromString<MissingSubmissionDto>(
            """{"id":7,"name":" ","due_at":"2026-07-01T10:00:00Z","course_id":3,"html_url":"https://c/x","points_possible":10}""",
        )
        val missing = dto.toDomain()!!
        assertEquals("Assignment 7", missing.name) // blank name → placeholder
        assertEquals("2026-07-01T10:00:00Z", missing.dueAt)
        assertEquals(3L, missing.courseId)
    }
}
