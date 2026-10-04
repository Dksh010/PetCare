package com.petcare.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.model.*
import com.petcare.app.data.repository.PetRepository
import com.petcare.app.ui.adapter.PlanDisplayItem
import com.petcare.app.util.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** A routine together with its steps. */
data class RoutineSummary(val routine: RoutineEntity, val steps: List<TaskEntity>)

/** One row of today's checklist. */
data class ChecklistItem(val task: TaskEntity, val routineName: String, val isDone: Boolean)

private data class Today(val date: String, val dayOfWeek: Int)

@OptIn(ExperimentalCoroutinesApi::class)
class PetViewModel(private val repository: PetRepository) : ViewModel() {

    private val _userId = MutableStateFlow<Long?>(null)

    val allPets: StateFlow<List<PetEntity>> = _userId
        .flatMapLatest { id -> if (id != null) repository.getAllPets(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPlaces: StateFlow<List<LocationEntity>> = _userId
        .flatMapLatest { id -> if (id != null) repository.getLocationsForUser(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPetId = MutableStateFlow<Long?>(null)
    val selectedPetId: StateFlow<Long?> = _selectedPetId

    /** The selected pet's current row (refreshes after edits). */
    val selectedPet: StateFlow<PetEntity?> = combine(allPets, _selectedPetId) { pets, id ->
        pets.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val today = MutableStateFlow(Today(TimeUtils.todayKey(), TimeUtils.todayDayOfWeek()))

    private fun <T> forSelectedPet(source: (Long) -> Flow<List<T>>): StateFlow<List<T>> = _selectedPetId
        .flatMapLatest { petId -> if (petId != null) source(petId) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val routines = forSelectedPet { repository.getRoutinesForPet(it) }
    private val tasks = forSelectedPet { repository.getTasksForPet(it) }
    val remindersForSelectedPet = forSelectedPet { repository.getRemindersForPet(it) }
    val appointmentsForSelectedPet = forSelectedPet { repository.getAppointmentsForPet(it) }
    val vaccinationsForSelectedPet = forSelectedPet { repository.getVaccinationsForPet(it) }
    val activityLogsForSelectedPet = forSelectedPet { repository.getActivityLogsForPet(it) }

    val routineSummaries: StateFlow<List<RoutineSummary>> = combine(routines, tasks) { rs, ts ->
        rs.map { r -> RoutineSummary(r, ts.filter { it.routineId == r.id }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Steps of every routine scheduled for today, sorted by time; completion resets each day. */
    val todayChecklist: StateFlow<List<ChecklistItem>> = combine(routines, tasks, today) { rs, ts, day ->
        val scheduled = rs.filter { it.isScheduledOn(day.dayOfWeek) }.associateBy { it.id }
        ts.filter { it.routineId in scheduled }
            .sortedBy { it.time }
            .map { ChecklistItem(it, scheduled.getValue(it.routineId).name, it.isDoneOn(day.date)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taskProgress: StateFlow<Pair<Int, Int>> = todayChecklist
        .map { list -> Pair(list.count { it.isDone }, list.size) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0, 0))

    // --- Care tab sections ---
    val appointmentItems: StateFlow<List<PlanDisplayItem>> = combine(appointmentsForSelectedPet, savedPlaces) { appointments, places ->
        val placeNames = places.associate { it.id to it.name }
        // Upcoming first (soonest at the top), then completed/past ones.
        appointments
            .sortedWith(compareBy<AppointmentEntity> { it.isCompleted }.thenBy { it.dateMillis })
            .map { PlanDisplayItem.Appointment(it, it.locationId?.let(placeNames::get)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminderItems: StateFlow<List<PlanDisplayItem>> = remindersForSelectedPet
        .map { list -> list.sortedWith(compareBy({ it.isCompleted }, { it.time })).map { PlanDisplayItem.Reminder(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaccinationItems: StateFlow<List<PlanDisplayItem>> = vaccinationsForSelectedPet
        .map { list -> list.map { PlanDisplayItem.Vaccination(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Home "Upcoming" ---
    /** The next appointment that is not completed and not in the past, with its tagged place name. */
    val nextAppointment: StateFlow<PlanDisplayItem.Appointment?> = appointmentItems
        .map { items ->
            val now = System.currentTimeMillis()
            items.filterIsInstance<PlanDisplayItem.Appointment>()
                .firstOrNull { !it.entity.isCompleted && it.entity.dateMillis >= now }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** The earliest reminder that has not been ticked off. */
    val nextReminder: StateFlow<ReminderEntity?> = remindersForSelectedPet
        .map { list -> list.filter { !it.isCompleted }.minByOrNull { it.time } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- USER ACTIONS ---
    fun setUserId(id: Long) { _userId.value = id }
    suspend fun registerUser(user: UserEntity): Long = repository.insertUser(user)
    suspend fun getUserByEmail(email: String): UserEntity? = repository.getUserByEmail(email)
    suspend fun getUserById(userId: Long): UserEntity? = repository.getUserById(userId)
    suspend fun updateUser(user: UserEntity) = repository.updateUser(user)
    suspend fun deleteAccount(userId: Long) = repository.deleteAccount(userId)

    /** Call from onResume so the checklist rolls over to a new day. */
    fun refreshToday() {
        today.value = Today(TimeUtils.todayKey(), TimeUtils.todayDayOfWeek())
    }

    // --- PET ACTIONS ---
    suspend fun getPetById(petId: Long): PetEntity? = repository.getPetById(petId)
    fun selectPet(petId: Long?) { _selectedPetId.value = petId }
    fun addPet(pet: PetEntity) = viewModelScope.launch { repository.insertPet(pet) }
    fun updatePet(pet: PetEntity) = viewModelScope.launch { repository.updatePet(pet) }
    fun deletePet(pet: PetEntity) = viewModelScope.launch { repository.deletePet(pet) }

    // --- ROUTINE ACTIONS ---
    suspend fun getRoutineById(id: Long): RoutineEntity? = repository.getRoutineById(id)
    suspend fun getTasksForRoutine(id: Long): List<TaskEntity> = repository.getTasksForRoutine(id)
    suspend fun saveRoutine(routine: RoutineEntity, steps: List<TaskEntity>): Long = repository.saveRoutine(routine, steps)
    fun deleteRoutine(summary: RoutineSummary) = viewModelScope.launch { repository.deleteRoutine(summary.routine) }
    fun restoreRoutine(summary: RoutineSummary) = viewModelScope.launch {
        repository.restoreRoutine(summary.routine, summary.steps)
    }

    // --- TASK (ROUTINE STEP) ACTIONS ---
    fun updateTask(task: TaskEntity) = viewModelScope.launch { repository.updateTask(task) }
    fun setTaskDone(task: TaskEntity, done: Boolean) = viewModelScope.launch {
        repository.setTaskCompletedOn(task.id, if (done) today.value.date else "")
    }
    fun deleteTask(task: TaskEntity) = viewModelScope.launch { repository.deleteTask(task) }
    fun restoreTask(task: TaskEntity) = viewModelScope.launch { repository.insertTask(task) }

    // --- REMINDER ACTIONS ---
    fun saveReminder(reminder: ReminderEntity) = viewModelScope.launch {
        if (reminder.id == 0L) repository.insertReminder(reminder) else repository.updateReminder(reminder)
    }
    fun setReminderDone(reminder: ReminderEntity, done: Boolean) = viewModelScope.launch {
        repository.updateReminder(reminder.copy(isCompleted = done))
    }
    fun deleteReminder(reminder: ReminderEntity) = viewModelScope.launch { repository.deleteReminder(reminder) }
    fun restoreReminder(reminder: ReminderEntity) = viewModelScope.launch { repository.insertReminder(reminder) }

    // --- APPOINTMENT ACTIONS ---
    fun saveAppointment(appointment: AppointmentEntity) = viewModelScope.launch {
        if (appointment.id == 0L) repository.insertAppointment(appointment) else repository.updateAppointment(appointment)
    }
    fun setAppointmentDone(appointment: AppointmentEntity, done: Boolean) = viewModelScope.launch {
        val status = if (done) AppointmentEntity.STATUS_COMPLETED else AppointmentEntity.STATUS_SCHEDULED
        repository.updateAppointment(appointment.copy(status = status))
    }
    fun deleteAppointment(appointment: AppointmentEntity) = viewModelScope.launch { repository.deleteAppointment(appointment) }
    fun restoreAppointment(appointment: AppointmentEntity) = viewModelScope.launch { repository.insertAppointment(appointment) }

    // --- ACTIVITY ACTIONS ---
    fun addActivityLog(log: ActivityLogEntity) = viewModelScope.launch { repository.insertActivityLog(log) }

    // --- VACCINATION ACTIONS ---
    fun saveVaccination(vac: VaccinationEntity) = viewModelScope.launch {
        if (vac.id == 0L) repository.insertVaccination(vac) else repository.updateVaccination(vac)
    }
    fun deleteVaccination(vac: VaccinationEntity) = viewModelScope.launch { repository.deleteVaccination(vac) }
    fun restoreVaccination(vac: VaccinationEntity) = viewModelScope.launch { repository.insertVaccination(vac) }
}
