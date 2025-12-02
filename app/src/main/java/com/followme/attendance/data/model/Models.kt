package com.followme.attendance.data.model

import com.google.gson.annotations.SerializedName

/**
 * App settings data model
 */
data class AppSettings(
    @SerializedName("api")
    var apiBaseUrl: String = "",
    
    @SerializedName("key")
    var apiKey: String = "",
    
    @SerializedName("username")
    var username: String = "",
    
    @SerializedName("password")
    var password: String = "",
    
    @SerializedName("authRoute")
    var authRoute: String = "",
    
    @SerializedName("validateRoute")
    var validateRoute: String = "",
    
    @SerializedName("mainRoute")
    var mainRoute: String = "",
    
    @SerializedName("extensions")
    var extensions: Map<String, String> = emptyMap(),
    
    @SerializedName("categories")
    var categories: Map<String, String> = mapOf("Main" to "Main")
) {
    companion object {
        val DEFAULT_CATEGORIES = mapOf("Main" to "Main")
    }
}

/**
 * Authentication token data model
 */
data class AuthToken(
    @SerializedName("accessToken")
    val accessToken: String,
    
    @SerializedName("expiresIn")
    val expiresIn: Int,
    
    @SerializedName("expiresAt")
    val expiresAt: Long,
    
    @SerializedName("userId")
    val userId: String? = null,
    
    @SerializedName("role")
    val role: String? = null,

    @SerializedName("canEditTags")
    val canEditTags: Boolean = false
) {
    fun isExpired(): Boolean {
        return System.currentTimeMillis() >= expiresAt
    }
    
    fun isAdmin(): Boolean {
        return role?.lowercase()?.trim() == "admin"
    }
}

/**
 * Scanned tag entry data model
 */
data class ScannedTagEntry(
    @SerializedName("data")
    val data: Map<String, String>,
    
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("category")
    val category: String = "Main",
    
    @SerializedName("isSelected")
    var isSelected: Boolean = false,
    
    @SerializedName("status")
    var status: EntryStatus = EntryStatus.PENDING,
    
    @SerializedName("submittedAt")
    var submittedAt: String? = null
) {
    val name: String
        get() = data["name"] ?: ""
    
    val phone: String?
        get() = data["phone"]
}

/**
 * Entry submission status
 */
enum class EntryStatus {
    @SerializedName("pending")
    PENDING,
    
    @SerializedName("submitted")
    SUBMITTED,
    
    @SerializedName("failed")
    FAILED,
    
    @SerializedName("unmatched")
    UNMATCHED
}

/**
 * Tag data for writing
 */
data class TagData(
    val data: Map<String, String>,
    val url: String? = null,
    var isSelected: Boolean = false
)
