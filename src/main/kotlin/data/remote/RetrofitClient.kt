package data.remote


import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import data.remote.services.AuditApiService
import data.remote.services.AuthApiService
import data.remote.services.CarePlanApiService
import data.remote.services.DiagnosisApiService
import data.remote.services.EventApiService
import data.remote.services.MedicationApiService
import data.remote.services.PatientApiService
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import utils.TokenManager
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://localhost:8080/api/v1/"
    private const val REQUEST_TIMEOUT_SECONDS = 30L
    private const val APPLICATION_JSON_CONTENT_TYPE = "application/json"

    // Список эндпоинтов, для которых НЕ нужно добавлять токен авторизации
    private val NO_AUTH_ENDPOINTS = listOf("auth/signin", "auth/signup")

    private class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()

            // Проверяем, нужно ли добавлять токен для текущего запроса
            val path = originalRequest.url.encodedPathSegments.joinToString("/")
            if (!NO_AUTH_ENDPOINTS.any { path.endsWith(it) }) {
                val token = TokenManager.getToken()
                token?.let {
                    requestBuilder.addHeader("Authorization", "Bearer $it")
                }
            }
            return chain.proceed(requestBuilder.build())
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor()) // Добавляем наш AuthInterceptor
            .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        val contentType = APPLICATION_JSON_CONTENT_TYPE.toMediaType()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val patientApiService: PatientApiService by lazy {
        retrofit.create(PatientApiService::class.java)
    }

    val medicationApiService: MedicationApiService by lazy {
        retrofit.create(MedicationApiService::class.java)
    }

    val diagnosisApiService: DiagnosisApiService by lazy {
        retrofit.create(DiagnosisApiService::class.java)
    }

    val carePlanApiService: CarePlanApiService by lazy {
        retrofit.create(CarePlanApiService::class.java)
    }

    val eventApiService: EventApiService by lazy {
        retrofit.create(EventApiService::class.java)
    }

    val auditApiService: AuditApiService by lazy {
        retrofit.create(AuditApiService::class.java)
    }

}
