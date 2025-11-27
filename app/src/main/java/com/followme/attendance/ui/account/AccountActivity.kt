package com.followme.attendance.ui.account

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.followme.attendance.R

/**
 * Account Activity - Display user account information
 * TODO: Implement account UI with:
 * - User ID, Name, Email, Role display
 * - Token expiration information
 * - Logout button
 */
class AccountActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: Create activity_account.xml layout
        // setContentView(R.layout.activity_account)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.account)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
