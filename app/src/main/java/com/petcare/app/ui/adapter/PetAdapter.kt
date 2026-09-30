package com.petcare.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.petcare.app.R
import com.petcare.app.data.model.PetEntity
import com.petcare.app.databinding.ItemPetAvatarBinding

class PetAdapter(
    private val onPetClick: (PetEntity) -> Unit,
    private val onPetLongClick: (PetEntity) -> Unit
) : ListAdapter<PetEntity, PetAdapter.PetViewHolder>(PetDiffCallback()) {

    private var selectedPetId: Long? = null

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
            binding.tvPetName.text = pet.name
            
            com.petcare.app.util.PetImageUtils.loadPetAvatar(binding.ivPetImage, pet.imageUri, pet.species)

            val isSelected = pet.id == selectedPetId
            binding.cardPetAvatar.strokeWidth = if (isSelected) 3.toPx() else 0
            binding.cardPetAvatar.scaleX = if (isSelected) 1.05f else 1.0f
            binding.cardPetAvatar.scaleY = if (isSelected) 1.05f else 1.0f

            binding.root.setOnClickListener { onPetClick(pet) }
            binding.root.setOnLongClickListener { 
                onPetLongClick(pet)
                true
            }
        }

        private fun Int.toPx(): Int = (this * binding.root.resources.displayMetrics.density).toInt()
    }

    class PetDiffCallback : DiffUtil.ItemCallback<PetEntity>() {
        override fun areItemsTheSame(oldItem: PetEntity, newItem: PetEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PetEntity, newItem: PetEntity) = oldItem == newItem
    }
}