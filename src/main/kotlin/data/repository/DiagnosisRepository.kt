package data.repository

import data.model.DiagnosisDto
import data.model.DiagnosisRequest
import data.model.DiagnosisResponse

/**
 * Repository interface for Diagnosis entities.
 * This interface defines methods for accessing and manipulating diagnosis data,
 * abstracting the data source (local database or remote API).
 */
interface DiagnosisRepository {
    /**
     * Gets all diagnoses.
     *
     * @return List of DiagnosisDto objects
     */
    suspend fun getAllDiagnoses(): List<DiagnosisDto>

    /**
     * Gets diagnoses for a patient.
     *
     * @param patientId The ID of the patient
     * @return List of DiagnosisDto objects
     */
    suspend fun getDiagnosesForPatient(patientId: Long): List<DiagnosisDto>

    /**
     * Gets a diagnosis by ID.
     *
     * @param patientId The ID of the patient
     * @param id The ID of the diagnosis to get
     * @return The DiagnosisDto object, or null if not found
     */
    suspend fun getDiagnosisById(patientId: Long, id: Long): DiagnosisDto?

    /**
     * Creates a new diagnosis.
     *
     * @param diagnosisRequest The diagnosis data to create
     * @return The created DiagnosisDto object
     */
    suspend fun createDiagnosis(diagnosisRequest: DiagnosisRequest): DiagnosisDto

    /**
     * Updates an existing diagnosis.
     *
     * @param patientId The ID of the patient
     * @param id The ID of the diagnosis to update
     * @param diagnosisRequest The updated diagnosis data
     * @return The updated DiagnosisDto object
     */
    suspend fun updateDiagnosis(patientId: Long, id: Long, diagnosisRequest: DiagnosisRequest): DiagnosisDto

    /**
     * Deletes a diagnosis.
     *
     * @param patientId The ID of the patient
     * @param id The ID of the diagnosis to delete
     * @return True if the diagnosis was deleted, false otherwise
     */
    suspend fun deleteDiagnosis(patientId: Long, id: Long): Boolean

    /**
     * Gets diagnoses that need to be synchronized with the server.
     *
     * @return List of DiagnosisDto objects that need to be synchronized
     */
    suspend fun getDiagnosesToSync(): List<DiagnosisDto>

    /**
     * Synchronizes local data with the remote server.
     * This method sends pending changes to the server and fetches the latest data.
     *
     * @return True if synchronization was successful, false otherwise
     */
    suspend fun synchronize(): Boolean

    /**
     * Converts a DiagnosisResponse to a DiagnosisDto.
     *
     * @param response The DiagnosisResponse to convert
     * @return The converted DiagnosisDto
     */
    fun mapResponseToDto(response: DiagnosisResponse): DiagnosisDto

    /**
     * Converts a DiagnosisDto to a DiagnosisRequest.
     *
     * @param dto The DiagnosisDto to convert
     * @return The converted DiagnosisRequest
     */
    fun mapDtoToRequest(dto: DiagnosisDto): DiagnosisRequest

    /**
     * Deletes all unsynchronized diagnoses from the local database.
     * This includes diagnoses with PENDING_CREATE, PENDING_UPDATE, or PENDING_DELETE status.
     *
     * @return The number of diagnoses deleted
     */
    suspend fun deleteUnsynchronizedDiagnoses(): Int
}
