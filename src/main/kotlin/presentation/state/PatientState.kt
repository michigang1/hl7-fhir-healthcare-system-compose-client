package presentation.state

import data.model.PatientDto

/**
 * Represents the state of the patient screen.
 */
data class PatientState(
    val patientsList: List<PatientDto> = emptyList(),
    val selectedPatient: PatientDto? = null,
    val isEditing: Boolean = false,
    val draftPatient: PatientDto? = null,
    val showAddPatientDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)