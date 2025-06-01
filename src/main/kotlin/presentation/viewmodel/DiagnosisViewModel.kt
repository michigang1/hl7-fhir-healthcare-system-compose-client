package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.DiagnosisDto
import data.model.DiagnosisRequest
import data.model.DiagnosisResponse
import data.remote.services.DiagnosisApiService
import kotlinx.coroutines.*
import presentation.state.DiagnosisState
import java.io.IOException
import java.time.LocalDate

/**
 * ViewModel for the diagnosis screen.
 */
class DiagnosisViewModel(
    private val diagnosisApiService: DiagnosisApiService,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    var state by mutableStateOf(DiagnosisState())
        private set

    private val viewModelScope = CoroutineScope(mainDispatcher + SupervisorJob())

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
            id =  this.patientId,
            code = this.diagnosisCode,
            isPrimary = this.isPrimary,
            description = this.description,
            diagnosedAt = this.date,
            diagnosedBy = this.prescribedBy
        )
    }

    // --- fetch and map ---
    private suspend fun fetchAndMapDiagnoses(patientId: Long): List<DiagnosisDto> {
        val response = withContext(ioDispatcher) {
            diagnosisApiService.getAllDiagnosesByPatient(patientId)
        }
        if (response.isSuccessful) {
            return response.body()?.map { it.toDiagnosisDto() } ?: emptyList()
        } else {
            throw IOException("Error loading diagnoses: ${response.code()} ${response.message()}")
        }
    }

    // --- API Calls ---
    fun loadDiagnosesForPatient(patientId: Long) {
        state = state.copy(currentPatientIdForContext = patientId)
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val diagnoses = fetchAndMapDiagnoses(patientId)
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
                val response = withContext(ioDispatcher) { diagnosisApiService.getAllDiagnoses() }
                if (response.isSuccessful) {
                    val diagnoses = response.body()?.map { it.toDiagnosisDto() } ?: emptyList()
                    state = state.copy(allDiagnosesList = diagnoses, isLoading = false)
                } else {
                    state = state.copy(
                        errorMessage = "Error loading all diagnoses: ${response.code()}",
                        isLoading = false
                    )
                }
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
                val response = if (isNew) {
                    withContext(ioDispatcher) {
                        diagnosisApiService.createDiagnosis(
                            diagnosisToSave.toDiagnosisRequest()
                        )
                    }
                } else {
                    withContext(ioDispatcher) {
                        diagnosisApiService.updateDiagnosis(
                            diagnosisToSave.patientId,
                            diagnosisToSave.id,
                            diagnosisToSave.toDiagnosisRequest()
                        )
                    }
                }

                if (response.isSuccessful) {
                    refreshDiagnosesListForCurrentPatient()
                    state = state.copy(
                        isLoading = false,
                        showAddOrEditDialog = false,
                        isEditing = false,
                        draftDiagnosis = null,
                        selectedDiagnosis = response.body()?.toDiagnosisDto() ?: state.selectedDiagnosis
                    )
                } else {
                    state = state.copy(
                        errorMessage = "Error saving diagnosis: ${response.code()} ${response.message()}",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                state = state.copy(errorMessage = "Exception saving diagnosis: ${e.message}", isLoading = false)
            }
        }
    }

    fun deleteDiagnosis(patientId: Long, diagnosisId: Long) {
        if (diagnosisId == NEW_DIAGNOSIS_ID) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val response = withContext(ioDispatcher) {
                    diagnosisApiService.deleteDiagnosis(patientId, diagnosisId)
                }
                if (response.isSuccessful && response.body() == true) {
                    refreshDiagnosesListForCurrentPatient()
                    state = state.copy(
                        isLoading = false,
                        selectedDiagnosis = if (state.selectedDiagnosis?.id == diagnosisId) null else state.selectedDiagnosis,
                        draftDiagnosis = null,
                        isEditing = false
                    )
                } else {
                    state = state.copy(
                        errorMessage = "Error deleting diagnosis: ${response.code()} ${response.message()} (Success: ${response.body()})",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                state = state.copy(errorMessage = "Exception deleting diagnosis: ${e.message}", isLoading = false)
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
