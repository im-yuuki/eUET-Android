package me.june8th.euet.core.data

import me.june8th.euet.core.model.ConflictFields
import me.june8th.euet.core.model.CourseGrade
import me.june8th.euet.core.model.DataConflict
import me.june8th.euet.core.model.Exam
import me.june8th.euet.core.model.FieldDiff
import me.june8th.euet.core.model.SourceId
import me.june8th.euet.core.model.StudentProfile
import me.june8th.euet.core.model.TermGrades
import kotlin.math.abs

/**
 * Pure cross-source comparison, shared semantics with the iOS implementation:
 *
 * - Grades match by (term key, course code); course names compare case/whitespace-insensitively,
 *   numbers with a tolerance of [NUMERIC_TOLERANCE]. A course present in only one source becomes a
 *   [DataConflict] with `onlyIn` set and no field diffs.
 * - Exams match by (term key, course code) and compare date, time, room, seat and method.
 * - The profile is a single record; only fields both sources actually provide are compared —
 *   absence on either side is never a conflict.
 *
 * The displayed list always stays purely the preferred source's records; conflicts only annotate.
 */
object ConflictDetector {

    /** Numeric values closer than this are considered the same reading. */
    const val NUMERIC_TOLERANCE = 0.005

    /** Record key of the single profile record. */
    const val PROFILE_RECORD_KEY = "profile"

    // --- Record keys (shared with the UI so rows can find their own conflict) ---

    /** Key a transcript row: normalized term (code or portal name) + normalized course code. */
    fun gradeRecordKey(termCodeOrName: String, courseCode: String): String =
        "${termKey(termCodeOrName)}|${courseKey(courseCode)}"

    /** Key an exam row within the term the screen fetched. */
    fun examRecordKey(termCodeOrName: String, exam: Exam): String =
        "${termKey(termCodeOrName)}|${courseKey(exam.courseCode ?: exam.courseName)}"

    /**
     * Canonical term key: "241", "2024-2025-1", "HỌC KỲ 1 - 2024-2025" and "Học kỳ 1 (2024-2025)"
     * all collapse to "2024:1", so records match even though each provider names terms its own
     * way. Falls back to the first candidate normalized when no pattern applies.
     */
    fun termKey(vararg candidates: String?): String {
        for (candidate in candidates) {
            val text = candidate?.trim().orEmpty()
            if (text.isEmpty()) continue
            parseTermKey(text)?.let { return it }
        }
        return candidates.firstNotNullOfOrNull { it?.takeIf(String::isNotBlank) }
            ?.let(::normalizeText).orEmpty()
    }

    // --- Grades / transcript ---

    fun compareTranscripts(
        preferred: List<TermGrades>,
        other: List<TermGrades>,
        preferredSource: SourceId,
        otherSource: SourceId,
    ): List<DataConflict> {
        val preferredRecords = flattenTranscript(preferred)
        val otherRecords = flattenTranscript(other)
        val conflicts = mutableListOf<DataConflict>()

        for ((key, record) in preferredRecords) {
            val match = otherRecords[key]
            if (match == null) {
                conflicts += DataConflict(key, record.label, emptyList(), onlyIn = preferredSource)
                continue
            }
            val fields = buildList {
                diffText(ConflictFields.COURSE_NAME, record.grade.name, match.grade.name)?.let(::add)
                diffNumber(ConflictFields.CREDITS, record.grade.credits, match.grade.credits)?.let(::add)
                diffNumber(ConflictFields.SCORE_10, record.grade.point10, match.grade.point10)?.let(::add)
                diffNumber(ConflictFields.SCORE_4, record.grade.point4, match.grade.point4)?.let(::add)
                diffText(ConflictFields.LETTER, record.grade.letter, match.grade.letter)?.let(::add)
            }
            if (fields.isNotEmpty()) conflicts += DataConflict(key, record.label, fields)
        }
        for ((key, record) in otherRecords) {
            if (key !in preferredRecords) {
                conflicts += DataConflict(key, record.label, emptyList(), onlyIn = otherSource)
            }
        }
        return conflicts
    }

    // --- Exams ---

    fun compareExams(
        termCodeOrName: String,
        preferred: List<Exam>,
        other: List<Exam>,
        preferredSource: SourceId,
        otherSource: SourceId,
    ): List<DataConflict> {
        val preferredRecords = keyExams(termCodeOrName, preferred)
        val otherRecords = keyExams(termCodeOrName, other)
        val conflicts = mutableListOf<DataConflict>()

        for ((key, exam) in preferredRecords) {
            val match = otherRecords[key]
            if (match == null) {
                conflicts += DataConflict(key, examLabel(exam), emptyList(), onlyIn = preferredSource)
                continue
            }
            val fields = buildList {
                diffText(ConflictFields.EXAM_DATE, exam.date, match.date)?.let(::add)
                diffText(ConflictFields.EXAM_TIME, exam.startTime, match.startTime)?.let(::add)
                diffText(ConflictFields.EXAM_ROOM, exam.room, match.room)?.let(::add)
                diffText(ConflictFields.EXAM_SEAT, exam.seat, match.seat)?.let(::add)
                diffText(ConflictFields.EXAM_METHOD, exam.method, match.method)?.let(::add)
            }
            if (fields.isNotEmpty()) conflicts += DataConflict(key, examLabel(exam), fields)
        }
        for ((key, exam) in otherRecords) {
            if (key !in preferredRecords) {
                conflicts += DataConflict(key, examLabel(exam), emptyList(), onlyIn = otherSource)
            }
        }
        return conflicts
    }

