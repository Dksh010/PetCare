package com.petcare.app.ui.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.petcare.app.data.model.TaskEntity
import com.petcare.app.databinding.ItemTaskRowBinding
import com.petcare.app.ui.dashboard.ChecklistItem
import com.petcare.app.util.TimeUtils

/**
 * Today's checklist: tick to mark a routine step done, tap to edit, bin to delete.
 */
class TaskAdapter(
    private val onTaskStatusChange: (TaskEntity, Boolean) -> Unit,
    private val onTaskClick: (TaskEntity) -> Unit,
    private val onDeleteClick: (TaskEntity) -> Unit
) : ListAdapter<ChecklistItem, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChecklistItem) {
            val task = item.task
            binding.tvTaskTitle.text = task.title
            binding.tvTaskTime.text = TimeUtils.formatTime(task.time)
            binding.tvTaskMeta.text = "${task.category} · ${item.routineName}"

            binding.cbTaskStatus.setOnCheckedChangeListener(null)
            binding.cbTaskStatus.isChecked = item.isDone
            binding.cbTaskStatus.setOnCheckedChangeListener { _, isChecked -> onTaskStatusChange(task, isChecked) }

            binding.tvTaskTitle.alpha = if (item.isDone) 0.55f else 1f
            binding.tvTaskTitle.paintFlags = if (item.isDone) {
                binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            binding.root.setOnClickListener { onTaskClick(task) }
            binding.btnDeleteTask.setOnClickListener { onDeleteClick(task) }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<ChecklistItem>() {
        override fun areItemsTheSame(oldItem: ChecklistItem, newItem: ChecklistItem) = oldItem.task.id == newItem.task.id
        override fun areContentsTheSame(oldItem: ChecklistItem, newItem: ChecklistItem) = oldItem == newItem
    }
}
