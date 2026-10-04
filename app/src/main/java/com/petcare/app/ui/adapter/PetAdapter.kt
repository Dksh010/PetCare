package com.petcare.app.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.petcare.app.R
import com.petcare.app.data.model.PetEntity
import com.petcare.app.databinding.ItemPetAvatarBinding
import com.petcare.app.util.PetImageUtils

/** Horizontal pet switcher; the selected pet gets a brand-colored ring and label. */
class PetAdapter(
    private val onPetClick: (PetEntity) -> Unit,
    private val onPetLongClick: (PetEntity) -> Unit
) : ListAdapter<PetEntity, PetAdapter.PetViewHolder>(PetDiffCallback()) {

    private var selectedPetId: Long? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedPet(id: Long?) {
        selectedPetId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding = ItemPetAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PetViewHolder(private val binding: ItemPetAvatarBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pet: PetEntity) {
            val context = binding.root.context
            binding.tvPetName.text = pet.name
            PetImageUtils.loadPetAvatar(binding.ivPetImage, pet.imageUri, pet.species)

            val isSelected = pet.id == selectedPetId
            binding.cardPetAvatar.strokeWidth = if (isSelected) context.resources.getDimensionPixelSize(R.dimen.stroke_focus) * 2 else 0
            binding.tvPetName.setTextColor(ContextCompat.getColor(context, if (isSelected) R.color.primary else R.color.text_secondary))
            binding.root.isSelected = isSelected

            binding.root.setOnClickListener { onPetClick(pet) }
            binding.root.setOnLongClickListener {
                onPetLongClick(pet)
                true
            }
        }
    }

    class PetDiffCallback : DiffUtil.ItemCallback<PetEntity>() {
        override fun areItemsTheSame(oldItem: PetEntity, newItem: PetEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PetEntity, newItem: PetEntity) = oldItem == newItem
    }
}
