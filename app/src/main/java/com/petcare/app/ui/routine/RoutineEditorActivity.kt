package com.petcare.app.ui.routine

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.petcare.app.R
import com.petcare.app.data.model.RoutineEntity
import com.petcare.app.data.model.TaskEntity
import com.petcare.app.databinding.ActivityRoutineEditorBinding
import com.petcare.app.databinding.ItemRoutineStepBinding
import com.petcare.app.ui.dashboard.PetViewModel
import com.petcare.app.ui.dashboard.PetViewModelFactory
import com.petcare.app.util.CareOptions
import com.petcare.app.util.Constants
import com.petcare.app.util.TimeUtils
import kotlinx.coroutines.launch

/**
 * Create or edit a pet care routine: a name, a repeat schedule and a list of timed steps.
 * Pass EXTRA_PET_ID to create, or EXTRA_ROUTINE_ID to edit an existing routine.
 */
class RoutineEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoutineEditorBinding
    private val viewModel: PetViewModel by viewModels { PetViewModelFactory.from(this) }

    private var petId: Long = -1L
    private var existingRoutine: RoutineEntity? = null
    private val stepRows = mutableListOf<StepRow>()

    /** One editable step card; [taskId] is 0 for a new step. */
    private inner class StepRow(val binding: ItemRoutineStepBinding, val taskId: Long, val completedOn: String) {
        var time: String = "08:00"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarRoutine.setNavigationOnClickListener { finish() }
        setupDayChips()
        binding.chipGroupRepeat.setOnCheckedStateChangeListener { _, _ -> updateDaysVisibility() }
        binding.btnAddStep.setOnClickListener { addStepRow(null) }
        binding.btnSaveRoutine.setOnClickListener { saveRoutine() }
        binding.btnDeleteRoutine.setOnClickListener { confirmDelete() }

        val routineId = intent.getLongExtra(Constants.EXTRA_ROUTINE_ID, -1L)
        petId = intent.getLongExtra(Constants.EXTRA_PET_ID, -1L)

        lifecycleScope.launch {
            if (routineId != -1L) {
                val routine = viewModel.getRoutineById(routineId) ?: run { finish(); return@launch }
                existingRoutine = routine
                petId = routine.petId
                bindExisting(routine, viewModel.getTasksForRoutine(routineId))
            } else {
                if (petId == -1L) { finish(); return@launch }
                addStepRow(null)
            }
            binding.tvRoutinePet.text = "For ${viewModel.getPetById(petId)?.name ?: "your pet"}"
        }
    }

    private fun setupDayChips() {
        TimeUtils.dayShortNames.forEachIndexed { index, name ->
            val chip = (layoutInflater.inflate(R.layout.item_day_chip, binding.chipGroupDays, false) as Chip).apply {
                id = View.generateViewId()
                text = name
                tag = index + 1 // Calendar.DAY_OF_WEEK
            }
            binding.chipGroupDays.addView(chip)
        }
    }

    private fun updateDaysVisibility() {
        binding.chipGroupDays.visibility = if (binding.chipRepeatWeekly.isChecked) View.VISIBLE else View.GONE
    }

    private fun bindExisting(routine: RoutineEntity, steps: List<TaskEntity>) {
        binding.toolbarRoutine.title = "Edit Care Routine"
        binding.btnDeleteRoutine.visibility = View.VISIBLE
        binding.etRoutineName.setText(routine.name)
        if (routine.repeatType == RoutineEntity.REPEAT_WEEKLY) {
            binding.chipRepeatWeekly.isChecked = true
            val days = routine.days()
            for (i in 0 until binding.chipGroupDays.childCount) {
                val chip = binding.chipGroupDays.getChildAt(i) as Chip
                chip.isChecked = (chip.tag as Int) in days
            }
        }
        updateDaysVisibility()
        steps.forEach { addStepRow(it) }
        if (steps.isEmpty()) addStepRow(null)
    }

    private fun addStepRow(task: TaskEntity?) {
        val stepBinding = ItemRoutineStepBinding.inflate(layoutInflater, binding.layoutSteps, false)
        val row = StepRow(stepBinding, task?.id ?: 0L, task?.completedOn ?: "")
        row.time = task?.time ?: (stepRows.lastOrNull()?.time ?: "08:00")

        stepBinding.etStepTitle.setText(task?.title)
        stepBinding.etStepNotes.setText(task?.notes)
        stepBinding.actvStepCategory.setSimpleItems(CareOptions.taskCategories.toTypedArray())
        stepBinding.actvStepCategory.setText(task?.category ?: CareOptions.CATEGORY_FEEDING, false)
        stepBinding.etStepTime.setText(TimeUtils.formatTime(row.time))
        stepBinding.etStepTime.setOnClickListener {
            TimeUtils.pickTime(supportFragmentManager, row.time, "Step time") {
                row.time = it
                stepBinding.etStepTime.setText(TimeUtils.formatTime(it))
            }
        }
        stepBinding.btnRemoveStep.setOnClickListener {
            binding.layoutSteps.removeView(stepBinding.root)
            stepRows.remove(row)
        }

        stepRows.add(row)
        binding.layoutSteps.addView(stepBinding.root)
    }

    private fun saveRoutine() {
        val name = binding.etRoutineName.text.toString().trim()
        binding.tilRoutineName.error = null
        if (name.isEmpty()) {
            binding.tilRoutineName.error = "Give the routine a name"
            return
        }

        val weekly = binding.chipRepeatWeekly.isChecked
        val days = (0 until binding.chipGroupDays.childCount)
            .map { binding.chipGroupDays.getChildAt(it) as Chip }
            .filter { it.isChecked }
            .map { it.tag as Int }
        if (weekly && days.isEmpty()) {
            Toast.makeText(this, "Choose at least one day", Toast.LENGTH_SHORT).show()
            return
        }

        var valid = true
        val steps = stepRows.map { row ->
            val title = row.binding.etStepTitle.text.toString().trim()
            row.binding.tilStepTitle.error = if (title.isEmpty()) "Required" else null
            if (title.isEmpty()) valid = false
            TaskEntity(
                id = row.taskId,
                petId = petId,
                routineId = existingRoutine?.id ?: 0L,
                title = title,
                category = row.binding.actvStepCategory.text.toString(),
                time = row.time,
                notes = row.binding.etStepNotes.text.toString().trim(),
                completedOn = row.completedOn
            )
        }
        if (!valid) return
        if (steps.isEmpty()) {
            Toast.makeText(this, "Add at least one step", Toast.LENGTH_SHORT).show()
            return
        }

        val routine = (existingRoutine ?: RoutineEntity(petId = petId, name = name)).copy(
            name = name,
            repeatType = if (weekly) RoutineEntity.REPEAT_WEEKLY else RoutineEntity.REPEAT_DAILY,
            daysOfWeek = if (weekly) days.sorted().joinToString(",") else ""
        )
        lifecycleScope.launch {
            viewModel.saveRoutine(routine, steps)
            Toast.makeText(this@RoutineEditorActivity,
                if (existingRoutine == null) "Routine created" else "Routine updated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun confirmDelete() {
        val routine = existingRoutine ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete \"${routine.name}\"?")
            .setMessage("The routine and all of its steps will be removed.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteRoutine(com.petcare.app.ui.dashboard.RoutineSummary(routine, emptyList())).join()
                    Toast.makeText(this@RoutineEditorActivity, "Routine deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
