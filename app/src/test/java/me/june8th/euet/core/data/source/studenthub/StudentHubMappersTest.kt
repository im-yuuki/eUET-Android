package me.june8th.euet.core.data.source.studenthub

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentHubMappersTest {

    /** Mirrors the Json configuration in AppContainer (the app's Retrofit converter). */
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    // --- Timetable / weekday scheme ---

    @Test
    fun `timetable entry keeps the portal weekday scheme Mon2 to Sun8`() {
        val monday = TkbItemDto(courseCode = "INT1008", courseName = "NMLT", weekday = 2).toDomain()
        assertEquals(2, monday.weekday)
        val sunday = TkbItemDto(courseCode = "INT1008", courseName = "NMLT", weekday = 8).toDomain()
        assertEquals(8, sunday.weekday)
    }

    @Test
    fun `timetable entry tolerates a fully null dto`() {
        val entry = TkbItemDto().toDomain()
        assertEquals("", entry.courseCode)
        assertEquals("", entry.courseName)
        assertEquals(0, entry.weekday) // null weekday collapses to 0 (never matches a real day)
        assertNull(entry.room)
        assertNull(entry.sessionStart)
        assertNull(entry.sessionEnd)
    }

    @Test
    fun `tkb decoding is lenient about numbers arriving as strings`() {
        val dto = json.decodeFromString<TkbItemDto>(
            """{"courseCode":"INT1008","weekday":"5","sessionStart":"1","sessionEnd":3,"unknown_key":true}""",
        )
        assertEquals(5, dto.weekday)
        assertEquals(1, dto.sessionStart)
        assertEquals(3, dto.sessionEnd)
    }

    // --- Grades / transcript grouping ---

    @Test
    fun `toTermGrades groups by term newest first and defaults missing terms to empty`() {
        val grades = listOf(
            GradeDto(courseCode = "INT1008", termCode = "241", point4 = 3.5),
            GradeDto(courseCode = "MAT1093", termCode = "242"),
            GradeDto(courseCode = "PHY1100", termCode = "241"),
            GradeDto(courseCode = "XYZ0000", termCode = null),
        )
        val terms = grades.toTermGrades()
        assertEquals(listOf("242", "241", ""), terms.map { it.termCode })
        assertEquals(listOf("INT1008", "PHY1100"), terms[1].courses.map { it.code })
        assertEquals(3.5, terms[1].courses[0].point4!!, 1e-9)
    }

    @Test
    fun `grade dto with all nulls maps to an empty-code course`() {
        val course = GradeDto().toDomain()
        assertEquals("", course.code)
        assertNull(course.name)
        assertNull(course.credits)
        assertNull(course.point10)
        assertNull(course.point4)
        assertNull(course.letter)
    }

    // --- Terms ---

    @Test
    fun `term name falls back to the term code`() {
        val term = TermDto(id = 7, index = 3, termCode = "242", name = null).toDomain()
        assertEquals("242", term.name)
        assertEquals("242", term.code)
        assertEquals(7L, term.id)
        assertEquals(3, term.index)
    }

    // --- Bills ---

    @Test
    fun `bill decoding maps remainingAmount and ignores unknown keys`() {
        val dto = json.decodeFromString<BillDto>(
            """
            {
              "name": "Học phí HK1",
              "termCode": "241",
              "amount": 6500000,
              "remainingAmount": 0,
              "status": "PAID",
              "somethingNew": {"nested": true}
            }
            """,
        )
        val bill = dto.toDomain()
        assertEquals("Học phí HK1", bill.name)
        assertEquals(6_500_000.0, bill.amount!!, 1e-9)
        assertEquals(0.0, bill.remaining!!, 1e-9) // @SerialName("remainingAmount")
        assertEquals("PAID", bill.status) // status is passed through verbatim for the UI to map
    }

    @Test
    fun `bill with missing fields decodes to nulls`() {
        val bill = json.decodeFromString<BillDto>("""{"status":"UNPAID"}""").toDomain()
        assertEquals("UNPAID", bill.status)
        assertNull(bill.name)
        assertNull(bill.amount)
        assertNull(bill.remaining)
        assertNull(bill.invoiceUrl)
    }

    // --- Notifications / news ---

    @Test
    fun `notification decoding maps the read key and defaults safely`() {
        val read = json.decodeFromString<NotiDto>(
            """{"id":10,"title":"Lịch thi","read":true,"extra":1}""",
        ).toDomain()
        assertEquals(10L, read.id)
        assertTrue(read.read)

        val bare = json.decodeFromString<NotiDto>("""{"title":"x"}""").toDomain()
        assertEquals(0L, bare.id) // null id collapses to 0
        assertFalse(bare.read) // null read defaults to unread
    }

    @Test
    fun `news summary falls back to content`() {
        val news = NewsDto(id = 1, title = "Tin", summary = null, content = "Nội dung").toDomain()
        assertEquals("Nội dung", news.summary)
        val withSummary = NewsDto(id = 1, title = "Tin", summary = "Tóm tắt", content = "Nội dung").toDomain()
        assertEquals("Tóm tắt", withSummary.summary)
    }

    // --- Profile / GPA ---

    @Test
    fun `student detail decodes with unknown keys and maps null-safely`() {
        val profile = json.decodeFromString<StudentDetailDto>(
            """{"studentCode":"21020001","name":"Nguyễn Văn A","schoolEmail":null,"campus":"HN"}""",
        ).toDomain()
        assertEquals("21020001", profile.code)
        assertEquals("Nguyễn Văn A", profile.name)
        assertNull(profile.email) // explicit JSON null tolerated
        assertNull(profile.className)
    }

    @Test
    fun `results dto maps accumulated credits`() {
        val gpa = ResultsDto(cpa = 3.45, gpa = 3.6, totalCredits = 120.0, totalAccumulatedCredits = 96.0).toDomain()
        assertEquals(3.45, gpa.cpa!!, 1e-9)
        assertEquals(96.0, gpa.accumulatedCredits!!, 1e-9)
    }

    // --- Envelopes ---

    @Test
    fun `page envelope decodes content and defaults pagination fields`() {
        val page = json.decodeFromString<PageDto<NotiDto>>(
            """{"content":[{"id":1,"title":"a"},{"id":2,"title":"b"}],"totalElements":2,"unknown":[]}""",
        )
        assertEquals(2, page.content.size)
        assertEquals(2L, page.totalElements)
        assertTrue(page.last) // default when absent
        assertEquals(0, page.number)

        val empty = json.decodeFromString<PageDto<NotiDto>>("{}")
        assertTrue(empty.content.isEmpty())
    }
}
