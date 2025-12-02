package com.followme.attendance.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.followme.attendance.data.model.AppSettings
import com.followme.attendance.data.model.AuthToken
import com.followme.attendance.data.model.ScannedTagEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manages app preferences and secure storage
 */
class PreferencesManager(context: Context) {
    
    private val gson = Gson()
    
    // Regular preferences for non-sensitive data
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "followme_prefs",
        Context.MODE_PRIVATE
    )
    
    // Encrypted preferences for sensitive data
    private val encryptedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            "followme_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    companion object {
        private const val KEY_SETTINGS = "app_settings"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_SCANNED_TAGS = "scanned_tags"
    }
    
    // ========== App Settings ==========
    
    fun saveSettings(settings: AppSettings) {
        val json = gson.toJson(settings)
        encryptedPreferences.edit().putString(KEY_SETTINGS, json).apply()
    }
    
    fun loadSettings(): AppSettings {
        val json = encryptedPreferences.getString(KEY_SETTINGS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, AppSettings::class.java)
            } catch (e: Exception) {
                AppSettings()
            }
        } else {
            AppSettings()
        }
    }
    
    // ========== Auth Token ==========
    
    fun saveAuthToken(token: AuthToken) {
        val json = gson.toJson(token)
        encryptedPreferences.edit().putString(KEY_AUTH_TOKEN, json).apply()
    }
    
    fun loadAuthToken(): AuthToken? {
        val json = encryptedPreferences.getString(KEY_AUTH_TOKEN, null)
        return if (json != null) {
            try {
                gson.fromJson(json, AuthToken::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    fun clearAuthToken() {
        encryptedPreferences.edit().remove(KEY_AUTH_TOKEN).apply()
    }
    
    fun isTokenExpired(): Boolean {
        val token = loadAuthToken() ?: return true
        return token.isExpired()
    }
    
    fun isAdmin(): Boolean {
        val token = loadAuthToken() ?: return false
        return token.isAdmin()
    }

    fun canEditTags(): Boolean {
        val token = loadAuthToken() ?: return false
        return token.isAdmin() || token.canEditTags
    }
    
    // ========== Scanned Tags ==========
    
    fun saveScannedTags(tags: List<ScannedTagEntry>) {
        val json = gson.toJson(tags)
        preferences.edit().putString(KEY_SCANNED_TAGS, json).apply()
    }
    
    fun loadScannedTags(): List<ScannedTagEntry> {
        val json = preferences.getString(KEY_SCANNED_TAGS, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<ScannedTagEntry>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    fun clearScannedTags() {
        preferences.edit().remove(KEY_SCANNED_TAGS).apply()
    }
    
    // ========== Category Migration ==========
    
    fun migrateCategoriesIfNeeded(currentTags: List<ScannedTagEntry>): List<ScannedTagEntry> {
        val settings = loadSettings()
        val validCategories = settings.categories.keys
        
        var needsMigration = false
        val migratedTags = currentTags.map { entry ->
            if (!validCategories.contains(entry.category)) {
                needsMigration = true
                entry.copy(category = "Main")
            } else {
                entry
            }
        }
        
        if (needsMigration) {
            saveScannedTags(migratedTags)
        }
        
        return migratedTags
    }
}
