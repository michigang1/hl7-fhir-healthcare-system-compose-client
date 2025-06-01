package presentation.state

import data.model.DiagnosisDto

/**
 * Represents the state of the diagnosis screen.
 */
data class DiagnosisState(
    val diagnosesForPatientList: List<DiagnosisDto> = emptyList(),
    val allDiagnosesList: List<DiagnosisDto> = emptyList(),
    val selectedDiagnosis: DiagnosisDto? = null,
    val isEditing: Boolean = false,
    val draftDiagnosis: DiagnosisDto? = null,
    val showAddOrEditDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPatientIdForContext: Long? = null
)