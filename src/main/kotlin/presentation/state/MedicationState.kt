package presentation.state

import data.model.MedicationDto
import data.sync.SyncStatus

/**
 * Represents the state of the medication screen.
 */
data class MedicationState(
    val medicationsForPatientList: List<MedicationDto> = emptyList(),
    val selectedMedication: MedicationDto? = null,
    val isEditing: Boolean = false,
    val draftMedication: MedicationDto? = null,
    val showAddOrEditDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPatientId: Long? = null, // for context
    val isNetworkAvailable: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val hasPendingChanges: Boolean = false,
    val showSyncNotification: Boolean = false
)
