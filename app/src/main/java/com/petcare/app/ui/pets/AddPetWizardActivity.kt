package com.petcare.app.ui.pets

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.petcare.app.data.local.AppDatabase
import com.petcare.app.data.model.PetEntity
import com.petcare.app.data.repository.PetRepository
import com.petcare.app.databinding.ActivityAddPetWizardBinding
import com.petcare.app.ui.dashboard.PetViewModel
import com.petcare.app.ui.dashboard.PetViewModelFactory
import com.petcare.app.util.PetImageUtils
import com.petcare.app.util.SessionManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddPetWizardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPetWizardBinding
    private lateinit var sessionManager: SessionManager
    private var currentStep = 1
    private var selectedImageUri: String? = null

    private val viewModel: PetViewModel by viewModels { PetViewModelFactory.from(this) }

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { PetImageUtils.copyToAppStorage(this, it)?.let(::setPhoto) }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { PetImageUtils.saveBitmapToAppStorage(this, it)?.let(::setPhoto) }
    }

    private fun setPhoto(uriString: String) {
        selectedImageUri = uriString
        PetImageUtils.loadPetAvatar(binding.ivWizardPetPhoto, uriString, getSelectedSpecies())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPetWizardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setupClickListeners()
        updateStepUI()
    }

    private fun setupClickListeners() {
        binding.btnWizardChoosePhoto.setOnClickListener {
            val options = arrayOf("Take Photo", "Choose from Gallery")
            MaterialAlertDialogBuilder(this)
                .setTitle("Select Photo")
                .setItems(options) { _, which ->
                    if (which == 0) takePhoto.launch(null) else pickPhoto.launch("image/*")
                }
                .show()
        }

        binding.btnWizardNext.setOnClickListener {
            if (currentStep < 5) {
                if (validateCurrentStep()) {
                    currentStep++
                    updateStepUI()
                }
            } else {
                createPet()
            }
        }

        binding.btnWizardPrev.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepUI()
            } else {
                finish()
            }
        }
    }

    private fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            2 -> {
                if (binding.etWizardName.text.isNullOrBlank()) {
                    Toast.makeText(this, "Please enter a pet name", Toast.LENGTH_SHORT).show()
                    false
                } else true
            }
            else -> true
        }
    }

    private fun updateStepUI() {
        binding.tvWizardStepIndicator.text = "Step $currentStep of 5"
        binding.pbWizardProgress.setProgressCompat(currentStep, true)
        binding.btnWizardPrev.text = if (currentStep == 1) "Cancel" else "Back"

        binding.layoutStepType.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        binding.layoutStepBasic.visibility = if (currentStep == 2) View.VISIBLE else View.GONE
        binding.layoutStepPhysical.visibility = if (currentStep == 3) View.VISIBLE else View.GONE
        binding.layoutStepHealth.visibility = if (currentStep == 4) View.VISIBLE else View.GONE
        binding.layoutStepReview.visibility = if (currentStep == 5) View.VISIBLE else View.GONE

        when (currentStep) {
            1 -> {
                binding.tvWizardTitle.text = "Pet type"
                binding.btnWizardNext.text = "Continue"
            }
            2 -> {
                binding.tvWizardTitle.text = "Basic details"
                binding.btnWizardNext.text = "Continue"
                if (selectedImageUri == null) {
                    PetImageUtils.loadPetAvatar(binding.ivWizardPetPhoto, null, getSelectedSpecies())
                }
            }
            3 -> {
                binding.tvWizardTitle.text = "Physical info"
                binding.btnWizardNext.text = "Continue"
            }
            4 -> {
                binding.tvWizardTitle.text = "Health and vet"
                binding.btnWizardNext.text = "Continue"
            }
            5 -> {
                binding.tvWizardTitle.text = "Review profile"
                binding.btnWizardNext.text = "Create profile"
                updateReviewSummary()
            }
        }
    }

    private fun updateReviewSummary() {
        val name = binding.etWizardName.text.toString().trim()
        val species = getSelectedSpecies()
        val breed = binding.etWizardBreed.text.toString().ifEmpty { "Mixed" }
        val age = binding.etWizardAge.text.toString().ifEmpty { "0" }
        val weight = binding.etWizardWeight.text.toString().ifEmpty { "Not specified" }

        binding.tvReviewSummary.text = "Name: $name\nSpecies: $species\nBreed: $breed\nAge: $age yrs\nWeight: $weight"
    }

    private fun getSelectedSpecies(): String {
        val checkedId = binding.chipGroupSpecies.checkedChipId
        return if (checkedId != View.NO_ID) {
            findViewById<Chip>(checkedId).text.toString().replace(Regex("[^a-zA-Z]"), "").trim()
        } else "Dog"
    }

    private fun createPet() {
        val name = binding.etWizardName.text.toString().trim()
        val species = getSelectedSpecies()
        val breed = binding.etWizardBreed.text.toString().trim()
        val ageStr = binding.etWizardAge.text.toString().trim()
        val ageInt = ageStr.toIntOrNull() ?: 0

        val pet = PetEntity(
            userId = sessionManager.getUserId(),
            name = name,
            species = species,
            breed = breed,
            age = ageInt,
            weight = binding.etWizardWeight.text.toString().trim(),
            gender = binding.etWizardGender.text.toString().trim(),
            furColor = binding.etWizardFurColor.text.toString().trim(),
            sterilized = binding.cbWizardSterilized.isChecked,
            dietaryPrefs = binding.etWizardDiet.text.toString().trim(),
            allergies = binding.etWizardAllergies.text.toString().trim(),
            veterinarianName = binding.etWizardVetInfo.text.toString().trim(),
            imageUri = selectedImageUri,
            ownerContact = sessionManager.getUserEmail() ?: ""
        )
        viewModel.addPet(pet)
        Toast.makeText(this, "Welcome home, $name!", Toast.LENGTH_LONG).show()
        finish()
    }
}