package me.june8th.euet.app.di

import android.content.Context
import kotlinx.serialization.json.Json
import me.june8th.euet.core.datastore.SessionManager
import me.june8th.euet.core.datastore.SnapshotCache
import me.june8th.euet.core.network.AuthInterceptor
import me.june8th.euet.core.network.StudentHubCookieJar
import me.june8th.euet.core.data.ProviderRegistry
import me.june8th.euet.core.data.repository.AggregateRepository
import me.june8th.euet.core.data.repository.AuthRepository
import me.june8th.euet.core.data.repository.CanvasRepository
import me.june8th.euet.core.data.repository.DaotaoRepository
import me.june8th.euet.core.data.repository.StudentRepository
import me.june8th.euet.core.data.source.canvas.CanvasApi
import me.june8th.euet.core.data.source.canvas.CanvasAuthInterceptor
import me.june8th.euet.core.data.source.daotao.DaotaoClient
import me.june8th.euet.core.data.source.studenthub.StudentHubApi
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manual dependency container — the single owner of app-scoped singletons. Built once in
 * [me.june8th.euet.app.App] and exposed to Compose via [LocalAppContainer].
 *
 * (Manual DI rather than Hilt: this toolchain's AGP built-in Kotlin rejects KAPT and no KSP build
 * exists for Kotlin 2.4.0, so no annotation processor is available.)
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val session: SessionManager = SessionManager(appContext)

    /** Offline snapshot store backing every screen's instant first render. */
    val snapshotCache: SnapshotCache = SnapshotCache(appContext)

    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    /**
     * StudentHub's session lives in cookies (the captured authenticated XHRs carry no
     * `Authorization` header at all), so the client needs a jar — persisted, because the captcha
     * on `api/auth/login` rules out renewing the session silently.
     */
    private val studentHubCookieJar: StudentHubCookieJar = StudentHubCookieJar(
        url = STUDENTHUB_BASE_URL.toHttpUrl(),
        load = session::loadStudentHubCookies,
        persist = session::saveStudentHubCookies,
    )

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(studentHubCookieJar)
        .addInterceptor(AuthInterceptor(session, studentHubCookieJar))
        // BASIC logs the request/response line and sizes only — never headers or bodies — so the
        // sign-in password and captcha answer stay out of logcat. Do not raise this level.
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(STUDENTHUB_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val studentHubApi: StudentHubApi = retrofit.create(StudentHubApi::class.java)

    val authRepository: AuthRepository =
        AuthRepository(session, studentHubApi, snapshotCache, studentHubCookieJar)
    val studentRepository: StudentRepository = StudentRepository(studentHubApi, session)

    /**
     * VNU daotao gets its own client: the portal is cookie-session based, so it must not share
     * the StudentHub client's cookie jar or bearer-token interceptor.
     */
    val daotaoRepository: DaotaoRepository = DaotaoRepository(DaotaoClient(), session)

    /**
     * Canvas also gets its own client: it authenticates with a user-generated access token, so it
     * must not share the StudentHub client's bearer-token interceptor (or its 401 handling).
     */
    private val canvasOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(CanvasAuthInterceptor(session))
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val canvasApi: CanvasApi = Retrofit.Builder()
        .baseUrl(CANVAS_BASE_URL)
        .client(canvasOkHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(CanvasApi::class.java)

    val canvasRepository: CanvasRepository = CanvasRepository(canvasApi, session)

    /** Capability → connected-provider resolution (iOS ProviderRegistry port). */
    val providerRegistry: ProviderRegistry = ProviderRegistry(session)

    /** The façade feature ViewModels read from; picks the best connected source per capability. */
    val aggregateRepository: AggregateRepository =
        AggregateRepository(providerRegistry, studentRepository, daotaoRepository)

    companion object {
        private const val STUDENTHUB_BASE_URL = "https://studenthub.uet.edu.vn/"

        /** The UET Canvas instance — the single switch point if the university moves hosts. */
        private const val CANVAS_BASE_URL = "https://portal.uet.vnu.edu.vn/"
    }
}
