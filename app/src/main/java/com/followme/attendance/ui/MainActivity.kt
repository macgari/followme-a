package com.followme.attendance.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.followme.attendance.FollowMeApplication
import com.followme.attendance.R
import com.followme.attendance.data.api.ApiResult
import com.followme.attendance.databinding.ActivityMainBinding
import com.followme.attendance.nfc.NfcHandler
import com.followme.attendance.ui.adapters.EntriesAdapter
import com.followme.attendance.ui.settings.SettingsActivity
import com.followme.attendance.ui.tags.TagManagementActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var nfcHandler: NfcHandler
    private lateinit var entriesAdapter: EntriesAdapter
    
    private val repository by lazy { (application as FollowMeApplication).attendanceRepository }
    private val preferencesManager by lazy { (application as FollowMeApplication).preferencesManager }
    private val submissionService by lazy { (application as FollowMeApplication).submissionService }
    
    private var isContinuousScanning = false
    private var selectedCategory = "Main"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupNfc()
        setupRecyclerView()
        setupListeners()
        observeData()
        checkAuthentication()
        
        // Handle NFC intent if app was launched from NFC tag
        handleNfcIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
        repository.loadEntries()
        checkAuthentication()
    }
    
    override fun onPause() {
        super.onPause()
        disableNfcForegroundDispatch()
    }
    
    // ========== Setup ==========
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
    
    private fun setupNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcHandler = NfcHandler()
        
        if (nfcAdapter == null) {
            Toast.makeText(this, R.string.nfc_not_supported, Toast.LENGTH_LONG).show()
            binding.scanTagButton.isEnabled = false
            binding.continuousScanButton.isEnabled = false
        } else {
            if (nfcAdapter?.isEnabled == false) {
                Toast.makeText(this, R.string.nfc_disabled, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setupRecyclerView() {
        entriesAdapter = EntriesAdapter(
            onItemClick = { index ->
                repository.toggleEntrySelection(index)
            },
            onCheckboxClick = { index ->
                repository.toggleEntrySelection(index)
            }
        )
        
        binding.entriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = entriesAdapter
        }
    }
    
    private fun setupListeners() {
        // Category dropdown
        binding.categoryDropdown.setOnClickListener {
            showCategoryPicker()
        }
        
        // Manual entry
        binding.addEntryButton.setOnClickListener {
            addManualEntry()
        }
        
        binding.manualEntryInput.setOnEditorActionListener { _, _, _ ->
            addManualEntry()
            true
        }
        
        // NFC scanning
        binding.scanTagButton.setOnClickListener {
             if (nfcAdapter != null) {
                Toast.makeText(this, R.string.hold_tag_near_device, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.nfc_not_supported, Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.continuousScanButton.setOnClickListener {
            if (nfcAdapter != null) {
                toggleContinuousScanning()
            } else {
                Toast.makeText(this, R.string.nfc_not_supported, Toast.LENGTH_SHORT).show()
            }
        }
        
        // Entry actions
        binding.checkAllButton.setOnClickListener {
            val allSelected = entriesAdapter.currentList.all { it.isSelected }
            repository.selectAllEntries(!allSelected)
            binding.checkAllButton.text = if (allSelected) {
                getString(R.string.check_all)
            } else {
                getString(R.string.uncheck_all)
            }
        }
        
        binding.deleteSelectedButton.setOnClickListener {
            confirmDeleteSelected()
        }
    }
    
    private fun observeData() {
        // Observe entries
        lifecycleScope.launch {
            repository.entries.collectLatest { entries ->
                val filteredEntries = entries.filter { it.category == selectedCategory }
                entriesAdapter.submitList(filteredEntries)
                
                // Show/hide empty state
                if (filteredEntries.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.entriesRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.entriesRecyclerView.visibility = View.VISIBLE
                }
            }
        }
        
        // Observe authentication status
        lifecycleScope.launch {
            repository.isAuthenticated.collectLatest { isAuthenticated ->
                updateAuthStatus(isAuthenticated)
            }
        }
    }
    
    // ========== Authentication ==========
    
    private fun checkAuthentication() {
        lifecycleScope.launch {
            val settings = preferencesManager.loadSettings()
            
            // If we have API credentials, try to authenticate
            if (settings.apiBaseUrl.isNotEmpty() && 
                settings.apiKey.isNotEmpty() &&
                settings.username.isNotEmpty() &&
                settings.password.isNotEmpty() &&
                settings.authRoute.isNotEmpty()) {
                
                // Attempt to authenticate with saved credentials
                val result = repository.authenticate(
                    baseUrl = settings.apiBaseUrl,
                    apiKey = settings.apiKey,
                    username = settings.username,
                    password = settings.password,
                    authRoute = settings.authRoute
                )
                
                // Show success message if authenticated
                if (result is ApiResult.Success) {
                    Toast.makeText(
                        this@MainActivity,
                        "Authenticated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // Silently fail on error - don't annoy user on startup
            }
        }
    }
    
    private fun updateAuthStatus(isAuthenticated: Boolean) {
        if (isAuthenticated) {
            binding.authStatusIcon.setColorFilter(getColor(R.color.authenticated))
        } else {
            binding.authStatusIcon.setColorFilter(getColor(R.color.not_authenticated))
        }
    }
    
    // ========== NFC Handling ==========
    
    private fun enableNfcForegroundDispatch() {
        nfcAdapter?.let { adapter ->
            val intent = Intent(this, javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_MUTABLE
            )

            val filters = arrayOf(
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            )

            adapter.enableForegroundDispatch(this, pendingIntent, filters, null)
        }
    }

    private fun disableNfcForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }
    
    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null || nfcAdapter == null) return
        
        val action = intent.action
        if (action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                handleNfcTag(tag)
            }
        }
    }
    
    private fun handleNfcTag(tag: Tag) {
        val tagData = nfcHandler.readTag(tag)
        
        if (tagData != null) {
            // Add entry from tag data
            val data = tagData.data.toMutableMap()
            
            // Add URL to data if present
            if (tagData.url != null) {
                data["url"] = tagData.url
            }
            
            repository.addEntry(data, selectedCategory)
            
            Toast.makeText(this, R.string.tag_read_success, Toast.LENGTH_SHORT).show()
            
            // Vibrate or play sound
            vibrateDevice()
        } else {
            Toast.makeText(this, R.string.tag_read_failed, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun toggleContinuousScanning() {
        isContinuousScanning = !isContinuousScanning
        
        if (isContinuousScanning) {
            binding.continuousScanButton.text = getString(R.string.stop_scanning)
            binding.continuousScanButton.setIconResource(R.drawable.ic_stop)
            Toast.makeText(this, R.string.hold_tag_near_device, Toast.LENGTH_SHORT).show()
        } else {
            binding.continuousScanButton.text = getString(R.string.start_continuous_scan)
            binding.continuousScanButton.setIconResource(R.drawable.ic_nfc)
        }
    }
    
    // ========== Manual Entry ==========
    
    private fun addManualEntry() {
        val input = binding.manualEntryInput.text?.toString()?.trim()
        
        if (input.isNullOrEmpty()) {
            Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_SHORT).show()
            return
        }
        
        val data = mapOf("name" to input)
        repository.addEntry(data, selectedCategory)
        
        binding.manualEntryInput.text?.clear()
        Toast.makeText(this, getString(R.string.add_entry), Toast.LENGTH_SHORT).show()
    }
    
    // ========== Category Management ==========
    
    private fun showCategoryPicker() {
        val settings = preferencesManager.loadSettings()
        val categories = settings.categories
        val categoryKeys = categories.keys.toList()
        val categoryNames = categoryKeys.map { categories[it] ?: it }.toTypedArray()
        
        val currentIndex = categoryKeys.indexOf(selectedCategory).coerceAtLeast(0)
        
        AlertDialog.Builder(this)
            .setTitle(R.string.select_category)
            .setSingleChoiceItems(categoryNames, currentIndex) { dialog, which ->
                selectedCategory = categoryKeys[which]
                binding.categoryDropdown.text = categoryNames[which]
                repository.loadEntries()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    // ========== Entry Actions ==========
    
    private fun confirmDeleteSelected() {
        val selectedCount = entriesAdapter.currentList.count { it.isSelected }
        
        if (selectedCount == 0) {
            Toast.makeText(this, "No entries selected", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete)
            .setMessage(getString(R.string.confirm_delete_message))
            .setPositiveButton(R.string.yes) { _, _ ->
                repository.deleteSelectedEntries()
                Toast.makeText(this, "Deleted $selectedCount entries", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
    
    // ========== Menu ==========
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        
        // Show/hide tags menu item based on permission
        val canEditTags = preferencesManager.canEditTags()
        menu?.findItem(R.id.action_tags)?.isVisible = canEditTags
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_tags -> {
                if (preferencesManager.canEditTags()) {
                    startActivity(Intent(this, TagManagementActivity::class.java))
                } else {
                    Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    // ========== Utility ==========
    
    private fun vibrateDevice() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? android.os.Vibrator
        vibrator?.vibrate(100)
    }
}
