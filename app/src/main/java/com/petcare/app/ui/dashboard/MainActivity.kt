package com.petcare.app.ui.dashboard

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.petcare.app.R
import com.petcare.app.data.model.PetEntity
import com.petcare.app.databinding.ActivityMainBinding
import com.petcare.app.databinding.DialogChangePasswordBinding
import com.petcare.app.ui.adapter.*
import com.petcare.app.ui.auth.EntryActivity
import com.petcare.app.ui.delegate.DelegateActivity
import com.petcare.app.ui.map.PetMapActivity
import com.petcare.app.ui.pets.AddPetWizardActivity
import com.petcare.app.ui.profile.PetDetailActivity
import com.petcare.app.ui.routine.RoutineEditorActivity
import com.petcare.app.util.Constants
import com.petcare.app.util.PetImageUtils
import com.petcare.app.util.SecurityUtils
import com.petcare.app.util.SessionManager
import com.petcare.app.util.TimeUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var petAdapter: PetAdapter
    private lateinit var planPetAdapter: PetAdapter
    private lateinit var petListAdapter: PetListAdapter
    private lateinit var routineAdapter: RoutineAdapter
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var appointmentAdapter: PlanItemAdapter
    private lateinit var reminderAdapter: PlanItemAdapter
    private lateinit var vaccinationAdapter: PlanItemAdapter
    private lateinit var logAdapter: ActivityLogAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var dialogs: CareItemDialogs

    private var isRadialMenuOpen = false

    private val viewModel: PetViewModel by viewModels { PetViewModelFactory.from(this) }

    private val pickUserImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { PetImageUtils.copyToAppStorage(this, it)?.let(::saveProfilePhoto) }
    }

    private val takeUserPhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { PetImageUtils.saveBitmapToAppStorage(this, it)?.let(::saveProfilePhoto) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            goToEntryScreen()
            return
        }
        viewModel.setUserId(sessionManager.getUserId())
        dialogs = CareItemDialogs(this, viewModel)

        setupAdapters()
        observeState()
        setupClickListeners()
        setupRadialMenu()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        if (!::dialogs.isInitialized) return
        viewModel.refreshToday()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userEmail = sessionManager.getUserEmail() ?: "User"
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.setText(
            when (hour) {
                in 0..11 -> R.string.greeting_morning
                in 12..16 -> R.string.greeting_afternoon
                else -> R.string.greeting_evening
            }
        )

        lifecycleScope.launch {
            val user = viewModel.getUserById(sessionManager.getUserId())
            val name = user?.name?.ifEmpty { null } ?: userEmail.substringBefore("@")
            binding.tvUserName.text = name
            binding.tvProfileName.text = name
            binding.tvProfileEmail.text = user?.email ?: userEmail
            user?.let {
                val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                binding.tvProfileMemberSince.text = getString(R.string.member_since_format, sdf.format(Date(it.createdAt)))
            }
            PetImageUtils.loadUserAvatar(binding.ivProfileAvatar, user?.profileImageUri)
            PetImageUtils.loadUserAvatar(binding.ivUserHeaderAvatar, user?.profileImageUri)
        }
    }

    private fun saveProfilePhoto(uriString: String) {
        lifecycleScope.launch {
            val user = viewModel.getUserById(sessionManager.getUserId()) ?: return@launch
            viewModel.updateUser(user.copy(profileImageUri = uriString, updatedAt = System.currentTimeMillis()))
            PetImageUtils.loadUserAvatar(binding.ivProfileAvatar, uriString)
            PetImageUtils.loadUserAvatar(binding.ivUserHeaderAvatar, uriString)
            Toast.makeText(this@MainActivity, "Profile photo updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAdapters() {
        petAdapter = PetAdapter(
            onPetClick = { pet -> viewModel.selectPet(pet.id) },
            onPetLongClick = { pet -> openPetDetail(pet.id) }
        )
        binding.rvPets.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = petAdapter
        }

        planPetAdapter = PetAdapter(
            onPetClick = { pet -> viewModel.selectPet(pet.id) },
            onPetLongClick = { pet -> openPetDetail(pet.id) }
        )
        binding.rvPlanPets.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = planPetAdapter
        }

        petListAdapter = PetListAdapter(
            onPetClick = { pet -> openPetDetail(pet.id) },
            onDeleteClick = { pet -> confirmDeletePet(pet) }
        )
        binding.rvAllPetsList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = petListAdapter
        }

        routineAdapter = RoutineAdapter(
            onRoutineClick = { summary -> openRoutineEditor(routineId = summary.routine.id) },
            onDeleteClick = { summary ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Delete \"${summary.routine.name}\"?")
                    .setMessage("This removes the routine and its ${summary.steps.size} step(s).")
                    .setPositiveButton(R.string.delete) { _, _ ->
                        viewModel.deleteRoutine(summary)
                        showUndo("Routine deleted") { viewModel.restoreRoutine(summary) }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )
        binding.rvRoutines.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = routineAdapter
        }

        taskAdapter = TaskAdapter(
            onTaskStatusChange = { task, isChecked -> viewModel.setTaskDone(task, isChecked) },
            onTaskClick = { task -> dialogs.showTaskEditor(task) },
            onDeleteClick = { task ->
                viewModel.deleteTask(task)
                showUndo("\"${task.title}\" deleted") { viewModel.restoreTask(task) }
            }
        )
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }

        fun planAdapter() = PlanItemAdapter(
            onItemClick = ::editPlanItem,
            onCompletedChange = { item, done ->
                when (item) {
                    is PlanDisplayItem.Reminder -> viewModel.setReminderDone(item.entity, done)
                    is PlanDisplayItem.Appointment -> viewModel.setAppointmentDone(item.entity, done)
                    is PlanDisplayItem.Vaccination -> Unit
                }
            },
            onDeleteClick = ::deletePlanItem
        )
        appointmentAdapter = planAdapter()
        reminderAdapter = planAdapter()
        vaccinationAdapter = planAdapter()
        binding.rvAppointments.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appointmentAdapter
        }
        binding.rvReminders.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = reminderAdapter
        }
        binding.rvVaccinations.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = vaccinationAdapter
        }

        logAdapter = ActivityLogAdapter()
        binding.rvRecentActivity.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = logAdapter
        }
    }

    private fun editPlanItem(item: PlanDisplayItem) {
        when (item) {
            is PlanDisplayItem.Reminder -> dialogs.showReminderEditor(item.entity.petId, item.entity)
            is PlanDisplayItem.Appointment ->
                dialogs.showAppointmentEditor(item.entity.petId, item.entity, viewModel.savedPlaces.value)
            is PlanDisplayItem.Vaccination -> dialogs.showVaccinationEditor(item.entity.petId, item.entity)
        }
    }

    private fun deletePlanItem(item: PlanDisplayItem) {
        when (item) {
            is PlanDisplayItem.Reminder -> {
                viewModel.deleteReminder(item.entity)
                showUndo("Reminder deleted") { viewModel.restoreReminder(item.entity) }
            }
            is PlanDisplayItem.Appointment -> {
                viewModel.deleteAppointment(item.entity)
                showUndo("Appointment deleted") { viewModel.restoreAppointment(item.entity) }
            }
            is PlanDisplayItem.Vaccination -> {
                viewModel.deleteVaccination(item.entity)
                showUndo("Vaccination deleted") { viewModel.restoreVaccination(item.entity) }
            }
        }
    }

    private fun showUndo(message: String, undo: () -> Unit) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            // Sit above the + button so it never covers the Undo action.
            .setAnchorView(if (binding.fabMainAdd.visibility == View.VISIBLE) binding.fabMainAdd else binding.bottomNavigation)
            .setAction("Undo") { undo() }
            .show()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allPets.collect { pets ->
                        petAdapter.submitList(pets)
                        planPetAdapter.submitList(pets)
                        petListAdapter.submitList(pets)

                        binding.layoutEmptyPets.visibility = if (pets.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvAllPetsList.visibility = if (pets.isNotEmpty()) View.VISIBLE else View.GONE
                        binding.tvPetsCount.text = resources.getQuantityString(R.plurals.pet_count, pets.size, pets.size)

                        // Keep a valid selection: pick the first pet if none, or if the selected one was deleted.
                        val selected = viewModel.selectedPetId.value
                        if (pets.none { it.id == selected }) viewModel.selectPet(pets.firstOrNull()?.id)
                    }
                }

                launch {
                    viewModel.selectedPet.collect { pet ->
                        petAdapter.setSelectedPet(pet?.id)
                        planPetAdapter.setSelectedPet(pet?.id)
                        bindHeroPet(pet)
                    }
                }

                launch {
                    viewModel.routineSummaries.collect { routines ->
                        routineAdapter.submitList(routines)
                        binding.tvRoutinesEmpty.visibility = if (routines.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.todayChecklist.collect { items ->
                        taskAdapter.submitList(items)
                        binding.tvChecklistEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                        val next = items.firstOrNull { !it.isDone }
                        binding.tvNextTask.text = when {
                            items.isEmpty() -> getString(R.string.empty_checklist)
                            next == null -> "All done for today. Great job!"
                            else -> "Next: ${next.task.title} at ${TimeUtils.formatTime(next.task.time)}"
                        }
                    }
                }

                launch {
                    viewModel.appointmentItems.collect { items ->
                        appointmentAdapter.submitList(items)
                        binding.tvAppointmentsEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.reminderItems.collect { items ->
                        reminderAdapter.submitList(items)
                        binding.tvRemindersEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.vaccinationItems.collect { items ->
                        vaccinationAdapter.submitList(items)
                        binding.tvVaccinationsEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch { viewModel.nextAppointment.collect(::bindUpcomingAppointment) }

                launch {
                    viewModel.nextReminder.collect { reminder ->
                        binding.vNotificationDot.visibility = if (reminder != null) View.VISIBLE else View.GONE
                        if (reminder != null) {
                            binding.tvReminderTitle.text = reminder.title
                            binding.tvReminderMeta.text = "${reminder.type} · ${TimeUtils.formatTime(reminder.time)}"
                        } else {
                            binding.tvReminderTitle.setText(R.string.no_pending_reminder)
                            binding.tvReminderMeta.text = "Tap to add a feeding or medication reminder"
                        }
                    }
                }

                launch {
                    viewModel.activityLogsForSelectedPet.collect { logs ->
                        logAdapter.submitList(logs.take(5))
                        binding.tvActivityEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.savedPlaces.collect { places ->
                        logAdapter.setPlaceNames(places.associate { it.id to it.name })
                    }
                }

                launch {
                    viewModel.taskProgress.collect { (completed, total) ->
                        val max = if (total == 0) 1 else total
                        binding.pbTaskProgress.max = max
                        binding.pbTaskProgress.setProgressCompat(completed, true)
                        binding.pbPlanProgress.max = max
                        binding.pbPlanProgress.setProgressCompat(completed, true)
                        val label = "$completed / $total done"
                        binding.tvProgressCount.text = label
                        binding.tvPlanProgress.text = label
                    }
                }
            }
        }
    }

    private fun bindHeroPet(pet: PetEntity?) {
        if (pet != null) {
            binding.tvActivePetName.text = pet.name
            binding.tvActivePetBreed.text = "${pet.species} · ${pet.breed.ifEmpty { "Mixed" }}"
            binding.tvPetAge.text = if (pet.age > 0) "${pet.age} yrs" else "—"
            binding.tvPetWeight.text = pet.weight.ifEmpty { "—" }
            binding.tvPetGender.text = pet.gender.ifEmpty { "—" }
            binding.tvPlanPetName.text = "Plan for ${pet.name}"
            binding.btnViewHeroProfile.setText(R.string.view_profile)
            PetImageUtils.loadPetAvatar(binding.ivActivePetAvatar, pet.imageUri, pet.species)
        } else {
            binding.tvActivePetName.setText(R.string.no_pet_name)
            binding.tvActivePetBreed.setText(R.string.no_pet_sub)
            binding.tvPetAge.text = "—"
            binding.tvPetWeight.text = "—"
            binding.tvPetGender.text = "—"
            binding.tvPlanPetName.text = getString(R.string.no_pet_sub)
            binding.btnViewHeroProfile.setText(R.string.add_pet)
            PetImageUtils.loadPetAvatar(binding.ivActivePetAvatar, null, null)
        }
    }

    private fun bindUpcomingAppointment(item: PlanDisplayItem.Appointment?) {
        // Empty state shows a calendar icon on a soft tile instead of a date.
        val empty = item == null
        binding.tvApptDay.visibility = if (empty) View.GONE else View.VISIBLE
        binding.tvApptMonth.visibility = if (empty) View.GONE else View.VISIBLE
        binding.ivApptEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.layoutApptDate.backgroundTintList =
            ColorStateList.valueOf(getColor(if (empty) R.color.primary_container else R.color.primary))
        if (item == null) {
            binding.tvApptTitle.setText(R.string.no_upcoming_appointment)
            binding.tvApptMeta.text = "Tap to book a vet or grooming visit"
            return
        }
        val a = item.entity
        val date = Date(a.dateMillis)
        binding.tvApptDay.text = SimpleDateFormat("dd", Locale.getDefault()).format(date)
        binding.tvApptMonth.text = SimpleDateFormat("MMM", Locale.getDefault()).format(date)
        binding.tvApptTitle.text = a.title
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        binding.tvApptMeta.text = "$time · ${item.placeName ?: a.clinic}"
    }

    private fun setupClickListeners() {
        binding.ivUserHeaderAvatar.setOnClickListener { selectTab(R.id.nav_profile) }
        binding.btnNotifications.setOnClickListener {
            selectTab(R.id.nav_plan)
            scrollTo(binding.tvRemindersHeader)
        }

        binding.btnViewHeroProfile.setOnClickListener {
            val petId = viewModel.selectedPetId.value
            if (petId != null) openPetDetail(petId) else launchAddPetWizard()
        }
        binding.cardActivePet.setOnClickListener {
            val petId = viewModel.selectedPetId.value
            if (petId != null) openPetDetail(petId) else launchAddPetWizard()
        }
        binding.cardTodayCare.setOnClickListener { selectTab(R.id.nav_plan) }

        binding.btnQuickAddPetHome.setOnClickListener { launchAddPetWizard() }
        binding.btnAddPetTab.setOnClickListener { launchAddPetWizard() }
        binding.btnEmptyAddPet.setOnClickListener { launchAddPetWizard() }

        // Quick actions
        binding.btnQuickAppointment.setOnClickListener {
            withSelectedPet { dialogs.showAppointmentEditor(it, null, viewModel.savedPlaces.value) }
        }
        binding.btnQuickReminder.setOnClickListener { withSelectedPet { dialogs.showReminderEditor(it, null) } }
        binding.btnQuickVaccination.setOnClickListener { withSelectedPet { dialogs.showVaccinationEditor(it, null) } }
        binding.btnQuickDelegate.setOnClickListener { openDelegate() }

        // Upcoming
        binding.btnSeeAllUpcoming.setOnClickListener {
            selectTab(R.id.nav_plan)
            scrollTo(binding.rvAppointments)
        }
        binding.cardUpcomingAppointment.setOnClickListener {
            val next = viewModel.nextAppointment.value
            if (next != null) editPlanItem(next)
            else withSelectedPet { dialogs.showAppointmentEditor(it, null, viewModel.savedPlaces.value) }
        }
        binding.cardUpcomingReminder.setOnClickListener {
            val next = viewModel.nextReminder.value
            if (next != null) dialogs.showReminderEditor(next.petId, next)
            else withSelectedPet { dialogs.showReminderEditor(it, null) }
        }

        binding.btnExploreMapQuick.setOnClickListener { openMap() }
        binding.btnLogActivity.setOnClickListener { showLogActivity() }

        // Care tab
        binding.btnNewRoutine.setOnClickListener { withSelectedPet { openRoutineEditor(petId = it) } }
        binding.btnAddAppointment.setOnClickListener {
            withSelectedPet { dialogs.showAppointmentEditor(it, null, viewModel.savedPlaces.value) }
        }
        binding.btnAddReminder.setOnClickListener { withSelectedPet { dialogs.showReminderEditor(it, null) } }
        binding.btnAddVaccination.setOnClickListener { withSelectedPet { dialogs.showVaccinationEditor(it, null) } }

        // Profile
        val changePhoto = View.OnClickListener {
            val options = arrayOf("Take photo", "Choose from gallery")
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.change_photo)
                .setItems(options) { _, which ->
                    if (which == 0) takeUserPhoto.launch(null) else pickUserImage.launch("image/*")
                }
                .show()
        }
        binding.btnChangeProfilePhoto.setOnClickListener(changePhoto)
        binding.btnEditAvatar.setOnClickListener(changePhoto)
        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnProfileDelegate.setOnClickListener { openDelegate() }
        binding.btnProfilePlaces.setOnClickListener { openMap() }
        binding.btnAbout.setOnClickListener { showAboutDialog() }

        binding.btnSignOut.setOnClickListener {
            sessionManager.logout()
            Toast.makeText(this, "See you soon!", Toast.LENGTH_SHORT).show()
            goToEntryScreen()
        }

        binding.btnDeleteAccount.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete account?")
                .setMessage("All your pets, routines, places and care data will be permanently removed.")
                .setPositiveButton(R.string.delete) { _, _ ->
                    val userId = sessionManager.getUserId()
                    lifecycleScope.launch {
                        viewModel.deleteAccount(userId)
                        sessionManager.logout()
                        goToEntryScreen()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
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
            withSelectedPet { openRoutineEditor(petId = it) }
        }
        binding.btnRadialLogActivity.setOnClickListener {
            closeRadialMenu()
            showLogActivity()
        }
        binding.btnRadialAddReminder.setOnClickListener {
            closeRadialMenu()
            withSelectedPet { dialogs.showReminderEditor(it, null) }
        }
        binding.btnRadialAddAppointment.setOnClickListener {
            closeRadialMenu()
            withSelectedPet { dialogs.showAppointmentEditor(it, null, viewModel.savedPlaces.value) }
        }
        binding.btnRadialAddVaccination.setOnClickListener {
            closeRadialMenu()
            withSelectedPet { dialogs.showVaccinationEditor(it, null) }
        }
    }

    private fun showLogActivity() = withSelectedPet { dialogs.showLogActivity(it, viewModel.savedPlaces.value) }

    private inline fun withSelectedPet(action: (Long) -> Unit) {
        val petId = viewModel.selectedPetId.value
        if (petId == null) {
            Toast.makeText(this, "Please add a pet first", Toast.LENGTH_SHORT).show()
        } else {
            action(petId)
        }
    }

    private fun toggleRadialMenu() {
        if (isRadialMenuOpen) closeRadialMenu() else openRadialMenu()
    }

    private fun openRadialMenu() {
        isRadialMenuOpen = true
        binding.vRadialBackdrop.visibility = View.VISIBLE
        binding.vRadialBackdrop.alpha = 0f
        binding.vRadialBackdrop.animate().alpha(1f).setDuration(200).start()

        binding.fabMainAdd.animate().rotation(45f).setDuration(200).start()

        binding.layoutRadialMenu.visibility = View.VISIBLE
        binding.layoutRadialMenu.alpha = 0f
        binding.layoutRadialMenu.translationY = 24f
        binding.layoutRadialMenu.animate().alpha(1f).translationY(0f).setDuration(200).start()
    }

    private fun closeRadialMenu() {
        if (!isRadialMenuOpen) return
        isRadialMenuOpen = false

        binding.vRadialBackdrop.animate().alpha(0f).setDuration(150).withEndAction {
            binding.vRadialBackdrop.visibility = View.GONE
        }.start()

        binding.fabMainAdd.animate().rotation(0f).setDuration(150).start()

        binding.layoutRadialMenu.animate().alpha(0f).translationY(24f).setDuration(150).withEndAction {
            binding.layoutRadialMenu.visibility = View.GONE
        }.start()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            closeRadialMenu()

            if (item.itemId == R.id.nav_explore) {
                openMap()
                return@setOnItemSelectedListener false
            }

            binding.viewHome.visibility = View.GONE
            binding.viewPetsList.visibility = View.GONE
            binding.viewPlan.visibility = View.GONE
            binding.viewProfile.visibility = View.GONE

            when (item.itemId) {
                R.id.nav_home -> binding.viewHome.visibility = View.VISIBLE
                R.id.nav_pets -> binding.viewPetsList.visibility = View.VISIBLE
                R.id.nav_plan -> binding.viewPlan.visibility = View.VISIBLE
                R.id.nav_profile -> binding.viewProfile.visibility = View.VISIBLE
            }
            binding.fabMainAdd.visibility = if (item.itemId == R.id.nav_profile) View.GONE else View.VISIBLE
            binding.mainScrollView.scrollTo(0, 0)
            true
        }
    }

    private fun selectTab(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }

    /** Scrolls the main page so [target] (inside the current tab) is near the top. */
    private fun scrollTo(target: View) {
        binding.mainScrollView.post {
            val rect = Rect()
            target.getDrawingRect(rect)
            (binding.mainScrollView.getChildAt(0) as ViewGroup).offsetDescendantRectToMyCoords(target, rect)
            binding.mainScrollView.smoothScrollTo(0, (rect.top - resources.getDimensionPixelSize(R.dimen.space_lg)).coerceAtLeast(0))
        }
    }

    private fun launchAddPetWizard() {
        startActivity(Intent(this, AddPetWizardActivity::class.java))
    }

    private fun openPetDetail(petId: Long) {
        startActivity(Intent(this, PetDetailActivity::class.java).putExtra(Constants.EXTRA_PET_ID, petId))
    }

    private fun openMap() {
        startActivity(Intent(this, PetMapActivity::class.java))
    }

    private fun openDelegate() {
        val intent = Intent(this, DelegateActivity::class.java)
        viewModel.selectedPetId.value?.let { intent.putExtra(Constants.EXTRA_PET_ID, it) }
        startActivity(intent)
    }

    private fun openRoutineEditor(petId: Long? = null, routineId: Long? = null) {
        val intent = Intent(this, RoutineEditorActivity::class.java)
        petId?.let { intent.putExtra(Constants.EXTRA_PET_ID, it) }
        routineId?.let { intent.putExtra(Constants.EXTRA_ROUTINE_ID, it) }
        startActivity(intent)
    }

    private fun goToEntryScreen() {
        startActivity(Intent(this, EntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
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

    private fun showAboutDialog() {
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "1.0"
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_petcare)
            .setMessage(getString(R.string.about_message, version))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.change_password) { _, _ ->
                val current = dialogBinding.etCurrentPassword.text.toString().trim()
                val newPass = dialogBinding.etNewPassword.text.toString().trim()
                if (newPass.length < 6) {
                    Toast.makeText(this, R.string.error_password_length, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val user = viewModel.getUserById(sessionManager.getUserId()) ?: return@launch
                    // The dialog asks for the current password, so actually check it.
                    if (!SecurityUtils.verifyPassword(current, user.passwordHash)) {
                        Toast.makeText(this@MainActivity, "Current password is incorrect", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    viewModel.updateUser(user.copy(passwordHash = SecurityUtils.hashPassword(newPass)))
                    Toast.makeText(this@MainActivity, "Password updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
