package com.petcare.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.model.*
import com.petcare.app.data.repository.PetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PetViewModel(private val repository: PetRepository) : ViewModel() {

    private val _userId = MutableStateFlow<Long?>(null)
    val userId: StateFlow<Long?> = _userId

    @OptIn(ExperimentalCoroutinesApi::class)
    val allPets: StateFlow<List<PetEntity>> = _userId
        .flatMapLatest { id ->
            if (id != null) repository.getAllPets(id) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedPetId = MutableStateFlow<Long?>(null)
    val selectedPetId: StateFlow<Long?> = _selectedPetId

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksForSelectedPet: StateFlow<List<TaskEntity>> = _selectedPetId
        .flatMapLatest { petId ->
            if (petId != null) repository.getTasksForPet(petId) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- CARE PLAN DATA STREAMS ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val remindersForSelectedPet: StateFlow<List<ReminderEntity>> = _selectedPetId
        .flatMapLatest { petId ->
            if (petId != null) repository.getRemindersForPet(petId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val appointmentsForSelectedPet: StateFlow<List<AppointmentEntity>> = _selectedPetId
        .flatMapLatest { petId ->
            if (petId != null) repository.getAppointmentsForPet(petId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activityLogsForSelectedPet: StateFlow<List<ActivityLogEntity>> = _selectedPetId
        .flatMapLatest { petId ->
            if (petId != null) repository.getActivityLogsForPet(petId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val vaccinationsForSelectedPet: StateFlow<List<VaccinationEntity>> = _selectedPetId
        .flatMapLatest { petId ->
            if (petId != null) repository.getVaccinationsForPet(petId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val taskProgress: StateFlow<Pair<Int, Int>> = tasksForSelectedPet
        .flatMapLatest { tasks ->
            val total = tasks.size
            val completed = tasks.count { it.isCompleted }
            flowOf(Pair(completed, total))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Pair(0, 0)
        )

    // --- USER ACTIONS ---
    fun setUserId(id: Long) { _userId.value = id }
    suspend fun registerUser(user: UserEntity): Long = repository.insertUser(user)
    suspend fun getUserByEmail(email: String): UserEntity? = repository.getUserByEmail(email)
    suspend fun getUserById(userId: Long): UserEntity? = repository.getUserById(userId)
    suspend fun updateUser(user: UserEntity) = repository.updateUser(user)
    suspend fun deleteAccount(userId: Long) = repository.deleteAccount(userId)

    // --- PET ACTIONS ---
    suspend fun getPetById(petId: Long): PetEntity? = repository.getPetById(petId)
    fun selectPet(petId: Long) { _selectedPetId.value = petId }
    fun addPet(pet: PetEntity) = viewModelScope.launch {
        val newPetId = repository.insertPet(pet)
        selectPet(newPetId)
    }
    fun updatePet(pet: PetEntity) = viewModelScope.launch { repository.updatePet(pet) }
    fun deletePet(pet: PetEntity) = viewModelScope.launch { repository.deletePet(pet) }

    // --- TASK ACTIONS ---
    fun addTask(task: TaskEntity) = viewModelScope.launch { repository.insertTask(task) }
    fun updateTask(task: TaskEntity) = viewModelScope.launch { repository.updateTask(task) }
    fun toggleTaskCompletion(task: TaskEntity, isCompleted: Boolean) = viewModelScope.launch {
        repository.setTaskCompletionStatus(task.id, isCompleted)
    }
    fun deleteTask(task: TaskEntity) = viewModelScope.launch { repository.deleteTask(task) }

    // --- REMINDER ACTIONS ---
    fun addReminder(reminder: ReminderEntity) = viewModelScope.launch { repository.insertReminder(reminder) }
    fun toggleReminderCompletion(reminder: ReminderEntity) = viewModelScope.launch {
        repository.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
    }
    fun deleteReminder(reminder: ReminderEntity) = viewModelScope.launch { repository.deleteReminder(reminder) }

    // --- APPOINTMENT ACTIONS ---
    fun addAppointment(appointment: AppointmentEntity) = viewModelScope.launch { repository.insertAppointment(appointment) }
    fun deleteAppointment(appointment: AppointmentEntity) = viewModelScope.launch { repository.deleteAppointment(appointment) }

    // --- ACTIVITY ACTIONS ---
    fun addActivityLog(log: ActivityLogEntity) = viewModelScope.launch { repository.insertActivityLog(log) }
    fun deleteActivityLog(log: ActivityLogEntity) = viewModelScope.launch { repository.deleteActivityLog(log) }

    // --- VACCINATION ACTIONS ---
    fun addVaccination(vac: VaccinationEntity) = viewModelScope.launch { repository.insertVaccination(vac) }
    fun deleteVaccination(vac: VaccinationEntity) = viewModelScope.launch { repository.deleteVaccination(vac) }
}