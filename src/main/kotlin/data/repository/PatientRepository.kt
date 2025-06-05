package data.repository

import data.model.PatientDto
import data.model.PatientRequest
import data.model.PatientResponse

/**
 * Repository interface for Patient entities.
 * This interface defines methods for accessing and manipulating patient data,
 * abstracting the data source (local database or remote API).
 */
interface PatientRepository {
    /**
     * Gets all patients.
     *
     * @return List of PatientDto objects
     */
    suspend fun getAllPatients(): List<PatientDto>

    /**
     * Gets a patient by ID.
     *
     * @param id The ID of the patient to get
     * @return The PatientDto object, or null if not found
     */
    suspend fun getPatientById(id: Long): PatientDto?

    /**
     * Creates a new patient.
     *
     * @param patientRequest The patient data to create
     * @return The created PatientDto object
     */
    suspend fun createPatient(patientRequest: PatientRequest): PatientDto

    /**
     * Updates an existing patient.
     *
     * @param id The ID of the patient to update
     * @param patientRequest The updated patient data
     * @return The updated PatientDto object
     */
    suspend fun updatePatient(id: Long, patientRequest: PatientRequest): PatientDto

    /**
     * Deletes a patient.
     *
     * @param id The ID of the patient to delete
     * @return True if the patient was deleted, false otherwise
     */
    suspend fun deletePatient(id: Long): Boolean

    /**
     * Gets patients that need to be synchronized with the server.
     *
     * @return List of PatientDto objects that need to be synchronized
     */
    suspend fun getPatientsToSync(): List<PatientDto>

    /**
     * Synchronizes local data with the remote server.
     * This method sends pending changes to the server and fetches the latest data.
     *
     * @return True if synchronization was successful, false otherwise
     */
    suspend fun synchronize(): Boolean

    /**
     * Converts a PatientResponse to a PatientDto.
     *
     * @param response The PatientResponse to convert
     * @return The converted PatientDto
     */
    fun mapResponseToDto(response: PatientResponse): PatientDto

    /**
     * Converts a PatientDto to a PatientRequest.
     *
     * @param dto The PatientDto to convert
     * @return The converted PatientRequest
     */
    fun mapDtoToRequest(dto: PatientDto): PatientRequest
}
