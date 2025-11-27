package com.followme.attendance.data.api

import com.google.gson.annotations.SerializedName

// ========== Authentication Request/Response ==========

data class AuthRequest(
    @SerializedName("username")
    val username: String,
    
    @SerializedName("password")
    val password: String
)

data class AuthResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("accessToken")
    val accessToken: String?,
    
    @SerializedName("expiresIn")
    val expiresIn: Int?,
    
    @SerializedName("user")
    val user: UserInfo?,
    
    @SerializedName("teacher")
    val teacher: UserInfo?,
    
    @SerializedName("id")
    val id: String?,
    
    @SerializedName("role")
    val role: String?
)

data class UserInfo(
    @SerializedName("id")
    val id: String?,
    
    @SerializedName("name")
    val name: String?,
    
    @SerializedName("email")
    val email: String?,
    
    @SerializedName("role")
    val role: String?
)

// ========== Token Validation Response ==========

data class TokenValidationResponse(
    @SerializedName("valid")
    val valid: Boolean,
    
    @SerializedName("user")
    val user: UserInfo?,
    
    @SerializedName("apiKey")
    val apiKey: String?,
    
    @SerializedName("expiresAt")
    val expiresAt: String?
)

// ========== Attendance Submission ==========

data class AttendanceEntry(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("ts")
    val ts: String,
    
    @SerializedName("category")
    val category: String,
    
    @SerializedName("user_id")
    val userId: String?
)

data class AttendanceResponseItem(
    @SerializedName("name")
    val name: String?,
    
    @SerializedName("phone")
    val phone: String?
)

// ========== API Result ==========

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}
