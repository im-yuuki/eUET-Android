package me.june8th.euet.core.data.source.daotao

import me.june8th.euet.core.model.CourseGrade
import me.june8th.euet.core.model.Exam
import me.june8th.euet.core.model.PortalDocument
import me.june8th.euet.core.model.StudentProfile
import me.june8th.euet.core.model.Term
import me.june8th.euet.core.model.TermGrades
import me.june8th.euet.core.model.TermPerformance
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Parses the classic-ASP daotao pages into domain models.
 *
 * Field names below were confirmed against a real logged-in session (captured on iOS), not
 * inferred: the profile page carries the student's identity in *disabled* inputs named
 * `StdCode` / `StdName` / `StdDob`, while class, major, faculty and gender come from the
 * `selected` option of dropdowns (`ClsID`, `BrcID`, `UnivID`, `StdSx`, …).
 */
object DaotaoScraper {

    /**
     * Detects an unauthenticated response.
     *
     * Checks for the two known *failure* signatures rather than requiring positive markers:
     * the portal re-renders the login form (HTTP 200) or serves a "chưa đăng nhập" notice.
     * Requiring positive markers was an iOS bug — pages like the transcript carry no logout link,
     * so valid content was rejected as expired.
     */
    fun isAuthenticated(document: Document): Boolean {
        if (document.selectFirst("input[name=txtLoginId]") != null &&
            document.selectFirst("input[name=txtPassword]") != null
        ) {
            return false
        }
        val text = document.body().text().lowercase()
        return !(
            text.contains("chưa đăng nhập") ||
                text.contains("phiên làm việc của bạn đã hết")
            )
    }

    /** Reads the profile page. Returns null when the student code can't be found. */
    fun parseProfile(document: Document): StudentProfile? {
        val code = inputValue(document, "StdCode")?.takeIf { it.isNotBlank() } ?: return null
        return StudentProfile(
            code = code,
            name = inputValue(document, "StdName")?.takeIf { it.isNotBlank() } ?: code,
            email = null, // Not exposed on this portal; StudentHub supplies the school email.
            className = selectedOption(document, "ClsID"),
            program = selectedOption(document, "PrmID"),
            major = selectedOption(document, "BrcID"),
        )
    }

    /** Internal ids needed to query the exam page (`selUniv` / `selStd`). */
    fun parseStudentContext(document: Document): DaotaoStudentContext = DaotaoStudentContext(
        studentCode = inputValue(document, "StdCode"),
        internalStudentId = inputValue(document, "hidStdID"),
        universityId = selectedOptionValue(document, "UnivID"),
    )

    /**
     * Parses the transcript page (`listpoint_Brc1.asp`) into per-term course lists, newest
     * term first (the page's own order).
     *
     * The grid has no header row: term-header rows ("HỌC KỲ 1 - 2024-2025. MÃ HỌC KỲ 241") are
     * interleaved with course rows. Column positions are resolved *relative to the course-code
     * cell* — [code, name, credits, point10, letter, point4] — which survives the portal's
     * variable leading columns (verified against 8 terms of live data on iOS).
     */
    fun parseGrades(document: Document): List<TermGrades> {
        val table = primaryTable(document) ?: return emptyList()
        val byTerm = linkedMapOf<String, MutableList<CourseGrade>>()
        var currentTerm: String? = null
        for (row in ownRows(table)) {
            val cells = cellTexts(row)
            val nonEmpty = cells.filter { it.isNotEmpty() }
            val first = nonEmpty.firstOrNull()
            if (first != null && looksLikeTerm(first) && nonEmpty.size <= 2) {
                currentTerm = cleanTermName(first)
                continue
            }
            val ci = cells.indexOfFirst(::looksLikeCourseCode)
            if (ci < 0) continue
            byTerm.getOrPut(currentTerm.orEmpty()) { mutableListOf() } += CourseGrade(
                code = cells[ci],
                name = cells.getOrNull(ci + 1)?.takeIf { it.isNotEmpty() },
                credits = cells.getOrNull(ci + 2)?.let(::parseDouble),
                point10 = cells.getOrNull(ci + 3)?.let(::parseDouble),
                point4 = cells.getOrNull(ci + 5)?.let(::parseDouble),
                letter = cells.getOrNull(ci + 4)?.takeIf { it.isNotEmpty() },
            )
        }
        return byTerm.map { (term, courses) -> TermGrades(termCode = term, courses = courses) }
    }

