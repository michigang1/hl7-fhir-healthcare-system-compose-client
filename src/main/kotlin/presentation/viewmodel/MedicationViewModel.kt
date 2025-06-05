package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.MedicationDto
import data.model.MedicationRequest
import data.model.MedicationResponse
import data.repository.MedicationRepository
import data.sync.SynchronizationManager
import data.sync.SyncStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import presentation.state.MedicationState
import java.io.IOException
import java.time.LocalDate

/**
 * ViewModel for the medication screen.
 */
class MedicationViewModel(
    private val medicationRepository: MedicationRepository,
    private val synchronizationManager: SynchronizationManager,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    var state by mutableStateOf(MedicationState())
        private set

    private val viewModelScope = CoroutineScope(mainDispatcher + SupervisorJob())

    init {
        // Check for pending changes on initialization
        viewModelScope.launch {
            checkForPendingChanges()
        }

        // Observe network connectivity
        viewModelScope.launch {
            synchronizationManager.isNetworkAvailable.collectLatest { isAvailable ->
                state = state.copy(isNetworkAvailable = isAvailable)

                // If network becomes available and we have pending changes, show sync notification
                if (isAvailable && state.hasPendingChanges) {
                    state = state.copy(showSyncNotification = true)
                }
            }
        }

        // Observe synchronization status
        viewModelScope.launch {
            synchronizationManager.syncStatus.collectLatest { status ->
                state = state.copy(syncStatus = status)

                // If sync completed successfully, refresh data and hide notification
                if (status == SyncStatus.COMPLETED) {
                    state.currentPatientId?.let { loadMedicationsForPatient(it) }
                    state = state.copy(
                        hasPendingChanges = false,
                        showSyncNotification = false
                    )
                }
            }
        }
    }

    companion object {
        const val NEW_MEDICATION_ID = 0L
    }

    // --- Mappers ---
    private fun MedicationResponse.toMedicationDto(): MedicationDto {
        return MedicationDto(
            id = this.id,
            patientId = this.patientId,
            medicationName = this.medicationName,
            dosage = this.dosage,
            frequency = this.frequency,
            startDate = this.startDate,
            endDate = this.endDate ?: LocalDate.now(),
            prescribedBy = this.prescribedBy,
            syncStatus = "SYNCED"
        )
    }

    fun loadMedicationsForPatient(patientId: Long) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null, currentPatientId = patientId)
            try {
                val medications = fetchAndMapMedications(patientId)
                state = state.copy(
                    medicationsForPatientList = medications,
                    isLoading = false
                )
            } catch (e: IOException) {
                state = state.copy(errorMessage = e.message, isLoading = false)
            } catch (e: Exception) {
                state = state.copy(errorMessage = "Unexpected exception: ${e.message}", isLoading = false)
            }
        }
    }

    private suspend fun fetchAndMapMedications(patientId: Long): List<MedicationDto> {
        return try {
            medicationRepository.getMedicationsForPatient(patientId)
        } catch (e: Exception) {
            println("Error fetching medications: ${e.message}")
            throw e
        }
    }

    fun openAddMedicationDialog(patientId: Long) {
        state = state.copy(
            showAddOrEditDialog = true,
            isEditing = false,
            draftMedication = MedicationDto(
                id = NEW_MEDICATION_ID,
                patientId = patientId,
                medicationName = "",
                dosage = "",
                frequency = "",
                startDate = LocalDate.now(),
                endDate = LocalDate.now(),
                prescribedBy = ""
            ),
            selectedMedication = null
        )
    }

    fun openEditMedicationDialog(medication: MedicationDto) {
        state = state.copy(
            showAddOrEditDialog = true,
            isEditing = true,
            draftMedication = medication.copy(),
            selectedMedication = medication
        )
    }

    fun closeAddOrEditMedicationDialog() {
        state = state.copy(
            showAddOrEditDialog = false,
            isEditing = false,
            draftMedication = null
        )
    }

    fun updateDraftMedication(updater: (MedicationDto) -> MedicationDto) {
        state.draftMedication?.let {
            state = state.copy(draftMedication = updater(it))
        }
    }

    fun saveMedication() {
        val medicationToSave = state.draftMedication ?: return
        val isNew = medicationToSave.id == NEW_MEDICATION_ID
        val patientId = medicationToSave.patientId

        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                // Create a MedicationRequest from the draft medication
                val medicationRequest = MedicationRequest(
                    patientId = medicationToSave.patientId,
                    medicationName = medicationToSave.medicationName,
                    dosage = medicationToSave.dosage,
                    frequency = medicationToSave.frequency,
                    startDate = medicationToSave.startDate,
                    endDate = medicationToSave.endDate,
                    prescribedBy = medicationToSave.prescribedBy
                )

                // Use the repository to create or update the medication
                val savedMedication = if (isNew) {
                    medicationRepository.createMedication(medicationRequest)
                } else {
                    medicationRepository.updateMedication(
                        patientId = medicationToSave.patientId,
                        id = medicationToSave.id,
                        medicationRequest = medicationRequest
                    )
                }

                // Reload the medications list and update the state
                loadMedicationsForPatient(patientId)
                state = state.copy(
                    isLoading = false,
                    showAddOrEditDialog = false,
                    isEditing = false,
                    draftMedication = null,
                    selectedMedication = savedMedication
                )

                // Check for pending changes after successful save
                checkForPendingChanges()
            } catch (e: IOException) {
                state = state.copy(errorMessage = "Network error: ${e.message}", isLoading = false)

                // Check for pending changes after network error
                // This is important because the repository will store changes locally
                checkForPendingChanges()
            } catch (e: Exception) {
                state = state.copy(errorMessage = "Exception saving medication: ${e.message}", isLoading = false)

                // Check for pending changes after other errors
                // This is important because the repository might store changes locally
                checkForPendingChanges()
            }
        }
    }

    fun deleteMedication(patientId: Long, medicationId: Long) {
        if (medicationId == NEW_MEDICATION_ID) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                // Use the repository to delete the medication
                val success = medicationRepository.deleteMedication(patientId, medicationId)

                if (success) {
                    // Reload the medications list and update the state
                    loadMedicationsForPatient(patientId)
                    state = state.copy(
                        isLoading = false,
                        selectedMedication = if (state.selectedMedication?.id == medicationId) null else state.selectedMedication,
                        draftMedication = null,
                        isEditing = false
                    )

                    // Check for pending changes after successful delete
                    checkForPendingChanges()
                } else {
                    state = state.copy(
                        errorMessage = "Error deleting medication",
                        isLoading = false
                    )
                }
            } catch (e: IOException) {
                state = state.copy(errorMessage = "Network error: ${e.message}", isLoading = false)

                // Check for pending changes after network error
                // This is important because the repository will mark for deletion locally
                checkForPendingChanges()
            } catch (e: Exception) {
                state = state.copy(errorMessage = "Exception deleting medication: ${e.message}", isLoading = false)

                // Check for pending changes after other errors
                // This is important because the repository might mark for deletion locally
                checkForPendingChanges()
            }
        }
    }

    fun clearErrorMessage() {
        state = state.copy(errorMessage = null)
    }

    /**
     * Checks if there are pending changes that need to be synchronized.
     * This is called after operations that might create pending changes.
     */
    private suspend fun checkForPendingChanges() {
        println("[DEBUG] MedicationViewModel.checkForPendingChanges() called")
        val medicationsToSync = withContext(ioDispatcher) {
            medicationRepository.getMedicationsToSync()
        }
        println("[DEBUG] MedicationViewModel.checkForPendingChanges() found ${medicationsToSync.size} medications to sync")

        val hasPendingChanges = medicationsToSync.isNotEmpty()
        val showSyncNotification = hasPendingChanges && state.isNetworkAvailable
        println("[DEBUG] MedicationViewModel.checkForPendingChanges() hasPendingChanges=$hasPendingChanges, showSyncNotification=$showSyncNotification, isNetworkAvailable=${state.isNetworkAvailable}")

        state = state.copy(
            hasPendingChanges = hasPendingChanges,
            showSyncNotification = showSyncNotification
        )
    }

    /**
     * Manually triggers synchronization.
     */
    fun triggerSynchronization() {
        println("[DEBUG] MedicationViewModel.triggerSynchronization() called")
        if (state.syncStatus == SyncStatus.SYNCING) {
            println("[DEBUG] MedicationViewModel.triggerSynchronization() - already syncing, returning")
            return
        }

        println("[DEBUG] MedicationViewModel.triggerSynchronization() - calling synchronizationManager.triggerSynchronization()")
        synchronizationManager.triggerSynchronization()
        println("[DEBUG] MedicationViewModel.triggerSynchronization() - hiding sync notification")
        state = state.copy(showSyncNotification = false)
    }

    /**
     * Dismisses the synchronization notification.
     */
    fun dismissSyncNotification() {
        state = state.copy(showSyncNotification = false)
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}
