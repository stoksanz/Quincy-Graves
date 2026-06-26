package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ActivityEntity
import com.example.data.model.FamilyEntity
import com.example.data.model.UserEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Supabase Auth Models
data class SupabaseSignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>
)

data class SupabaseSignInRequest(
    val email: String,
    val password: String
)

data class SupabaseRecoverRequest(
    val email: String
)

data class SupabaseAuthUser(
    val id: String?,
    val email: String?
)

data class SupabaseAuthResponse(
    val access_token: String?,
    val refresh_token: String?,
    val user: SupabaseAuthUser?
)

// Retrofit API interfaces
interface SupabaseAuthService {
    @Headers("Content-Type: application/json")
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Body request: SupabaseSignUpRequest
    ): retrofit2.Response<SupabaseAuthResponse>

    @Headers("Content-Type: application/json")
    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Header("apikey") apiKey: String,
        @Body request: SupabaseSignInRequest
    ): retrofit2.Response<SupabaseAuthResponse>

    @Headers("Content-Type: application/json")
    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Header("apikey") apiKey: String,
        @Body request: SupabaseRecoverRequest
    ): retrofit2.Response<Unit>
}

interface SupabaseDatabaseService {
    // Users Table REST
    @GET("rest/v1/users")
    suspend fun getUsers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*",
        @Query("email") emailFilter: String? = null,
        @Query("familyCode") familyCodeFilter: String? = null
    ): List<UserEntity>

    @Headers("Content-Type: application/json", "Prefer: return=representation")
    @POST("rest/v1/users")
    suspend fun insertUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body user: UserEntity
    ): List<UserEntity>

    @Headers("Content-Type: application/json", "Prefer: return=representation")
    @PATCH("rest/v1/users")
    suspend fun updateUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("email") emailFilter: String,
        @Body fields: Map<String, Any?>
    ): List<UserEntity>

    // Families Table REST
    @GET("rest/v1/families")
    suspend fun getFamilies(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*",
        @Query("code") codeFilter: String? = null
    ): List<FamilyEntity>

    @Headers("Content-Type: application/json", "Prefer: return=representation")
    @POST("rest/v1/families")
    suspend fun insertFamily(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body family: FamilyEntity
    ): List<FamilyEntity>

    // Activities (Laporan) Table REST
    @GET("rest/v1/activities")
    suspend fun getActivities(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*",
        @Query("familyCode") familyCodeFilter: String? = null,
        @Query("order") order: String = "timestamp.desc"
    ): List<ActivityEntity>

    @Headers("Content-Type: application/json", "Prefer: return=representation")
    @POST("rest/v1/activities")
    suspend fun insertActivity(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body activity: ActivityEntity
    ): List<ActivityEntity>
}

object SupabaseClient {
    private const val TAG = "SupabaseClient"

    // Configuration values read directly from the Android BuildConfig secrets panel injection
    val supabaseUrl: String by lazy {
        try {
            BuildConfig.EXPO_PUBLIC_SUPABASE_URL.trim()
        } catch (e: Throwable) {
            "https://placeholder-project.supabase.co"
        }
    }

    val supabaseAnonKey: String by lazy {
        try {
            BuildConfig.EXPO_PUBLIC_SUPABASE_ANON_KEY.trim()
        } catch (e: Throwable) {
            "placeholder"
        }
    }

    // Checking if config is valid (i.e. not using the default placeholder URLs)
    fun isConfigured(): Boolean {
        return supabaseUrl.isNotEmpty() &&
                !supabaseUrl.contains("placeholder-project") &&
                supabaseAnonKey.isNotEmpty() &&
                supabaseAnonKey != "placeholder"
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    // Build Retrofit services
    private val retrofit: Retrofit? by lazy {
        if (supabaseUrl.isNotEmpty()) {
            val baseUrl = if (supabaseUrl.endsWith("/")) supabaseUrl else "$supabaseUrl/"
            try {
                Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "Error building Retrofit instance", e)
                null
            }
        } else {
            null
        }
    }

    val authService: SupabaseAuthService? by lazy {
        retrofit?.create(SupabaseAuthService::class.java)
    }

    val databaseService: SupabaseDatabaseService? by lazy {
        retrofit?.create(SupabaseDatabaseService::class.java)
    }

    // Helper for Authorization Header
    fun getBearerToken(accessToken: String?): String {
        return if (accessToken.isNullOrEmpty()) "Bearer $supabaseAnonKey" else "Bearer $accessToken"
    }
}
