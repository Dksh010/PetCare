package com.petcare.app.ui.adapter

import android.annotation.SuppressLint
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

    private var placeNames: Map<Long, String> = emptyMap()

    @SuppressLint("NotifyDataSetChanged")
    fun setPlaceNames(names: Map<Long, String>) {
        placeNames = names
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemActivityLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LogViewHolder(private val binding: ItemActivityLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(log: ActivityLogEntity) {
            binding.tvActivityType.text = if (log.duration > 0) "${log.type} · ${log.duration} min" else log.type
            val place = log.locationId?.let { placeNames[it] }
            binding.tvActivityNotes.text = if (place != null) "${log.notes}\n📍 $place" else log.notes
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            binding.tvActivityTime.text = sdf.format(Date(log.timestamp))
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<ActivityLogEntity>() {
        override fun areItemsTheSame(oldItem: ActivityLogEntity, newItem: ActivityLogEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ActivityLogEntity, newItem: ActivityLogEntity) = oldItem == newItem
    }
}
