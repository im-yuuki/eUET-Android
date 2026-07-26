package me.june8th.euet.core.data.source.studenthub

import me.june8th.euet.core.model.AppNotification
import me.june8th.euet.core.model.Bill
import me.june8th.euet.core.model.CourseGrade
import me.june8th.euet.core.model.Exam
import me.june8th.euet.core.model.GpaSummary
import me.june8th.euet.core.model.NewsItem
import me.june8th.euet.core.model.StudentProfile
import me.june8th.euet.core.model.Term
import me.june8th.euet.core.model.TermGrades
import me.june8th.euet.core.model.TimetableEntry

fun StudentDetailDto.toDomain(): StudentProfile = StudentProfile(
    code = studentCode.orEmpty(),
    name = name.orEmpty(),
    email = schoolEmail,
    className = classCode,
    program = programName,
    major = majorName,
)

fun TermDto.toDomain(): Term = Term(
    id = id,
    index = index,
    code = termCode.orEmpty(),
    name = name ?: termCode.orEmpty(),
)

fun TkbItemDto.toDomain(): TimetableEntry = TimetableEntry(
    courseCode = courseCode.orEmpty(),
    courseName = courseName.orEmpty(),
    room = roomName,
    weekday = weekday ?: 0,
    sessionStart = sessionStart,
    sessionEnd = sessionEnd,
)

fun GradeDto.toDomain(): CourseGrade = CourseGrade(
    code = courseCode.orEmpty(),
    name = courseName,
    credits = courseCredit,
    point10 = point10,
    point4 = point4,
    letter = letterGrade,
)

/** Groups a flat transcript into per-term buckets, newest term first. */
fun List<GradeDto>.toTermGrades(): List<TermGrades> =
    groupBy { it.termCode.orEmpty() }
        .map { (term, grades) -> TermGrades(term, grades.map { it.toDomain() }) }
        .sortedByDescending { it.termCode }

fun ResultsDto.toDomain(): GpaSummary = GpaSummary(
    cpa = cpa,
    gpa = gpa,
    totalCredits = totalCredits,
    accumulatedCredits = totalAccumulatedCredits,
)

fun ExamDto.toDomain(): Exam = Exam(
    courseCode = courseCode,
    courseName = courseName,
    date = date,
    startTime = startTime,
    room = room,
    method = method,
    type = type,
    seat = seatNumber,
)

fun BillDto.toDomain(): Bill = Bill(
    name = name,
    termCode = termCode,
    amount = amount,
    remaining = remaining,
    status = status,
    invoiceUrl = invoiceUrl,
)

fun NotiDto.toDomain(): AppNotification = AppNotification(
    id = id ?: 0,
    title = title.orEmpty(),
    content = content,
    createdAt = createdAt,
    read = isRead ?: false,
)

fun NewsDto.toDomain(): NewsItem = NewsItem(
    id = id ?: 0,
    title = title.orEmpty(),
    summary = summary ?: content,
    imageUrl = imageUrl,
    createdAt = createdAt,
)
