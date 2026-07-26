package me.june8th.euet.app.di

import android.content.Context
import kotlinx.serialization.json.Json
import me.june8th.euet.core.datastore.SessionManager
import me.june8th.euet.core.network.AuthInterceptor
import me.june8th.euet.core.data.repository.AuthRepository
import me.june8th.euet.core.data.repository.StudentRepository
import me.june8th.euet.core.data.source.studenthub.StudentHubApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manual dependency container — the single owner of app-scoped singletons. Built once in
 * [me.june8th.euet.app.EUetApplication] and exposed to Compose via [LocalAppContainer].
 *
 * (Manual DI rather than Hilt: this toolchain's AGP built-in Kotlin rejects KAPT and no KSP build
 * exists for Kotlin 2.4.0, so no annotation processor is available.)
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val session: SessionManager = SessionManager(appContext)

    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(session))
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

    val authRepository: AuthRepository = AuthRepository(session, studentHubApi)
    val studentRepository: StudentRepository = StudentRepository(studentHubApi, session)

    companion object {
        private const val STUDENTHUB_BASE_URL = "https://studenthub.uet.edu.vn/"
    }
}
