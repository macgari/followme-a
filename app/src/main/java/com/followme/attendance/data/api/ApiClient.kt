package com.followme.attendance.data.api

import com.followme.attendance.data.local.PreferencesManager
import com.followme.attendance.data.model.AppSettings
import com.followme.attendance.data.model.AuthToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API client for handling network requests
 */
class ApiClient(private val preferencesManager: PreferencesManager) {
    
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()
    
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://placeholder.com/") // Base URL is required but we use dynamic URLs
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    private val apiService: ApiService = retrofit.create(ApiService::class.java)
    
    // ========== Helper Methods ==========
    
    private fun normalizeBaseUrl(url: String): String {
        var cleaned = url.trim()
        if (!cleaned.contains("://")) {
            cleaned = "http://$cleaned"
        }
        if (cleaned.endsWith("/")) {
            cleaned = cleaned.dropLast(1)
        }
        return cleaned
    }
    
    private fun buildUrl(baseUrl: String, route: String): String {
        val normalizedBase = normalizeBaseUrl(baseUrl)
        val normalizedRoute = route.trim().removePrefix("/")
        return if (normalizedRoute.isEmpty()) {
            normalizedBase
        } else {
            "$normalizedBase/$normalizedRoute"
        }
    }
    
    private fun buildHeaders(apiKey: String? = null, token: String? = null, extensions: Map<String, String> = emptyMap()): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        headers["Content-Type"] = "application/json"
        
        if (apiKey != null) {
            headers["X-API-Key"] = apiKey
        }
        
        if (token != null) {
            headers["Authorization"] = "Bearer $token"
        }
        
        headers.putAll(extensions)
        
        return headers
    }
    
    // ========== Authentication ==========
    
    suspend fun authenticate(
        baseUrl: String,
        apiKey: String,
        username: String,
        password: String,
        authRoute: String,
        extensions: Map<String, String> = emptyMap()
    ): ApiResult<AuthToken> {
        return try {
            val url = buildUrl(baseUrl, authRoute)
            val headers = buildHeaders(apiKey = apiKey, extensions = extensions)
            val request = AuthRequest(username, password)
            
            val response = apiService.authenticate(url, headers, request)
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                
                if (body.success && body.accessToken != null && body.expiresIn != null) {
                    // Extract user ID and role from various response formats
                    val userId = body.user?.id ?: body.teacher?.id ?: body.id
                    val role = body.user?.role ?: body.teacher?.role ?: body.role
                    val canEditTags = body.canEditTags ?: body.user?.canEditTags ?: body.teacher?.canEditTags ?: false
                    
                    val expiresAt = System.currentTimeMillis() + (body.expiresIn * 1000L)
                    
                    val token = AuthToken(
                        accessToken = body.accessToken,
                        expiresIn = body.expiresIn,
                        expiresAt = expiresAt,
                        userId = userId,
                        role = role,
                        canEditTags = canEditTags
                    )
                    
                    // Save token
                    preferencesManager.saveAuthToken(token)
                    
                    ApiResult.Success(token)
                } else {
                    ApiResult.Error("Authentication failed: Invalid response")
                }
            } else {
                ApiResult.Error("Authentication failed: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error("Authentication error: ${e.message}")
        }
    }
    
    // ========== Token Validation ==========
    
    suspend fun validateToken(
        baseUrl: String,
        validateRoute: String
    ): ApiResult<Boolean> {
        return try {
            val token = preferencesManager.loadAuthToken()
            if (token == null || token.isExpired()) {
                return ApiResult.Success(false)
            }
            
            val url = buildUrl(baseUrl, validateRoute)
            val headers = buildHeaders(token = token.accessToken)
            
            val response = apiService.validateToken(url, headers)
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                ApiResult.Success(body.valid)
            } else {
                ApiResult.Success(false)
            }
        } catch (e: Exception) {
            ApiResult.Success(false)
        }
    }
    
    // ========== Ensure Authenticated ==========
    
    suspend fun ensureAuthenticated(settings: AppSettings): ApiResult<AuthToken> {
        // Check if token exists and is valid
        val token = preferencesManager.loadAuthToken()
        if (token != null && !token.isExpired()) {
            // Validate token
            val validationResult = validateToken(settings.apiBaseUrl, settings.validateRoute)
            if (validationResult is ApiResult.Success && validationResult.data) {
                return ApiResult.Success(token)
            }
        }
        
        // Token is invalid or expired, re-authenticate
        return authenticate(
            baseUrl = settings.apiBaseUrl,
            apiKey = settings.apiKey,
            username = settings.username,
            password = settings.password,
            authRoute = settings.authRoute,
            extensions = settings.extensions
        )
    }
    
    // ========== Submit Attendance ==========
    
    suspend fun submitAttendance(
        baseUrl: String,
        mainRoute: String,
        entries: List<AttendanceEntry>
    ): ApiResult<List<AttendanceResponseItem>> {
        return try {
            val token = preferencesManager.loadAuthToken()
            if (token == null || token.isExpired()) {
                return ApiResult.Error("Not authenticated")
            }
            
            val url = buildUrl(baseUrl, mainRoute)
            val headers = buildHeaders(token = token.accessToken)
            
            val response = apiService.submitAttendance(url, headers, entries)
            
            if (response.isSuccessful) {
                val responseItems = parseAttendanceResponse(response)
                ApiResult.Success(responseItems)
            } else {
                ApiResult.Error("Submission failed: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error("Submission error: ${e.message}")
        }
    }
    
    private fun parseAttendanceResponse(response: Response<Any>): List<AttendanceResponseItem> {
        val body = response.body() ?: return emptyList()
        
        return try {
            when (body) {
                is List<*> -> {
                    body.mapNotNull { item ->
                        when (item) {
                            is Map<*, *> -> {
                                AttendanceResponseItem(
                                    name = item["name"] as? String,
                                    phone = item["phone"] as? String
                                )
                            }
                            is String -> {
                                AttendanceResponseItem(name = item, phone = null)
                            }
                            else -> null
                        }
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
