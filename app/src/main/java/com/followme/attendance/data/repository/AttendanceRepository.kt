package com.followme.attendance.data.repository

import android.content.Context
import com.followme.attendance.data.api.ApiClient
import com.followme.attendance.data.api.ApiResult
import com.followme.attendance.data.api.AttendanceEntry
import com.followme.attendance.data.local.PreferencesManager
import com.followme.attendance.data.model.AuthToken
import com.followme.attendance.data.model.EntryStatus
import com.followme.attendance.data.model.ScannedTagEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for managing attendance entries
 */
class AttendanceRepository(
    context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val apiClient = ApiClient(preferencesManager)
    
    private val _entries = MutableStateFlow<List<ScannedTagEntry>>(emptyList())
    val entries: StateFlow<List<ScannedTagEntry>> = _entries.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    init {
        loadEntries()
        checkInitialAuthStatus()
    }
    
    private fun checkInitialAuthStatus() {
        val token = preferencesManager.loadAuthToken()
        _isAuthenticated.value = token != null && !token.isExpired()
    }
    
    // ========== Entry Management ==========
    
    fun loadEntries() {
        val loadedEntries = preferencesManager.loadScannedTags()
        val migratedEntries = preferencesManager.migrateCategoriesIfNeeded(loadedEntries)
        _entries.value = migratedEntries
    }
    
    fun addEntry(data: Map<String, String>, category: String = "Main") {
        val timestamp = getCurrentTimestamp()
        val entry = ScannedTagEntry(
            data = data,
            timestamp = timestamp,
            category = category,
            status = EntryStatus.PENDING
        )
        
        val currentEntries = _entries.value.toMutableList()
        currentEntries.add(0, entry) // Add to beginning
        _entries.value = currentEntries
        saveEntries()
    }
    
    fun deleteSelectedEntries() {
        val currentEntries = _entries.value.toMutableList()
        currentEntries.removeAll { it.isSelected }
        _entries.value = currentEntries
        saveEntries()
    }
    
    fun toggleEntrySelection(index: Int) {
        val currentEntries = _entries.value.toMutableList()
        if (index in currentEntries.indices) {
            currentEntries[index] = currentEntries[index].copy(
                isSelected = !currentEntries[index].isSelected
            )
            _entries.value = currentEntries
        }
    }
    
    fun selectAllEntries(selected: Boolean) {
        val currentEntries = _entries.value.map { it.copy(isSelected = selected) }
        _entries.value = currentEntries
    }
    
    fun filterEntriesByCategory(category: String): List<ScannedTagEntry> {
        return _entries.value.filter { it.category == category }
    }
    
    fun getPendingOrFailedEntries(): List<ScannedTagEntry> {
        return _entries.value.filter { 
            it.status == EntryStatus.PENDING || it.status == EntryStatus.FAILED 
        }
    }
    
    private fun saveEntries() {
        preferencesManager.saveScannedTags(_entries.value)
    }
    
    // ========== Authentication ==========
    
    suspend fun authenticate(
        baseUrl: String,
        apiKey: String,
        username: String,
        password: String,
        authRoute: String
    ): ApiResult<AuthToken> {
        val result = apiClient.authenticate(
            baseUrl = baseUrl,
            apiKey = apiKey,
            username = username,
            password = password,
            authRoute = authRoute
        )
        
        _isAuthenticated.value = result is ApiResult.Success
        return result
    }
    
    suspend fun validateToken(baseUrl: String, validateRoute: String): ApiResult<Boolean> {
        val result = apiClient.validateToken(baseUrl, validateRoute)
        _isAuthenticated.value = result is ApiResult.Success && result.data
        return result
    }
    
    suspend fun ensureAuthenticated(): ApiResult<AuthToken> {
        val settings = preferencesManager.loadSettings()
        val result = apiClient.ensureAuthenticated(settings)
        _isAuthenticated.value = result is ApiResult.Success
        return result
    }
    
    fun logout() {
        preferencesManager.clearAuthToken()
        _isAuthenticated.value = false
    }
    
    // ========== Attendance Submission ==========
    
    suspend fun submitPendingEntries(): ApiResult<Int> {
        val settings = preferencesManager.loadSettings()
        val pendingEntries = getPendingOrFailedEntries()
        
        if (pendingEntries.isEmpty()) {
            return ApiResult.Success(0)
        }
        
        // Ensure authenticated first
        val authResult = ensureAuthenticated()
        if (authResult !is ApiResult.Success) {
            return ApiResult.Error("Not authenticated")
        }
        
        // Convert to API format
        val token = preferencesManager.loadAuthToken()
        val attendanceEntries = pendingEntries.map { entry ->
            AttendanceEntry(
                name = entry.name,
                ts = entry.timestamp,
                category = entry.category,
                userId = token?.userId
            )
        }
        
        // Submit to API
        val result = apiClient.submitAttendance(
            baseUrl = settings.apiBaseUrl,
            mainRoute = settings.mainRoute,
            entries = attendanceEntries
        )
        
        return when (result) {
            is ApiResult.Success -> {
                // Update entry statuses based on response
                updateEntryStatuses(pendingEntries, result.data)
                ApiResult.Success(pendingEntries.size)
            }
            is ApiResult.Error -> {
                // Mark all as failed
                markEntriesAsFailed(pendingEntries)
                result
            }
            else -> ApiResult.Error("Unknown error")
        }
    }
    
    private fun updateEntryStatuses(
        submittedEntries: List<ScannedTagEntry>,
        responseItems: List<com.followme.attendance.data.api.AttendanceResponseItem>
    ) {
        val currentEntries = _entries.value.toMutableList()
        val submittedTimestamp = getCurrentTimestamp()
        
        submittedEntries.forEach { entry ->
            val index = currentEntries.indexOfFirst { 
                it.timestamp == entry.timestamp && it.category == entry.category 
            }
            
            if (index != -1) {
                // Check if this entry was unmatched
                val responseItem = responseItems.find { 
                    it.phone == entry.phone || it.name == entry.name 
                }
                
                val status = if (responseItem?.name == "UNMATCHED") {
                    EntryStatus.UNMATCHED
                } else {
                    EntryStatus.SUBMITTED
                }
                
                currentEntries[index] = currentEntries[index].copy(
                    status = status,
                    submittedAt = submittedTimestamp
                )
            }
        }
        
        _entries.value = currentEntries
        saveEntries()
    }
    
    private fun markEntriesAsFailed(entries: List<ScannedTagEntry>) {
        val currentEntries = _entries.value.toMutableList()
        
        entries.forEach { entry ->
            val index = currentEntries.indexOfFirst { 
                it.timestamp == entry.timestamp && it.category == entry.category 
            }
            
            if (index != -1) {
                currentEntries[index] = currentEntries[index].copy(
                    status = EntryStatus.FAILED
                )
            }
        }
        
        _entries.value = currentEntries
        saveEntries()
    }
    
    // ========== Utility ==========
    
    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date())
    }
}
