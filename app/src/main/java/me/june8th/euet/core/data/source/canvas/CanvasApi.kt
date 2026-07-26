package me.june8th.euet.core.data.source.canvas

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Canvas LMS REST API (the UET instance at `portal.uet.vnu.edu.vn`). Auth is
 * `Authorization: Bearer <access token>` attached by [CanvasAuthInterceptor]; [getSelf] accepts an
 * explicit header so a pasted token can be validated before it is stored.
 */
interface CanvasApi {

    /** The token owner's profile — the connect-time validation call. */
    @GET("api/v1/users/self")
    suspend fun getSelf(@Header("Authorization") authorization: String? = null): CanvasSelfDto

    /** The active-course cards shown on the Canvas dashboard. */
    @GET("api/v1/dashboard/dashboard_cards")
    suspend fun getDashboardCards(): List<DashboardCardDto>

    /** Upcoming planner items from [startDate] (ISO `yyyy-MM-dd`) onwards. */
    @GET("api/v1/planner/items")
    suspend fun getPlannerItems(
        @Query("start_date") startDate: String,
        @Query("per_page") perPage: Int = 20,
    ): List<PlannerItemDto>

    /** Past-due assignments that can still be submitted. */
    @GET("api/v1/users/self/missing_submissions")
    suspend fun getMissingSubmissions(
        @Query("filter[]") filter: String = "submittable",
    ): List<MissingSubmissionDto>

    /** Unread inbox conversations; the count comes back as a string, see [UnreadCountDto]. */
    @GET("api/v1/conversations/unread_count")
    suspend fun getUnreadCount(): UnreadCountDto
}
