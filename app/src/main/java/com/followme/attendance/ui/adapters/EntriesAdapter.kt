package com.followme.attendance.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.followme.attendance.R
import com.followme.attendance.data.model.EntryStatus
import com.followme.attendance.data.model.ScannedTagEntry
import com.followme.attendance.databinding.ItemEntryBinding
import java.text.SimpleDateFormat
import java.util.*

class EntriesAdapter(
    private val onItemClick: (Int) -> Unit,
    private val onCheckboxClick: (Int) -> Unit
) : ListAdapter<ScannedTagEntry, EntriesAdapter.EntryViewHolder>(EntryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = ItemEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EntryViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }
    
    inner class EntryViewHolder(
        private val binding: ItemEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(entry: ScannedTagEntry, position: Int) {
            // Set name
            binding.entryName.text = entry.name
            
            // Set timestamp (format for display)
            binding.entryTimestamp.text = formatTimestamp(entry.timestamp)
            
            // Set category
            binding.entryCategory.text = entry.category
            
            // Set checkbox
            binding.entryCheckbox.isChecked = entry.isSelected
            binding.entryCheckbox.setOnClickListener {
                onCheckboxClick(position)
            }
            
            // Set status icon
            when (entry.status) {
                EntryStatus.PENDING -> {
                    binding.statusIcon.setImageResource(R.drawable.ic_pending)
                    binding.statusIcon.setColorFilter(
                        ContextCompat.getColor(binding.root.context, R.color.status_pending)
                    )
                    binding.statusIcon.contentDescription = 
                        binding.root.context.getString(R.string.status_pending)
                }
                EntryStatus.SUBMITTED -> {
                    binding.statusIcon.setImageResource(R.drawable.ic_submitted)
                    binding.statusIcon.setColorFilter(
                        ContextCompat.getColor(binding.root.context, R.color.status_submitted)
                    )
                    binding.statusIcon.contentDescription = 
                        binding.root.context.getString(R.string.status_submitted)
                }
                EntryStatus.FAILED -> {
                    binding.statusIcon.setImageResource(R.drawable.ic_failed)
                    binding.statusIcon.setColorFilter(
                        ContextCompat.getColor(binding.root.context, R.color.status_failed)
                    )
                    binding.statusIcon.contentDescription = 
                        binding.root.context.getString(R.string.status_failed)
                }
                EntryStatus.UNMATCHED -> {
                    binding.statusIcon.setImageResource(R.drawable.ic_unmatched)
                    binding.statusIcon.setColorFilter(
                        ContextCompat.getColor(binding.root.context, R.color.status_unmatched)
                    )
                    binding.statusIcon.contentDescription = 
                        binding.root.context.getString(R.string.status_unmatched)
                }
            }
            
            // Set click listener
            binding.root.setOnClickListener {
                onItemClick(position)
            }
        }
        
        private fun formatTimestamp(timestamp: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(timestamp)
                
                val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                timestamp
            }
        }
    }
    
    private class EntryDiffCallback : DiffUtil.ItemCallback<ScannedTagEntry>() {
        override fun areItemsTheSame(oldItem: ScannedTagEntry, newItem: ScannedTagEntry): Boolean {
            return oldItem.timestamp == newItem.timestamp && oldItem.category == newItem.category
        }
        
        override fun areContentsTheSame(oldItem: ScannedTagEntry, newItem: ScannedTagEntry): Boolean {
            return oldItem == newItem
        }
    }
}
