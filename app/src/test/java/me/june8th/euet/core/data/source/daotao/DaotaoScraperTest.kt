package me.june8th.euet.core.data.source.daotao

import me.june8th.euet.core.model.TermGrades
import me.june8th.euet.core.model.CourseGrade
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DaotaoScraperTest {

    // --- isAuthenticated ---

    @Test
    fun `login form present means unauthenticated`() {
        val doc = Jsoup.parse(
            """
            <form>
              <input name="txtLoginId" type="text">
              <input name="txtPassword" type="password">
            </form>
            """,
        )
        assertFalse(DaotaoScraper.isAuthenticated(doc))
    }

    @Test
    fun `not-logged-in notice means unauthenticated`() {
        val doc = Jsoup.parse("<p>Bạn CHƯA ĐĂNG NHẬP hoặc phiên đã hết.</p>")
        assertFalse(DaotaoScraper.isAuthenticated(doc))
    }

    @Test
    fun `expired session notice means unauthenticated`() {
        val doc = Jsoup.parse("<p>Phiên làm việc của bạn đã hết. Vui lòng đăng nhập lại.</p>")
        assertFalse(DaotaoScraper.isAuthenticated(doc))
    }

    @Test
    fun `plain content page without logout link is still authenticated`() {
        val doc = Jsoup.parse("<table><tr><td>INT1008</td></tr></table>")
        assertTrue(DaotaoScraper.isAuthenticated(doc))
    }

    // --- parseProfile / parseStudentContext ---

    private val profileHtml = """
        <form>
          <input name="StdCode" value=" 21020001 " disabled>
          <input name="StdName" value="Nguyễn Văn A" disabled>
          <input name="StdDob" value="01/01/2003" disabled>
          <input type="hidden" name="hidStdID" value="123456">
          <select name="ClsID">
            <option value="1">K66-CA</option>
            <option value="2" selected>K66-CB</option>
          </select>
          <select name="PrmID"><option value="9" selected>Chuẩn CNTT</option></select>
          <select name="BrcID"><option value="7" selected>Công nghệ thông tin</option></select>
          <select name="UnivID"><option value="002" selected>Trường ĐH Công nghệ</option></select>
        </form>
    """

    @Test
    fun `parseProfile reads disabled inputs and selected options`() {
        val profile = DaotaoScraper.parseProfile(Jsoup.parse(profileHtml))!!
        assertEquals("21020001", profile.code) // trimmed
        assertEquals("Nguyễn Văn A", profile.name)
        assertNull(profile.email) // never exposed by this portal
        assertEquals("K66-CB", profile.className) // the selected option, not the first
        assertEquals("Chuẩn CNTT", profile.program)
        assertEquals("Công nghệ thông tin", profile.major)
    }

    @Test
    fun `parseProfile returns null without a student code`() {
        assertNull(DaotaoScraper.parseProfile(Jsoup.parse("<input name='StdName' value='X'>")))
        assertNull(DaotaoScraper.parseProfile(Jsoup.parse("<input name='StdCode' value='  '>")))
    }

    @Test
    fun `parseProfile falls back to code when the name is missing`() {
        val doc = Jsoup.parse("<input name='StdCode' value='21020001'>")
        val profile = DaotaoScraper.parseProfile(doc)!!
        assertEquals("21020001", profile.name)
        assertNull(profile.className)
    }

    @Test
    fun `parseStudentContext extracts internal ids`() {
        val ctx = DaotaoScraper.parseStudentContext(Jsoup.parse(profileHtml))
        assertEquals("21020001", ctx.studentCode)
        assertEquals("123456", ctx.internalStudentId)
        assertEquals("002", ctx.universityId) // option *value*, not label
    }

    @Test
    fun `parseStudentContext tolerates a page with nothing on it`() {
        val ctx = DaotaoScraper.parseStudentContext(Jsoup.parse("<p>hi</p>"))
        assertNull(ctx.studentCode)
        assertNull(ctx.internalStudentId)
        assertNull(ctx.universityId)
    }

    // --- parseGrades ---

    @Test
    fun `parseGrades groups courses under interleaved term headers`() {
        val doc = Jsoup.parse(
            """
            <table>
              <tr><td colspan="7">HỌC KỲ 2 - 2024-2025. MÃ HỌC KỲ 242</td></tr>
              <tr><td>1</td><td>INT1008</td><td>Nhập môn lập trình</td><td>4</td><td>8,5</td><td>B+</td><td>3.5</td></tr>
              <tr><td>2</td><td>MAT1093</td><td>Đại số</td><td>4</td><td>9.0</td><td>A</td><td>3.7</td></tr>
              <tr><td colspan="7">HỌC KỲ 1 - 2024-2025. MÃ HỌC KỲ 241</td></tr>
              <tr><td>1</td><td>PHY1100</td><td>Cơ - Nhiệt</td><td>3</td><td>7,8</td><td>B</td><td>3.0</td></tr>
            </table>
            """,
        )
        val terms = DaotaoScraper.parseGrades(doc)
        assertEquals(2, terms.size)
        assertEquals("HỌC KỲ 2 - 2024-2025", terms[0].termCode) // "MÃ HỌC KỲ" suffix stripped
        assertEquals("HỌC KỲ 1 - 2024-2025", terms[1].termCode)
        assertEquals(2, terms[0].courses.size)
        val first = terms[0].courses[0]
        assertEquals("INT1008", first.code)
        assertEquals("Nhập môn lập trình", first.name)
        assertEquals(4.0, first.credits!!, 1e-9)
        assertEquals(8.5, first.point10!!, 1e-9) // decimal comma normalized
        assertEquals("B+", first.letter)
        assertEquals(3.5, first.point4!!, 1e-9)
        assertEquals("PHY1100", terms[1].courses.single().code)
    }

    @Test
    fun `parseGrades resolves columns relative to the course-code cell`() {
        // No leading STT column: code sits at index 0, so all offsets shift with it.
        val doc = Jsoup.parse(
            """
            <table>
              <tr><td>HỌC KỲ 1 - 2023-2024. MÃ HỌC KỲ 231</td></tr>
              <tr><td>INT2204</td><td>OOP</td><td>3</td><td>8.0</td><td>B+</td><td>3.5</td></tr>
            </table>
            """,
        )
        val course = DaotaoScraper.parseGrades(doc).single().courses.single()
        assertEquals("INT2204", course.code)
        assertEquals("OOP", course.name)
        assertEquals(3.0, course.credits!!, 1e-9)
        assertEquals(8.0, course.point10!!, 1e-9)
        assertEquals("B+", course.letter)
        assertEquals(3.5, course.point4!!, 1e-9)
    }

    @Test
    fun `parseGrades skips malformed rows and tolerates short rows`() {
        val doc = Jsoup.parse(
            """
            <table>
              <tr><td>HỌC KỲ 1 - 2023-2024. MÃ HỌC KỲ 231</td></tr>
              <tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
              <tr><td>Tổng kết</td><td>không phải mã</td></tr>
              <tr><td>1</td><td>INT2204</td><td>OOP</td></tr>
            </table>
            """,
        )
        val terms = DaotaoScraper.parseGrades(doc)
        val course = terms.single().courses.single() // NBSP + summary rows dropped
        assertEquals("INT2204", course.code)
        assertEquals("OOP", course.name)
        assertNull(course.credits) // cells past the row's end are simply null
        assertNull(course.point10)
        assertNull(course.point4)
        assertNull(course.letter)
    }

    @Test
    fun `parseGrades leaves non-numeric scores null`() {
        val doc = Jsoup.parse(
            """
            <table>
              <tr><td>HỌC KỲ 1 - 2023-2024. MÃ HỌC KỲ 231</td></tr>
              <tr><td>1</td><td>INT2204</td><td>OOP</td><td>3</td><td>Miễn</td><td>P</td><td>-</td></tr>
            </table>
            """,
        )
        val course = DaotaoScraper.parseGrades(doc).single().courses.single()
        assertNull(course.point10)
        assertNull(course.point4)
        assertEquals("P", course.letter)
    }

    @Test
    fun `parseGrades buckets courses before any term header under an empty term`() {
        val doc = Jsoup.parse(
            """
            <table>
              <tr><td>1</td><td>INT2204</td><td>OOP</td><td>3</td><td>8.0</td><td>B+</td><td>3.5</td></tr>
            </table>
            """,
        )
        val terms = DaotaoScraper.parseGrades(doc)
        assertEquals("", terms.single().termCode)
    }

    @Test
    fun `parseGrades returns empty for a page without tables or with an empty table`() {
        assertTrue(DaotaoScraper.parseGrades(Jsoup.parse("<p>no data</p>")).isEmpty())
        assertTrue(DaotaoScraper.parseGrades(Jsoup.parse("<table></table>")).isEmpty())
    }

    // --- parseExamTerms ---

    @Test
    fun `parseExamTerms reads the term dropdown newest first`() {
        val doc = Jsoup.parse(
            """
            <select name="selTerm">
              <option value="">-- Chọn kỳ --</option>
              <option value="242">Học kỳ 2 - 2024-2025</option>
              <option value="241"></option>
            </select>
            """,
        )
        val terms = DaotaoScraper.parseExamTerms(doc)
        assertEquals(2, terms.size) // empty-value placeholder dropped
        assertEquals("242", terms[0].code)
        assertEquals("Học kỳ 2 - 2024-2025", terms[0].name)
        assertEquals(2, terms[0].index) // newest gets the highest index
        assertNull(terms[0].id) // this portal has no numeric term ids
        assertEquals("241", terms[1].name) // empty label falls back to the value
        assertEquals(1, terms[1].index)
    }

    @Test
    fun `parseExamTerms returns empty without a term select`() {
        assertTrue(DaotaoScraper.parseExamTerms(Jsoup.parse("<select name='other'><option value='1'>x</option></select>")).isEmpty())
    }

    // --- parseExams ---

    @Test
    fun `parseExams locates the table by header signature and maps columns`() {
        val doc = Jsoup.parse(
            """
            <table><tr><td>menu</td></tr><tr><td>home</td></tr><tr><td>logout</td></tr></table>
            <table>
              <tr><td>STT</td><td>Mã KT</td><td>Kỳ thi</td><td>Ngày thi</td><td>Ca thi</td><td>H.Thức thi</td><td>Phòng</td><td>SBD</td></tr>
              <tr><td>1</td><td>INT1008 1</td><td>Nhập môn lập trình</td><td>15/06/2025</td><td>08:00</td><td>Viết</td><td>301-G2</td><td>21</td></tr>
              <tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
            </table>
            """,
        )
        val exams = DaotaoScraper.parseExams(doc)
        val exam = exams.single() // NBSP layout row skipped, nav table ignored
        assertEquals("INT1008 1", exam.courseCode)
        assertEquals("Nhập môn lập trình", exam.courseName)
        assertEquals("15/06/2025", exam.date)
        assertEquals("08:00", exam.startTime)
        assertEquals("Viết", exam.method)
        assertEquals("301-G2", exam.room)
        assertEquals("21", exam.seat)
        assertNull(exam.type)
    }

    @Test
    fun `parseExams keeps rows that have a name plus either code or date`() {
        val doc = Jsoup.parse(
            """
            <table>
              <tr><td>STT</td><td>Mã KT</td><td>Kỳ thi</td><td>Ngày thi</td><td>Ca thi</td><td>H.Thức thi</td><td>Phòng</td><td>SBD</td></tr>
              <tr><td>1</td><td></td><td>Chỉ có ngày</td><td>20/06/2025</td><td></td><td></td><td></td><td></td></tr>
              <tr><td>2</td><td></td><td>Không mã không ngày</td><td></td><td></td><td></td><td></td><td></td></tr>
            </table>
            """,
        )
        val exams = DaotaoScraper.parseExams(doc)
        assertEquals(1, exams.size)
        assertNull(exams[0].courseCode)
        assertEquals("Chỉ có ngày", exams[0].courseName)
        assertNull(exams[0].room) // empty cells become null, not ""
    }

    @Test
    fun `parseExams falls back to positional columns when no header signature matches`() {
        val doc = Jsoup.parse(
            """
            <table>
              <tr><td>#</td><td>Code</td><td>Course</td></tr>
              <tr><td>1</td><td>INT1008</td><td>Nhập môn lập trình</td></tr>
            </table>
            """,
        )
        val exam = DaotaoScraper.parseExams(doc).single()
        assertEquals("INT1008", exam.courseCode) // default codeCol = 1
        assertEquals("Nhập môn lập trình", exam.courseName) // default nameCol = 2
        assertNull(exam.date)
    }

    @Test
    fun `parseExams returns empty on an empty page`() {
        assertTrue(DaotaoScraper.parseExams(Jsoup.parse("<p>nothing</p>")).isEmpty())
    }

    // --- parseTermPerformance ---

    @Test
    fun `parseTermPerformance maps header columns and skips non-term rows`() {
        val doc = Jsoup.parse(
            """
            <table>
              <tr><td>Học kỳ</td><td>TBC học kỳ</td><td>TBC tích lũy</td><td>Điểm rèn luyện</td></tr>
              <tr><td>HỌC KỲ 1 - 2024-2025</td><td>3,45</td><td>3.50</td><td>85</td></tr>
              <tr><td>Ghi chú</td><td></td><td></td><td></td></tr>
            </table>
            """,
        )
        val rows = DaotaoScraper.parseTermPerformance(doc)
        val row = rows.single()
        assertEquals("HỌC KỲ 1 - 2024-2025", row.termCode)
        assertEquals(3.45, row.termGpa!!, 1e-9)
        assertEquals(3.50, row.cumulativeGpa!!, 1e-9)
        assertEquals(85, row.conductScore)
        assertNull(row.credits)
    }

    @Test
    fun `parseTermPerformance returns empty without a table`() {
        assertTrue(DaotaoScraper.parseTermPerformance(Jsoup.parse("<p>x</p>")).isEmpty())
    }

    // --- computeTermPerformance (transcript-derived GPA fallback) ---

    @Test
    fun `computeTermPerformance credit-weights GPA and accumulates CPA oldest to newest`() {
        val transcript = listOf( // newest first, as parseGrades emits
            TermGrades(
                "242",
                listOf(
                    grade(point4 = 4.0, credits = 3.0),
                    grade(point4 = 3.0, credits = 4.0),
                ),
            ),
            TermGrades("241", listOf(grade(point4 = 3.5, credits = 3.0))),
        )
        val perf = DaotaoScraper.computeTermPerformance(transcript)
        assertEquals(listOf("242", "241"), perf.map { it.termCode }) // input order preserved
        // 242: (4*3 + 3*4) / 7 = 3.4285… → 3.43; CPA = (10.5 + 24) / 10 = 3.45
        assertEquals(3.43, perf[0].termGpa!!, 1e-9)
        assertEquals(3.45, perf[0].cumulativeGpa!!, 1e-9)
        assertEquals(7.0, perf[0].credits!!, 1e-9)
        // 241 (oldest): GPA = CPA = 3.5 over 3 credits
        assertEquals(3.5, perf[1].termGpa!!, 1e-9)
        assertEquals(3.5, perf[1].cumulativeGpa!!, 1e-9)
        assertEquals(3.0, perf[1].credits!!, 1e-9)
    }

    @Test
    fun `computeTermPerformance skips ungradable courses and carries CPA through empty terms`() {
        val transcript = listOf(
            TermGrades(
                "242",
                listOf(
                    grade(point4 = null, credits = 2.0), // no 4.0-scale grade (e.g. P/F)
                    grade(point4 = 4.0, credits = null), // no credits
                ),
            ),
            TermGrades("241", listOf(grade(point4 = 3.0, credits = 3.0))),
        )
        val perf = DaotaoScraper.computeTermPerformance(transcript)
        assertNull(perf[0].termGpa) // nothing countable this term
        assertNull(perf[0].credits)
        assertEquals(3.0, perf[0].cumulativeGpa!!, 1e-9) // CPA carried from the older term
    }

    @Test
    fun `computeTermPerformance of an empty transcript is empty`() {
        assertTrue(DaotaoScraper.computeTermPerformance(emptyList()).isEmpty())
    }

    private fun grade(point4: Double?, credits: Double?) = CourseGrade(
        code = "INT0000",
        name = null,
        credits = credits,
        point10 = null,
        point4 = point4,
        letter = null,
    )

    // --- parseSyllabusDocuments ---

    @Test
    fun `parseSyllabusDocuments resolves relative urls, dedups and skips non-pdfs`() {
        val doc = Jsoup.parse(
            """
            <table>
              <tr><td>Đề cương Giải tích</td><td><a href="docs/gt1.pdf">Đề cương Giải tích</a></td></tr>
              <tr><td>Trùng lặp</td><td><a href="docs/gt1.pdf">Đề cương Giải tích</a></td></tr>
              <tr><td>Khung chương trình</td><td><a href="/files/CTDT.PDF?download=1"></a></td></tr>
              <tr><td>Trang khác</td><td><a href="other.asp">Trang khác</a></td></tr>
            </table>
            <a href="http://files.vnu.edu.vn/form.pdf?v=2"></a>
            """,
            "http://daotao.vnu.edu.vn/Syllabus/default.asp",
        )
        val docs = DaotaoScraper.parseSyllabusDocuments(doc)
        assertEquals(3, docs.size) // duplicate collapsed, .asp link skipped
        assertEquals("Đề cương Giải tích", docs[0].title)
        assertEquals("http://daotao.vnu.edu.vn/Syllabus/docs/gt1.pdf", docs[0].url)
        // Icon-only link: title falls back to the row's text; uppercase .PDF + query still match.
        assertEquals("Khung chương trình", docs[1].title)
        assertEquals("http://daotao.vnu.edu.vn/files/CTDT.PDF?download=1", docs[1].url)
        // No text, no row: title falls back to the file name without the query.
        assertEquals("form.pdf", docs[2].title)
    }

    @Test
    fun `parseSyllabusDocuments returns empty when there are no pdf links`() {
        val doc = Jsoup.parse("<a href='a.asp'>x</a>", "http://daotao.vnu.edu.vn/")
        assertTrue(DaotaoScraper.parseSyllabusDocuments(doc).isEmpty())
    }
}
