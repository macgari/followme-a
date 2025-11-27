package com.followme.attendance.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.followme.attendance.data.local.PreferencesManager
import com.followme.attendance.data.repository.AttendanceRepository
import kotlinx.coroutines.*
import java.util.*

/**
 * Service for automatic attendance submission
 */
class AttendanceSubmissionService(
    private val context: Context,
    private val repository: AttendanceRepository,
    private val preferencesManager: PreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var submissionTimer: Timer? = null
    private var isSubmitting = false
    private var submissionStartTime: Long? = null
    private val submissionTimeout = 30_000L // 30 seconds
    
    private var isOnline = false
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            isOnline = true
            // Trigger submission when coming back online
            scope.launch {
                checkAndSubmitIfNeeded()
            }
        }
        
        override fun onLost(network: Network) {
            isOnline = false
        }
    }
    
    init {
        // Register network callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Check initial network state
        isOnline = isNetworkAvailable()
    }
    
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    // ========== Periodic Submission ==========
    
    fun startPeriodicSubmission() {
        stopPeriodicSubmission()
        
        submissionTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    scope.launch {
                        checkAndSubmitIfNeeded()
                    }
                }
            }, 60_000L, 60_000L) // Every 60 seconds
        }
    }
    
    fun stopPeriodicSubmission() {
        submissionTimer?.cancel()
        submissionTimer = null
    }
    
    // ========== Submission Logic ==========
    
    suspend fun checkAndSubmitIfNeeded() {
        // Check if already submitting
        if (isSubmitting) {
            // Check for timeout
            submissionStartTime?.let { startTime ->
                if (System.currentTimeMillis() - startTime > submissionTimeout) {
                    // Reset stuck submission
                    isSubmitting = false
                    submissionStartTime = null
                }
            }
            return
        }
        
        // Check if online
        if (!isOnline) {
            return
        }
        
        // Check if there are pending entries
        val pendingEntries = repository.getPendingOrFailedEntries()
        if (pendingEntries.isEmpty()) {
            return
        }
        
        // Check if authenticated
        val token = preferencesManager.loadAuthToken()
        if (token == null || token.isExpired()) {
            // Try to re-authenticate
            val authResult = repository.ensureAuthenticated()
            if (authResult !is com.followme.attendance.data.api.ApiResult.Success) {
                return
            }
        }
        
        // Submit entries
        submitEntries()
    }
    
    suspend fun submitEntries() {
        if (isSubmitting) {
            return
        }
        
        isSubmitting = true
        submissionStartTime = System.currentTimeMillis()
        
        try {
            repository.submitPendingEntries()
        } catch (e: Exception) {
            // Log error
            e.printStackTrace()
        } finally {
            isSubmitting = false
            submissionStartTime = null
        }
    }
    
    // ========== Cleanup ==========
    
    fun cleanup() {
        stopPeriodicSubmission()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        scope.cancel()
    }
}
