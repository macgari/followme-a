package com.followme.attendance.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.followme.attendance.FollowMeApplication
import com.followme.attendance.R
import com.followme.attendance.data.api.ApiResult
import com.followme.attendance.databinding.ActivitySettingsBinding
import android.widget.EditText
import com.followme.attendance.ui.help.HelpActivity
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private val preferencesManager by lazy { (application as FollowMeApplication).preferencesManager }
    private val repository by lazy { (application as FollowMeApplication).attendanceRepository }
    
    private lateinit var categoriesAdapter: KeyValueAdapter
    private lateinit var extensionsAdapter: KeyValueAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
        
        setupViews()
        loadSettings()
        setupListeners()
    }
    
    override fun onStart() {
        super.onStart()
        checkAuthenticationStatus()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onPause() {
        super.onPause()
        saveSettings()
    }
    
    private fun setupViews() {
        // Setup categories RecyclerView
        categoriesAdapter = KeyValueAdapter(
            onItemClick = { key, value -> editCategory(key, value) },
            onDeleteClick = { key -> deleteCategory(key) }
        )
        binding.categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = categoriesAdapter
        }
        
        // Setup extensions RecyclerView
        extensionsAdapter = KeyValueAdapter(
            onItemClick = { key, value -> editExtension(key, value) },
            onDeleteClick = { key -> deleteExtension(key) }
        )
        binding.extensionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = extensionsAdapter
        }
    }
    
    private fun loadSettings() {
        val settings = preferencesManager.loadSettings()
        
        binding.apiBaseUrlInput.setText(settings.apiBaseUrl)
        binding.apiKeyInput.setText(settings.apiKey)
        binding.usernameInput.setText(settings.username)
        binding.passwordInput.setText(settings.password)
        binding.authRouteInput.setText(settings.authRoute)
        binding.validateRouteInput.setText(settings.validateRoute)
        binding.mainRouteInput.setText(settings.mainRoute)
        
        categoriesAdapter.submitList(settings.categories.toList())
        extensionsAdapter.submitList(settings.extensions.toList())
    }
    
    private fun saveSettings() {
        val settings = preferencesManager.loadSettings().copy(
            apiBaseUrl = binding.apiBaseUrlInput.text.toString().trim(),
            apiKey = binding.apiKeyInput.text.toString().trim(),
            username = binding.usernameInput.text.toString().trim(),
            password = binding.passwordInput.text.toString(),
            authRoute = binding.authRouteInput.text.toString().trim(),
            validateRoute = binding.validateRouteInput.text.toString().trim(),
            mainRoute = binding.mainRouteInput.text.toString().trim()
        )
        
        preferencesManager.saveSettings(settings)
    }
    
    private fun setupListeners() {
        // Test API button
        binding.testApiButton.setOnClickListener {
            testApi()
        }
        
        // Add category button
        binding.addCategoryButton.setOnClickListener {
            addCategory()
        }
        
        // Add extension button
        binding.addExtensionButton.setOnClickListener {
            addExtension()
        }
        
        // Help card
        binding.helpCard.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
        
        // Logout button
        binding.logoutButton.setOnClickListener {
            confirmLogout()
        }
    }
    
    private fun checkAuthenticationStatus() {
        lifecycleScope.launch {
            val settings = preferencesManager.loadSettings()
            
            // If we have API credentials, try to authenticate
            if (settings.apiBaseUrl.isNotEmpty() && 
                settings.apiKey.isNotEmpty() &&
                settings.username.isNotEmpty() &&
                settings.password.isNotEmpty() &&
                settings.authRoute.isNotEmpty()) {
                
                // Attempt to authenticate with saved credentials
                repository.authenticate(
                    baseUrl = settings.apiBaseUrl,
                    apiKey = settings.apiKey,
                    username = settings.username,
                    password = settings.password,
                    authRoute = settings.authRoute
                )
            }
            
            // Observe authentication status
            repository.isAuthenticated.collect { isAuthenticated ->
                updateAuthStatus(isAuthenticated)
            }
        }
    }
    
    private fun updateAuthStatus(isAuthenticated: Boolean) {
        if (isAuthenticated) {
            binding.authStatusText.text = getString(R.string.authenticated)
            binding.authStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.authenticated))
            binding.authStatusText.setTextColor(ContextCompat.getColor(this, R.color.authenticated))
        } else {
            binding.authStatusText.text = getString(R.string.not_authenticated)
            binding.authStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.not_authenticated))
            binding.authStatusText.setTextColor(ContextCompat.getColor(this, R.color.not_authenticated))
        }
    }
    
    private fun testApi() {
        saveSettings()
        
        val settings = preferencesManager.loadSettings()
        
        if (settings.apiBaseUrl.isEmpty()) {
            Toast.makeText(this, "Please enter API Base URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (settings.apiKey.isEmpty() || settings.username.isEmpty() || settings.password.isEmpty()) {
            Toast.makeText(this, "Please enter API Key, Username, and Password", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show checking status
        binding.authStatusText.text = getString(R.string.checking_status)
        binding.authStatusIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_orange_light))
        binding.authStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
        
        lifecycleScope.launch {
            when (val result = repository.authenticate(
                settings.apiBaseUrl,
                settings.apiKey,
                settings.username,
                settings.password,
                settings.authRoute
            )) {
                is ApiResult.Success -> {
                    preferencesManager.saveAuthToken(result.data)
                    Toast.makeText(this@SettingsActivity, "Authentication successful!", Toast.LENGTH_SHORT).show()
                }
                is ApiResult.Error -> {
                    Toast.makeText(this@SettingsActivity, "Authentication failed: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Handle other cases if any
                }
            }
        }
    }
    
    // ========== Category Management ==========
    
    private fun addCategory() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_key_value, null)
        val keyInput = dialogView.findViewById<EditText>(R.id.keyInput)
        val valueInput = dialogView.findViewById<EditText>(R.id.valueInput)
        
        keyInput.hint = "API Key (e.g., Main, tasbeha)"
        valueInput.hint = "Display Name (e.g., Main, Tasbeha)"
        
        AlertDialog.Builder(this)
            .setTitle("Add Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val key = keyInput.text.toString().trim()
                val value = valueInput.text.toString().trim()
                
                if (key.isEmpty() || value.isEmpty()) {
                    Toast.makeText(this, "Both fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val settings = preferencesManager.loadSettings()
                
                // Check for duplicate (case-insensitive)
                if (settings.categories.keys.any { it.equals(key, ignoreCase = true) }) {
                    Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val updatedCategories = settings.categories.toMutableMap()
                updatedCategories[key] = value
                
                preferencesManager.saveSettings(settings.copy(categories = updatedCategories))
                categoriesAdapter.submitList(updatedCategories.toList())
                
                Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun editCategory(oldKey: String, oldValue: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_key_value, null)
        val keyInput = dialogView.findViewById<EditText>(R.id.keyInput)
        val valueInput = dialogView.findViewById<EditText>(R.id.valueInput)
        
        keyInput.setText(oldKey)
        valueInput.setText(oldValue)
        keyInput.hint = "API Key"
        valueInput.hint = "Display Name"
        
        AlertDialog.Builder(this)
            .setTitle("Edit Category")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newKey = keyInput.text.toString().trim()
                val newValue = valueInput.text.toString().trim()
                
                if (newKey.isEmpty() || newValue.isEmpty()) {
                    Toast.makeText(this, "Both fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // Don't allow changing "Main" key
                if (oldKey == "Main" && newKey != "Main") {
                    Toast.makeText(this, "Cannot change the default category key", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val settings = preferencesManager.loadSettings()
                val updatedCategories = settings.categories.toMutableMap()
                
                // Check for duplicate if key changed
                if (newKey != oldKey && updatedCategories.keys.any { it.equals(newKey, ignoreCase = true) }) {
                    Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // Remove old key if changed
                if (newKey != oldKey) {
                    updatedCategories.remove(oldKey)
                }
                
                updatedCategories[newKey] = newValue
                
                preferencesManager.saveSettings(settings.copy(categories = updatedCategories))
                categoriesAdapter.submitList(updatedCategories.toList())
                
                Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteCategory(key: String) {
        if (key == "Main") {
            Toast.makeText(this, "Cannot delete default category", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete this category?")
            .setPositiveButton("Delete") { _, _ ->
                val settings = preferencesManager.loadSettings()
                val updatedCategories = settings.categories.toMutableMap()
                updatedCategories.remove(key)
                
                preferencesManager.saveSettings(settings.copy(categories = updatedCategories))
                categoriesAdapter.submitList(updatedCategories.toList())
                
                Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    // ========== Extension Management ==========
    
    private fun addExtension() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_key_value, null)
        val keyInput = dialogView.findViewById<EditText>(R.id.keyInput)
        val valueInput = dialogView.findViewById<EditText>(R.id.valueInput)
        
        keyInput.hint = "Header Key"
        valueInput.hint = "Header Value"
        
        AlertDialog.Builder(this)
            .setTitle("Add Extension")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val key = keyInput.text.toString().trim()
                val value = valueInput.text.toString().trim()
                
                if (key.isEmpty()) {
                    Toast.makeText(this, "Key is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val settings = preferencesManager.loadSettings()
                val updatedExtensions = settings.extensions.toMutableMap()
                updatedExtensions[key] = value
                
                preferencesManager.saveSettings(settings.copy(extensions = updatedExtensions))
                extensionsAdapter.submitList(updatedExtensions.toList())
                
                Toast.makeText(this, "Extension added", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun editExtension(oldKey: String, oldValue: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_key_value, null)
        val keyInput = dialogView.findViewById<EditText>(R.id.keyInput)
        val valueInput = dialogView.findViewById<EditText>(R.id.valueInput)
        
        keyInput.setText(oldKey)
        valueInput.setText(oldValue)
        keyInput.isEnabled = false // Don't allow changing key
        
        AlertDialog.Builder(this)
            .setTitle("Edit Extension")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newValue = valueInput.text.toString().trim()
                
                val settings = preferencesManager.loadSettings()
                val updatedExtensions = settings.extensions.toMutableMap()
                updatedExtensions[oldKey] = newValue
                
                preferencesManager.saveSettings(settings.copy(extensions = updatedExtensions))
                extensionsAdapter.submitList(updatedExtensions.toList())
                
                Toast.makeText(this, "Extension updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteExtension(key: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Extension")
            .setMessage("Are you sure you want to delete this extension?")
            .setPositiveButton("Delete") { _, _ ->
                val settings = preferencesManager.loadSettings()
                val updatedExtensions = settings.extensions.toMutableMap()
                updatedExtensions.remove(key)
                
                preferencesManager.saveSettings(settings.copy(extensions = updatedExtensions))
                extensionsAdapter.submitList(updatedExtensions.toList())
                
                Toast.makeText(this, "Extension deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? This will clear your authentication token.")
            .setPositiveButton("Logout") { _, _ ->
                repository.logout()
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

// ========== Adapter for Categories and Extensions ==========

class KeyValueAdapter(
    private val onItemClick: (String, String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<KeyValueAdapter.ViewHolder>() {
    
    private var items: List<Pair<String, String>> = emptyList()
    
    fun submitList(newItems: List<Pair<String, String>>) {
        items = newItems.sortedBy { it.first }
        notifyItemRangeChanged(0, items.size)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_key_value, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (key, value) = items[position]
        holder.bind(key, value, onItemClick, onDeleteClick)
    }
    
    override fun getItemCount() = items.size
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val keyValueText: TextView = itemView.findViewById(R.id.keyValueText)
        
        fun bind(
            key: String,
            value: String,
            onItemClick: (String, String) -> Unit,
            onDeleteClick: (String) -> Unit
        ) {
            keyValueText.text = "$key: $value"
            
            itemView.setOnClickListener {
                onItemClick(key, value)
            }
            
            itemView.setOnLongClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Delete")
                    .setMessage("Delete this item?")
                    .setPositiveButton("Delete") { _, _ ->
                        onDeleteClick(key)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
        }
    }
}
