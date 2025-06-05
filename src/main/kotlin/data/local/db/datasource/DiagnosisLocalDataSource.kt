package data.local.db.datasource

import data.local.db.DatabaseManager
import data.model.DiagnosisDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Local data source for Diagnosis entities.
 * This class provides methods for accessing and manipulating diagnosis data in the local database.
 */
class DiagnosisLocalDataSource {
    private val database = DatabaseManager.getDatabase()
    private val diagnosisQueries = database.diagnosisQueries
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Gets all diagnoses from the local database.
     *
     * @return List of DiagnosisDto objects
     */
    suspend fun getAllDiagnoses(): List<DiagnosisDto> = withContext(Dispatchers.IO) {
        diagnosisQueries.getAllDiagnoses().executeAsList().map { it.toDiagnosisDto() }
    }

    /**
     * Gets diagnoses for a patient from the local database.
     *
     * @param patientId The ID of the patient
     * @return List of DiagnosisDto objects
     */
    suspend fun getDiagnosesForPatient(patientId: Long): List<DiagnosisDto> = withContext(Dispatchers.IO) {
        diagnosisQueries.getDiagnosesForPatient(patientId).executeAsList().map { it.toDiagnosisDto() }
    }

    /**
     * Gets a diagnosis by ID from the local database.
     *
     * @param id The ID of the diagnosis to get
     * @return The DiagnosisDto object, or null if not found
     */
    suspend fun getDiagnosisById(id: Long): DiagnosisDto? = withContext(Dispatchers.IO) {
        diagnosisQueries.getDiagnosisById(id).executeAsOneOrNull()?.toDiagnosisDto()
    }

    /**
     * Inserts a new diagnosis into the local database.
     *
     * @param diagnosis The diagnosis to insert
     * @param syncStatus The synchronization status (default: PENDING_CREATE)
     */
    suspend fun insertDiagnosis(diagnosis: DiagnosisDto, syncStatus: String = "PENDING_CREATE") = withContext(Dispatchers.IO) {
        diagnosisQueries.insertDiagnosis(
            id = diagnosis.id,
            patientId = diagnosis.patientId,
            diagnosisCode = diagnosis.diagnosisCode,
            isPrimary = if (diagnosis.isPrimary) 1L else 0L,
            description = diagnosis.description,
            date = diagnosis.date.format(dateFormatter),
            prescribedBy = diagnosis.prescribedBy,
            syncStatus = syncStatus
        )
    }

    /**
     * Updates an existing diagnosis in the local database.
     *
     * @param diagnosis The diagnosis to update
     * @param syncStatus The synchronization status (default: PENDING_UPDATE)
     */
    suspend fun updateDiagnosis(diagnosis: DiagnosisDto, syncStatus: String = "PENDING_UPDATE") = withContext(Dispatchers.IO) {
        diagnosisQueries.updateDiagnosis(
            patientId = diagnosis.patientId,
            diagnosisCode = diagnosis.diagnosisCode,
            isPrimary = if (diagnosis.isPrimary) 1L else 0L,
            description = diagnosis.description,
            date = diagnosis.date.format(dateFormatter),
            prescribedBy = diagnosis.prescribedBy,
            syncStatus = syncStatus,
            id = diagnosis.id
        )
    }

    /**
     * Marks a diagnosis for deletion in the local database.
     *
     * @param id The ID of the diagnosis to mark for deletion
     */
    suspend fun markDiagnosisForDeletion(id: Long) = withContext(Dispatchers.IO) {
        diagnosisQueries.markDiagnosisForDeletion(id)
    }

    /**
     * Deletes a diagnosis from the local database.
     *
     * @param id The ID of the diagnosis to delete
     */
    suspend fun deleteDiagnosis(id: Long) = withContext(Dispatchers.IO) {
        diagnosisQueries.deleteDiagnosis(id)
    }

    /**
     * Gets all diagnoses that need to be synchronized with the server.
     *
     * @return List of DiagnosisDto objects that need to be synchronized
     */
    suspend fun getDiagnosesToSync(): List<DiagnosisDto> = withContext(Dispatchers.IO) {
        println("[DEBUG] DiagnosisLocalDataSource.getDiagnosesToSync() called")
        val diagnoses = diagnosisQueries.getDiagnosesToSync().executeAsList().map { it.toDiagnosisDto() }
        println("[DEBUG] DiagnosisLocalDataSource.getDiagnosesToSync() found ${diagnoses.size} diagnoses to sync")
        diagnoses.forEach { diagnosis ->
            println("[DEBUG] Diagnosis to sync: id=${diagnosis.id}, patientId=${diagnosis.patientId}, code=${diagnosis.diagnosisCode}, syncStatus=${diagnosis.syncStatus}")
        }
        diagnoses
    }

    /**
     * Updates the synchronization status of a diagnosis.
     *
     * @param id The ID of the diagnosis
     * @param syncStatus The new synchronization status
     */
    suspend fun updateSyncStatus(id: Long, syncStatus: String) = withContext(Dispatchers.IO) {
        diagnosisQueries.updateSyncStatus(syncStatus, id)
    }

    /**
     * Deletes all unsynchronized diagnoses from the local database.
     * This includes diagnoses with PENDING_CREATE, PENDING_UPDATE, or PENDING_DELETE status.
     *
     * @return The number of diagnoses deleted
     */
    suspend fun deleteUnsynchronizedDiagnoses(): Int = withContext(Dispatchers.IO) {
        val diagnosesToDelete = getDiagnosesToSync()
        diagnosisQueries.deleteUnsynchronizedDiagnoses()
        return@withContext diagnosesToDelete.size
    }

    /**
     * Converts a Diagnosis database entity to a DiagnosisDto.
     */
    private fun data.local.db.Diagnosis.toDiagnosisDto(): DiagnosisDto {
        return DiagnosisDto(
            id = id,
            patientId = patientId,
            diagnosisCode = diagnosisCode,
            isPrimary = isPrimary == 1L,
            description = description,
            date = LocalDate.parse(date, dateFormatter),
            prescribedBy = prescribedBy,
            syncStatus = syncStatus
        )
    }
}
