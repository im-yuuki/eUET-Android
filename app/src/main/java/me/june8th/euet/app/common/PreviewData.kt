package me.june8th.euet.app.common

import me.june8th.euet.core.model.AppNotification
import me.june8th.euet.core.model.Bill
import me.june8th.euet.core.model.ConflictFields
import me.june8th.euet.core.model.ConflictReport
import me.june8th.euet.core.model.DataConflict
import me.june8th.euet.core.model.FieldDiff
import me.june8th.euet.core.model.SourceId
import me.june8th.euet.core.model.CanvasCourse
import me.june8th.euet.core.model.CanvasSummary
import me.june8th.euet.core.model.CaptchaChallenge
import me.june8th.euet.core.model.CourseGrade
import me.june8th.euet.core.model.Exam
import me.june8th.euet.core.model.GpaSummary
import me.june8th.euet.core.model.MissingSubmission
import me.june8th.euet.core.model.NewsItem
import me.june8th.euet.core.model.PlannerItem
import me.june8th.euet.core.model.PortalDocument
import me.june8th.euet.core.model.StudentProfile
import me.june8th.euet.core.model.Term
import me.june8th.euet.core.model.TermGrades
import me.june8th.euet.core.model.TermPerformance
import me.june8th.euet.core.model.TimetableEntry
import java.util.Base64

/**
 * Realistic sample domain data for `@Preview` composables. Previews render entirely from these
 * values — no network, DataStore, or [me.june8th.euet.app.di.AppContainer] involved. Never used
 * at runtime.
 */
object PreviewData {

    val profile = StudentProfile(
        code = "22028123",
        name = "Nguyễn Văn An",
        email = "22028123@vnu.edu.vn",
        className = "QH-2022-I/CQ-C-A-CLC",
        program = "Công nghệ thông tin (chất lượng cao)",
        major = "Công nghệ thông tin",
    )

    val terms = listOf(
        Term(id = 42, index = 8, code = "2024-2025-2", name = "Học kỳ 2 (2024-2025)"),
        Term(id = 41, index = 7, code = "2024-2025-1", name = "Học kỳ 1 (2024-2025)"),
        Term(id = 40, index = 6, code = "2023-2024-2", name = "Học kỳ 2 (2023-2024)"),
    )

    val activeTermCode = terms.first().code

    /** One term's classes spread across the StudentHub weekday scheme (Mon=2 … Sat=7). */
    val timetable = listOf(
        TimetableEntry("INT2204 1", "Lập trình hướng đối tượng", "308-GĐ2", 2, 1, 2),
        TimetableEntry("INT2210 2", "Cấu trúc dữ liệu và giải thuật", "205-GĐ2", 2, 3, 4),
        TimetableEntry("MAT1101 3", "Đại số", "102-GĐ3", 3, 1, 2),
        TimetableEntry("INT2211 1", "Cơ sở dữ liệu", "308-GĐ2", 4, 7, 8),
        TimetableEntry("INT2213 2", "Mạng máy tính", "205-GĐ2", 5, 3, 4),
        TimetableEntry("FLF1107 4", "Tiếng Anh B1", "410-E5", 6, 1, 4),
    )

    /** Today's slice used by the Home preview (weekday-independent). */
    val todayClasses = timetable.take(2)

    val gpa = GpaSummary(cpa = 3.24, gpa = 3.41, totalCredits = 155.0, accumulatedCredits = 98.0)

    val transcript = listOf(
        TermGrades(
            termCode = "2024-2025-1",
            courses = listOf(
                CourseGrade("INT2204", "Lập trình hướng đối tượng", 4.0, 8.5, 3.7, "A"),
                CourseGrade("INT2210", "Cấu trúc dữ liệu và giải thuật", 4.0, 7.8, 3.0, "B+"),
                CourseGrade("MAT1101", "Đại số", 4.0, 6.9, 2.5, "C+"),
            ),
        ),
        TermGrades(
            termCode = "2023-2024-2",
            courses = listOf(
                CourseGrade("INT1008", "Nhập môn lập trình", 4.0, 9.0, 4.0, "A+"),
                CourseGrade("PHY1100", "Cơ - Nhiệt", 3.0, 7.2, 3.0, "B"),
            ),
        ),
    )

    val exams = listOf(
        Exam("INT2204 1", "Lập trình hướng đối tượng", "12/06/2025", "08:00", "308-GĐ2", "Trắc nghiệm trên máy", "Cuối kỳ", "45"),
        Exam("INT2210 2", "Cấu trúc dữ liệu và giải thuật", "16/06/2025", "13:00", "205-GĐ2", "Tự luận", "Cuối kỳ", "12"),
        Exam("MAT1101 3", "Đại số", "20/06/2025", "08:00", "102-GĐ3", "Tự luận", "Cuối kỳ", "07"),
    )

