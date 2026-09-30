package com.petcare.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.petcare.app.R
import com.petcare.app.data.model.AppointmentEntity
import com.petcare.app.data.model.ReminderEntity
import com.petcare.app.data.model.VaccinationEntity
import com.petcare.app.databinding.ItemTaskRowBinding

sealed class PlanDisplayItem {
    data class Reminder(val entity: ReminderEntity) : PlanDisplayItem()
    data class Appointment(val entity: AppointmentEntity) : PlanDisplayItem()
    data class Vaccination(val entity: VaccinationEntity) : PlanDisplayItem()
}

class PlanItemAdapter(
    private val onItemClick: (PlanDisplayItem) -> Unit,
    private val onDeleteClick: (PlanDisplayItem) -> Unit
) : ListAdapter<PlanDisplayItem, PlanItemAdapter.PlanViewHolder>(PlanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemTaskRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlanViewHolder(private val binding: ItemTaskRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PlanDisplayItem) {
            when (item) {
                is PlanDisplayItem.Reminder -> {
                    binding.tvTaskTitle.text = item.entity.title
                    binding.tvTaskMeta.text = "${item.entity.type} · ${item.entity.dateTime}"
                    binding.cbTaskStatus.isChecked = item.entity.isCompleted
                }
                is PlanDisplayItem.Appointment -> {
                    binding.tvTaskTitle.text = item.entity.title
                    binding.tvTaskMeta.text = "${item.entity.clinic} · ${item.entity.dateTime}"
                    binding.cbTaskStatus.isChecked = false
                }
                is PlanDisplayItem.Vaccination -> {
                    binding.tvTaskTitle.text = item.entity.vaccineName
                    binding.tvTaskMeta.text = "Due: ${item.entity.nextDueDate}"
                    binding.cbTaskStatus.isChecked = false
                }
            }

            binding.btnDeleteTask.setOnClickListener { onDeleteClick(item) }
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class PlanDiffCallback : DiffUtil.ItemCallback<PlanDisplayItem>() {
        override fun areItemsTheSame(oldItem: PlanDisplayItem, newItem: PlanDisplayItem): Boolean {
            return when {
                oldItem is PlanDisplayItem.Reminder && newItem is PlanDisplayItem.Reminder -> oldItem.entity.id == newItem.entity.id
                oldItem is PlanDisplayItem.Appointment && newItem is PlanDisplayItem.Appointment -> oldItem.entity.id == newItem.entity.id
                oldItem is PlanDisplayItem.Vaccination && newItem is PlanDisplayItem.Vaccination -> oldItem.entity.id == newItem.entity.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: PlanDisplayItem, newItem: PlanDisplayItem) = oldItem == newItem
    }
}