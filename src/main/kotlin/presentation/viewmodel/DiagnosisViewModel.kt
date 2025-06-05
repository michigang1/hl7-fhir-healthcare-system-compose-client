package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.DiagnosisDto
import data.model.DiagnosisRequest
import data.model.DiagnosisResponse
import data.repository.DiagnosisRepository
import data.sync.SynchronizationManager
import data.sync.SyncStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import presentation.state.DiagnosisState
import java.io.IOException
import java.time.LocalDate

/**
 * ViewModel for the diagnosis screen.
 */
class DiagnosisViewModel(
    private val diagnosisRepository: DiagnosisRepository,
    private val synchronizationManager: SynchronizationManager,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    var state by mutableStateOf(DiagnosisState())
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
                    refreshData()
                    state = state.copy(
                        hasPendingChanges = false,
                        showSyncNotification = false
                    )
                }
            }
        }
    }

    companion object {
        const val NEW_DIAGNOSIS_ID = 0L
    }

    // --- Mappers ---
    private fun DiagnosisResponse.toDiagnosisDto(): DiagnosisDto {
        return DiagnosisDto(
            id = this.id,
            patientId = this.patientId,
            diagnosisCode = this.code,
            isPrimary = this.isPrimary,
            description = this.description,
            date = this.diagnosedAt,
            prescribedBy = this.diagnosedBy
        )
    }

    private fun DiagnosisDto.toDiagnosisRequest(): DiagnosisRequest {
        return DiagnosisRequest(
            id = this.patientId,
            code = this.diagnosisCode,
            isPrimary = this.isPrimary,
            description = this.description,
            diagnosedAt = this.date,
            diagnosedBy = this.prescribedBy
        )
    }

    // --- API Calls ---
    fun loadDiagnosesForPatient(patientId: Long) {
        state = state.copy(currentPatientIdForContext = patientId)
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val diagnoses = diagnosisRepository.getDiagnosesForPatient(patientId)
                state = state.copy(
                    diagnosesForPatientList = diagnoses,
                    isLoading = false
                )
            } catch (e: IOException) {
                state = state.copy(errorMessage = e.message, isLoading = false)
            } catch (e: Exception) {
                state = state.copy(errorMessage = "Unexpected exception: ${e.message}", isLoading = false)
            }
        }
    }

    fun loadAllDiagnosesGlobally() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val diagnoses = diagnosisRepository.getAllDiagnoses()
                state = state.copy(allDiagnosesList = diagnoses, isLoading = false)
            } catch (e: IOException) {
                state = state.copy(errorMessage = "Network error: ${e.message}", isLoading = false)
            } catch (e: Exception) {
                state = state.copy(errorMessage = "Exception loading all diagnoses: ${e.message}", isLoading = false)
            }
        }
    }

    private fun refreshDiagnosesListForCurrentPatient() {
        state.currentPatientIdForContext?.let {
            loadDiagnosesForPatient(it)
        }
    }

    fun selectDiagnosis(diagnosis: DiagnosisDto?) {
        if (state.isEditing) return
        state = state.copy(selectedDiagnosis = diagnosis, draftDiagnosis = null)
    }

    fun openAddDiagnosisDialog(patientId: Long) {
        state = state.copy(
            showAddOrEditDialog = true,
            isEditing = false,
            draftDiagnosis = DiagnosisDto(
                id = NEW_DIAGNOSIS_ID,
                patientId = patientId,
                diagnosisCode = "",
                isPrimary = false,
                description = "",
                date = LocalDate.now(),
                prescribedBy = ""
            ),
            selectedDiagnosis = null
        )
    }

    fun openEditDiagnosisDialog(diagnosis: DiagnosisDto) {
        state = state.copy(
            showAddOrEditDialog = true,
            isEditing = true,
            draftDiagnosis = diagnosis.copy(),
            selectedDiagnosis = diagnosis
        )
    }

    fun closeAddOrEditDiagnosisDialog() {
        state = state.copy(
            showAddOrEditDialog = false,
            isEditing = false,
            draftDiagnosis = null
        )
    }

    fun updateDraftDiagnosis(updater: (DiagnosisDto) -> DiagnosisDto) {
        state.draftDiagnosis?.let {
            state = state.copy(draftDiagnosis = updater(it))
        }
    }

    fun saveDiagnosis() {
        val diagnosisToSave = state.draftDiagnosis ?: return
        val isNew = diagnosisToSave.id == NEW_DIAGNOSIS_ID

        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val savedDiagnosis = if (isNew) {
                    diagnosisRepository.createDiagnosis(diagnosisToSave.toDiagnosisRequest())
                } else {
                    diagnosisRepository.updateDiagnosis(
                        diagnosisToSave.patientId,
                        diagnosisToSave.id,
                        diagnosisToSave.toDiagnosisRequest()
                    )
                }

                refreshDiagnosesListForCurrentPatient()
                state = state.copy(
                    isLoading = false,
                    showAddOrEditDialog = false,
                    isEditing = false,
                    draftDiagnosis = null,
                    selectedDiagnosis = savedDiagnosis
                )

                // Check for pending changes after successful save
                checkForPendingChanges()
            } catch (e: IOException) {
                state = state.copy(errorMessage = "Network error: ${e.message}", isLoading = false)

                // Check for pending changes after network error
                // This is important because the repository will store changes locally
                checkForPendingChanges()
            } catch (e: Exception) {
                state = state.copy(errorMessage = "Exception saving diagnosis: ${e.message}", isLoading = false)

                // Check for pending changes after other errors
                // This is important because the repository might store changes locally
                checkForPendingChanges()
            }
        }
    }

    fun deleteDiagnosis(patientId: Long, diagnosisId: Long) {
        if (diagnosisId == NEW_DIAGNOSIS_ID) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val success = diagnosisRepository.deleteDiagnosis(patientId, diagnosisId)
                if (success) {
                    refreshDiagnosesListForCurrentPatient()
                    state = state.copy(
                        isLoading = false,
                        selectedDiagnosis = if (state.selectedDiagnosis?.id == diagnosisId) null else state.selectedDiagnosis,
                        draftDiagnosis = null,
                        isEditing = false
                    )
                } else {
                    state = state.copy(
                        errorMessage = "Error deleting diagnosis",
                        isLoading = false
                    )
                }
            } catch (e: IOException) {
                state = state.copy(errorMessage = "Network error: ${e.message}", isLoading = false)
            } catch (e: Exception) {
                state = state.copy(errorMessage = "Exception deleting diagnosis: ${e.message}", isLoading = false)
            }
        }
    }

    fun clearErrorMessage() {
        state = state.copy(errorMessage = null)
    }

    /**
     * Refreshes data based on the current context.
     */
    private fun refreshData() {
        state.currentPatientIdForContext?.let {
            loadDiagnosesForPatient(it)
        } ?: loadAllDiagnosesGlobally()
    }

    /**
     * Checks if there are pending changes that need to be synchronized.
     * This is called after operations that might create pending changes.
     */
    private suspend fun checkForPendingChanges() {
        val diagnosesToSync = withContext(ioDispatcher) {
            diagnosisRepository.getDiagnosesToSync()
        }

        state = state.copy(
            hasPendingChanges = diagnosesToSync.isNotEmpty(),
            showSyncNotification = diagnosesToSync.isNotEmpty() && state.isNetworkAvailable
        )
    }

    /**
     * Manually triggers synchronization.
     */
    fun triggerSynchronization() {
        if (state.syncStatus == SyncStatus.SYNCING) return

        synchronizationManager.triggerSynchronization()
        state = state.copy(showSyncNotification = false)
    }

    /**
     * Dismisses the synchronization notification.
     */
    fun dismissSyncNotification() {
        state = state.copy(showSyncNotification = false)
    }

    /**
     * Deletes all unsynchronized diagnoses from the local database.
     * This includes diagnoses with PENDING_CREATE, PENDING_UPDATE, or PENDING_DELETE status.
     * 
     * @return The number of diagnoses deleted
     */
    fun deleteUnsynchronizedDiagnoses() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val count = diagnosisRepository.deleteUnsynchronizedDiagnoses()
                refreshData()
                state = state.copy(
                    isLoading = false,
                    hasPendingChanges = false,
                    showSyncNotification = false
                )
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Error deleting unsynchronized diagnoses: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}
