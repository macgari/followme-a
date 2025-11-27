package com.followme.attendance.data.api

import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface
 */
interface ApiService {
    
    @POST
    suspend fun authenticate(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: AuthRequest
    ): Response<AuthResponse>
    
    @GET
    suspend fun validateToken(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<TokenValidationResponse>
    
    @POST
    suspend fun submitAttendance(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body entries: List<AttendanceEntry>
    ): Response<Any>
}