    /**
     * Reads the term dropdown from the exam page (options of the `<select>` whose name contains
     * "Term"), newest first. The portal has no numeric term ids, so [Term.id] stays null.
     */
    fun parseExamTerms(document: Document): List<Term> {
        val options = document.select("select[name*=Term] option")
            .map { it.attr("value").trim() to cellText(it) }
            .filter { (value, _) -> value.isNotEmpty() }
        return options.mapIndexed { idx, (value, label) ->
            Term(id = null, index = options.size - idx, code = value, name = label.ifEmpty { value })
        }
    }

    /**
     * Parses the exam schedule (`StdExamination.asp?selViewType=StdExam`). The data table is
     * located by its header signature — STT | Mã KT | Kỳ thi | Ngày thi | Ca thi | H.Thức thi |
     * Phòng | SBD — with the largest table as fallback; columns resolve by header text.
     */
    fun parseExams(document: Document): List<Exam> {
        val table = document.select("table").firstOrNull { candidate ->
            val header = ownRows(candidate).firstOrNull()?.let(::cellTexts) ?: return@firstOrNull false
            val joined = header.joinToString("|").lowercase()
            joined.contains("sbd") || joined.contains("phòng") || joined.contains("mã kt")
        } ?: primaryTable(document) ?: return emptyList()

        val rows = ownRows(table).map(::cellTexts)
        val header = rows.firstOrNull() ?: return emptyList()
        val codeCol = columnIndex(header, "mã kt", "mã") ?: 1
        val nameCol = columnIndex(header, "kỳ thi", "tên", "môn") ?: 2
        val dateCol = columnIndex(header, "ngày")
        val timeCol = columnIndex(header, "ca thi", "giờ", "ca")
        val methodCol = columnIndex(header, "thức", "hình thức")
        val roomCol = columnIndex(header, "phòng", "room")
        val seatCol = columnIndex(header, "sbd", "số báo danh")

        return rows.drop(1).mapNotNull { row ->
            val name = row.getOrNull(nameCol).orEmpty()
            val code = row.getOrNull(codeCol).orEmpty()
            val date = dateCol?.let(row::getOrNull)?.takeIf { it.isNotEmpty() }
            // Skip layout / non-data rows: need a name and either a date or an exam code.
            if (name.isEmpty() || (code.isEmpty() && date == null)) return@mapNotNull null
            Exam(
                courseCode = code.takeIf { it.isNotEmpty() },
                courseName = name,
                date = date,
                startTime = timeCol?.let(row::getOrNull)?.takeIf { it.isNotEmpty() },
                room = roomCol?.let(row::getOrNull)?.takeIf { it.isNotEmpty() },
                method = methodCol?.let(row::getOrNull)?.takeIf { it.isNotEmpty() },
                type = null, // This page doesn't distinguish midterm/final.
                seat = seatCol?.let(row::getOrNull)?.takeIf { it.isNotEmpty() },
            )
        }
    }

    /**
     * Parses the study tab (`TabStdStudy.asp`) into per-term GPA rows. Conduct columns are read
     * when present, but the 0–100 conduct scores live on a lazily-loaded sub-tab whose request
     * isn't captured yet, so they're normally null.
     */
    fun parseTermPerformance(document: Document): List<TermPerformance> {
        val table = primaryTable(document) ?: return emptyList()
        val rows = ownRows(table).map(::cellTexts)
        val header = rows.firstOrNull() ?: return emptyList()
        val termCol = columnIndex(header, "học kỳ", "kỳ", "term") ?: 0
        val conductCol = columnIndex(header, "rèn luyện", "conduct", "điểm rl")
        val termGpaCol = columnIndex(header, "tbc học kỳ", "gpa kỳ", "điểm tb học kỳ")
        val cumGpaCol = columnIndex(header, "tích lũy", "cpa", "tbc tích lũy")

        return rows.drop(1).mapNotNull { row ->
            val term = row.getOrNull(termCol)?.takeIf(::looksLikeTerm) ?: return@mapNotNull null
            TermPerformance(
                termCode = term,
                termName = term,
                termGpa = termGpaCol?.let(row::getOrNull)?.let(::parseDouble),
                cumulativeGpa = cumGpaCol?.let(row::getOrNull)?.let(::parseDouble),
                conductScore = conductCol?.let(row::getOrNull)?.let(::parseDouble)?.toInt(),
                credits = null,
            )
        }
    }

