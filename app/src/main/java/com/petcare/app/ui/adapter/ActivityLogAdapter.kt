package com.petcare.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.petcare.app.data.model.ActivityLogEntity
import com.petcare.app.databinding.ItemActivityLogBinding
import java.text.SimpleDateFormat
import java.util.*

class ActivityLogAdapter : ListAdapter<ActivityLogEntity, ActivityLogAdapter.LogViewHolder>(LogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemActivityLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(private val binding: ItemActivityLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(log: ActivityLogEntity) {
            binding.tvActivityType.text = log.type
            binding.tvActivityNotes.text = log.notes
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            binding.tvActivityTime.text = sdf.format(Date(log.timestamp))
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<ActivityLogEntity>() {
        override fun areItemsTheSame(oldItem: ActivityLogEntity, newItem: ActivityLogEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ActivityLogEntity, newItem: ActivityLogEntity) = oldItem == newItem
    }
}