    // --- Profile ---

    fun compareProfiles(preferred: StudentProfile, other: StudentProfile): List<DataConflict> {
        val fields = buildList {
            diffText(ConflictFields.FULL_NAME, preferred.name, other.name)?.let(::add)
            diffText(ConflictFields.STUDENT_CODE, preferred.code, other.code)?.let(::add)
            diffText(ConflictFields.EMAIL, preferred.email, other.email)?.let(::add)
            diffText(ConflictFields.CLASS_NAME, preferred.className, other.className)?.let(::add)
            diffText(ConflictFields.MAJOR, preferred.major, other.major)?.let(::add)
            diffText(ConflictFields.PROGRAM, preferred.program, other.program)?.let(::add)
        }
        if (fields.isEmpty()) return emptyList()
        return listOf(DataConflict(PROFILE_RECORD_KEY, preferred.name, fields))
    }

    // --- Internals ---

    private data class GradeRecord(val grade: CourseGrade, val label: String)

    private fun flattenTranscript(transcript: List<TermGrades>): LinkedHashMap<String, GradeRecord> {
        val records = linkedMapOf<String, GradeRecord>()
        for (term in transcript) {
            for (grade in term.courses) {
                val key = gradeRecordKey(term.termCode, grade.code)
                // First occurrence wins so a duplicated row never pairs against itself.
                records.putIfAbsent(key, GradeRecord(grade, grade.name ?: grade.code))
            }
        }
        return records
    }

    private fun keyExams(termCodeOrName: String, exams: List<Exam>): LinkedHashMap<String, Exam> {
        val records = linkedMapOf<String, Exam>()
        for (exam in exams) records.putIfAbsent(examRecordKey(termCodeOrName, exam), exam)
        return records
    }

    private fun examLabel(exam: Exam): String =
        exam.courseName ?: exam.courseCode.orEmpty()

    /**
     * Text diff: skipped when either side doesn't provide the field (absence is not a conflict);
     * values compare trimmed, whitespace-collapsed and case-insensitively.
     */
    private fun diffText(field: String, preferred: String?, other: String?): FieldDiff? {
        if (preferred.isNullOrBlank() || other.isNullOrBlank()) return null
        if (normalizeText(preferred) == normalizeText(other)) return null
        return FieldDiff(field, preferred.trim(), other.trim())
    }

    /** Numeric diff with [NUMERIC_TOLERANCE]; skipped when either side is absent. */
    private fun diffNumber(field: String, preferred: Double?, other: Double?): FieldDiff? {
        if (preferred == null || other == null) return null
        if (abs(preferred - other) <= NUMERIC_TOLERANCE) return null
        return FieldDiff(field, formatNumber(preferred), formatNumber(other))
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private fun normalizeText(text: String): String =
        text.trim().replace(WHITESPACE, " ").lowercase()

    private fun parseTermKey(text: String): String? {
        // StudentHub-style compact code: "241" → 2024, semester 1. The leading digit must be
        // non-zero and the semester 1–3 so the portal's zero-padded internal ids ("038") never
        // masquerade as a term code.
        COMPACT_TERM.matchEntire(text)?.let { match ->
            val (yy, semester) = match.destructured
            return "20$yy:$semester"
        }
        val lowered = text.lowercase()
        val year = YEAR.find(lowered)?.value ?: return null
        val semester = SEMESTER.find(lowered)?.groupValues?.get(1)
            ?: YEAR_RANGE_SEMESTER.find(lowered)?.groupValues?.get(2)
            ?: return null
        return "$year:$semester"
    }

    private val WHITESPACE = Regex("\\s+")
    private val COMPACT_TERM = Regex("^([1-9]\\d)([1-3])$")
    private val YEAR = Regex("\\b20\\d{2}\\b")

    /** "học kỳ 1", "hk1", "kỳ 2" (input is lowercased before matching). */
    private val SEMESTER = Regex("(?:học kỳ|hk|kỳ)\\s*(\\d)")

    /** "2024-2025-1" → semester 1. */
    private val YEAR_RANGE_SEMESTER = Regex("(20\\d{2})\\s*-\\s*20\\d{2}\\s*-\\s*(\\d)")

    /** Leading course code ("INT2204 1" and "INT2204" both key as "INT2204"). */
    private val COURSE_CODE = Regex("[A-Z]{2,4}\\d{3,4}")

    private fun courseKey(code: String?): String {
        val trimmed = code?.trim()?.uppercase().orEmpty()
        return COURSE_CODE.find(trimmed)?.value ?: normalizeText(trimmed)
    }
}
