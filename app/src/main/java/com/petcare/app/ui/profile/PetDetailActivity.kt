package com.petcare.app.ui.profile

import android.content.res.ColorStateList
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.petcare.app.R
import com.petcare.app.data.model.PetEntity
import com.petcare.app.databinding.ActivityPetDetailBinding
import com.petcare.app.ui.dashboard.PetViewModel
import com.petcare.app.ui.dashboard.PetViewModelFactory
import com.petcare.app.util.Constants
import com.petcare.app.util.PetImageUtils
import com.petcare.app.util.TimeUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class PetDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetDetailBinding
    private var petId: Long = -1L
    private var currentPet: PetEntity? = null
    private var selectedImageUri: String? = null

    private val viewModel: PetViewModel by viewModels { PetViewModelFactory.from(this) }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { PetImageUtils.copyToAppStorage(this, it)?.let(::setPhoto) }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { PetImageUtils.saveBitmapToAppStorage(this, it)?.let(::setPhoto) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        petId = intent.getLongExtra(Constants.EXTRA_PET_ID, -1L)
        if (petId == -1L) {
            finish()
            return
        }

        setupToolbar()
        loadPetData()
        observeVaccinations()
        setupClickListeners()
    }

    private fun setPhoto(uriString: String) {
        selectedImageUri = uriString
        PetImageUtils.loadPetAvatar(binding.ivPetDetailImage, uriString, currentPet?.species)
    }

    private fun setupToolbar() {
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
        binding.tvProfileAge.text = if (pet.age > 0) "${pet.age} yrs" else "—"
        binding.tvProfileWeight.text = pet.weight.ifEmpty { "—" }
        binding.tvProfileGender.text = pet.gender.ifEmpty { "—" }
        binding.tvProfileSize.text = pet.bodySize.ifEmpty { "—" }

        binding.tvSterilized.text = if (pet.sterilized) "Sterilized" else "Not sterilized"
        val health = listOf(
            "Allergies" to pet.allergies,
            "Medications" to pet.currentMedications,
            "Conditions" to pet.medicalConditions,
            "Diet" to pet.dietaryPrefs,
            "Avoid" to pet.dietaryRestrictions
        ).filter { it.second.isNotBlank() }
        binding.tvHealthDetails.text = if (health.isEmpty()) {
            "No health notes yet."
        } else {
            health.joinToString("\n") { (label, value) -> "$label: $value" }
        }

        val vet = listOf(pet.veterinarianName, pet.veterinarianClinic).filter { it.isNotBlank() }
        binding.tvVetInfo.text = if (vet.isEmpty()) getString(R.string.no_vet) else vet.joinToString(" · ")
        binding.btnCallVet.visibility = if (pet.veterinarianPhone.isNotBlank()) View.VISIBLE else View.GONE
        binding.btnCallVet.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(pet.veterinarianPhone)}")))
        }

        binding.etPetName.setText(pet.name)
        binding.etPetBreed.setText(pet.breed)
        binding.etDietaryPrefs.setText(pet.dietaryPrefs)

        PetImageUtils.loadPetAvatar(binding.ivPetDetailImage, pet.imageUri, pet.species, sizeDp = 240)
    }

    /** Vaccination summary card: count, next due date and an overall status badge. */
    private fun observeVaccinations() {
        viewModel.selectPet(petId)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vaccinationsForSelectedPet.collect { list ->
                    val nextDue = list.minOfOrNull { it.nextDueDate }
                    val now = System.currentTimeMillis()
                    binding.tvVaccineSummary.text = if (nextDue == null) {
                        getString(R.string.no_vaccinations_short)
                    } else {
                        "${list.size} recorded · next due ${TimeUtils.formatDate(nextDue)}"
                    }
                    val (label, fg, bg) = when {
                        nextDue == null -> Triple(R.string.status_pending, R.color.text_secondary, R.color.surface_variant)
                        nextDue < now -> Triple(R.string.status_overdue, R.color.error, R.color.error_container)
                        nextDue - now < TimeUnit.DAYS.toMillis(30) -> Triple(R.string.status_due_soon, R.color.warning, R.color.warning_container)
                        else -> Triple(R.string.status_up_to_date, R.color.success, R.color.success_container)
                    }
                    binding.tvVaccineStatus.setText(label)
                    binding.tvVaccineStatus.setTextColor(getColor(fg))
                    binding.tvVaccineStatus.backgroundTintList = ColorStateList.valueOf(getColor(bg))
                }
            }
        }
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

        binding.btnSavePet.setOnClickListener { savePet() }
        binding.btnDeletePet.setOnClickListener { showDeleteConfirmation() }
        binding.btnDownloadReport.setOnClickListener { sharePdfReport() }
    }

    private fun savePet() {
        val pet = currentPet ?: return
        val name = binding.etPetName.text.toString().trim()
        if (name.isEmpty()) {
            binding.etPetName.error = "Name is required"
            return
        }
        val updatedPet = pet.copy(
            name = name,
            breed = binding.etPetBreed.text.toString().trim(),
            dietaryPrefs = binding.etDietaryPrefs.text.toString().trim(),
            imageUri = selectedImageUri ?: pet.imageUri,
            updatedAt = System.currentTimeMillis()
        )
        viewModel.updatePet(updatedPet)
        Toast.makeText(this, "Companion profile updated", Toast.LENGTH_SHORT).show()
        finish()
    }

    /** Builds a one-page PDF profile and opens the share sheet (save to Files, email, etc.). */
    private fun sharePdfReport() {
        val pet = currentPet ?: return
        try {
            val doc = PdfDocument()
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val canvas = page.canvas
            val paint = Paint().apply { isAntiAlias = true }

            paint.textSize = 22f
            paint.isFakeBoldText = true
            canvas.drawText("PetCare Profile: ${pet.name}", 40f, 60f, paint)

            paint.textSize = 13f
            paint.isFakeBoldText = false
            val lines = listOf(
                "Species" to pet.species,
                "Breed" to pet.breed,
                "Age" to "${pet.age} yrs",
                "Weight" to pet.weight,
                "Gender" to pet.gender,
                "Diet" to pet.dietaryPrefs,
                "Allergies" to pet.allergies,
                "Medications" to pet.currentMedications,
                "Veterinarian" to pet.veterinarianName
            ).filter { it.second.isNotBlank() }
            lines.forEachIndexed { i, (label, value) ->
                canvas.drawText("$label: $value", 40f, 100f + i * 24f, paint)
            }

            doc.finishPage(page)
            val dir = File(cacheDir, "reports").apply { mkdirs() }
            val file = File(dir, "${pet.name.replace(Regex("[^A-Za-z0-9]"), "_")}_Report.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()

            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(share, "Share ${pet.name}'s report"))
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
