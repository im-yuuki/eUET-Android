package me.june8th.euet.core.data

import me.june8th.euet.core.model.ConflictFields
import me.june8th.euet.core.model.CourseGrade
import me.june8th.euet.core.model.Exam
import me.june8th.euet.core.model.SourceId
import me.june8th.euet.core.model.StudentProfile
import me.june8th.euet.core.model.TermGrades
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictDetectorTest {

    private val hub = SourceId.STUDENT_HUB
    private val portal = SourceId.VNU_PORTAL

    private fun grade(
        code: String = "INT2204",
        name: String? = "Lập trình hướng đối tượng",
        credits: Double? = 4.0,
        point10: Double? = 8.5,
        point4: Double? = 3.7,
        letter: String? = "A",
    ) = CourseGrade(code, name, credits, point10, point4, letter)

    // --- Grades: matching & term-key normalization ---

    @Test
    fun `identical transcripts produce no conflicts`() {
        val transcript = listOf(TermGrades("241", listOf(grade())))
        assertTrue(ConflictDetector.compareTranscripts(transcript, transcript, hub, portal).isEmpty())
    }

    @Test
    fun `records match across provider-specific term naming`() {
        // StudentHub uses compact codes; the portal uses the term header text.
        val preferred = listOf(TermGrades("241", listOf(grade())))
        val other = listOf(TermGrades("HỌC KỲ 1 - 2024-2025", listOf(grade())))
        assertTrue(ConflictDetector.compareTranscripts(preferred, other, hub, portal).isEmpty())
    }

    @Test
    fun `course codes match ignoring the class-section suffix`() {
        val preferred = listOf(TermGrades("2024-2025-1", listOf(grade(code = "INT2204 1"))))
        val other = listOf(TermGrades("241", listOf(grade(code = "INT2204"))))
        assertTrue(ConflictDetector.compareTranscripts(preferred, other, hub, portal).isEmpty())
    }

    // --- Grades: field comparison ---

    @Test
    fun `course names compare case and whitespace insensitively`() {
        val preferred = listOf(TermGrades("241", listOf(grade(name = "Lập trình  hướng đối tượng "))))
        val other = listOf(TermGrades("241", listOf(grade(name = "LẬP TRÌNH HƯỚNG ĐỐI TƯỢNG"))))
        assertTrue(ConflictDetector.compareTranscripts(preferred, other, hub, portal).isEmpty())
    }

    @Test
    fun `scores within the tolerance are not conflicts`() {
        val preferred = listOf(TermGrades("241", listOf(grade(point10 = 8.5))))
        val other = listOf(TermGrades("241", listOf(grade(point10 = 8.504))))
        assertTrue(ConflictDetector.compareTranscripts(preferred, other, hub, portal).isEmpty())
    }

    @Test
    fun `a score difference beyond the tolerance is reported with both values`() {
        val preferred = listOf(TermGrades("241", listOf(grade(point10 = 7.8, letter = "B+"))))
        val other = listOf(TermGrades("241", listOf(grade(point10 = 8.0, letter = "A"))))
        val conflicts = ConflictDetector.compareTranscripts(preferred, other, hub, portal)

        assertEquals(1, conflicts.size)
        val conflict = conflicts.single()
        assertNull(conflict.onlyIn)
        assertEquals("Lập trình hướng đối tượng", conflict.recordLabel)
        assertEquals(
            listOf(ConflictFields.SCORE_10, ConflictFields.LETTER),
            conflict.fields.map { it.fieldLabel },
        )
        val score = conflict.fields.first()
        assertEquals("7.8", score.preferredValue)
        assertEquals("8", score.otherValue) // whole numbers render without a decimal point
    }

    @Test
    fun `a field one source does not provide is never a conflict`() {
        val preferred = listOf(TermGrades("241", listOf(grade(point4 = null, letter = null))))
        val other = listOf(TermGrades("241", listOf(grade())))
        assertTrue(ConflictDetector.compareTranscripts(preferred, other, hub, portal).isEmpty())
    }

    // --- Grades: onlyIn ---

    @Test
    fun `a course present in a single source becomes an onlyIn conflict without field diffs`() {
        val preferred = listOf(TermGrades("241", listOf(grade(), grade(code = "MAT1101", name = "Đại số"))))
        val other = listOf(TermGrades("241", listOf(grade(), grade(code = "PES1025", name = "GDTC"))))
        val conflicts = ConflictDetector.compareTranscripts(preferred, other, hub, portal)

        assertEquals(2, conflicts.size)
        val preferredOnly = conflicts.first { it.recordLabel == "Đại số" }
        assertEquals(hub, preferredOnly.onlyIn)
        assertTrue(preferredOnly.fields.isEmpty())
        val otherOnly = conflicts.first { it.recordLabel == "GDTC" }
        assertEquals(portal, otherOnly.onlyIn)
        assertTrue(otherOnly.fields.isEmpty())
    }

    // --- Exams ---

    private fun exam(
        code: String? = "INT2204 1",
        name: String? = "Lập trình hướng đối tượng",
        date: String? = "12/06/2025",
        time: String? = "08:00",
        room: String? = "308-GĐ2",
        seat: String? = "45",
        method: String? = "Trắc nghiệm",
    ) = Exam(code, name, date, time, room, method, type = null, seat = seat)

    @Test
    fun `identical exam schedules produce no conflicts`() {
        val exams = listOf(exam())
        assertTrue(ConflictDetector.compareExams("241", exams, exams, hub, portal).isEmpty())
    }

    @Test
    fun `differing room and seat are reported per field`() {
        val preferred = listOf(exam(room = "205-GĐ2", seat = "12"))
        val other = listOf(exam(code = "INT2204", room = "308-GĐ2", seat = "21"))
        val conflicts = ConflictDetector.compareExams("241", preferred, other, hub, portal)

        assertEquals(1, conflicts.size)
        assertEquals(
            listOf(ConflictFields.EXAM_ROOM, ConflictFields.EXAM_SEAT),
            conflicts.single().fields.map { it.fieldLabel },
        )
    }

    @Test
    fun `an exam missing from the other source is onlyIn`() {
        val preferred = listOf(exam(), exam(code = "MAT1101", name = "Đại số"))
        val other = listOf(exam())
        val conflicts = ConflictDetector.compareExams("241", preferred, other, hub, portal)

        assertEquals(1, conflicts.size)
        assertEquals(hub, conflicts.single().onlyIn)
        assertTrue(conflicts.single().fields.isEmpty())
    }

    // --- Profile ---

    private fun profile(
        code: String = "22028123",
        name: String = "Nguyễn Văn An",
        email: String? = "22028123@vnu.edu.vn",
        className: String? = "QH-2022-I/CQ-C-A-CLC",
        program: String? = "CNTT CLC",
        major: String? = "CNTT",
    ) = StudentProfile(code, name, email, className, program, major)

    @Test
    fun `matching profiles produce no conflicts`() {
        assertTrue(ConflictDetector.compareProfiles(profile(), profile()).isEmpty())
    }

    @Test
    fun `profile fields the portal does not provide are skipped`() {
        // The portal has no school email or class; absence must not read as a conflict.
        assertTrue(
            ConflictDetector.compareProfiles(profile(), profile(email = null, className = null)).isEmpty(),
        )
    }

    @Test
    fun `a differing profile field yields a single record conflict`() {
        val conflicts = ConflictDetector.compareProfiles(profile(), profile(className = "QH-2022-I/CQ-C-A"))
        assertEquals(1, conflicts.size)
        val conflict = conflicts.single()
        assertEquals(ConflictDetector.PROFILE_RECORD_KEY, conflict.recordKey)
        assertEquals(listOf(ConflictFields.CLASS_NAME), conflict.fields.map { it.fieldLabel })
        assertEquals("QH-2022-I/CQ-C-A-CLC", conflict.fields.single().preferredValue)
        assertEquals("QH-2022-I/CQ-C-A", conflict.fields.single().otherValue)
    }

    // --- Term keys ---

    @Test
    fun `term keys collapse every known naming scheme onto the same value`() {
        val fromCompact = ConflictDetector.termKey("241")
        assertEquals(fromCompact, ConflictDetector.termKey("2024-2025-1"))
        assertEquals(fromCompact, ConflictDetector.termKey("HỌC KỲ 1 - 2024-2025"))
        assertEquals(fromCompact, ConflictDetector.termKey("Học kỳ 1 (2024-2025)"))
    }

    @Test
    fun `unrecognized term names fall back to normalized text`() {
        assertEquals(ConflictDetector.termKey("Kỳ phụ"), ConflictDetector.termKey("  kỳ  PHỤ "))
        // The first parseable candidate wins over the fallback.
        assertEquals(ConflictDetector.termKey("242"), ConflictDetector.termKey("038", "Học kỳ 2 (2024-2025)"))
    }
}
