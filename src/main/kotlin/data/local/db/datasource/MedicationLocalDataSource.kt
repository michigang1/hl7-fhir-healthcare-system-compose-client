package data.local.db.datasource

import data.local.db.DatabaseManager
import data.model.MedicationDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Local data source for Medication entities.
 * This class provides methods for accessing and manipulating medication data in the local database.
 */
class MedicationLocalDataSource {
    private val database = DatabaseManager.getDatabase()
    private val medicationQueries = database.medicationQueries
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Gets all medications from the local database.
     *
     * @return List of MedicationDto objects
     */
    suspend fun getAllMedications(): List<MedicationDto> = withContext(Dispatchers.IO) {
        medicationQueries.getAllMedications().executeAsList().map { it.toMedicationDto() }
    }

    /**
     * Gets medications for a patient from the local database.
     *
     * @param patientId The ID of the patient
     * @return List of MedicationDto objects
     */
    suspend fun getMedicationsForPatient(patientId: Long): List<MedicationDto> = withContext(Dispatchers.IO) {
        medicationQueries.getMedicationsForPatient(patientId).executeAsList().map { it.toMedicationDto() }
    }

    /**
     * Gets a medication by ID from the local database.
     *
     * @param id The ID of the medication to get
     * @return The MedicationDto object, or null if not found
     */
    suspend fun getMedicationById(id: Long): MedicationDto? = withContext(Dispatchers.IO) {
        medicationQueries.getMedicationById(id).executeAsOneOrNull()?.toMedicationDto()
    }

    /**
     * Inserts a new medication into the local database.
     *
     * @param medication The medication to insert
     * @param syncStatus The synchronization status (default: PENDING_CREATE)
     */
    suspend fun insertMedication(medication: MedicationDto, syncStatus: String = "PENDING_CREATE") = withContext(Dispatchers.IO) {
        medicationQueries.insertMedication(
            id = medication.id,
            patientId = medication.patientId,
            medicationName = medication.medicationName,
            dosage = medication.dosage,
            frequency = medication.frequency,
            startDate = medication.startDate.format(dateFormatter),
            endDate = medication.endDate.format(dateFormatter),
            prescribedBy = medication.prescribedBy,
            syncStatus = syncStatus
        )
    }

    /**
     * Updates an existing medication in the local database.
     *
     * @param medication The medication to update
     * @param syncStatus The synchronization status (default: PENDING_UPDATE)
     */
    suspend fun updateMedication(medication: MedicationDto, syncStatus: String = "PENDING_UPDATE") = withContext(Dispatchers.IO) {
        medicationQueries.updateMedication(
            patientId = medication.patientId,
            medicationName = medication.medicationName,
            dosage = medication.dosage,
            frequency = medication.frequency,
            startDate = medication.startDate.format(dateFormatter),
            endDate = medication.endDate.format(dateFormatter),
            prescribedBy = medication.prescribedBy,
            syncStatus = syncStatus,
            id = medication.id
        )
    }

    /**
     * Marks a medication for deletion in the local database.
     *
     * @param id The ID of the medication to mark for deletion
     */
    suspend fun markMedicationForDeletion(id: Long) = withContext(Dispatchers.IO) {
        medicationQueries.markMedicationForDeletion(id)
    }

    /**
     * Deletes a medication from the local database.
     *
     * @param id The ID of the medication to delete
     */
    suspend fun deleteMedication(id: Long) = withContext(Dispatchers.IO) {
        medicationQueries.deleteMedication(id)
    }

    /**
     * Gets all medications that need to be synchronized with the server.
     *
     * @return List of MedicationDto objects that need to be synchronized
     */
    suspend fun getMedicationsToSync(): List<MedicationDto> = withContext(Dispatchers.IO) {
        println("[DEBUG] MedicationLocalDataSource.getMedicationsToSync() called")
        val medications = medicationQueries.getMedicationsToSync().executeAsList().map { it.toMedicationDto() }
        println("[DEBUG] MedicationLocalDataSource.getMedicationsToSync() found ${medications.size} medications to sync")
        medications.forEach { medication ->
            println("[DEBUG] Medication to sync: id=${medication.id}, patientId=${medication.patientId}, name=${medication.medicationName}, syncStatus=${medication.syncStatus}")
        }
        medications
    }

    /**
     * Updates the synchronization status of a medication.
     *
     * @param id The ID of the medication
     * @param syncStatus The new synchronization status
     */
    suspend fun updateSyncStatus(id: Long, syncStatus: String) = withContext(Dispatchers.IO) {
        medicationQueries.updateSyncStatus(syncStatus, id)
    }

    /**
     * Deletes all unsynchronized medications from the local database.
     * This includes medications with PENDING_CREATE, PENDING_UPDATE, or PENDING_DELETE status.
     *
     * @return The number of medications deleted
     */
    suspend fun deleteUnsynchronizedMedications(): Int = withContext(Dispatchers.IO) {
        val medicationsToDelete = getMedicationsToSync()
        medicationQueries.deleteUnsynchronizedMedications()
        return@withContext medicationsToDelete.size
    }

    /**
     * Converts a Medication database entity to a MedicationDto.
     */
    private fun data.local.db.Medication.toMedicationDto(): MedicationDto {
        return MedicationDto(
            id = id,
            patientId = patientId,
            medicationName = medicationName,
            dosage = dosage,
            frequency = frequency,
            startDate = LocalDate.parse(startDate, dateFormatter),
            endDate = LocalDate.parse(endDate, dateFormatter),
            prescribedBy = prescribedBy,
            syncStatus = syncStatus
        )
    }
}
