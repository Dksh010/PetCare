package com.petcare.app.ui.analytics

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.petcare.app.data.local.AppDatabase
import com.petcare.app.data.repository.PetRepository
import com.petcare.app.databinding.ActivityPetAnalyticsBinding
import com.petcare.app.ui.dashboard.PetViewModel
import com.petcare.app.ui.dashboard.PetViewModelFactory
import com.petcare.app.util.Constants
import kotlinx.coroutines.launch

class PetAnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetAnalyticsBinding
    private var petId: Long = -1L

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
        binding = ActivityPetAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        petId = intent.getLongExtra(Constants.EXTRA_PET_ID, -1L)
        if (petId == -1L) {
            finish()
            return
        }

        setupUI()
        observeData()
    }

    private fun setupUI() {
        binding.rvActivityTimeline.layoutManager = LinearLayoutManager(this)
        // Set mock trend for visual if no data
        binding.activityChart.setData(listOf(0.3f, 0.6f, 0.2f, 0.8f, 0.5f, 0.9f, 0.7f))
    }

    private fun observeData() {
        viewModel.selectPet(petId)
        
        lifecycleScope.launch {
            val pet = viewModel.getPetById(petId)
            binding.tvAnalyticsPetName.text = "Insights for ${pet?.name ?: "Pet"}"
        }

        lifecycleScope.launch {
            viewModel.activityLogsForSelectedPet.collect { logs ->
                binding.tvTasksDoneVal.text = logs.size.toString()
                // Calculate simple consistency (mock calculation for demo)
                val consistency = if (logs.isNotEmpty()) "92%" else "0%"
                binding.tvConsistencyVal.text = consistency
            }
        }
    }
}