package presentation.state

import data.model.PatientDto
import data.sync.SyncStatus

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
    val errorMessage: String? = null,
    val isNetworkAvailable: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val hasPendingChanges: Boolean = false,
    val showSyncNotification: Boolean = false
)