    /**
     * Computes per-term GPA and running CPA (4.0 scale, credit-weighted) from the transcript.
     * Input is newest-term-first; output preserves that order for display. Ported from the
     * iOS provider, where this proved more reliable than the study tab's lazily-loaded table.
     */
    fun computeTermPerformance(transcript: List<TermGrades>): List<TermPerformance> {
        var cumulativePoints = 0.0
        var cumulativeCredits = 0.0
        val byTerm = mutableMapOf<String, TermPerformance>()
        for (term in transcript.asReversed()) { // oldest → newest so CPA accumulates correctly
            var points = 0.0
            var credits = 0.0
            for (course in term.courses) {
                val point4 = course.point4 ?: continue
                val credit = course.credits ?: continue
                points += point4 * credit
                credits += credit
            }
            cumulativePoints += points
            cumulativeCredits += credits
            byTerm[term.termCode] = TermPerformance(
                termCode = term.termCode,
                termName = term.termCode,
                termGpa = if (credits > 0) round2(points / credits) else null,
                cumulativeGpa = if (cumulativeCredits > 0) round2(cumulativePoints / cumulativeCredits) else null,
                conductScore = null,
                credits = credits.takeIf { it > 0 },
            )
        }
        return transcript.map { byTerm.getValue(it.termCode) }
    }

    /**
     * Reads the syllabus listing (`Syllabus/default.asp`, first page only): every PDF link with
     * its visible title. URLs resolve against the page's base URL; duplicates are dropped.
     */
    fun parseSyllabusDocuments(document: Document): List<PortalDocument> {
        val seen = mutableSetOf<String>()
        return document.select("a[href]").mapNotNull { link ->
            val url = link.absUrl("href")
            if (!url.substringBefore('?').lowercase().endsWith(".pdf")) return@mapNotNull null
            if (!seen.add(url)) return@mapNotNull null
            // Icon-only links carry no text; fall back to the row's text, then the file name.
            val title = cellText(link)
                .ifEmpty { link.closest("tr")?.let(::cellText).orEmpty() }
                .ifEmpty { url.substringBefore('?').substringAfterLast('/') }
            PortalDocument(title = title, url = url)
        }
    }

    private fun inputValue(document: Document, name: String): String? =
        document.selectFirst("input[name=$name]")?.attr("value")?.trim()

    /** The visible label of the `selected` option, e.g. "Công nghệ thông tin". */
    private fun selectedOption(document: Document, name: String): String? =
        document.selectFirst("select[name=$name] option[selected]")?.text()?.trim()
            ?.takeIf { it.isNotBlank() }

    /** The `value` of the `selected` option, e.g. "002". */
    private fun selectedOptionValue(document: Document, name: String): String? =
        document.selectFirst("select[name=$name] option[selected]")?.attr("value")?.trim()
            ?.takeIf { it.isNotBlank() }

    /** The largest table by (own) row count — usually the data grid on these pages. */
    private fun primaryTable(document: Document): Element? =
        document.select("table").maxByOrNull { ownRows(it).size }

    /** Rows belonging to [table] itself, excluding rows of nested layout tables. */
    private fun ownRows(table: Element): List<Element> =
        table.select("tr").filter { it.closest("table") === table }

    /** Direct cell texts of a row, in order. */
    private fun cellTexts(row: Element): List<String> =
        row.children().filter { it.tagName() == "td" || it.tagName() == "th" }.map(::cellText)

    /** Normalized visible text: `&nbsp;` collapsed, whitespace trimmed. */
    private fun cellText(element: Element): String =
        element.text().replace('\u00A0', ' ').trim()

    /** Index of the first header cell containing any of [needles] (case-insensitive). */
    private fun columnIndex(header: List<String>, vararg needles: String): Int? {
        val lowered = header.map { it.lowercase() }
        for (needle in needles) {
            val idx = lowered.indexOfFirst { it.contains(needle) }
            if (idx >= 0) return idx
        }
        return null
    }

    /** "HỌC KỲ 2 - 2025-2026. MÃ HỌC KỲ 252" → "HỌC KỲ 2 - 2025-2026". */
    private fun cleanTermName(raw: String): String {
        val idx = raw.indexOf("MÃ HỌC KỲ", ignoreCase = true)
        return if (idx >= 0) raw.take(idx).trim(' ', '.') else raw.trim()
    }

    private fun looksLikeTerm(text: String): Boolean {
        val lowered = text.lowercase()
        return lowered.contains("học kỳ") || lowered.contains("hk") || YEAR_REGEX.containsMatchIn(text)
    }

    private fun looksLikeCourseCode(text: String): Boolean = COURSE_CODE_REGEX.containsMatchIn(text)

    private fun parseDouble(text: String): Double? =
        text.replace(',', '.').trim().toDoubleOrNull()

    private fun round2(value: Double): Double = kotlin.math.round(value * 100) / 100

    private val COURSE_CODE_REGEX = Regex("^[A-Z]{2,4}[0-9]{3,4}")
    private val YEAR_REGEX = Regex("\\b20\\d{2}\\b")
}

/** Internal identifiers scraped from the profile page. */
data class DaotaoStudentContext(
    val studentCode: String?,
    val internalStudentId: String?,
    val universityId: String?,
)