    /** One unpaid bill (outstanding balance) and one settled bill with an invoice. */
    val bills = listOf(
        Bill(
            name = "Học phí học kỳ 2 năm học 2024-2025",
            termCode = "2024-2025-2",
            amount = 14_560_000.0,
            remaining = 14_560_000.0,
            status = "Chưa thanh toán",
            invoiceUrl = null,
        ),
        Bill(
            name = "Học phí học kỳ 1 năm học 2024-2025",
            termCode = "2024-2025-1",
            amount = 13_650_000.0,
            remaining = 0.0,
            status = "Đã thanh toán",
            invoiceUrl = "https://portal.vnu.edu.vn/invoice/2024-1",
        ),
    )

    val notifications = listOf(
        AppNotification(1, "Lịch thi cuối kỳ học kỳ 2 năm học 2024-2025", "Sinh viên xem chi tiết lịch thi, phòng thi và số báo danh trong mục Lịch thi.", "26/07/2025 08:30", read = false),
        AppNotification(2, "Đăng ký học phần học kỳ 1 năm học 2025-2026", "Cổng đăng ký mở từ 08:00 ngày 04/08/2025 đến 17:00 ngày 08/08/2025.", "24/07/2025 14:00", read = false),
        AppNotification(3, "Thông báo nộp học phí học kỳ 2", "Hạn cuối nộp học phí là ngày 15/06/2025. Sinh viên hoàn thành trước hạn để tránh bị khóa tài khoản.", "20/07/2025 09:15", read = true),
        AppNotification(4, "Kế hoạch nghỉ hè năm học 2024-2025", "Sinh viên nghỉ hè từ ngày 30/06/2025 và quay lại trường vào ngày 01/09/2025.", "18/07/2025 10:00", read = true),
    )

    val news = listOf(
        NewsItem(1, "UET khai mạc Ngày hội việc làm công nghệ 2025", "Hơn 60 doanh nghiệp công nghệ tham gia tuyển dụng trực tiếp tại trường.", null, "25/07/2025"),
        NewsItem(2, "Sinh viên UET giành giải Nhất ICPC khu vực châu Á", "Đội tuyển ba sinh viên Khoa CNTT xuất sắc vượt qua 120 đội tham dự.", null, "22/07/2025"),
    )

    val documents = listOf(
        PortalDocument("Đề cương học phần INT2204 - Lập trình hướng đối tượng", "https://uet.vnu.edu.vn/decuong/INT2204.pdf"),
        PortalDocument("Đề cương học phần INT2210 - Cấu trúc dữ liệu và giải thuật", "https://uet.vnu.edu.vn/decuong/INT2210.pdf"),
        PortalDocument("Chương trình đào tạo ngành Công nghệ thông tin (CLC)", "https://uet.vnu.edu.vn/ctdt/CNTT-CLC.pdf"),
    )

    val termPerformance = listOf(
        TermPerformance("2024-2025-1", "Học kỳ 1 (2024-2025)", 3.41, 3.24, null, 16.0),
        TermPerformance("2023-2024-2", "Học kỳ 2 (2023-2024)", 3.18, 3.15, null, 17.0),
        TermPerformance("2023-2024-1", "Học kỳ 1 (2023-2024)", 3.05, 3.12, null, 15.0),
    )

    /**
     * A small dual-source disagreement sample for the conflict banner / diff sheet previews.
     * Record keys follow `ConflictDetector.gradeRecordKey` so the row badge previews line up
     * with [transcript]'s 2024-2025-1 entries.
     */
    val conflictReport = ConflictReport(
        source = SourceId.STUDENT_HUB,
        conflicts = listOf(
            DataConflict(
                recordKey = "2024:1|INT2210",
                recordLabel = "Cấu trúc dữ liệu và giải thuật",
                fields = listOf(
                    FieldDiff(ConflictFields.SCORE_10, "7.8", "8.0"),
                    FieldDiff(ConflictFields.LETTER, "B+", "A"),
                ),
            ),
            DataConflict(
                recordKey = "2024:1|PES1025",
                recordLabel = "Giáo dục thể chất",
                fields = emptyList(),
                onlyIn = SourceId.VNU_PORTAL,
            ),
        ),
    )

    /** Exams variant of the sample; keys follow `ConflictDetector.examRecordKey` for [exams]. */
    val examConflictReport = ConflictReport(
        source = SourceId.STUDENT_HUB,
        conflicts = listOf(
            DataConflict(
                recordKey = "2024:2|INT2210",
                recordLabel = "Cấu trúc dữ liệu và giải thuật",
                fields = listOf(
                    FieldDiff(ConflictFields.EXAM_ROOM, "205-GĐ2", "308-GĐ2"),
                    FieldDiff(ConflictFields.EXAM_SEAT, "12", "21"),
                ),
            ),
        ),
    )

