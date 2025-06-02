package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.CarePlanDto
import data.model.CarePlanRequest
import data.model.CarePlanMeasureDto
import data.model.CarePlanGoalDto
import data.model.GoalDto
import data.model.MeasureDto
import data.model.PatientDto
import data.model.PatientResponse
import data.remote.services.CarePlanApiService
import data.remote.services.PatientApiService
import kotlinx.coroutines.*
import presentation.state.CarePlanState
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import retrofit2.Response

/**
 * ViewModel for the care plan screen.
 */
class CarePlanViewModel(
    private val carePlanApiService: CarePlanApiService,
    private val patientApiService: PatientApiService,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<CarePlanState>(mainDispatcher, ioDispatcher) {
    override var state by mutableStateOf(CarePlanState())

    companion object {
        const val NEW_CAREPLAN_ID = 0L
        const val DEFAULT_PATIENT_ID = 1L
    }

    // --- Mappers ---
    private fun PatientResponse.toPatientDto(): PatientDto {
        return PatientDto(
            id = this.id,
            name = this.name,
            surname = this.surname,
            roomNo = this.roomNo,
            dateOfBirth = this.dateOfBirth,
            gender = this.gender,
            address = this.address,
            email = this.email,
            phone = this.phone,
            identifier = this.identifier,
            organizationId = this.organizationId
        )
    }

    private fun CarePlanDto.toCarePlanRequest(): CarePlanRequest {
        return CarePlanRequest(
            patientId = this.patientId,
            title = this.title,
            description = this.description,
            startDate = this.startDate,
            endDate = this.endDate,
            measures = this.measures.map { 
                data.model.CarePlanMeasureRequest(
                    name = it.name,
                    description = it.description,
                    scheduledDateTime = it.scheduledDateTime,
                    isCompleted = it.isCompleted
                ) 
            },
            goal = data.model.CarePlanGoalRequest(
                name = this.goal.name,
                description = this.goal.description,
                frequency = this.goal.frequency,
                duration = this.goal.duration
            )
        )
    }

    // --- API Calls ---
    fun loadCarePlansForPatient(patientId: Long) {
        println("DEBUG: Loading care plans for patient $patientId")
        state = state.copy(currentPatientIdForContext = patientId, isLoading = true, errorMessage = null)
        launchCoroutine {
            try {
                val measuresResponse = carePlanApiService.getCarePlanByPatientId(patientId)
                if (measuresResponse.isSuccessful) {
                    val measures = measuresResponse.body() ?: emptyList()
                    println("DEBUG: Loaded ${measures.size} measures for patient $patientId")

                    // Group measures by goalId and create CarePlanDto objects
                    val goalMeasuresMap = measures.groupBy { it.goalId }
                    println("DEBUG: Grouped measures into ${goalMeasuresMap.size} goals")
                    val carePlans = mutableListOf<CarePlanDto>()

                    // For each goal, create a CarePlanDto
                    goalMeasuresMap.forEach { (goalId, goalMeasures) ->
                        // Get the goal details
                        try {
                            val goalResponse = carePlanApiService.getGoalById(patientId, goalId)
                            if (goalResponse.isSuccessful) {
                                val goal = goalResponse.body()
                                if (goal != null) {
                                    // Create a CarePlanDto for this goal and its measures
                                    val carePlan = CarePlanDto(
                                        id = goalId, // Use goalId as carePlanId
                                        patientId = patientId,
                                        title = goal.name,
                                        description = goal.description,
                                        startDate = LocalDate.now(), // Default to today
                                        endDate = LocalDate.now().plusMonths(1), // Default to 1 month from today
                                        goal = CarePlanGoalDto(
                                            id = goalId,
                                            name = goal.name,
                                            description = goal.description,
                                            frequency = goal.frequency,
                                            duration = goal.duration
                                        ),
                                        measures = goalMeasures.map { measure ->
                                            CarePlanMeasureDto(
                                                id = measure.id ?: 0L,
                                                name = measure.name,
                                                description = measure.description,
                                                scheduledDateTime = measure.scheduledDateTime,
                                                isCompleted = measure.isCompleted
                                            )
                                        }
                                    )
                                    carePlans.add(carePlan)
                                }
                            }
                        } catch (e: Exception) {
                            println("Error loading goal $goalId: ${e.message}")
                        }
                    }

                    println("DEBUG: Created ${carePlans.size} care plans")

                    // Get all measures from the API response
                    val allMeasures = measures.map { it }
                    println("DEBUG: Total measures from API: ${allMeasures.size}")

                    // Create a map of goalId to measures
                    val measuresMap = mutableMapOf<Long, List<MeasureDto>>()
                    goalMeasuresMap.forEach { (goalId, goalMeasures) ->
                        measuresMap[goalId] = goalMeasures
                    }
                    println("DEBUG: Created measures map with ${measuresMap.size} entries")

                    // Get all goals from the API
                    val goalsResponse = carePlanApiService.getAllGoalsByPatient(patientId)
                    val goals = if (goalsResponse.isSuccessful) {
                        goalsResponse.body() ?: emptyList()
                    } else {
                        emptyList()
                    }
                    println("DEBUG: Loaded ${goals.size} goals for patient $patientId")

                    // For each goal, load its measures using getMeasuresByGoalId
                    goals.forEach { goal ->
                        goal.id?.let { goalId ->
                            // Only load measures if they're not already in the map
                            if (!measuresMap.containsKey(goalId)) {
                                try {
                                    val measuresResponse = carePlanApiService.getMeasuresByGoalId(patientId, goalId)
                                    if (measuresResponse.isSuccessful) {
                                        val measures = measuresResponse.body() ?: emptyList()
                                        println("DEBUG: Loaded ${measures.size} additional measures for goal $goalId")
                                        // Add the measures to the map for this goal
                                        measuresMap[goalId] = measures
                                    }
                                } catch (e: Exception) {
                                    println("Error loading additional measures for goal $goalId: ${e.message}")
                                }
                            }
                        }
                    }
                    println("DEBUG: After loading additional measures, measures map has ${measuresMap.size} entries")

                    // Update the state with all the data
                    state = state.copy(
                        carePlansForPatientList = carePlans,
                        goalsForPatient = goals,
                        measuresForGoals = measuresMap,
                        isLoading = false
                    )
                    println("DEBUG: Updated state with care plans, goals, and measures")
                } else {
                    state = state.copy(
                        errorMessage = "Error loading care plans: ${measuresResponse.code()} ${measuresResponse.message()}",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Error loading care plans: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun loadGoalsForPatient(patientId: Long) {
        println("DEBUG: Inside loadGoalsForPatient for patient $patientId")
        launchCoroutine {
            try {
                println("DEBUG: Fetching goals for patient $patientId")
                val goalsResponse = carePlanApiService.getAllGoalsByPatient(patientId)
                if (goalsResponse.isSuccessful) {
                    val goals = goalsResponse.body() ?: emptyList()
                    println("DEBUG: Loaded ${goals.size} goals for patient $patientId")
                    val measuresMap = mutableMapOf<Long, List<MeasureDto>>()

                    // For each goal, load its measures
                    goals.forEach { goal ->
                        goal.id?.let { goalId ->
                            // Load measures for this goal
                            try {
                                val measuresResponse = carePlanApiService.getMeasuresByGoalId(patientId, goalId)
                                if (measuresResponse.isSuccessful) {
                                    val measures = measuresResponse.body() ?: emptyList()
                                    println("DEBUG: Loaded ${measures.size} measures for goal $goalId")
                                    measuresMap[goalId] = measures
                                } else {
                                    println("DEBUG: Failed to load measures for goal $goalId: ${measuresResponse.code()} ${measuresResponse.message()}")
                                }
                            } catch (e: Exception) {
                                println("Error loading measures for goal $goalId: ${e.message}")
                            }
                        }
                    }

                    println("DEBUG: Updating state with ${goals.size} goals and ${measuresMap.size} measure entries")
                    println("DEBUG: Measures map contains goals with IDs: ${measuresMap.keys.joinToString()}")
                    println("DEBUG: Total measures across all goals: ${measuresMap.values.sumOf { it.size }}")

                    state = state.copy(
                        goalsForPatient = goals,
                        measuresForGoals = measuresMap,
                        isLoading = false
                    )

                    println("DEBUG: State updated. goalsForPatient size: ${state.goalsForPatient.size}, measuresForGoals size: ${state.measuresForGoals.size}")
                } else {
                    state = state.copy(
                        errorMessage = "Error loading goals: ${goalsResponse.code()} ${goalsResponse.message()}",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Error loading goals: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun saveCarePlan() {
        val carePlanToSave = state.draftCarePlan ?: return
        val isNew = carePlanToSave.id == NEW_CAREPLAN_ID
        val patientId = carePlanToSave.patientId

        launchCoroutine {
            executeApiCall(
                loadingState = { state.copy(isLoading = true, errorMessage = null) },
                errorState = { error -> state.copy(errorMessage = "Error saving care plan: $error", isLoading = false) },
                apiCall = {
                    if (isNew) {
                        carePlanApiService.createCarePlan(patientId, carePlanToSave.toCarePlanRequest())
                    } else {
                        carePlanApiService.updateCarePlan(
                            patientId,
                            carePlanToSave.id,
                            carePlanToSave.toCarePlanRequest()
                        )
                    }
                },
                onSuccess = { carePlan ->
                    refreshCarePlansForCurrentPatient()
                    state.copy(
                        isLoading = false,
                        showAddOrEditDialog = false,
                        isEditing = false,
                        draftCarePlan = null,
                        selectedCarePlan = carePlan
                    )
                }
            )
        }
    }

    fun deleteCarePlan(carePlanId: Long) {
        if (carePlanId == NEW_CAREPLAN_ID) return
        val patientId = state.currentPatientIdForContext ?: DEFAULT_PATIENT_ID

        launchCoroutine {
            executeApiCall(
                loadingState = { state.copy(isLoading = true, errorMessage = null) },
                errorState = { error -> state.copy(errorMessage = "Error deleting care plan: $error", isLoading = false) },
                apiCall = { carePlanApiService.deleteCarePlan(patientId, carePlanId) },
                onSuccess = { success ->
                    refreshCarePlansForCurrentPatient()
                    state.copy(
                        isLoading = false,
                        selectedCarePlan = if (state.selectedCarePlan?.id == carePlanId) null else state.selectedCarePlan,
                        draftCarePlan = null,
                        isEditing = false
                    )
                }
            )
        }
    }

    // --- Goal and Measure Operations ---
    fun createGoal(goalDto: GoalDto) {
        launchCoroutine {
            val patientId = state.currentPatientIdForContext ?: DEFAULT_PATIENT_ID
            executeApiCall(
                loadingState = { state.copy(isLoading = true, errorMessage = null) },
                errorState = { error -> state.copy(errorMessage = "Error creating goal: $error", isLoading = false) },
                apiCall = { carePlanApiService.createGoal(patientId, goalDto) },
                onSuccess = { goal ->
                    refreshCarePlansForCurrentPatient()
                    state.copy(isLoading = false)
                }
            )
        }
    }

    fun createMeasure(goalId: Long, measureDto: MeasureDto) {
        launchCoroutine {
            val patientId = state.currentPatientIdForContext ?: DEFAULT_PATIENT_ID
            executeApiCall(
                loadingState = { state.copy(isLoading = true, errorMessage = null) },
                errorState = { error -> state.copy(errorMessage = "Error creating measure: $error", isLoading = false) },
                apiCall = { carePlanApiService.createMeasure(patientId, goalId, measureDto) },
                onSuccess = { measure ->
                    refreshCarePlansForCurrentPatient()
                    state.copy(isLoading = false)
                }
            )
        }
    }

    fun getGoalById(goalId: Long) {
        launchCoroutine {
            val patientId = state.currentPatientIdForContext ?: DEFAULT_PATIENT_ID
            executeApiCall(
                loadingState = { state.copy(isLoading = true, errorMessage = null) },
                errorState = { error -> state.copy(errorMessage = "Error getting goal: $error", isLoading = false) },
                apiCall = { carePlanApiService.getGoalById(patientId, goalId) },
                onSuccess = { goal ->
                    // Handle the goal data as needed
                    state.copy(isLoading = false)
                }
            )
        }
    }

    fun getMeasureById(goalId: Long, measureId: Long) {
        launchCoroutine {
            val patientId = state.currentPatientIdForContext ?: DEFAULT_PATIENT_ID
            executeApiCall(
                loadingState = { state.copy(isLoading = true, errorMessage = null) },
                errorState = { error -> state.copy(errorMessage = "Error getting measure: $error", isLoading = false) },
                apiCall = { carePlanApiService.getMeasureById(patientId, goalId, measureId) },
                onSuccess = { measure ->
                    // Handle the measure data as needed
                    state.copy(isLoading = false)
                }
            )
        }
    }

    fun updateGoal(goalId: Long, goalDto: GoalDto) {
        launchCoroutine {
            val patientId = state.currentPatientIdForContext ?: DEFAULT_PATIENT_ID
            executeApiCall(
                loadingState = { state.copy(isLoading = true, errorMessage = null) },
                errorState = { error -> state.copy(errorMessage = "Error updating goal: $error", isLoading = false) },
                apiCall = { carePlanApiService.updateGoal(patientId, goalId, goalDto) },
                onSuccess = { goal ->
                    refreshCarePlansForCurrentPatient()
                    state.copy(isLoading = false)
                }
            )
        }
    }

    fun updateMeasure(goalId: Long, measureId: Long, measureDto: MeasureDto) {
        launchCoroutine {
            val patientId = state.currentPatientIdForContext ?: DEFAULT_PATIENT_ID
            executeApiCall(
                loadingState = { state.copy(isLoading = true, errorMessage = null) },
                errorState = { error -> state.copy(errorMessage = "Error updating measure: $error", isLoading = false) },
                apiCall = { carePlanApiService.updateMeasure(patientId, goalId, measureId, measureDto) },
                onSuccess = { measure ->
                    refreshCarePlansForCurrentPatient()
                    state.copy(isLoading = false)
                }
            )
        }
    }

    fun deleteGoal(goalId: Long) {
        launchCoroutine {
            val patientId = state.currentPatientIdForContext ?: DEFAULT_PATIENT_ID
            executeApiCall(
                loadingState = { state.copy(isLoading = true, errorMessage = null) },
                errorState = { error -> state.copy(errorMessage = "Error deleting goal: $error", isLoading = false) },
                apiCall = { carePlanApiService.deleteGoal(patientId, goalId) },
                onSuccess = { _ ->
                    refreshCarePlansForCurrentPatient()
                    state.copy(isLoading = false)
                }
            )
        }
    }

    fun deleteMeasure(goalId: Long, measureId: Long) {
        launchCoroutine {
            val patientId = state.currentPatientIdForContext ?: DEFAULT_PATIENT_ID
            executeApiCall(
                loadingState = { state.copy(isLoading = true, errorMessage = null) },
                errorState = { error -> state.copy(errorMessage = "Error deleting measure: $error", isLoading = false) },
                apiCall = { carePlanApiService.deleteMeasure(patientId, goalId, measureId) },
                onSuccess = { _ ->
                    refreshCarePlansForCurrentPatient()
                    state.copy(isLoading = false)
                }
            )
        }
    }

    fun getAllGoalsByPatient(patientId: Long) {
        launchCoroutine {
            executeApiCall(
                loadingState = { state.copy(isLoading = true, errorMessage = null) },
                errorState = { error -> state.copy(errorMessage = "Error getting goals: $error", isLoading = false) },
                apiCall = { carePlanApiService.getAllGoalsByPatient(patientId) },
                onSuccess = { goals ->
                    // Handle the goals data as needed
                    state.copy(isLoading = false)
                }
            )
        }
    }

    private fun refreshCarePlansForCurrentPatient() {
        state.currentPatientIdForContext?.let {
            loadCarePlansForPatient(it)
        }
    }

    /**
     * Fetches all patients and maps them to PatientDto objects.
     */
    private suspend fun fetchAndMapPatients(): List<PatientDto> {
        val response = withContext(ioDispatcher) {
            patientApiService.getAllPatients()
        }
        if (response.isSuccessful) {
            return response.body()?.map { it.toPatientDto() } ?: emptyList()
        } else {
            throw IOException("Error loading patients: ${response.code()} ${response.message()}")
        }
    }

    /**
     * Loads all patients with care plans.
     * If a patient doesn't have a care plan, an empty one is generated.
     */
    fun loadPatientsWithCarePlans() {
        launchCoroutine {
            try {
                state = state.copy(isLoading = true, errorMessage = null)

                // Fetch all patients
                val patients = fetchAndMapPatients()

                // Process each patient to check for care plans
                val patientsWithCarePlans = mutableListOf<PatientDto>()

                patients.forEach { patient ->
                    try {
                        // Get care plans for this patient
                        val carePlansResponse = carePlanApiService.getCarePlanByPatientId(patient.id)

                        if (carePlansResponse.isSuccessful) {
                            val carePlans = carePlansResponse.body() ?: emptyList()

                            // No need to create empty care plans automatically
                            // Users will create goals and measures explicitly

                            // Add patient to the list
                            patientsWithCarePlans.add(patient)
                        }
                    } catch (e: Exception) {
                        // Log error but continue with other patients
                        println("Error processing patient ${patient.id}: ${e.message}")
                    }
                }

                state = state.copy(
                    patientsWithCarePlans = patientsWithCarePlans,
                    isLoading = false
                )
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Error loading patients: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Creates an empty care plan for a patient who doesn't have one.
     */
    private suspend fun createEmptyCarePlan(patientId: Long) {
        val today = LocalDate.now()
        val endDate = today.plusMonths(1) // Default to 1 month duration

        val emptyCarePlan = CarePlanDto(
            id = NEW_CAREPLAN_ID,
            patientId = patientId,
            title = "Care Plan",
            description = "Generated care plan",
            startDate = today,
            endDate = endDate,
            measures = emptyList(),
            goal = CarePlanGoalDto(
                id = NEW_CAREPLAN_ID,
                name = "Default Goal",
                description = "Please update this goal",
                frequency = "Daily",
                duration = "1 month"
            )
        )

        try {
            carePlanApiService.createCarePlan(patientId, emptyCarePlan.toCarePlanRequest())
        } catch (e: Exception) {
            println("Error creating empty care plan for patient $patientId: ${e.message}")
        }
    }

    /**
     * Creates measures for each day in the specified date range for the given care plan.
     * This is used when the user selects a date range in the DateRangeDialog.
     */
    fun addMeasuresForDateRange(carePlanId: Long?, startDate: LocalDate, endDate: LocalDate) {
        if (carePlanId == null) return

        launchCoroutine {
            try {
                state = state.copy(isLoading = true, errorMessage = null)

                // Get the care plan
                val patientId = state.currentPatientIdForContext ?: DEFAULT_PATIENT_ID
                val carePlanResponse = carePlanApiService.getCarePlanById(patientId, carePlanId)
                if (!carePlanResponse.isSuccessful) {
                    state = state.copy(
                        errorMessage = "Error loading care plan: ${carePlanResponse.code()} ${carePlanResponse.message()}",
                        isLoading = false
                    )
                    return@launchCoroutine
                }

                val carePlan = carePlanResponse.body() ?: return@launchCoroutine
                val goalId = carePlan.goal.id

                // Create a measure for each day in the range
                var currentDate = startDate
                while (!currentDate.isAfter(endDate)) {
                    // Create a measure for this day
                    val measure = MeasureDto(
                        id = null,
                        goalId = goalId,
                        name = "Measure for ${currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                        description = "Generated measure for ${currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                        scheduledDateTime = LocalDateTime.of(currentDate, LocalDateTime.now().toLocalTime()),
                        isCompleted = false
                    )

                    try {
                        carePlanApiService.createMeasure(carePlan.patientId, goalId, measure)
                    } catch (e: Exception) {
                        println("Error creating measure for date $currentDate: ${e.message}")
                    }

                    // Move to the next day
                    currentDate = currentDate.plusDays(1)
                }

                // Refresh the care plan to show the new measures
                loadCarePlansForPatient(carePlan.patientId)

                state = state.copy(
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Error creating measures: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    // --- UI State Management ---
    fun selectCarePlan(carePlan: CarePlanDto?) {
        if (state.isEditing) return
        state = state.copy(selectedCarePlan = carePlan, draftCarePlan = null)
    }

    fun openAddCarePlanDialog(patientId: Long) {
        state = state.copy(
            showAddOrEditDialog = true,
            isEditing = false,
            draftCarePlan = CarePlanDto(
                id = NEW_CAREPLAN_ID,
                patientId = patientId,
                title = "",
                description = "",
                startDate = LocalDate.of(2023, 1, 1), // Default date
                endDate = LocalDate.of(2023, 1, 8), // Default date + 1 week
                measures = emptyList(),
                goal = CarePlanGoalDto(
                    id = NEW_CAREPLAN_ID,
                    name = "",
                    description = "",
                    frequency = "",
                    duration = ""
                )
            ),
            selectedCarePlan = null
        )
    }

    fun openEditCarePlanDialog(carePlan: CarePlanDto) {
        state = state.copy(
            showAddOrEditDialog = true,
            isEditing = true,
            draftCarePlan = carePlan.copy(),
            selectedCarePlan = carePlan
        )
    }

    fun closeAddOrEditCarePlanDialog() {
        state = state.copy(
            showAddOrEditDialog = false,
            isEditing = false,
            draftCarePlan = null
        )
    }

    fun updateDraftCarePlan(updater: (CarePlanDto) -> CarePlanDto) {
        state.draftCarePlan?.let {
            state = state.copy(draftCarePlan = updater(it))
        }
    }

    fun clearErrorMessage() {
        state = state.copy(errorMessage = null)
    }

    // --- Goal UI State Management ---
    fun selectGoal(goal: CarePlanGoalDto?) {
        if (state.isEditingGoal) return
        state = state.copy(selectedGoal = goal, draftGoal = null)
    }

    fun openAddGoalDialog() {
        val patientId = state.currentPatientIdForContext ?: DEFAULT_PATIENT_ID
        state = state.copy(
            showGoalDialog = true,
            isEditingGoal = false,
            draftGoal = GoalDto(
                id = null,
                patientId = patientId,
                name = "",
                description = "",
                frequency = "",
                duration = ""
            ),
            selectedGoal = null
        )
    }

    fun openEditGoalDialog(goal: CarePlanGoalDto) {
        val patientId = state.currentPatientIdForContext ?: DEFAULT_PATIENT_ID
        state = state.copy(
            showGoalDialog = true,
            isEditingGoal = true,
            draftGoal = GoalDto(
                id = goal.id,
                patientId = patientId,
                name = goal.name,
                description = goal.description,
                frequency = goal.frequency,
                duration = goal.duration
            ),
            selectedGoal = goal
        )
    }

    fun closeGoalDialog() {
        state = state.copy(
            showGoalDialog = false,
            isEditingGoal = false,
            draftGoal = null
        )
    }

    fun updateDraftGoal(updater: (GoalDto) -> GoalDto) {
        state.draftGoal?.let {
            state = state.copy(draftGoal = updater(it))
        }
    }

    fun saveGoal() {
        val goalToSave = state.draftGoal ?: return

        if (state.isEditingGoal) {
            goalToSave.id?.let { goalId ->
                updateGoal(goalId, goalToSave)
            }
        } else {
            createGoal(goalToSave)
        }

        closeGoalDialog()
    }

    // --- Measure UI State Management ---
    fun selectMeasure(measure: CarePlanMeasureDto?) {
        if (state.isEditingMeasure) return
        state = state.copy(selectedMeasure = measure, draftMeasure = null)
    }

    fun openAddMeasureDialog(goalId: Long) {
        state = state.copy(
            showMeasureDialog = true,
            isEditingMeasure = false,
            draftMeasure = MeasureDto(
                id = null,
                goalId = goalId,
                name = "",
                description = "",
                scheduledDateTime = LocalDateTime.now(),
                isCompleted = false
            ),
            selectedMeasure = null
        )
    }

    fun openEditMeasureDialog(measure: CarePlanMeasureDto, goalId: Long) {
        state = state.copy(
            showMeasureDialog = true,
            isEditingMeasure = true,
            draftMeasure = MeasureDto(
                id = measure.id,
                goalId = goalId,
                name = measure.name,
                description = measure.description,
                scheduledDateTime = measure.scheduledDateTime,
                isCompleted = measure.isCompleted
            ),
            selectedMeasure = measure
        )
    }

    fun closeMeasureDialog() {
        state = state.copy(
            showMeasureDialog = false,
            isEditingMeasure = false,
            draftMeasure = null
        )
    }

    fun updateDraftMeasure(updater: (MeasureDto) -> MeasureDto) {
        state.draftMeasure?.let {
            state = state.copy(draftMeasure = updater(it))
        }
    }

    fun saveMeasure() {
        val measureToSave = state.draftMeasure ?: return
        val goalId = measureToSave.goalId

        if (state.isEditingMeasure) {
            measureToSave.id?.let { measureId ->
                updateMeasure(goalId, measureId, measureToSave)
            }
        } else {
            createMeasure(goalId, measureToSave)
        }

        closeMeasureDialog()
    }

    override fun onCleared() {
        super.onCleared()
    }
}
