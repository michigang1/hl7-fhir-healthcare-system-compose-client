package presentation.state

import data.model.CarePlanDto
import data.model.CarePlanGoalDto
import data.model.CarePlanMeasureDto
import data.model.GoalDto
import data.model.MeasureDto
import data.model.PatientDto

/**
 * Represents the state of the care plan screen.
 */
data class CarePlanState(
    val patientsWithCarePlans: List<PatientDto> = emptyList(),
    val carePlansForPatientList: List<CarePlanDto> = emptyList(),
    val selectedCarePlan: CarePlanDto? = null,
    val isEditing: Boolean = false,
    val draftCarePlan: CarePlanDto? = null,
    val showAddOrEditDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPatientIdForContext: Long? = null,

    // Goals and measures for the patient
    val goalsForPatient: List<GoalDto> = emptyList(),
    val measuresForGoals: Map<Long, List<MeasureDto>> = emptyMap(),

    // Goal editing state
    val selectedGoal: CarePlanGoalDto? = null,
    val isEditingGoal: Boolean = false,
    val draftGoal: GoalDto? = null,
    val showGoalDialog: Boolean = false,

    // Measure editing state
    val selectedMeasure: CarePlanMeasureDto? = null,
    val isEditingMeasure: Boolean = false,
    val draftMeasure: MeasureDto? = null,
    val showMeasureDialog: Boolean = false
)
