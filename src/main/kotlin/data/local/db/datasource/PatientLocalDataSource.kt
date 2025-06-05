package data.local.db.datasource

import data.local.db.DatabaseManager
import data.model.PatientDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Local data source for Patient entities.
 * This class provides methods for accessing and manipulating patient data in the local database.
 */
class PatientLocalDataSource {
    private val database = DatabaseManager.getDatabase()
    private val patientQueries = database.patientQueries
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Gets all patients from the local database.
     *
     * @return List of PatientDto objects
     */
    suspend fun getAllPatients(): List<PatientDto> = withContext(Dispatchers.IO) {
        patientQueries.getAllPatients().executeAsList().map { it.toPatientDto() }
    }

    /**
     * Gets a patient by ID from the local database.
     *
     * @param id The ID of the patient to get
     * @return The PatientDto object, or null if not found
     */
    suspend fun getPatientById(id: Long): PatientDto? = withContext(Dispatchers.IO) {
        patientQueries.getPatientById(id).executeAsOneOrNull()?.toPatientDto()
    }

    /**
     * Inserts a new patient into the local database.
     *
     * @param patient The patient to insert
     * @param syncStatus The synchronization status (default: PENDING_CREATE)
     */
    suspend fun insertPatient(patient: PatientDto, syncStatus: String = "PENDING_CREATE") = withContext(Dispatchers.IO) {
        patientQueries.insertPatient(
            id = patient.id,
            name = patient.name,
            surname = patient.surname,
            roomNo = patient.roomNo,
            dateOfBirth = patient.dateOfBirth,
            gender = patient.gender,
            address = patient.address,
            email = patient.email,
            phone = patient.phone,
            identifier = patient.identifier,
            organizationId = patient.organizationId,
            syncStatus = syncStatus
        )
    }

    /**
     * Updates an existing patient in the local database.
     *
     * @param patient The patient to update
     * @param syncStatus The synchronization status (default: PENDING_UPDATE)
     */
    suspend fun updatePatient(patient: PatientDto, syncStatus: String = "PENDING_UPDATE") = withContext(Dispatchers.IO) {
        patientQueries.updatePatient(
            name = patient.name,
            surname = patient.surname,
            roomNo = patient.roomNo,
            dateOfBirth = patient.dateOfBirth,
            gender = patient.gender,
            address = patient.address,
            email = patient.email,
            phone = patient.phone,
            identifier = patient.identifier,
            organizationId = patient.organizationId,
            syncStatus = syncStatus,
            id = patient.id
        )
    }

    /**
     * Marks a patient for deletion in the local database.
     *
     * @param id The ID of the patient to mark for deletion
     */
    suspend fun markPatientForDeletion(id: Long) = withContext(Dispatchers.IO) {
        patientQueries.markPatientForDeletion(id)
    }

    /**
     * Deletes a patient from the local database.
     *
     * @param id The ID of the patient to delete
     */
    suspend fun deletePatient(id: Long) = withContext(Dispatchers.IO) {
        patientQueries.deletePatient(id)
    }

    /**
     * Gets all patients that need to be synchronized with the server.
     *
     * @return List of PatientDto objects that need to be synchronized
     */
    suspend fun getPatientsToSync(): List<PatientDto> = withContext(Dispatchers.IO) {
        patientQueries.getPatientsToSync().executeAsList().map { it.toPatientDto() }
    }

    /**
     * Updates the synchronization status of a patient.
     *
     * @param id The ID of the patient
     * @param syncStatus The new synchronization status
     */
    suspend fun updateSyncStatus(id: Long, syncStatus: String) = withContext(Dispatchers.IO) {
        patientQueries.updateSyncStatus(syncStatus, id)
    }

    /**
     * Converts a Patient database entity to a PatientDto.
     */
    private fun data.local.db.Patient.toPatientDto(): PatientDto {
        return PatientDto(
            id = id,
            name = name,
            surname = surname,
            roomNo = roomNo,
            dateOfBirth = dateOfBirth,
            gender = gender,
            address = address,
            email = email,
            phone = phone,
            identifier = identifier,
            organizationId = organizationId,
            syncStatus = syncStatus
        )
    }
}
