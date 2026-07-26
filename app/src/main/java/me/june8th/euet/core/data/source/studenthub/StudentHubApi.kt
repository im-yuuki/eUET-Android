package me.june8th.euet.core.data.source.studenthub

import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * StudentHub REST API (`studenthub.uet.edu.vn`).
 *
 * Endpoints and field shapes come from the HAR notes. Authentication is whichever of two schemes
 * the session was established with: the session cookie the password login sets (held by
 * [me.june8th.euet.core.network.StudentHubCookieJar]) or a bearer token captured from the web
 * view (attached by [me.june8th.euet.core.network.AuthInterceptor]). The two coexist — the
 * captured traffic sends no `Authorization` header at all once a cookie session exists.
 */
interface StudentHubApi {

    /**
     * Issues a captcha challenge for [login]. Returns a raw object: the field names were never
     * observed, so [StudentHubAuthParser] reads it leniently instead of a typed DTO doing it.
     */
    @GET("api/auth/captcha")
    suspend fun getCaptcha(): JsonObject

    /**
     * Username + password + captcha sign-in. Answers HTTP 200 whether it accepts or rejects, so
     * the verdict comes from [StudentHubAuthParser.parseLogin], never from the status code.
     */
    @POST("api/auth/login")
    suspend fun login(@Body body: PasswordLoginRequest): JsonObject

    @GET("api/student/detail")
    suspend fun getDetail(): StudentDetailDto

    @POST("api/student/term/getTerm")
    suspend fun getTerms(@Body body: EmptyRequest = EmptyRequest()): List<TermDto>

    @POST("api/student/tkb")
    suspend fun getTimetable(@Body body: TermRequest): List<TkbItemDto>

    @GET("api/student/kqht")
    suspend fun getTranscript(): List<GradeDto>

    @GET("api/student/results")
    suspend fun getResults(): ResultsDto

    @POST("api/student/exam-schedule")
    suspend fun getExamSchedule(@Body body: TermRequest): List<ExamDto>

    @POST("api/student/getAllBills")
    suspend fun getBills(@Body body: EmptyRequest = EmptyRequest()): List<BillDto>

    @GET("api/noti/user/{studentCode}")
    suspend fun getNotifications(
        @Path("studentCode") studentCode: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageDto<NotiDto>

    @GET("api/student/news")
    suspend fun getNews(): List<NewsDto>
}
