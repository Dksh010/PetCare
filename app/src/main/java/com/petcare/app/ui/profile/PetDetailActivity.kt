package com.petcare.app.ui.profile

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.data.local.AppDatabase
import com.petcare.app.data.model.PetEntity
import com.petcare.app.data.repository.PetRepository
import com.petcare.app.ui.dashboard.PetViewModel
import com.petcare.app.ui.dashboard.PetViewModelFactory
import com.petcare.app.util.Constants
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class PetDetailActivity : AppCompatActivity() {

    private lateinit var binding: com.petcare.app.databinding.ActivityPetDetailBinding
    private var petId: Long = -1L
    private var currentPet: PetEntity? = null
    private var selectedImageUri: Uri? = null

    private val viewModel: PetViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PetRepository(
            database.petDao(),
            database.taskDao(),
            database.userDao(),
            database.reminderDao(),
            database.appointmentDao(),
            database.activityLogDao(),
            database.vaccinationDao()
        )
        PetViewModelFactory(repository)
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivPetDetailImage.setImageURI(it)
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            binding.ivPetDetailImage.setImageBitmap(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = com.petcare.app.databinding.ActivityPetDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        petId = intent.getLongExtra(Constants.EXTRA_PET_ID, -1L)
        if (petId == -1L) {
            finish()
            return
        }

        setupToolbar()
        loadPetData()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbarDetail.setNavigationOnClickListener { finish() }
    }

    private fun loadPetData() {
        lifecycleScope.launch {
            val pet = viewModel.getPetById(petId)
            if (pet != null) {
                currentPet = pet
                bindPetData(pet)
            } else {
                finish()
            }
        }
    }

    private fun bindPetData(pet: PetEntity) {
        binding.tvPetProfileName.text = pet.name
        binding.tvPetProfileBreed.text = "${pet.species} · ${pet.breed.ifEmpty { "Mixed" }}"
        binding.tvProfileWeight.text = if (pet.weight.isNotEmpty()) pet.weight else "--"
        binding.tvProfileAge.text = "${pet.age} yrs"

        binding.etPetName.setText(pet.name)
        binding.etPetBreed.setText(pet.breed)
        binding.etDietaryPrefs.setText(pet.dietaryPrefs)
        
        com.petcare.app.util.PetImageUtils.loadPetAvatar(binding.ivPetDetailImage, pet.imageUri, pet.species)
    }

    private fun setupClickListeners() {
        binding.btnChangeImage.setOnClickListener {
            val options = arrayOf("Take Photo", "Choose from Gallery")
            MaterialAlertDialogBuilder(this)
                .setTitle("Update Photo")
                .setItems(options) { _, which ->
                    if (which == 0) takePhoto.launch(null) else pickImage.launch("image/*")
                }
                .show()
        }

        binding.btnSavePet.setOnClickListener {
            savePet()
        }

        binding.btnDeletePet.setOnClickListener {
            showDeleteConfirmation()
        }

        binding.btnDownloadReport.setOnClickListener {
            generatePdfReport()
        }
    }

    private fun savePet() {
        val pet = currentPet ?: return
        val updatedPet = pet.copy(
            name = binding.etPetName.text.toString().trim(),
            breed = binding.etPetBreed.text.toString().trim(),
            dietaryPrefs = binding.etDietaryPrefs.text.toString().trim(),
            imageUri = selectedImageUri?.toString() ?: pet.imageUri
        )
        viewModel.updatePet(updatedPet)
        Toast.makeText(this, "Companion profile updated", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun generatePdfReport() {
        val pet = currentPet ?: return
        try {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            paint.textSize = 16f
            paint.isFakeBoldText = true
            canvas.drawText("PetCare: ${pet.name}", 20f, 40f, paint)

            paint.textSize = 12f
            paint.isFakeBoldText = false
            canvas.drawText("Species: ${pet.species}", 20f, 80f, paint)
            canvas.drawText("Breed: ${pet.breed}", 20f, 105f, paint)
            canvas.drawText("Age: ${pet.age}", 20f, 130f, paint)
            canvas.drawText("Weight: ${pet.weight}", 20f, 155f, paint)

            doc.finishPage(page)
            val file = File(getExternalFilesDir(null), "${pet.name}_Report.pdf")
            doc.writeTo(FileOutputStream(file))
            doc.close()

            Toast.makeText(this, "Report saved to Documents", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to generate report", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Companion?")
            .setMessage("This will delete all care history for this pet. This action cannot be undone.")
            .setPositiveButton("Remove") { _, _ ->
                currentPet?.let { 
                    viewModel.deletePet(it)
                    finish()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}