    /** Profile variant: the single record differs on class and program. */
    val profileConflictReport = ConflictReport(
        source = SourceId.STUDENT_HUB,
        conflicts = listOf(
            DataConflict(
                recordKey = "profile",
                recordLabel = profile.name,
                fields = listOf(
                    FieldDiff(ConflictFields.CLASS_NAME, "QH-2022-I/CQ-C-A-CLC", "QH-2022-I/CQ-C-A"),
                    FieldDiff(ConflictFields.PROGRAM, "Công nghệ thông tin (chất lượng cao)", "Công nghệ thông tin"),
                ),
            ),
        ),
    )

    // --- Canvas LMS ---

    val canvasCourses = listOf(
        CanvasCourse("101", "Lập trình hướng đối tượng (INT2204 1)", "INT2204 1", "HK2 2024-2025", null, null),
        CanvasCourse("102", "Cơ sở dữ liệu (INT2211 1)", "INT2211 1", "HK2 2024-2025", null, null),
    )

    val plannerItems = listOf(
        PlannerItem("p1", PlannerItem.Kind.ASSIGNMENT, "Bài tập lớn: Quản lý thư viện", "Lập trình hướng đối tượng", "2025-07-30T15:00:00Z", null, isSubmitted = false),
        PlannerItem("p2", PlannerItem.Kind.QUIZ, "Quiz 5: Chuẩn hóa cơ sở dữ liệu", "Cơ sở dữ liệu", "2025-07-31T09:00:00Z", null, isSubmitted = true),
        PlannerItem("p3", PlannerItem.Kind.ANNOUNCEMENT, "Thông báo lịch bảo vệ bài tập lớn", "Lập trình hướng đối tượng", "2025-08-01T02:00:00Z", null, isSubmitted = false),
    )

    val missingSubmissions = listOf(
        MissingSubmission(9001, "Tuần 10: Bài tập JDBC", "2025-07-20T15:00:00Z", 102, null),
    )

    val canvasSummary = CanvasSummary(unreadInbox = 2, missingSubmissions = missingSubmissions.size)

    // --- Sign-in ---

    /**
     * A synthetic 120×44 PNG standing in for a StudentHub captcha, so the sign-in form can be
     * previewed without a network call. Not a real challenge — the boxes spell nothing.
     */
    val captchaChallenge = CaptchaChallenge(
        id = "0f1c8b6e-6a1a-4f3d-9d70-1f7f5b2a91c4",
        image = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAHgAAAAsCAIAAAAhGetkAAACn0lEQVR42t1bW45CIQztclzE7D9+z4YYMybm5kJLH6cFJMYP" +
                "hb5fFKD2P57P3yaO6QTNiANxQ4DQH4FMrWTI1GhoFea8/rr9u1CjHBzKkGlP5YaWWDyogEMHqCD2j7Kv32t1ST2+rYxo6B9A" +
                "JQWj1hYWrbGj65wyBesRAXVGEPmiclE8YqSqJGL+BPTZyniyBH4k1g8E/Xj8aD499JqFblKvSJULe9m5SaUhEVMtcdRHFsp+" +
                "ygl6al/WhdycN0Dlwp59Usqit0EI9TVsWzEKwE0YrxIjWSEawxR+HLre/oKGYLzxTkrLX8v2Cz7Qhzho+oWyfAyh411yCOCW" +
                "hw7fQjlwWzFynYZJMrRmGGVYUFKv96FpqtDLa2qYSlcWqJKaSjLbqERRr6HpjzcnxmqI5GJLmQx9MdrtyNgIYC0oZclwCYCm" +
                "1A8hAjMMvBwOFuBJCwlORDzwfWUWpX3YNmkIVVBaI4C7LqQT7StpgyfHnKBq6cTe0IkLMTvDSGqyZlFgHV2Wh7ShI69lgVoo" +
                "lFxD0cjyytgr+HeGy1sWkV7a7uUdvMkr+FBvg73Zuh05vhe1NoKozDA5f4zvdOQT3mM2LNbAZ91SAtke2lpqVjhyw7KwqTRs" +
                "eDoON2Bt0iM2LDfOl5DaS58c1GvsiztpTS0ept3hxTFaI8SeemzjP6Ny/zhyKqnKbtTuocN0piPnbdnMlcBhp+BcUOOqWkdq" +
                "yrAvyPEm59lDDaVXHbsdZaEKSkewMt3F8F8Ji1yX6guDnZtwwQ4lW3U0/q70cffvuU1jPSPUThvA281KUNM7AQBBY+9sf82D" +
                "FAc7FMQxRAa5zW6aGbfNfUNH5cvDtS+XTo3RGntMemAKiW++txAUgbjKOpLwwh9x7lJ1OOqtfcpNK/F/uTHtzFUAV6UAAAAA" +
                "SUVORK5CYII=",
        ),
    )
}
