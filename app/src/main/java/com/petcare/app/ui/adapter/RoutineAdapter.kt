package com.petcare.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.petcare.app.data.model.RoutineEntity
import com.petcare.app.databinding.ItemRoutineBinding
import com.petcare.app.ui.dashboard.RoutineSummary
import com.petcare.app.util.TimeUtils

class RoutineAdapter(
    private val onRoutineClick: (RoutineSummary) -> Unit,
    private val onDeleteClick: (RoutineSummary) -> Unit
) : ListAdapter<RoutineSummary, RoutineAdapter.RoutineViewHolder>(RoutineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val binding = ItemRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoutineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RoutineViewHolder(private val binding: ItemRoutineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(summary: RoutineSummary) {
            val routine = summary.routine
            binding.tvRoutineName.text = routine.name
            val stepCount = summary.steps.size
            binding.tvRoutineMeta.text = "${scheduleLabel(routine)} · $stepCount ${if (stepCount == 1) "step" else "steps"}"
            binding.tvRoutineSteps.text = summary.steps.joinToString("\n") {
                "${TimeUtils.formatTime(it.time)}   ${it.title}"
            }
            binding.root.setOnClickListener { onRoutineClick(summary) }
            binding.btnDeleteRoutine.setOnClickListener { onDeleteClick(summary) }
        }
    }

    class RoutineDiffCallback : DiffUtil.ItemCallback<RoutineSummary>() {
        override fun areItemsTheSame(oldItem: RoutineSummary, newItem: RoutineSummary) = oldItem.routine.id == newItem.routine.id
        override fun areContentsTheSame(oldItem: RoutineSummary, newItem: RoutineSummary) = oldItem == newItem
    }

    companion object {
        fun scheduleLabel(routine: RoutineEntity): String =
            if (routine.repeatType == RoutineEntity.REPEAT_DAILY) "Every day"
            else routine.days().sorted().joinToString(", ") { TimeUtils.dayShortNames[it - 1] }
    }
}
