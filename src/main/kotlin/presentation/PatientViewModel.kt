package presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.PatientApiService
import data.model.PatientDto
import data.model.PatientRequest
import data.model.PatientResponse
import kotlinx.coroutines.*
import java.io.IOException

data class PatientUiState(
    val patientsList: List<PatientDto> = emptyList(),
    val selectedPatient: PatientDto? = null,
    val isEditing: Boolean = false, // Относится к редактированию в PatientDetailPane
    val draftPatient: PatientDto? = null, // Черновик для PatientDetailPane или для нового пациента при сохранении из диалога
    val showAddPatientDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PatientViewModel(
    private val patientApiService: PatientApiService,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    var uiState by mutableStateOf(PatientUiState())
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
            return response.body()?.flatten()?.map { it.toPatientDto() } ?: emptyList()
        } else {
            throw IOException("Error loading patients: ${response.code()} ${response.message()}")
        }
    }

    fun loadPatients() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            try {
                val newPatientsList = fetchAndMapPatients()
                uiState = uiState.copy(patientsList = newPatientsList, isLoading = false)
            } catch (e: IOException) {
                uiState = uiState.copy(errorMessage = e.message, isLoading = false)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    errorMessage = "Unexpected exception while loading patients: ${e.message}",
                    isLoading = false
                )
                e.printStackTrace()
            }
        }
    }

    fun selectPatient(patientId: Long) {
        // Если мы находимся в режиме редактирования в PatientDetailPane, не даем выбрать другого пациента,
        // чтобы избежать потери несохраненных изменений в draftPatient.
        // Пользователь должен сначала сохранить или отменить изменения.
        if (uiState.isEditing) return

        val patientFromList = uiState.patientsList.find { it.id == patientId }
        if (patientFromList != null) {
            uiState = uiState.copy(
                selectedPatient = patientFromList,
                draftPatient = null
            ) // Сбрасываем draftPatient при выборе
        }
        // Загрузка деталей для выбранного пациента, если это необходимо,
        // или можно считать, что в списке уже достаточно данных.
        // loadPatientDetails(patientId) // Раскомментируйте, если детальная загрузка все еще нужна
    }

    private fun loadPatientDetails(patientId: Long) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            try {
                val response = withContext(ioDispatcher) {
                    patientApiService.getPatientById(patientId)
                }
                if (response.isSuccessful) {
                    val loadedPatient = response.body()?.toPatientDto()
                    uiState = uiState.copy(selectedPatient = loadedPatient, isLoading = false)
                } else {
                    uiState = uiState.copy(
                        errorMessage = "Error loading patient details: ${response.code()} ${response.message()}",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    errorMessage = "Exception while loading patient details: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun startEditing() {
        val currentSelected = uiState.selectedPatient ?: return
        // Если уже в режиме редактирования, ничего не делаем
        if (uiState.isEditing) return

        uiState = uiState.copy(
            isEditing = true,
            draftPatient = currentSelected.copy() // Создаем копию для редактирования
        )
    }

    fun saveChanges() {
        // draftPatient может быть как для редактируемого существующего пациента,
        // так и для нового пациента, добавленного через диалог.
        val patientToSave = uiState.draftPatient ?: return

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            try {
                val response =
                    if (patientToSave.id == NEW_PATIENT_ID) { // Создание нового пациента
                        withContext(ioDispatcher) {
                            patientApiService.createPatient(patientToSave.toPatientRequest())
                        }
                    } else { // Обновление существующего
                        withContext(ioDispatcher) {
                            patientApiService.updatePatient(patientToSave.id, patientToSave.toPatientRequest())
                        }
                    }

                if (response.isSuccessful) {
                    val savedPatientDto = response.body()?.toPatientDto()
                    // Обновляем список пациентов в любом случае
                    val newPatientsList = fetchAndMapPatients()

                    var finalSelectedPatient = uiState.selectedPatient
                    // Если мы сохраняли нового пациента или редактировали текущего выбранного,
                    // то обновляем selectedPatient на сохраненного.
                    if (savedPatientDto != null) {
                        if (patientToSave.id == NEW_PATIENT_ID || uiState.selectedPatient?.id == patientToSave.id) {
                            finalSelectedPatient = savedPatientDto
                        }
                    }

                    uiState = uiState.copy(
                        patientsList = newPatientsList,
                        selectedPatient = finalSelectedPatient,
                        isEditing = false, // Выходим из режима редактирования
                        draftPatient = null, // Очищаем черновик
                        showAddPatientDialog = false, // Закрываем диалог, если он был открыт для добавления
                        isLoading = false,
                        errorMessage = null
                    )
                } else {
                    uiState = uiState.copy(
                        errorMessage = "Error saving: ${response.code()} ${response.message()}",
                        isLoading = false
                    )
                }
            } catch (e: IOException) {
                uiState = uiState.copy(
                    errorMessage = "Error while saving or updating list: ${e.message}",
                    isLoading = false
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    errorMessage = "Exception while saving: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun cancelEditing() {
        // Эта функция вызывается для отмены редактирования в PatientDetailPane.
        // Она не должна напрямую влиять на showAddPatientDialog.
        // Если draftPatient был для нового пациента (что не должно случиться, если логика разделена),
        // то это состояние должно было бы управляться отменой из диалога.
        if (uiState.draftPatient?.id == NEW_PATIENT_ID) {
            // Этот сценарий (отмена НОВОГО пациента через PatientDetailPane) не должен происходить
            // при правильном разделении. Если все же произошел, то мы также должны скрыть диалог.
            uiState = uiState.copy(
                isEditing = false,
                draftPatient = null,
                showAddPatientDialog = false // Закрываем диалог, так как отменяется создание "нового"
            )
        } else {
            // Отмена редактирования существующего пациента
            uiState = uiState.copy(
                isEditing = false,
                draftPatient = null
                // selectedPatient остается прежним
            )
        }
    }


    fun updateDraftPatient(updater: (PatientDto) -> PatientDto) {
        // Эта функция вызывается, когда поля редактируются в PatientDetailPane.
        uiState.draftPatient?.let {
            uiState = uiState.copy(draftPatient = updater(it))
        }
    }

    fun openAddPatientDialog() {
        // Просто устанавливаем флаг для показа диалога.
        // Не меняем isEditing или draftPatient, так как они относятся к PatientDetailPane.
        // AddPatientDialog будет иметь свои собственные состояния для полей ввода.
        uiState = uiState.copy(showAddPatientDialog = true)
    }

    fun closeAddPatientDialog() {
        // Вызывается при отмене/закрытии AddPatientDialog
        uiState = uiState.copy(
            showAddPatientDialog = false,
        )
    }

    // Этот метод вызывается из AddPatientDialog при нажатии "Add"
    fun addPatient(patientDataFromDialog: PatientDto) {
        // Устанавливаем draftPatient для нового пациента и вызываем saveChanges
        uiState = uiState.copy(draftPatient = patientDataFromDialog.copy(id = NEW_PATIENT_ID))
        saveChanges() // saveChanges обработает создание, обновит список и закроет диалог
    }

    fun deletePatient(patientId: Long) {
        if (patientId == NEW_PATIENT_ID) return // Нельзя удалить еще не созданного пациента

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            try {
                val response = withContext(ioDispatcher) {
                    patientApiService.deletePatient(patientId)
                }
                if (response.isSuccessful) {
                    val newPatientsList = fetchAndMapPatients()
                    var newSelectedPatient = uiState.selectedPatient
                    // Если удалили выбранного пациента, сбрасываем выбор и выходим из режима редактирования
                    if (uiState.selectedPatient?.id == patientId) {
                        newSelectedPatient = null
                        uiState = uiState.copy(isEditing = false, draftPatient = null)
                    }
                    uiState = uiState.copy(
                        patientsList = newPatientsList,
                        selectedPatient = newSelectedPatient,
                        isLoading = false,
                        errorMessage = null
                    )
                } else {
                    uiState = uiState.copy(
                        errorMessage = "Error deleting: ${response.code()} ${response.message()}",
                        isLoading = false
                    )
                }
            } catch (e: IOException) {
                uiState = uiState.copy(
                    errorMessage = "Error while deleting or updating list: ${e.message}",
                    isLoading = false
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    errorMessage = "Exception while deleting: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}