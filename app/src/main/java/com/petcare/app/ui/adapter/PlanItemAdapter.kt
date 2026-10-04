package com.petcare.app.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.petcare.app.R
import com.petcare.app.data.model.AppointmentEntity
import com.petcare.app.data.model.ReminderEntity
import com.petcare.app.data.model.VaccinationEntity
import com.petcare.app.databinding.ItemPlanItemBinding
import com.petcare.app.util.TimeUtils
import java.util.concurrent.TimeUnit

sealed class PlanDisplayItem {
    data class Reminder(val entity: ReminderEntity) : PlanDisplayItem()
    data class Appointment(val entity: AppointmentEntity, val placeName: String?) : PlanDisplayItem()
    data class Vaccination(val entity: VaccinationEntity) : PlanDisplayItem()
}

/**
 * Appointments, reminders and vaccinations: tap to edit, tick to complete, bin to delete.
 * Each card shows a colored status badge.
 */
class PlanItemAdapter(
    private val onItemClick: (PlanDisplayItem) -> Unit,
    private val onCompletedChange: (PlanDisplayItem, Boolean) -> Unit,
    private val onDeleteClick: (PlanDisplayItem) -> Unit
) : ListAdapter<PlanDisplayItem, PlanItemAdapter.PlanViewHolder>(PlanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemPlanItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /** Visual tone of a card: icon tile + badge colors. */
    private enum class Tone(@ColorRes val fg: Int, @ColorRes val bg: Int) {
        INFO(R.color.info, R.color.info_container),
        SUCCESS(R.color.success, R.color.success_container),
        WARNING(R.color.warning, R.color.warning_container),
        ERROR(R.color.error, R.color.error_container)
    }

    inner class PlanViewHolder(private val binding: ItemPlanItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlanDisplayItem) {
            val now = System.currentTimeMillis()
            val done: Boolean
            when (item) {
                is PlanDisplayItem.Appointment -> {
                    val a = item.entity
                    done = a.isCompleted
                    binding.tvPlanTitle.text = a.title
                    binding.tvPlanMeta.text = TimeUtils.formatDateTime(a.dateMillis)
                    showDetail(item.placeName ?: a.clinic, R.drawable.ic_location_sm)
                    setIcon(R.drawable.ic_appointment, Tone.INFO)
                    when {
                        a.isCompleted -> setStatus(R.string.status_completed, Tone.SUCCESS)
                        a.dateMillis < now -> setStatus(R.string.status_missed, Tone.WARNING)
                        else -> setStatus(R.string.status_upcoming, Tone.INFO)
                    }
                }
                is PlanDisplayItem.Reminder -> {
                    val r = item.entity
                    done = r.isCompleted
                    binding.tvPlanTitle.text = r.title
                    binding.tvPlanMeta.text = "${TimeUtils.formatTime(r.time)} · ${r.type}"
                    showDetail(r.notes.ifBlank { null }, null)
                    setIcon(R.drawable.ic_reminder, Tone.WARNING)
                    if (r.isCompleted) setStatus(R.string.status_done, Tone.SUCCESS)
                    else setStatus(R.string.status_pending, Tone.WARNING)
                }
                is PlanDisplayItem.Vaccination -> {
                    val v = item.entity
                    done = false
                    binding.tvPlanTitle.text = v.vaccineName
                    binding.tvPlanMeta.text = binding.root.context.getString(R.string.next_due_format, TimeUtils.formatDate(v.nextDueDate))
                    showDetail(binding.root.context.getString(R.string.given_format, TimeUtils.formatDate(v.dateGiven)), null)
                    setIcon(R.drawable.ic_vaccination, Tone.SUCCESS)
                    when {
                        v.nextDueDate < now -> setStatus(R.string.status_overdue, Tone.ERROR)
                        v.nextDueDate - now < TimeUnit.DAYS.toMillis(30) -> setStatus(R.string.status_due_soon, Tone.WARNING)
                        else -> setStatus(R.string.status_up_to_date, Tone.SUCCESS)
                    }
                }
            }

            // Vaccinations are records, not to-dos, so they have no checkbox.
            binding.cbPlanDone.visibility = if (item is PlanDisplayItem.Vaccination) View.GONE else View.VISIBLE
            binding.cbPlanDone.setOnCheckedChangeListener(null)
            binding.cbPlanDone.isChecked = done
            binding.cbPlanDone.setOnCheckedChangeListener { _, isChecked -> onCompletedChange(item, isChecked) }

            binding.tvPlanTitle.alpha = if (done) 0.55f else 1f
            binding.tvPlanTitle.paintFlags = if (done) {
                binding.tvPlanTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvPlanTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            binding.btnPlanDelete.setOnClickListener { onDeleteClick(item) }
            binding.root.setOnClickListener { onItemClick(item) }
        }

        private fun showDetail(text: String?, @DrawableRes icon: Int?) {
            binding.tvPlanDetail.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.tvPlanDetail.text = text
            binding.tvPlanDetail.setCompoundDrawablesRelativeWithIntrinsicBounds(icon ?: 0, 0, 0, 0)
        }

        private fun setIcon(@DrawableRes icon: Int, tone: Tone) {
            val context = binding.root.context
            binding.ivPlanIcon.setImageResource(icon)
            binding.ivPlanIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, tone.fg))
            binding.ivPlanIcon.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, tone.bg))
        }

        private fun setStatus(@StringRes label: Int, tone: Tone) {
            val context = binding.root.context
            binding.tvPlanStatus.setText(label)
            binding.tvPlanStatus.setTextColor(ContextCompat.getColor(context, tone.fg))
            binding.tvPlanStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, tone.bg))
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
