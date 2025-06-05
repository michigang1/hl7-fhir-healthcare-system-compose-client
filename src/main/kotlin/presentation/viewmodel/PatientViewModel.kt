package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.repository.PatientRepository
import data.model.PatientDto
import data.model.PatientRequest
import data.sync.SynchronizationManager
import data.sync.SyncStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import presentation.state.PatientState
import java.io.IOException

/**
 * ViewModel for the patient screen.
 */
class PatientViewModel(
    private val patientRepository: PatientRepository,
    private val synchronizationManager: SynchronizationManager,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    var state by mutableStateOf(PatientState())
        private set

    private val viewModelScope = CoroutineScope(mainDispatcher + SupervisorJob())

    private companion object {
        const val NEW_PATIENT_ID = 0L
    }

    init {
        loadPatients()

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
                    loadPatients()
                    state = state.copy(
                        hasPendingChanges = false,
                        showSyncNotification = false
                    )
                }
            }
        }
    }

    fun loadPatients() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val newPatientsList = patientRepository.getAllPatients()
                state = state.copy(patientsList = newPatientsList, isLoading = false)
            } catch (e: IOException) {
                state = state.copy(errorMessage = e.message, isLoading = false)
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Unexpected exception while loading patients: ${e.message}",
                    isLoading = false
                )
                e.printStackTrace()
            }
        }
    }

    fun selectPatient(patientId: Long) {
        if (state.isEditing) return

        val patientFromList = state.patientsList.find { it.id == patientId }
        if (patientFromList != null) {
            state = state.copy(
                selectedPatient = patientFromList,
                draftPatient = null
            )
        }
    }

    private fun loadPatientDetails(patientId: Long) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val loadedPatient = patientRepository.getPatientById(patientId)
                state = state.copy(selectedPatient = loadedPatient, isLoading = false)
            } catch (e: IOException) {
                state = state.copy(
                    errorMessage = "Error loading patient details: ${e.message}",
                    isLoading = false
                )
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Exception while loading patient details: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun startEditing() {
        val currentSelected = state.selectedPatient ?: return
        if (state.isEditing) return

        state = state.copy(
            isEditing = true,
            draftPatient = currentSelected.copy()
        )
    }

    fun saveChanges() {
        val patientToSave = state.draftPatient ?: return

        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val patientRequest = PatientRequest(
                    id = patientToSave.id,
                    name = patientToSave.name,
                    surname = patientToSave.surname,
                    roomNo = patientToSave.roomNo,
                    dateOfBirth = patientToSave.dateOfBirth,
                    gender = patientToSave.gender,
                    address = patientToSave.address,
                    email = patientToSave.email,
                    phone = patientToSave.phone,
                    identifier = patientToSave.identifier,
                    organizationId = patientToSave.organizationId
                )

                val savedPatientDto = if (patientToSave.id == NEW_PATIENT_ID) {
                    patientRepository.createPatient(patientRequest)
                } else {
                    patientRepository.updatePatient(patientToSave.id, patientRequest)
                }

                val newPatientsList = patientRepository.getAllPatients()

                var finalSelectedPatient = state.selectedPatient
                if (patientToSave.id == NEW_PATIENT_ID || state.selectedPatient?.id == patientToSave.id) {
                    finalSelectedPatient = savedPatientDto
                }

                state = state.copy(
                    patientsList = newPatientsList,
                    selectedPatient = finalSelectedPatient,
                    isEditing = false,
                    draftPatient = null,
                    showAddPatientDialog = false,
                    isLoading = false,
                    errorMessage = null
                )

                // Check for pending changes after successful save
                checkForPendingChanges()
            } catch (e: IOException) {
                state = state.copy(
                    errorMessage = "Error while saving or updating list: ${e.message}",
                    isLoading = false
                )

                // Check for pending changes after network error
                // This is important because the repository will store changes locally
                checkForPendingChanges()
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Exception while saving: ${e.message}",
                    isLoading = false
                )

                // Check for pending changes after other errors
                // This is important because the repository might store changes locally
                checkForPendingChanges()
            }
        }
    }

    fun cancelEditing() {
        if (state.draftPatient?.id == NEW_PATIENT_ID) {
            state = state.copy(
                isEditing = false,
                draftPatient = null,
                showAddPatientDialog = false
            )
        } else {
            state = state.copy(
                isEditing = false,
                draftPatient = null
            )
        }
    }

    fun updateDraftPatient(updater: (PatientDto) -> PatientDto) {
        state.draftPatient?.let {
            state = state.copy(draftPatient = updater(it))
        }
    }

    fun openAddPatientDialog() {
        state = state.copy(showAddPatientDialog = true)
    }

    fun closeAddPatientDialog() {
        state = state.copy(
            showAddPatientDialog = false,
        )
    }

    fun addPatient(patientDataFromDialog: PatientDto) {
        state = state.copy(draftPatient = patientDataFromDialog.copy(id = NEW_PATIENT_ID))
        saveChanges()
    }

    fun deletePatient(patientId: Long) {
        if (patientId == NEW_PATIENT_ID) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val success = patientRepository.deletePatient(patientId)
                if (success) {
                    val newPatientsList = patientRepository.getAllPatients()
                    var newSelectedPatient = state.selectedPatient
                    if (state.selectedPatient?.id == patientId) {
                        newSelectedPatient = null
                        state = state.copy(isEditing = false, draftPatient = null)
                    }
                    state = state.copy(
                        patientsList = newPatientsList,
                        selectedPatient = newSelectedPatient,
                        isLoading = false,
                        errorMessage = null
                    )

                    // Check for pending changes after successful delete
                    checkForPendingChanges()
                } else {
                    state = state.copy(
                        errorMessage = "Error deleting patient",
                        isLoading = false
                    )
                }
            } catch (e: IOException) {
                state = state.copy(
                    errorMessage = "Error while deleting or updating list: ${e.message}",
                    isLoading = false
                )

                // Check for pending changes after network error
                // This is important because the repository will mark for deletion locally
                checkForPendingChanges()
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Exception while deleting: ${e.message}",
                    isLoading = false
                )

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
        val patientsToSync = withContext(ioDispatcher) {
            patientRepository.getPatientsToSync()
        }

        state = state.copy(
            hasPendingChanges = patientsToSync.isNotEmpty(),
            showSyncNotification = patientsToSync.isNotEmpty() && state.isNetworkAvailable
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

    fun onCleared() {
        viewModelScope.cancel()
    }
}
