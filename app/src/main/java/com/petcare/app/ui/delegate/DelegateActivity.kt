package com.petcare.app.ui.delegate

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.petcare.app.R
import com.petcare.app.data.model.PetEntity
import com.petcare.app.databinding.ActivityDelegateBinding
import com.petcare.app.ui.adapter.RoutineAdapter
import com.petcare.app.ui.dashboard.PetViewModel
import com.petcare.app.ui.dashboard.PetViewModelFactory
import com.petcare.app.util.CareOptions
import com.petcare.app.util.Constants
import com.petcare.app.util.SessionManager
import com.petcare.app.util.TimeUtils
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Item delegation: builds a care checklist, feeding schedule, medication reminder or daily
 * routine for the chosen pet and hands it to the phone's SMS app for a typed or picked contact.
 */
class DelegateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDelegateBinding
    private val viewModel: PetViewModel by viewModels { PetViewModelFactory.from(this) }

    private var pets: List<PetEntity> = emptyList()

    // Picking from the Phone content URI grants one-off read access, so no READ_CONTACTS permission is needed.
    private val pickContact = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@registerForActivityResult
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                binding.etCaregiverPhone.setText(cursor.getString(0))
                binding.tvContactName.text = "Sending to: ${cursor.getString(1)}"
                binding.tvContactName.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDelegateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.setUserId(SessionManager(this).getUserId())
        // Start with the pet that was selected on the Home screen.
        intent.getLongExtra(Constants.EXTRA_PET_ID, -1L).takeIf { it != -1L }?.let(viewModel::selectPet)

        binding.btnDelegateBack.setOnClickListener { finish() }
        binding.tilCaregiverPhone.setEndIconOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            try {
                pickContact.launch(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No contacts app available", Toast.LENGTH_SHORT).show()
            }
        }
        // Typing over a picked number clears the contact label (the picker sets the label after the text).
        binding.etCaregiverPhone.doAfterTextChanged {
            binding.tvContactName.visibility = View.GONE
            binding.tilCaregiverPhone.error = null
        }
        binding.chipGroupMessageType.setOnCheckedStateChangeListener { _, _ -> rebuildMessage() }
        binding.actvSelectPet.setOnItemClickListener { _, _, position, _ ->
            viewModel.selectPet(pets[position].id)
        }
        binding.btnSendSms.setOnClickListener { sendSms() }

        observeData()
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allPets.collect { list ->
                        pets = list
                        binding.actvSelectPet.setSimpleItems(list.map { it.name }.toTypedArray())
                        if (list.isNotEmpty() && list.none { it.id == viewModel.selectedPetId.value }) {
                            viewModel.selectPet(list.first().id)
                        }
                    }
                }
                launch {
                    viewModel.selectedPet.collect { pet ->
                        binding.actvSelectPet.setText(pet?.name ?: "", false)
                    }
                }
                // Regenerate the message whenever the pet or its care data changes.
                launch {
                    combine(
                        viewModel.selectedPet,
                        viewModel.routineSummaries,
                        viewModel.todayChecklist,
                        viewModel.remindersForSelectedPet
                    ) { _, _, _, _ -> Unit }.collect { rebuildMessage() }
                }
            }
        }
    }

    private fun rebuildMessage() {
        val pet = viewModel.selectedPet.value
        if (pet == null) {
            binding.etMessage.setText(if (pets.isEmpty()) "Add a pet first to delegate its care." else "")
            return
        }
        val body = when (binding.chipGroupMessageType.checkedChipId) {
            R.id.chipTypeFeeding -> buildFeedingSchedule(pet)
            R.id.chipTypeMedication -> buildMedicationReminder(pet)
            R.id.chipTypeRoutine -> buildDailyRoutine(pet)
            else -> buildChecklist(pet)
        }
        binding.etMessage.setText("$body\n\nSent via PetCare 🐾")
    }

    private fun buildChecklist(pet: PetEntity): String {
        val items = viewModel.todayChecklist.value
        val sb = StringBuilder("Hi! Here is ${pet.name}'s care checklist for today (${TimeUtils.formatDate(System.currentTimeMillis())}):")
        if (items.isEmpty()) sb.append("\nNothing scheduled today.")
        items.forEach { item ->
            val box = if (item.isDone) "[x]" else "[ ]"
            sb.append("\n$box ${TimeUtils.formatTime(item.task.time)} ${item.task.title}")
            if (item.task.notes.isNotBlank()) sb.append(" - ${item.task.notes}")
        }
        if (items.any { it.isDone }) sb.append("\n\n[x] = already done today")
        return sb.toString()
    }

    private fun buildFeedingSchedule(pet: PetEntity): String {
        val sb = StringBuilder("Hi! Here is ${pet.name}'s feeding schedule:")
        val meals = viewModel.routineSummaries.value.flatMap { summary ->
            summary.steps.filter { it.category == CareOptions.CATEGORY_FEEDING }
                .map { it to RoutineAdapter.scheduleLabel(summary.routine) }
        }.sortedBy { it.first.time }
        if (meals.isEmpty()) sb.append("\nNo feeding steps in ${pet.name}'s routines yet.")
        meals.forEach { (task, schedule) ->
            sb.append("\n• ${TimeUtils.formatTime(task.time)} ${task.title}")
            if (task.notes.isNotBlank()) sb.append(" - ${task.notes}")
            sb.append(" ($schedule)")
        }
        if (pet.dietaryPrefs.isNotBlank()) sb.append("\n\nDiet: ${pet.dietaryPrefs}")
        if (pet.dietaryRestrictions.isNotBlank()) sb.append("\nAvoid: ${pet.dietaryRestrictions}")
        if (pet.allergies.isNotBlank() && !pet.allergies.equals("none", ignoreCase = true)) {
            sb.append("\nAllergies: ${pet.allergies}")
        }
        return sb.toString()
    }

    private fun buildMedicationReminder(pet: PetEntity): String {
        val sb = StringBuilder("Medication reminder for ${pet.name}:")
        val steps = viewModel.routineSummaries.value.flatMap { summary ->
            summary.steps.filter { it.category == CareOptions.CATEGORY_MEDICATION }
                .map { it to RoutineAdapter.scheduleLabel(summary.routine) }
        }.sortedBy { it.first.time }
        val reminders = viewModel.remindersForSelectedPet.value
            .filter { it.type == CareOptions.CATEGORY_MEDICATION && !it.isCompleted }

        steps.forEach { (task, schedule) ->
            sb.append("\n💊 ${TimeUtils.formatTime(task.time)} ${task.title}")
            if (task.notes.isNotBlank()) sb.append(" - ${task.notes}")
            sb.append(" ($schedule)")
        }
        reminders.forEach { r ->
            sb.append("\n💊 ${TimeUtils.formatTime(r.time)} ${r.title}")
            if (r.notes.isNotBlank()) sb.append(" - ${r.notes}")
        }
        if (pet.currentMedications.isNotBlank()) sb.append("\n\nCurrent medications: ${pet.currentMedications}")
        if (steps.isEmpty() && reminders.isEmpty() && pet.currentMedications.isBlank()) {
            sb.append("\nNo medication scheduled for ${pet.name}.")
        }
        if (pet.veterinarianPhone.isNotBlank()) sb.append("\nVet: ${pet.veterinarianName} ${pet.veterinarianPhone}".trimEnd())
        return sb.toString()
    }

    private fun buildDailyRoutine(pet: PetEntity): String {
        val sb = StringBuilder("Hi! Here is ${pet.name}'s care routine:")
        val routines = viewModel.routineSummaries.value
        if (routines.isEmpty()) sb.append("\n\nNo routines set up yet.")
        routines.forEach { summary ->
            sb.append("\n\n${summary.routine.name} (${RoutineAdapter.scheduleLabel(summary.routine)})")
            summary.steps.sortedBy { it.time }.forEach { task ->
                sb.append("\n• ${TimeUtils.formatTime(task.time)} ${task.title}")
                if (task.notes.isNotBlank()) sb.append(" - ${task.notes}")
            }
        }
        return sb.toString()
    }

    private fun sendSms() {
        val phone = binding.etCaregiverPhone.text.toString().trim()
        val message = binding.etMessage.text.toString().trim()

        if (viewModel.selectedPet.value == null) {
            Toast.makeText(this, "Please select a pet first", Toast.LENGTH_SHORT).show()
            return
        }
        if (phone.count { it.isDigit() } < 3) {
            binding.tilCaregiverPhone.error = "Enter a phone number or pick a contact"
            return
        }
        binding.tilCaregiverPhone.error = null
        if (message.isEmpty()) {
            Toast.makeText(this, "The message is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(phone)}")
            putExtra("sms_body", message)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No SMS app found on this device", Toast.LENGTH_SHORT).show()
        }
    }
}
