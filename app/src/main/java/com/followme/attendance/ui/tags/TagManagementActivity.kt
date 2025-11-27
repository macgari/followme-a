package com.followme.attendance.ui.tags

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.followme.attendance.R
import com.followme.attendance.databinding.ActivityTagManagementBinding
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

class TagManagementActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTagManagementBinding
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var tagEntriesAdapter: TagEntriesAdapter
    private var tagEntries: MutableList<TagEntry> = mutableListOf()
    private var isWritingMode = false
    private var currentWriteIndex = 0
    private var manualTagData: MutableMap<String, String> = mutableMapOf()
    
    companion object {
        private const val REQUEST_CODE_PICK_CSV = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTagManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.tag_management)
        
        setupNfc()
        setupViews()
        setupListeners()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
    }
    
    override fun onPause() {
        super.onPause()
        disableNfcForegroundDispatch()
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (isWritingMode) {
            handleNfcIntent(intent)
        }
    }
    
    private fun setupNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported. You can still import CSV data, but writing to tags will not be available.", Toast.LENGTH_LONG).show()
            // Don't finish - allow user to import and view CSV data
            return
        }
        
        if (nfcAdapter?.isEnabled == false) {
            Toast.makeText(this, R.string.nfc_disabled, Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupViews() {
        tagEntriesAdapter = TagEntriesAdapter(
            tagEntries,
            onSelectionChanged = { position, isChecked ->
                tagEntries[position].isSelected = isChecked
                updateWriteButton()
                updateSelectAllButton()
            },
            onEditClicked = { entry ->
                showManualTagCreationDialog(entry, mutableMapOf())
            },
            onDeleteClicked = { entry ->
                deleteTag(entry)
            }
        )
        
        binding.tagEntriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@TagManagementActivity)
            adapter = tagEntriesAdapter
        }
    }
    
    private fun setupListeners() {
        binding.importCsvButton.setOnClickListener {
            openCsvFilePicker()
        }
        
        binding.createManualTagButton.setOnClickListener {
            showManualTagCreationDialog()
        }
        
        binding.writeTagsButton.setOnClickListener {
            startWritingTags()
        }
        
        binding.selectAllButton.setOnClickListener {
            toggleSelectAll()
        }
        
        binding.deleteSelectedButton.setOnClickListener {
            deleteSelectedTags()
        }
    }
    
    private fun openCsvFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        try {
            startActivityForResult(
                Intent.createChooser(intent, "Select CSV File"),
                REQUEST_CODE_PICK_CSV
            )
        } catch (e: Exception) {
            Toast.makeText(this, "No file manager found", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_PICK_CSV && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                parseCsvFile(uri)
            }
        }
    }
    
    private fun parseCsvFile(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream, Charset.forName("UTF-8")))
            
            val lines = reader.readLines().filter { it.trim().isNotEmpty() }
            reader.close()
            
            if (lines.size < 2) {
                Toast.makeText(this, "CSV file must have at least 2 rows (header + data)", Toast.LENGTH_LONG).show()
                return
            }
            
            // Parse header
            val headers = parseCsvLine(lines[0])
            
            // Parse data rows
            val entries = mutableListOf<TagEntry>()
            for (i in 1 until lines.size) {
                val values = parseCsvLine(lines[i])
                val data = mutableMapOf<String, String>()
                
                for (j in headers.indices) {
                    if (j < values.size) {
                        data[headers[j]] = values[j]
                    }
                }
                
                if (data.isNotEmpty()) {
                    entries.add(TagEntry(data, false))
                }
            }
            
            if (entries.isEmpty()) {
                Toast.makeText(this, "No valid entries found in CSV", Toast.LENGTH_SHORT).show()
                return
            }
            
            tagEntries.clear()
            tagEntries.addAll(entries)
            tagEntriesAdapter.notifyDataSetChanged()
            
            binding.entriesCountText.text = "${entries.size} entries loaded"
            binding.entriesCountText.visibility = View.VISIBLE
            binding.tagEntriesRecyclerView.visibility = View.VISIBLE
            binding.writeTagsButton.visibility = View.VISIBLE
            
            Toast.makeText(this, "Loaded ${entries.size} entries", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        var currentField = StringBuilder()
        var insideQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> insideQuotes = !insideQuotes
                char == ',' && !insideQuotes -> {
                    fields.add(currentField.toString().trim())
                    currentField = StringBuilder()
                }
                else -> currentField.append(char)
            }
        }
        fields.add(currentField.toString().trim())
        
        return fields
    }
    
    private fun updateWriteButton() {
        val selectedCount = tagEntries.count { it.isSelected }
        binding.writeTagsButton.text = if (selectedCount > 0) {
            "Write $selectedCount Tags"
        } else {
            "Write Tags"
        }
    }
    
    private fun startWritingTags() {
        // Check if NFC is available
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not supported on this device. Cannot write tags.", Toast.LENGTH_LONG).show()
            return
        }
        
        if (nfcAdapter?.isEnabled == false) {
            Toast.makeText(this, "Please enable NFC in your device settings to write tags.", Toast.LENGTH_LONG).show()
            return
        }
        
        val selectedEntries = tagEntries.filter { it.isSelected }
        
        if (selectedEntries.isEmpty()) {
            Toast.makeText(this, "Please select at least one entry", Toast.LENGTH_SHORT).show()
            return
        }
        
        isWritingMode = true
        currentWriteIndex = 0
        
        AlertDialog.Builder(this)
            .setTitle("Write Tags")
            .setMessage("Ready to write ${selectedEntries.size} tags.\n\nHold your phone near an NFC tag to write.")
            .setPositiveButton("Start") { _, _ ->
                showWriteProgress()
            }
            .setNegativeButton("Cancel") { _, _ ->
                isWritingMode = false
            }
            .show()
    }
    
    private fun showWriteProgress() {
        val selectedEntries = tagEntries.filter { it.isSelected }
        
        if (currentWriteIndex >= selectedEntries.size) {
            isWritingMode = false
            AlertDialog.Builder(this)
                .setTitle("Complete")
                .setMessage("All tags have been written successfully!")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        val entry = selectedEntries[currentWriteIndex]
        val displayData = entry.data.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        
        AlertDialog.Builder(this)
            .setTitle("Writing Tag ${currentWriteIndex + 1}/${selectedEntries.size}")
            .setMessage("Hold phone near tag to write:\n\n$displayData")
            .setNegativeButton("Cancel") { _, _ ->
                isWritingMode = false
            }
            .setCancelable(false)
            .show()
    }
    
    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null || !isWritingMode) return
        
        val action = intent.action
        if (action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                writeTagData(tag)
            }
        }
    }
    
    private fun writeTagData(tag: Tag) {
        val selectedEntries = tagEntries.filter { it.isSelected }
        if (currentWriteIndex >= selectedEntries.size) return
        
        val entry = selectedEntries[currentWriteIndex]
        
        try {
            val ndef = Ndef.get(tag)
            
            if (ndef == null) {
                Toast.makeText(this, "Tag is not NDEF formatted", Toast.LENGTH_SHORT).show()
                return
            }
            
            ndef.connect()
            
            if (!ndef.isWritable) {
                Toast.makeText(this, "Tag is not writable", Toast.LENGTH_SHORT).show()
                ndef.close()
                return
            }
            
            // Create NDEF message
            val records = mutableListOf<NdefRecord>()
            
            // Check if there's a URL field
            val url = entry.data["url"]
            val jsonData = entry.data.filterKeys { it != "url" }
            
            // Add URI record if URL exists
            if (!url.isNullOrEmpty()) {
                records.add(NdefRecord.createUri(url))
            }
            
            // Add JSON record
            if (jsonData.isNotEmpty()) {
                val jsonObject = JSONObject(jsonData)
                val jsonBytes = jsonObject.toString().toByteArray(Charset.forName("UTF-8"))
                val mimeRecord = NdefRecord.createMime("application/json", jsonBytes)
                records.add(mimeRecord)
            }
            
            val message = NdefMessage(records.toTypedArray())
            
            // Write to tag
            ndef.writeNdefMessage(message)
            ndef.close()
            
            // Success - move to next tag
            currentWriteIndex++
            
            runOnUiThread {
                Toast.makeText(this, "Tag written successfully!", Toast.LENGTH_SHORT).show()
                
                // Vibrate
                val vibrator = getSystemService(VIBRATOR_SERVICE) as? android.os.Vibrator
                vibrator?.vibrate(100)
                
                // If this was a manual tag, clear the data and remove the temporary entry
                if (manualTagData.isNotEmpty() && currentWriteIndex == 0) {
                    manualTagData.clear()
                    tagEntries.removeAt(0)
                    isWritingMode = false
                    Toast.makeText(this, "Manual tag written! You can create another tag.", Toast.LENGTH_LONG).show()
                } else {
                    // Show next tag or completion
                    showWriteProgress()
                }
            }
            
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Error writing tag: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
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
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            )
            
            adapter.enableForegroundDispatch(this, pendingIntent, filters, null)
        }
    }
    
    private fun disableNfcForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }
    
    private fun showManualTagCreationDialog() {
        showManualTagCreationDialog(null, manualTagData)
    }
    
    private fun showManualTagCreationDialog(existingEntry: TagEntry?, workingData: MutableMap<String, String> = mutableMapOf()) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_tag, null)
        val keyInput = dialogView.findViewById<android.widget.EditText>(R.id.keyInput)
        val valueInput = dialogView.findViewById<android.widget.EditText>(R.id.valueInput)
        
        // Use the working data passed in, or create from existing entry
        val editingData = if (workingData.isEmpty() && existingEntry != null) {
            existingEntry.data.toMutableMap()
        } else if (workingData.isNotEmpty()) {
            workingData
        } else {
            manualTagData
        }
        
        val currentDataText = if (editingData.isEmpty()) {
            "No data added yet"
        } else {
            "Current data:\n" + editingData.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        }
        
        val dialogTitle = if (existingEntry != null) "Edit Tag" else "Create Tag Manually"
        
        AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setMessage(currentDataText)
            .setView(dialogView)
            .setPositiveButton("Add Field") { _, _ ->
                val key = keyInput.text.toString().trim()
                val value = valueInput.text.toString().trim()
                
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    editingData[key] = value
                    Toast.makeText(this, "Added: $key = $value", Toast.LENGTH_SHORT).show()
                    // Show dialog again to add more fields, passing the same working data
                    showManualTagCreationDialog(existingEntry, editingData)
                } else {
                    Toast.makeText(this, "Both key and value are required", Toast.LENGTH_SHORT).show()
                    showManualTagCreationDialog(existingEntry, editingData)
                }
            }
            .setNeutralButton("Save Tag") { _, _ ->
                if (editingData.isEmpty()) {
                    Toast.makeText(this, "Please add at least one field", Toast.LENGTH_SHORT).show()
                } else {
                    saveManualTag(editingData, existingEntry)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                if (editingData.isNotEmpty() && existingEntry == null) {
                    // Ask if user wants to discard (only for new tags)
                    AlertDialog.Builder(this)
                        .setTitle("Discard Tag Data?")
                        .setMessage("You have unsaved tag data. Discard it?")
                        .setPositiveButton("Discard") { _, _ ->
                            manualTagData.clear()
                            dialog.dismiss()
                        }
                        .setNegativeButton("Keep Editing") { _, _ ->
                            showManualTagCreationDialog(existingEntry, editingData)
                        }
                        .show()
                } else {
                    dialog.dismiss()
                }
            }
            .show()
    }
    
    private fun saveManualTag(data: MutableMap<String, String>, existingEntry: TagEntry?) {
        if (existingEntry != null) {
            // Update existing entry
            val index = tagEntries.indexOf(existingEntry)
            if (index >= 0) {
                tagEntries[index] = TagEntry(data.toMap(), existingEntry.isSelected)
                tagEntriesAdapter.notifyItemChanged(index)
                Toast.makeText(this, "Tag updated!", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Add new entry
            val newEntry = TagEntry(data.toMap(), false)
            tagEntries.add(newEntry)
            tagEntriesAdapter.notifyItemInserted(tagEntries.size - 1)
            manualTagData.clear()
            
            // Show the list
            binding.entriesCountText.text = "${tagEntries.size} tag(s) created"
            binding.entriesCountText.visibility = View.VISIBLE
            binding.tagEntriesRecyclerView.visibility = View.VISIBLE
            binding.writeTagsButton.visibility = View.VISIBLE
            binding.selectAllButton.visibility = View.VISIBLE
            binding.deleteSelectedButton.visibility = View.VISIBLE
            
            Toast.makeText(this, "Tag saved! Create more or select tags to write.", Toast.LENGTH_SHORT).show()
        }
        updateWriteButton()
    }
    
    private fun deleteTag(entry: TagEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete Tag?")
            .setMessage("Are you sure you want to delete this tag?")
            .setPositiveButton("Delete") { _, _ ->
                val index = tagEntries.indexOf(entry)
                if (index >= 0) {
                    tagEntries.removeAt(index)
                    tagEntriesAdapter.notifyItemRemoved(index)
                    updateWriteButton()
                    updateEntriesCount()
                    Toast.makeText(this, "Tag deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteSelectedTags() {
        val selectedCount = tagEntries.count { it.isSelected }
        if (selectedCount == 0) {
            Toast.makeText(this, "No tags selected", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Delete Selected Tags?")
            .setMessage("Delete $selectedCount selected tag(s)?")
            .setPositiveButton("Delete") { _, _ ->
                tagEntries.removeAll { it.isSelected }
                tagEntriesAdapter.notifyDataSetChanged()
                updateWriteButton()
                updateEntriesCount()
                Toast.makeText(this, "Deleted $selectedCount tag(s)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun toggleSelectAll() {
        val allSelected = tagEntries.all { it.isSelected }
        tagEntries.forEach { it.isSelected = !allSelected }
        tagEntriesAdapter.notifyDataSetChanged()
        updateWriteButton()
        updateSelectAllButton()
    }
    
    private fun updateSelectAllButton() {
        val allSelected = tagEntries.isNotEmpty() && tagEntries.all { it.isSelected }
        binding.selectAllButton.text = if (allSelected) "Deselect All" else "Select All"
    }
    
    private fun updateEntriesCount() {
        if (tagEntries.isEmpty()) {
            binding.entriesCountText.visibility = View.GONE
            binding.tagEntriesRecyclerView.visibility = View.GONE
            binding.writeTagsButton.visibility = View.GONE
            binding.selectAllButton.visibility = View.GONE
            binding.deleteSelectedButton.visibility = View.GONE
        } else {
            binding.entriesCountText.text = "${tagEntries.size} tag(s) created"
            binding.entriesCountText.visibility = View.VISIBLE
            binding.tagEntriesRecyclerView.visibility = View.VISIBLE
            binding.writeTagsButton.visibility = View.VISIBLE
            binding.selectAllButton.visibility = View.VISIBLE
            binding.deleteSelectedButton.visibility = View.VISIBLE
        }
    }
    
    private fun writeManualTag() {
        // This method is no longer needed - keeping for compatibility
    }
}


// ========== Data Models ==========

data class TagEntry(
    val data: Map<String, String>,
    var isSelected: Boolean = false
)

// ========== Adapter ==========

class TagEntriesAdapter(
    private val entries: List<TagEntry>,
    private val onSelectionChanged: (Int, Boolean) -> Unit,
    private val onEditClicked: ((TagEntry) -> Unit)? = null,
    private val onDeleteClicked: ((TagEntry) -> Unit)? = null
) : RecyclerView.Adapter<TagEntriesAdapter.ViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tag_entry, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(entries[position], position, onSelectionChanged, onEditClicked, onDeleteClicked)
    }
    
    override fun getItemCount() = entries.size
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkbox: CheckBox = itemView.findViewById(R.id.entryCheckbox)
        private val nameText: TextView = itemView.findViewById(R.id.entryNameText)
        private val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        
        fun bind(
            entry: TagEntry, 
            position: Int, 
            onSelectionChanged: (Int, Boolean) -> Unit,
            onEditClicked: ((TagEntry) -> Unit)?,
            onDeleteClicked: ((TagEntry) -> Unit)?
        ) {
            checkbox.isChecked = entry.isSelected
            
            // Display name property only
            val name = entry.data["name"] ?: "Unnamed Tag"
            nameText.text = name
            
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                onSelectionChanged(position, isChecked)
            }
            
            itemView.setOnClickListener {
                checkbox.isChecked = !checkbox.isChecked
            }
            
            editButton.setOnClickListener {
                onEditClicked?.invoke(entry)
            }
            
            deleteButton.setOnClickListener {
                onDeleteClicked?.invoke(entry)
            }
        }
    }
}
