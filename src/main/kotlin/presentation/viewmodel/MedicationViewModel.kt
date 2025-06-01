package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.MedicationDto
import data.model.MedicationRequest
import data.model.MedicationResponse
import data.remote.services.MedicationApiService
import kotlinx.coroutines.*
import presentation.state.MedicationState
import java.io.IOException
import java.time.LocalDate

/**
 * ViewModel for the medication screen.
 */
class MedicationViewModel(
    private val medicationApiService: MedicationApiService,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    var state by mutableStateOf(MedicationState())
        private set

    private val viewModelScope = CoroutineScope(mainDispatcher + SupervisorJob())

    companion object {
        const val NEW_MEDICATION_ID = 0L
    }

    private fun MedicationResponse.toMedicationDto(): MedicationDto = MedicationDto(
        id = id,
        patientId = patientId,
        medicationName = medicationName,
        dosage = dosage,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate!!,
        prescribedBy = prescribedBy
    )

    private fun MedicationDto.toMedicationRequest(): MedicationRequest = MedicationRequest(
        patientId = patientId,
        medicationName = medicationName,
        dosage = dosage,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        prescribedBy = prescribedBy
    )

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
        val response = withContext(ioDispatcher) {
            medicationApiService.getAllByPatient(patientId)
        }

        // Log the result
        println("=== fetchAndMapMedications ===")
        println("HTTP status: ${response.code()} (${response.message()})")
        println("isSuccessful: ${response.isSuccessful}")

        // Print raw body (as string)
        try {
            val rawJson = response.raw().peekBody(Long.MAX_VALUE).string()
            println("Raw JSON: $rawJson")
        } catch (e: Exception) {
            println("Error reading raw body: ${e.message}")
        }

        if (response.isSuccessful) {
            val body = response.body()
            println("Parsed body: $body")
            body?.forEach { println(it) }
            return body?.map { it.toMedicationDto() } ?: emptyList()
        } else {
            val errorBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
            println("Error body: $errorBody")
            throw IOException("Error loading medications: ${response.code()} ${response.message()}")
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
                val response = if (isNew) {
                    withContext(ioDispatcher) {
                        medicationApiService.createMedication(medicationToSave.toMedicationRequest())
                    }
                } else {
                    withContext(ioDispatcher) {
                        medicationApiService.updateMedication(
                            medicationToSave.patientId,
                            medicationToSave.id,
                            medicationToSave.toMedicationRequest()
                        )
                    }
                }

                if (response.isSuccessful) {
                    loadMedicationsForPatient(patientId)
                    state = state.copy(
                        isLoading = false,
                        showAddOrEditDialog = false,
                        isEditing = false,
                        draftMedication = null,
                        selectedMedication = response.body()?.toMedicationDto() ?: state.selectedMedication
                    )
                } else {
                    state = state.copy(
                        errorMessage = "Error saving medication: ${response.code()} ${response.message()}",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                state = state.copy(errorMessage = "Exception saving medication: ${e.message}", isLoading = false)
            }
        }
    }

    fun deleteMedication(patientId: Long, medicationId: Long) {
        if (medicationId == NEW_MEDICATION_ID) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val response = withContext(ioDispatcher) {
                    medicationApiService.deleteMedication(patientId, medicationId)
                }
                if (response.isSuccessful && response.body() == true) {
                    loadMedicationsForPatient(patientId)
                    state = state.copy(
                        isLoading = false,
                        selectedMedication = if (state.selectedMedication?.id == medicationId) null else state.selectedMedication,
                        draftMedication = null,
                        isEditing = false
                    )
                } else {
                    state = state.copy(
                        errorMessage = "Error deleting medication: ${response.code()} ${response.message()} (Success: ${response.body()})",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                state = state.copy(errorMessage = "Exception deleting medication: ${e.message}", isLoading = false)
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