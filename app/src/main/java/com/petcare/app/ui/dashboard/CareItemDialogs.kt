package com.petcare.app.ui.dashboard

import android.content.Intent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.petcare.app.data.model.*
import com.petcare.app.databinding.*
import com.petcare.app.ui.map.PetMapActivity
import com.petcare.app.util.CareOptions
import com.petcare.app.util.Constants
import com.petcare.app.util.TimeUtils
import java.util.Calendar

/**
 * Add/edit dialogs for every care item. Passing an existing entity opens the dialog in edit
 * mode with its fields pre-filled; passing null creates a new item.
 */
class CareItemDialogs(
    private val activity: AppCompatActivity,
    private val viewModel: PetViewModel
) {
    private val fm get() = activity.supportFragmentManager

    // --- Routine step (task) ---
    fun showTaskEditor(task: TaskEntity) {
        val b = DialogAddTaskBinding.inflate(activity.layoutInflater)
        var time = task.time
        b.etTaskTitle.setText(task.title)
        setupDropdown(b.actvTaskCategory, CareOptions.taskCategories, task.category)
        b.etTaskTime.setText(TimeUtils.formatTime(time))
        b.etTaskTime.setOnClickListener {
            TimeUtils.pickTime(fm, time, "Task time") { time = it; b.etTaskTime.setText(TimeUtils.formatTime(it)) }
        }
        b.etTaskNotes.setText(task.notes)

        showValidatingDialog(b.root, "Save") {
            val title = b.etTaskTitle.text.toString().trim()
            if (title.isEmpty()) {
                b.tilTaskTitle.error = "Title is required"
                return@showValidatingDialog false
            }
            viewModel.updateTask(task.copy(
                title = title,
                category = b.actvTaskCategory.text.toString(),
                time = time,
                notes = b.etTaskNotes.text.toString().trim()
            ))
            toast("Task updated")
            true
        }
    }

    // --- Reminder ---
    fun showReminderEditor(petId: Long, existing: ReminderEntity?) {
        val b = DialogAddReminderBinding.inflate(activity.layoutInflater)
        var time = existing?.time ?: "09:00"
        if (existing != null) b.tvDialogTitle.text = "Edit reminder"
        b.etReminderTitle.setText(existing?.title)
        setupDropdown(b.actvReminderType, CareOptions.reminderTypes, existing?.type ?: CareOptions.CATEGORY_MEDICATION)
        b.etReminderTime.setText(TimeUtils.formatTime(time))
        b.etReminderTime.setOnClickListener {
            TimeUtils.pickTime(fm, time, "Reminder time") { time = it; b.etReminderTime.setText(TimeUtils.formatTime(it)) }
        }
        b.etReminderNotes.setText(existing?.notes)

        showValidatingDialog(b.root, if (existing == null) "Add Reminder" else "Save") {
            val title = b.etReminderTitle.text.toString().trim()
            if (title.isEmpty()) {
                b.tilReminderTitle.error = "Title is required"
                return@showValidatingDialog false
            }
            val base = existing ?: ReminderEntity(petId = petId, title = "", type = "", time = "")
            viewModel.saveReminder(base.copy(
                title = title,
                type = b.actvReminderType.text.toString(),
                time = time,
                notes = b.etReminderNotes.text.toString().trim()
            ))
            toast(if (existing == null) "Reminder saved" else "Reminder updated")
            true
        }
    }

    // --- Appointment (can be linked to a geotagged place) ---
    fun showAppointmentEditor(petId: Long, existing: AppointmentEntity?, places: List<LocationEntity>) {
        val b = DialogAddAppointmentBinding.inflate(activity.layoutInflater)
        var dateMillis = existing?.dateMillis ?: Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0)
        }.timeInMillis
        if (existing != null) b.tvDialogTitle.text = "Edit appointment"
        b.etAppointmentTitle.setText(existing?.title)
        b.etAppointmentClinic.setText(existing?.clinic)
        b.etAppointmentTime.setText(TimeUtils.formatDateTime(dateMillis))
        b.etAppointmentTime.setOnClickListener {
            TimeUtils.pickDateTime(fm, dateMillis) { dateMillis = it; b.etAppointmentTime.setText(TimeUtils.formatDateTime(it)) }
        }
        val placePicker = PlacePicker(b.actvAppointmentPlace, places, existing?.locationId)
        // Choosing a tagged place fills in the clinic name if it is empty.
        b.actvAppointmentPlace.setOnItemClickListener { _, _, _, _ ->
            val place = placePicker.selectedPlace()
            if (place != null && b.etAppointmentClinic.text.isNullOrBlank()) b.etAppointmentClinic.setText(place.name)
        }

        val viewOnMap = existing?.locationId?.let { locationId ->
            "View on map" to { openPlaceOnMap(locationId) }
        }
        showValidatingDialog(b.root, if (existing == null) "Add Appointment" else "Save", viewOnMap) {
            val title = b.etAppointmentTitle.text.toString().trim()
            if (title.isEmpty()) {
                b.tilAppointmentTitle.error = "Title is required"
                return@showValidatingDialog false
            }
            val place = placePicker.selectedPlace()
            val base = existing ?: AppointmentEntity(petId = petId, title = "", clinic = "", dateMillis = 0)
            viewModel.saveAppointment(base.copy(
                title = title,
                clinic = b.etAppointmentClinic.text.toString().trim().ifEmpty { place?.name ?: "Vet clinic" },
                dateMillis = dateMillis,
                locationId = place?.id
            ))
            toast(if (existing == null) "Appointment scheduled" else "Appointment updated")
            true
        }
    }

    // --- Vaccination ---
    fun showVaccinationEditor(petId: Long, existing: VaccinationEntity?) {
        val b = DialogAddVaccinationBinding.inflate(activity.layoutInflater)
        var given = existing?.dateGiven ?: System.currentTimeMillis()
        var nextDue = existing?.nextDueDate ?: Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.timeInMillis
        if (existing != null) b.tvDialogTitle.text = "Edit vaccination"
        b.etVaccineName.setText(existing?.vaccineName)
        b.etVaccineGiven.setText(TimeUtils.formatDate(given))
        b.etVaccineNextDue.setText(TimeUtils.formatDate(nextDue))
        b.etVaccineGiven.setOnClickListener {
            TimeUtils.pickDate(fm, given, "Date given") { given = it; b.etVaccineGiven.setText(TimeUtils.formatDate(it)) }
        }
        b.etVaccineNextDue.setOnClickListener {
            TimeUtils.pickDate(fm, nextDue, "Next due date") { nextDue = it; b.etVaccineNextDue.setText(TimeUtils.formatDate(it)) }
        }

        showValidatingDialog(b.root, if (existing == null) "Add Vaccination" else "Save") {
            val name = b.etVaccineName.text.toString().trim()
            if (name.isEmpty()) {
                b.tilVaccineName.error = "Vaccine name is required"
                return@showValidatingDialog false
            }
            val base = existing ?: VaccinationEntity(petId = petId, vaccineName = "", nextDueDate = 0)
            viewModel.saveVaccination(base.copy(vaccineName = name, dateGiven = given, nextDueDate = nextDue))
            toast(if (existing == null) "Vaccination recorded" else "Vaccination updated")
            true
        }
    }

    // --- Activity log (can be linked to a geotagged place) ---
    fun showLogActivity(petId: Long, places: List<LocationEntity>) {
        val b = DialogLogActivityBinding.inflate(activity.layoutInflater)
        val placePicker = PlacePicker(b.actvActivityPlace, places, null)

        showValidatingDialog(b.root, "Save Activity") {
            val checkedChipId = b.chipGroupActivityType.checkedChipId
            val selectedType = if (checkedChipId != View.NO_ID) {
                b.root.findViewById<Chip>(checkedChipId).text.toString().replace(Regex("[^a-zA-Z\\s]"), "").trim()
            } else "Walk"
            val duration = b.etActivityDuration.text.toString().trim().toIntOrNull() ?: 0
            val notes = b.etActivityNotes.text.toString().trim().ifEmpty { "Routine activity logged" }

            viewModel.addActivityLog(ActivityLogEntity(
                petId = petId, type = selectedType, duration = duration, notes = notes,
                locationId = placePicker.selectedPlace()?.id
            ))
            toast("$selectedType logged 🐾")
            true
        }
    }

    private fun openPlaceOnMap(locationId: Long) {
        activity.startActivity(Intent(activity, PetMapActivity::class.java).putExtra(Constants.EXTRA_LOCATION_ID, locationId))
    }

    /** Dialog whose positive button only closes it when [onSave] returns true. */
    private fun showValidatingDialog(
        view: View,
        positiveText: String,
        neutral: Pair<String, () -> Unit>? = null,
        onSave: () -> Boolean
    ) {
        val builder = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .setPositiveButton(positiveText, null)
            .setNegativeButton("Cancel", null)
        neutral?.let { (label, action) -> builder.setNeutralButton(label) { _, _ -> action() } }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (onSave()) dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun setupDropdown(view: MaterialAutoCompleteTextView, options: List<String>, selected: String) {
        view.setSimpleItems(options.toTypedArray())
        view.setText(if (selected in options) selected else options.last(), false)
    }

    private fun toast(message: String) = Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

    /** Dropdown of the user's geotagged places with a leading "No place" option. */
    private class PlacePicker(
        private val view: MaterialAutoCompleteTextView,
        private val places: List<LocationEntity>,
        selectedId: Long?
    ) {
        private val labels = listOf(NO_PLACE) + places.map { it.name }

        init {
            view.setAdapter(ArrayAdapter(view.context, android.R.layout.simple_list_item_1, labels))
            val selected = places.firstOrNull { it.id == selectedId }
            view.setText(selected?.name ?: NO_PLACE, false)
        }

        fun selectedPlace(): LocationEntity? = places.firstOrNull { it.name == view.text.toString() }

        companion object { const val NO_PLACE = "No place" }
    }
}
