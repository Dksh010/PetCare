package com.petcare.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.petcare.app.R
import com.petcare.app.data.model.TaskEntity
import com.petcare.app.databinding.ItemTaskRowBinding

/**
 * RecyclerView Adapter for the dashboard task list.
 */
class TaskAdapter(
    private val onTaskStatusChange: (TaskEntity, Boolean) -> Unit,
    private val onDeleteClick: (TaskEntity) -> Unit
) : ListAdapter<TaskEntity, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TaskEntity) {
            binding.tvTaskTitle.text = task.title
            binding.tvTaskMeta.text = binding.root.context.getString(
                R.string.task_time_category_format,
                task.dueDate,
                task.category
            )
            
            binding.cbTaskStatus.setOnCheckedChangeListener(null)
            binding.cbTaskStatus.isChecked = task.isCompleted
            
            // Visual feedback for completion
            binding.tvTaskTitle.alpha = if (task.isCompleted) 0.5f else 1.0f
            
            binding.cbTaskStatus.setOnCheckedChangeListener { _, isChecked ->
                onTaskStatusChange(task, isChecked)
            }

            binding.btnDeleteTask.setOnClickListener {
                onDeleteClick(task)
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem == newItem
        }
    }
}