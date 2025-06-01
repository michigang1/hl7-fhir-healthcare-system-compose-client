package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.remote.services.PatientApiService
import data.model.PatientDto
import data.model.PatientRequest
import data.model.PatientResponse
import kotlinx.coroutines.*
import presentation.state.PatientState
import java.io.IOException

/**
 * ViewModel for the patient screen.
 */
class PatientViewModel(
    private val patientApiService: PatientApiService,
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
    }

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

    private fun PatientDto.toPatientRequest(): PatientRequest {
        return PatientRequest(
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

    fun loadPatients() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val newPatientsList = fetchAndMapPatients()
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
                val response = withContext(ioDispatcher) {
                    patientApiService.getPatientById(patientId)
                }
                if (response.isSuccessful) {
                    val loadedPatient = response.body()?.toPatientDto()
                    state = state.copy(selectedPatient = loadedPatient, isLoading = false)
                } else {
                    state = state.copy(
                        errorMessage = "Error loading patient details: ${response.code()} ${response.message()}",
                        isLoading = false
                    )
                }
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
                val response =
                    if (patientToSave.id == NEW_PATIENT_ID) {
                        withContext(ioDispatcher) {
                            patientApiService.createPatient(patientToSave.toPatientRequest())
                        }
                    } else {
                        withContext(ioDispatcher) {
                            patientApiService.updatePatient(patientToSave.id, patientToSave.toPatientRequest())
                        }
                    }

                if (response.isSuccessful) {
                    val savedPatientDto = response.body()?.toPatientDto()
                    val newPatientsList = fetchAndMapPatients()

                    var finalSelectedPatient = state.selectedPatient
                    if (savedPatientDto != null) {
                        if (patientToSave.id == NEW_PATIENT_ID || state.selectedPatient?.id == patientToSave.id) {
                            finalSelectedPatient = savedPatientDto
                        }
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
                } else {
                    state = state.copy(
                        errorMessage = "Error saving: ${response.code()} ${response.message()}",
                        isLoading = false
                    )
                }
            } catch (e: IOException) {
                state = state.copy(
                    errorMessage = "Error while saving or updating list: ${e.message}",
                    isLoading = false
                )
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Exception while saving: ${e.message}",
                    isLoading = false
                )
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
                val response = withContext(ioDispatcher) {
                    patientApiService.deletePatient(patientId)
                }
                if (response.isSuccessful) {
                    val newPatientsList = fetchAndMapPatients()
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
                } else {
                    state = state.copy(
                        errorMessage = "Error deleting: ${response.code()} ${response.message()}",
                        isLoading = false
                    )
                }
            } catch (e: IOException) {
                state = state.copy(
                    errorMessage = "Error while deleting or updating list: ${e.message}",
                    isLoading = false
                )
            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Exception while deleting: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun clearErrorMessage() {
        state = state.copy(errorMessage = null)
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}