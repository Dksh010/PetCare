package com.petcare.app.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.petcare.app.R
import com.petcare.app.data.local.AppDatabase
import com.petcare.app.data.model.*
import com.petcare.app.data.repository.PetRepository
import com.petcare.app.databinding.*
import com.petcare.app.ui.adapter.*
import com.petcare.app.ui.auth.LoginActivity
import com.petcare.app.ui.delegate.DelegateActivity
import com.petcare.app.ui.map.PetMapActivity
import com.petcare.app.ui.pets.AddPetWizardActivity
import com.petcare.app.ui.profile.PetDetailActivity
import com.petcare.app.util.Constants
import com.petcare.app.util.PetImageUtils
import com.petcare.app.util.SecurityUtils
import com.petcare.app.util.SessionManager
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var petAdapter: PetAdapter
    private lateinit var petListAdapter: PetListAdapter
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var reminderAdapter: PlanItemAdapter
    private lateinit var logAdapter: ActivityLogAdapter
    private lateinit var sessionManager: SessionManager

    private var isRadialMenuOpen = false

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

    private val pickUserImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val uriString = it.toString()
            sessionManager.saveProfileImageUri(uriString)
            PetImageUtils.loadUserAvatar(binding.ivProfileAvatar, uriString)
            PetImageUtils.loadUserAvatar(binding.ivUserHeaderAvatar, uriString)
            val userId = sessionManager.getUserId()
            lifecycleScope.launch {
                val user = viewModel.getUserById(userId)
                user?.let { u -> viewModel.updateUser(u.copy(profileImageUri = uriString)) }
            }
            Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show()
        }
    }

    private val takeUserPhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            binding.ivProfileAvatar.setImageBitmap(it)
            binding.ivUserHeaderAvatar.setImageBitmap(it)
            Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        viewModel.setUserId(sessionManager.getUserId())

        setupUI()
        setupAdapters()
        observeState()
        setupClickListeners()
        setupRadialMenu()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        val profileUri = sessionManager.getProfileImageUri()
        PetImageUtils.loadUserAvatar(binding.ivProfileAvatar, profileUri)
        PetImageUtils.loadUserAvatar(binding.ivUserHeaderAvatar, profileUri)
    }

    private fun setupUI() {
        val userEmail = sessionManager.getUserEmail() ?: "User"
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingPrefix = when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }

        lifecycleScope.launch {
            val user = viewModel.getUserById(sessionManager.getUserId())
            val name = user?.name?.ifEmpty { null } ?: userEmail.substringBefore("@")
            binding.tvGreeting.text = "$greetingPrefix, $name"
            binding.tvProfileName.text = name
            binding.tvProfileEmail.text = user?.email ?: userEmail

            user?.let {
                val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                binding.tvProfileMemberSince.text = "Member since ${sdf.format(Date(it.createdAt))}"
            }
        }

        PetImageUtils.loadUserAvatar(binding.ivProfileAvatar, sessionManager.getProfileImageUri())
        PetImageUtils.loadUserAvatar(binding.ivUserHeaderAvatar, sessionManager.getProfileImageUri())
    }

    private fun setupAdapters() {
        petAdapter = PetAdapter(
            onPetClick = { selectedPet ->
                viewModel.selectPet(selectedPet.id)
            },
            onPetLongClick = { pet ->
                openPetDetail(pet.id)
            }
        )
        binding.rvPets.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = petAdapter
        }

        petListAdapter = PetListAdapter(
            onPetClick = { pet -> openPetDetail(pet.id) },
            onDeleteClick = { pet -> confirmDeletePet(pet) }
        )
        binding.rvAllPetsList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = petListAdapter
        }

        taskAdapter = TaskAdapter(
            onTaskStatusChange = { task, isChecked ->
                viewModel.toggleTaskCompletion(task, isChecked)
            },
            onDeleteClick = { task ->
                viewModel.deleteTask(task)
            }
        )
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }

        reminderAdapter = PlanItemAdapter(
            onItemClick = {},
            onDeleteClick = { item ->
                if (item is PlanDisplayItem.Reminder) {
                    viewModel.deleteReminder(item.entity)
                }
            }
        )
        binding.rvPlanReminders.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = reminderAdapter
        }

        logAdapter = ActivityLogAdapter()
        binding.rvRecentActivity.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = logAdapter
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allPets.collect { pets ->
                        petAdapter.submitList(pets)
                        petListAdapter.submitList(pets)

                        binding.layoutEmptyPets.visibility = if (pets.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvAllPetsList.visibility = if (pets.isNotEmpty()) View.VISIBLE else View.GONE

                        if (pets.isNotEmpty() && viewModel.selectedPetId.value == null) {
                            viewModel.selectPet(pets.first().id)
                        } else if (pets.isEmpty()) {
                            binding.tvActivePetName.text = "No Pet"
                            binding.tvActivePetBreed.text = "Add a companion to start"
                            binding.tvPetWeight.text = "--"
                        }
                    }
                }

                launch {
                    viewModel.selectedPetId.collect { petId ->
                        petAdapter.setSelectedPet(petId)
                        if (petId != null) {
                            val pet = viewModel.getPetById(petId)
                            pet?.let {
                                binding.tvActivePetName.text = it.name
                                binding.tvActivePetBreed.text = "${it.species} · ${it.breed.ifEmpty { "Mixed" }}"
                                binding.tvPetWeight.text = if (it.weight.isNotEmpty()) it.weight else "--"
                                PetImageUtils.loadPetAvatar(binding.ivActivePetAvatar, it.imageUri, it.species)
                            }
                        }
                    }
                }

                launch {
                    viewModel.tasksForSelectedPet.collect { tasks ->
                        taskAdapter.submitList(tasks)
                    }
                }

                launch {
                    viewModel.remindersForSelectedPet.collect { reminders ->
                        val displayItems = reminders.map { PlanDisplayItem.Reminder(it) }
                        reminderAdapter.submitList(displayItems)
                    }
                }

                launch {
                    viewModel.activityLogsForSelectedPet.collect { logs ->
                        logAdapter.submitList(logs.take(6))
                    }
                }

                launch {
                    viewModel.taskProgress.collect { progress ->
                        val (completed, total) = progress
                        binding.pbTaskProgress.max = if (total == 0) 100 else total
                        binding.pbTaskProgress.progress = completed
                        binding.tvProgressCount.text = "$completed / $total"
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.ivUserHeaderAvatar.setOnClickListener {
            binding.bottomNavigation.selectedItemId = R.id.nav_profile
        }

        binding.btnViewHeroProfile.setOnClickListener {
            val petId = viewModel.selectedPetId.value
            if (petId != null) openPetDetail(petId) else launchAddPetWizard()
        }

        binding.cardActivePet.setOnClickListener {
            val petId = viewModel.selectedPetId.value
            if (petId != null) openPetDetail(petId)
        }

        binding.btnQuickAddPetHome.setOnClickListener { launchAddPetWizard() }
        binding.btnAddPetTab.setOnClickListener { launchAddPetWizard() }

        binding.btnLogActivity.setOnClickListener { showLogActivityDialog() }
        binding.btnQuickDelegate.setOnClickListener { startActivity(Intent(this, DelegateActivity::class.java)) }
        binding.btnExploreMapQuick.setOnClickListener { startActivity(Intent(this, PetMapActivity::class.java)) }

        binding.btnViewAllActivity.setOnClickListener {
            binding.bottomNavigation.selectedItemId = R.id.nav_plan
        }

        binding.btnChangeProfilePhoto.setOnClickListener {
            val options = arrayOf("Take Photo", "Choose from Gallery")
            MaterialAlertDialogBuilder(this)
                .setTitle("Update Profile Photo")
                .setItems(options) { _, which ->
                    if (which == 0) takeUserPhoto.launch(null) else pickUserImage.launch("image/*")
                }
                .show()
        }

        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        binding.btnSignOut.setOnClickListener {
            sessionManager.logout()
            Toast.makeText(this, "See you soon!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        binding.btnDeleteAccount.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account?")
                .setMessage("All your pets and care data will be permanently removed.")
                .setPositiveButton("Delete") { _, _ ->
                    val userId = sessionManager.getUserId()
                    lifecycleScope.launch {
                        viewModel.deleteAccount(userId)
                        sessionManager.logout()
                        val intent = Intent(this@MainActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupRadialMenu() {
        binding.fabMainAdd.setOnClickListener { toggleRadialMenu() }
        binding.vRadialBackdrop.setOnClickListener { closeRadialMenu() }

        binding.btnRadialAddPet.setOnClickListener {
            closeRadialMenu()
            launchAddPetWizard()
        }

        binding.btnRadialAddTask.setOnClickListener {
            closeRadialMenu()
            showAddTaskDialog()
        }

        binding.btnRadialLogActivity.setOnClickListener {
            closeRadialMenu()
            showLogActivityDialog()
        }

        binding.btnRadialAddReminder.setOnClickListener {
            closeRadialMenu()
            showAddReminderDialog()
        }

        binding.btnRadialAddAppointment.setOnClickListener {
            closeRadialMenu()
            showAddAppointmentDialog()
        }

        binding.btnRadialAddVaccination.setOnClickListener {
            closeRadialMenu()
            showAddVaccinationDialog()
        }
    }

    private fun toggleRadialMenu() {
        if (isRadialMenuOpen) closeRadialMenu() else openRadialMenu()
    }

    private fun openRadialMenu() {
        isRadialMenuOpen = true
        binding.vRadialBackdrop.visibility = View.VISIBLE
        binding.vRadialBackdrop.alpha = 0f
        binding.vRadialBackdrop.animate().alpha(1f).setDuration(250).start()

        binding.fabMainAdd.animate().rotation(135f).setDuration(250).start()

        binding.layoutRadialMenu.visibility = View.VISIBLE
        binding.layoutRadialMenu.alpha = 0f
        binding.layoutRadialMenu.scaleX = 0.5f
        binding.layoutRadialMenu.scaleY = 0.5f
        binding.layoutRadialMenu.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .start()
    }

    private fun closeRadialMenu() {
        if (!isRadialMenuOpen) return
        isRadialMenuOpen = false

        binding.vRadialBackdrop.animate().alpha(0f).setDuration(200).withEndAction {
            binding.vRadialBackdrop.visibility = View.GONE
        }.start()

        binding.fabMainAdd.animate().rotation(0f).setDuration(200).start()

        binding.layoutRadialMenu.animate()
            .alpha(0f)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .setDuration(200)
            .withEndAction {
                binding.layoutRadialMenu.visibility = View.GONE
            }
            .start()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            closeRadialMenu()

            binding.viewHome.visibility = View.GONE
            binding.viewPetsList.visibility = View.GONE
            binding.viewPlan.visibility = View.GONE
            binding.viewProfile.visibility = View.GONE

            when (item.itemId) {
                R.id.nav_home -> {
                    binding.viewHome.visibility = View.VISIBLE
                    binding.fabMainAdd.visibility = View.VISIBLE
                }
                R.id.nav_pets -> {
                    binding.viewPetsList.visibility = View.VISIBLE
                    binding.fabMainAdd.visibility = View.VISIBLE
                }
                R.id.nav_plan -> {
                    binding.viewPlan.visibility = View.VISIBLE
                    binding.fabMainAdd.visibility = View.VISIBLE
                }
                R.id.nav_explore -> {
                    startActivity(Intent(this, PetMapActivity::class.java))
                    return@setOnItemSelectedListener false
                }
                R.id.nav_profile -> {
                    binding.viewProfile.visibility = View.VISIBLE
                    binding.fabMainAdd.visibility = View.GONE
                }
            }
            true
        }
    }

    private fun launchAddPetWizard() {
        startActivity(Intent(this, AddPetWizardActivity::class.java))
    }

    private fun openPetDetail(petId: Long) {
        val intent = Intent(this, PetDetailActivity::class.java).apply {
            putExtra(Constants.EXTRA_PET_ID, petId)
        }
        startActivity(intent)
    }

    private fun confirmDeletePet(pet: PetEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_pet_title)
            .setMessage(R.string.dialog_delete_pet_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deletePet(pet)
                Toast.makeText(this, "${pet.name} removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAddTaskDialog() {
        val petId = viewModel.selectedPetId.value
        if (petId == null) {
            Toast.makeText(this, "Please add a pet first", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)

        dialogBinding.etTaskDueDate.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(8)
                .setMinute(0)
                .setTitleText("Select Due Time")
                .build()
            picker.addOnPositiveButtonClickListener {
                val formatted = String.format(Locale.getDefault(), "%02d:%02d %s",
                    if (picker.hour % 12 == 0) 12 else picker.hour % 12,
                    picker.minute,
                    if (picker.hour >= 12) "PM" else "AM"
                )
                dialogBinding.etTaskDueDate.setText(formatted)
            }
            picker.show(supportFragmentManager, "time_picker")
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Add Task") { _, _ ->
                val title = dialogBinding.etTaskTitle.text.toString().trim()
                if (title.isNotEmpty()) {
                    val task = TaskEntity(
                        petId = petId,
                        title = title,
                        category = dialogBinding.etTaskCategory.text.toString().trim().ifEmpty { "General" },
                        dueDate = dialogBinding.etTaskDueDate.text.toString().trim().ifEmpty { "08:00 AM" },
                        description = ""
                    )
                    viewModel.addTask(task)
                    Toast.makeText(this, "Task added for pet", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddReminderDialog() {
        val petId = viewModel.selectedPetId.value
        if (petId == null) {
            Toast.makeText(this, "Please add a pet first", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogBinding = DialogAddReminderBinding.inflate(layoutInflater)

        dialogBinding.etReminderTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(9)
                .setMinute(0)
                .setTitleText("Select Reminder Time")
                .build()
            picker.addOnPositiveButtonClickListener {
                val formatted = String.format(Locale.getDefault(), "%02d:%02d %s",
                    if (picker.hour % 12 == 0) 12 else picker.hour % 12,
                    picker.minute,
                    if (picker.hour >= 12) "PM" else "AM"
                )
                dialogBinding.etReminderTime.setText(formatted)
            }
            picker.show(supportFragmentManager, "time_picker_reminder")
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Add Reminder") { _, _ ->
                val title = dialogBinding.etReminderTitle.text.toString().trim()
                if (title.isNotEmpty()) {
                    val reminder = ReminderEntity(
                        petId = petId,
                        title = title,
                        type = dialogBinding.etReminderType.text.toString().trim().ifEmpty { "General" },
                        dateTime = dialogBinding.etReminderTime.text.toString().trim().ifEmpty { "09:00 AM" }
                    )
                    viewModel.addReminder(reminder)
                    Toast.makeText(this, "Reminder saved", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddAppointmentDialog() {
        val petId = viewModel.selectedPetId.value
        if (petId == null) {
            Toast.makeText(this, "Please add a pet first", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogBinding = DialogAddAppointmentBinding.inflate(layoutInflater)

        dialogBinding.etAppointmentTime.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Appointment Date")
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                dialogBinding.etAppointmentTime.setText(sdf.format(Date(selection)))
            }
            datePicker.show(supportFragmentManager, "date_picker_appt")
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Add Appointment") { _, _ ->
                val title = dialogBinding.etAppointmentTitle.text.toString().trim()
                if (title.isNotEmpty()) {
                    val appointment = AppointmentEntity(
                        petId = petId,
                        title = title,
                        clinic = dialogBinding.etAppointmentClinic.text.toString().trim().ifEmpty { "Vet Clinic" },
                        dateTime = dialogBinding.etAppointmentTime.text.toString().trim().ifEmpty { "Tomorrow" }
                    )
                    viewModel.addAppointment(appointment)
                    Toast.makeText(this, "Appointment scheduled", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddVaccinationDialog() {
        val petId = viewModel.selectedPetId.value
        if (petId == null) {
            Toast.makeText(this, "Please add a pet first", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogBinding = DialogAddVaccinationBinding.inflate(layoutInflater)

        dialogBinding.etVaccineNextDue.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Next Due Date")
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                dialogBinding.etVaccineNextDue.setText(sdf.format(Date(selection)))
            }
            datePicker.show(supportFragmentManager, "date_picker_vac")
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Add Vaccination") { _, _ ->
                val name = dialogBinding.etVaccineName.text.toString().trim()
                if (name.isNotEmpty()) {
                    val vac = VaccinationEntity(
                        petId = petId,
                        vaccineName = name,
                        dateGiven = "Today",
                        nextDueDate = dialogBinding.etVaccineNextDue.text.toString().trim().ifEmpty { "In 1 Year" }
                    )
                    viewModel.addVaccination(vac)
                    Toast.makeText(this, "Vaccination recorded", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogActivityDialog() {
        val petId = viewModel.selectedPetId.value
        if (petId == null) {
            Toast.makeText(this, "Please add a pet first", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogBinding = DialogLogActivityBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Save Activity") { _, _ ->
                val checkedChipId = dialogBinding.chipGroupActivityType.checkedChipId
                val selectedType = if (checkedChipId != View.NO_ID) {
                    dialogBinding.root.findViewById<Chip>(checkedChipId).text.toString().replace(Regex("[^a-zA-Z\\s]"), "").trim()
                } else "Walk"

                val durationStr = dialogBinding.etActivityDuration.text.toString().trim()
                val duration = durationStr.toIntOrNull() ?: 20
                val notes = dialogBinding.etActivityNotes.text.toString().trim().ifEmpty { "Routine activity logged" }

                val log = ActivityLogEntity(
                    petId = petId,
                    type = selectedType,
                    duration = duration,
                    notes = notes
                )
                viewModel.addActivityLog(log)
                Toast.makeText(this, "$selectedType logged 🐾", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Change Password") { _, _ ->
                val newPass = dialogBinding.etNewPassword.text.toString().trim()
                if (newPass.length >= 6) {
                    val userId = sessionManager.getUserId()
                    lifecycleScope.launch {
                        val user = viewModel.getUserById(userId)
                        user?.let {
                            val newHash = SecurityUtils.hashPassword(newPass)
                            viewModel.updateUser(it.copy(passwordHash = newHash))
                            Toast.makeText(this@MainActivity, "Password updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}