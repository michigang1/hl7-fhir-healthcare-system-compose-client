package data.repository

import data.model.MedicationDto
import data.model.MedicationRequest
import data.model.MedicationResponse

/**
 * Repository interface for Medication entities.
 * This interface defines methods for accessing and manipulating medication data,
 * abstracting the data source (local database or remote API).
 */
interface MedicationRepository {
    /**
     * Gets all medications.
     *
     * @return List of MedicationDto objects
     */
    suspend fun getAllMedications(): List<MedicationDto>

    /**
     * Gets medications for a patient.
     *
     * @param patientId The ID of the patient
     * @return List of MedicationDto objects
     */
    suspend fun getMedicationsForPatient(patientId: Long): List<MedicationDto>

    /**
     * Gets a medication by ID.
     *
     * @param patientId The ID of the patient
     * @param id The ID of the medication to get
     * @return The MedicationDto object, or null if not found
     */
    suspend fun getMedicationById(patientId: Long, id: Long): MedicationDto?

    /**
     * Creates a new medication.
     *
     * @param medicationRequest The medication data to create
     * @return The created MedicationDto object
     */
    suspend fun createMedication(medicationRequest: MedicationRequest): MedicationDto

    /**
     * Updates an existing medication.
     *
     * @param patientId The ID of the patient
     * @param id The ID of the medication to update
     * @param medicationRequest The updated medication data
     * @return The updated MedicationDto object
     */
    suspend fun updateMedication(patientId: Long, id: Long, medicationRequest: MedicationRequest): MedicationDto

    /**
     * Deletes a medication.
     *
     * @param patientId The ID of the patient
     * @param id The ID of the medication to delete
     * @return True if the medication was deleted, false otherwise
     */
    suspend fun deleteMedication(patientId: Long, id: Long): Boolean

    /**
     * Gets medications that need to be synchronized with the server.
     *
     * @return List of MedicationDto objects that need to be synchronized
     */
    suspend fun getMedicationsToSync(): List<MedicationDto>

    /**
     * Synchronizes local data with the remote server.
     * This method sends pending changes to the server and fetches the latest data.
     *
     * @return True if synchronization was successful, false otherwise
     */
    suspend fun synchronize(): Boolean

    /**
     * Converts a MedicationResponse to a MedicationDto.
     *
     * @param response The MedicationResponse to convert
     * @return The converted MedicationDto
     */
    fun mapResponseToDto(response: MedicationResponse): MedicationDto

    /**
     * Converts a MedicationDto to a MedicationRequest.
     *
     * @param dto The MedicationDto to convert
     * @return The converted MedicationRequest
     */
    fun mapDtoToRequest(dto: MedicationDto): MedicationRequest

    /**
     * Deletes all unsynchronized medications from the local database.
     * This includes medications with PENDING_CREATE, PENDING_UPDATE, or PENDING_DELETE status.
     *
     * @return The number of medications deleted
     */
    suspend fun deleteUnsynchronizedMedications(): Int
}
