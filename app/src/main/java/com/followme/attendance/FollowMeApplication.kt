package com.followme.attendance

import android.app.Application
import com.followme.attendance.data.local.PreferencesManager
import com.followme.attendance.data.repository.AttendanceRepository
import com.followme.attendance.service.AttendanceSubmissionService

class FollowMeApplication : Application() {
    
    lateinit var preferencesManager: PreferencesManager
        private set
    
    lateinit var attendanceRepository: AttendanceRepository
        private set
    
    lateinit var submissionService: AttendanceSubmissionService
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize preferences manager
        preferencesManager = PreferencesManager(this)
        
        // Initialize repository
        attendanceRepository = AttendanceRepository(this, preferencesManager)
        
        // Initialize submission service
        submissionService = AttendanceSubmissionService(this, attendanceRepository, preferencesManager)
        submissionService.startPeriodicSubmission()
    }
    
    companion object {
        lateinit var instance: FollowMeApplication
            private set
    }
}
