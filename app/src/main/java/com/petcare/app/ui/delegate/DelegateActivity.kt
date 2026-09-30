package com.petcare.app.ui.delegate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.petcare.app.data.local.AppDatabase
import com.petcare.app.data.model.PetEntity
import com.petcare.app.data.model.TaskEntity
import com.petcare.app.data.repository.PetRepository
import com.petcare.app.databinding.ActivityDelegateBinding
import com.petcare.app.ui.dashboard.PetViewModel
import com.petcare.app.ui.dashboard.PetViewModelFactory
import com.petcare.app.util.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DelegateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDelegateBinding
    private var selectedPet: PetEntity? = null
    private var activeTasks: List<TaskEntity> = emptyList()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDelegateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sm = SessionManager(this)
        viewModel.setUserId(sm.getUserId())

        setupPetSpinner()
        setupStateObservation()
        setupClickListeners()
    }

    private fun setupPetSpinner() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allPets.collectLatest { pets ->
                    val petNames = pets.map { it.name }
                    val adapter = ArrayAdapter(this@DelegateActivity, android.R.layout.simple_dropdown_item_1line, petNames)
                    binding.actvSelectPet.setAdapter(adapter)

                    binding.actvSelectPet.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                        selectedPet = pets[position]
                        viewModel.selectPet(pets[position].id)
                    }
                }
            }
        }
    }

    private fun setupStateObservation() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tasksForSelectedPet.collect { tasks ->
                    activeTasks = tasks.filter { !it.isCompleted }
                    updatePreview()
                }
            }
        }
    }

    private fun updatePreview() {
        val pet = selectedPet
        if (pet == null) {
            binding.tvPayloadPreview.text = "Choose a companion to review instructions..."
            return
        }

        val sb = StringBuilder()
        sb.append("Hi! Here are the care instructions for ${pet.name}:\n\n")
        
        if (activeTasks.isNotEmpty()) {
            sb.append("Pending Tasks:\n")
            activeTasks.take(5).forEach { task ->
                sb.append("• ${task.title} [${task.category}]\n")
            }
        } else {
            sb.append("No pending tasks for today.\n")
        }

        if (pet.dietaryPrefs.isNotEmpty()) {
            sb.append("\nDietary Notes:\n${pet.dietaryPrefs}\n")
        }

        sb.append("\nSent via PetCare 🐾")
        binding.tvPayloadPreview.text = sb.toString()
    }

    private fun setupClickListeners() {
        binding.btnSendSms.setOnClickListener {
            val phone = binding.etCaregiverPhone.text.toString().trim()
            val message = binding.tvPayloadPreview.text.toString()

            if (selectedPet == null) {
                Toast.makeText(this, "Please select a pet first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone")
                putExtra("sms_body", message)
            }
            startActivity(intent)
        }
    }
}