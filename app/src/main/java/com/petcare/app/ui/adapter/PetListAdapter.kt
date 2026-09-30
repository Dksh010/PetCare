package com.petcare.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.petcare.app.data.model.PetEntity
import com.petcare.app.databinding.ItemPetCardBinding
import com.petcare.app.util.PetImageUtils

class PetListAdapter(
    private val onPetClick: (PetEntity) -> Unit,
    private val onDeleteClick: (PetEntity) -> Unit
) : ListAdapter<PetEntity, PetListAdapter.PetCardViewHolder>(PetCardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetCardViewHolder {
        val binding = ItemPetCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PetCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PetCardViewHolder(private val binding: ItemPetCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pet: PetEntity) {
            binding.tvPetCardName.text = pet.name
            binding.tvPetCardBreed.text = "${pet.species} · ${pet.breed.ifEmpty { "Mixed" }}"
            
            val ageText = if (pet.age > 0) "${pet.age} yrs" else "Young"
            val weightText = if (pet.weight.isNotEmpty()) pet.weight else "Weight not set"
            binding.tvPetCardAgeWeight.text = "$ageText · $weightText"

            PetImageUtils.loadPetAvatar(binding.ivPetCardAvatar, pet.imageUri, pet.species)

            binding.root.setOnClickListener { onPetClick(pet) }
            binding.btnViewPetDetails.setOnClickListener { onPetClick(pet) }
            binding.btnDeletePetCard.setOnClickListener { onDeleteClick(pet) }
        }
    }

    class PetCardDiffCallback : DiffUtil.ItemCallback<PetEntity>() {
        override fun areItemsTheSame(oldItem: PetEntity, newItem: PetEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PetEntity, newItem: PetEntity) = oldItem == newItem
    }
}