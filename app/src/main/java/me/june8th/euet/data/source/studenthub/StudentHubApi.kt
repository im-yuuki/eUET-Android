package me.june8th.euet.data.source.studenthub

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * StudentHub REST API (`studenthub.uet.edu.vn`). Endpoints and field shapes come from the HAR
 * notes; auth is a bearer token attached by [me.june8th.euet.core.network.AuthInterceptor].
 */
interface StudentHubApi {

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
